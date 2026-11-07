package com.roomcheck.app

import android.graphics.Bitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToString
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.services.storage.TestStorage
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Walks through the main flow on a real device/emulator and saves a screenshot of each screen:
 * rooms → room → assessment → results/priorities → to-do, plus a second assessment for trends.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class WalkthroughTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private val device: UiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    /**
     * Slow CI emulators sometimes show a system "<app> isn't responding" dialog (e.g. for the launcher).
     * It steals window focus from our app, so tap "Wait" to dismiss it.
     */
    private fun dismissSystemDialogs() {
        repeat(3) {
            val wait = device.findObject(By.text("Wait")) ?: return
            if (device.findObject(By.textContains("isn't responding")) == null) return
            wait.click()
            device.waitForIdle()
        }
    }

    private fun waitFor(matcher: SemanticsMatcher, timeoutMs: Long = 15_000) {
        try {
            rule.waitUntil(timeoutMs) {
                dismissSystemDialogs()
                rule.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty()
            }
        } catch (e: Throwable) {
            // Make failures easy to diagnose: save what was on screen and describe it in the error.
            runCatching { shot("zz-failure") }
            val tree = runCatching { rule.onRoot().printToString(maxDepth = 30) }.getOrElse { "<no tree: $it>" }
            throw AssertionError("Timed out waiting for ${matcher.description}. Screen was:\n$tree", e)
        }
    }

    private fun waitForText(text: String, substring: Boolean = false) = waitFor(hasText(text, substring = substring))

    private fun shot(name: String) {
        dismissSystemDialogs()
        rule.waitForIdle()
        Thread.sleep(700) // let ripples / snackbars settle so the screenshot is clean
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        TestStorage().openOutputFile("$name.png").use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun tap(tag: String) {
        rule.onNodeWithTag("assess-list").performScrollToNode(hasTestTag(tag))
        rule.onNodeWithTag(tag).performClick()
    }

    private fun rate(category: String, item: String, rating: String) = tap("rate|$category|$item|$rating")

    private fun save() {
        rule.onNode(hasText("Save", substring = true) and hasClickAction()).performClick()
        waitForText("Top priorities")
    }

    @Test
    fun walkthrough() {
        // Starter rooms are seeded on first launch.
        waitForText("Kitchen")
        waitForText("Living Room")
        shot("01-rooms")

        rule.onNodeWithText("Kitchen").performClick()
        waitForText("New assessment")
        shot("02-room-before")

        // First assessment: a mixed bag.
        rule.onNodeWithText("New assessment").performClick()
        waitFor(hasTestTag("assess-list"))
        shot("03-assessment-blank")

        tap("allgood|Clutter")
        rate("Clutter", "Countertops", "RED")
        rate("Clutter", "Kitchen table", "YELLOW")
        rate("Clutter", "Floor", "RED")
        tap("allgood|Cleanliness")
        rate("Cleanliness", "Stovetop", "RED")
        rate("Cleanliness", "Microwave", "YELLOW")
        rate("Organization", "Pantry", "YELLOW")
        rate("Organization", "Fridge contents", "GREEN")
        rate("Organization", "Junk drawer", "RED")
        rate("Organization", "Under the sink", "YELLOW")
        tap("allgood|Trash & Laundry")
        rule.onNodeWithTag("assess-list").performScrollToNode(hasTestTag("rate|Clutter|Countertops|RED"))
        shot("04-assessment-rated")

        save()
        shot("05-results-priorities")

        rule.onNode(hasText("to To-Do", substring = true) and hasClickAction()).performClick()
        waitForText("Added", substring = true)
        shot("06-added-to-todo")

        // Second assessment, starting from last time's ratings, with some improvements.
        Espresso.pressBack()
        waitForText("New assessment")
        rule.onNodeWithText("New assessment").performClick()
        waitForText("Start from last assessment's ratings")
        rule.onNodeWithText("Start from last assessment's ratings").performClick()
        waitForText("19 of 19 rated") // copied ratings are loaded before we change any
        rate("Clutter", "Countertops", "GREEN")
        rate("Clutter", "Floor", "YELLOW")
        rate("Cleanliness", "Stovetop", "GREEN")
        rate("Organization", "Junk drawer", "YELLOW")
        save()
        shot("07-results-second")

        Espresso.pressBack()
        waitForText("Overall trend")
        shot("08-room-trends")

        rule.onNodeWithContentDescription("Edit checklist").performClick()
        waitForText("Changes apply", substring = true)
        shot("09-edit-checklist")

        Espresso.pressBack()
        waitForText("New assessment")
        Espresso.pressBack()
        waitForText("Whole home")
        shot("10-rooms-after")

        rule.onNodeWithText("To-Do").performClick()
        waitForText("Kitchen (", substring = true)
        shot("11-todo")
    }
}
