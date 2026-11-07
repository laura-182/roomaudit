@file:OptIn(ExperimentalMaterial3Api::class)

package com.roomcheck.app.ui.result

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.roomcheck.app.data.Assessment
import com.roomcheck.app.data.AssessmentRating
import com.roomcheck.app.data.HomeRoom
import com.roomcheck.app.data.Repository
import com.roomcheck.app.data.toRatedItem
import com.roomcheck.app.domain.CategorySummary
import com.roomcheck.app.domain.Priority
import com.roomcheck.app.domain.PriorityCalculator
import com.roomcheck.app.domain.Rating
import com.roomcheck.app.ui.components.ConfirmDialog
import com.roomcheck.app.ui.components.RatingCounts
import com.roomcheck.app.ui.components.ScoreBadge
import com.roomcheck.app.ui.components.SectionHeader
import com.roomcheck.app.ui.components.StatusDot
import com.roomcheck.app.ui.components.formatDateTime
import com.roomcheck.app.ui.repoFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ResultState(
    val assessment: Assessment,
    val room: HomeRoom?,
    val ratings: List<AssessmentRating>,
    val categories: List<CategorySummary>,
    val priorities: List<Priority>,
)

@OptIn(ExperimentalCoroutinesApi::class)
class ResultViewModel(private val repo: Repository, handle: SavedStateHandle) : ViewModel() {
    private val assessmentId: Long = checkNotNull(handle["assessmentId"])

    val state: StateFlow<ResultState?> = repo.observeAssessment(assessmentId)
        .filterNotNull()
        .flatMapLatest { assessment ->
            val previous = repo.previousRatings(assessment)
            combine(repo.observeRoom(assessment.roomId), repo.observeRatings(assessment.id)) { room, ratings ->
                val rated = ratings.map { it.toRatedItem() }
                ResultState(
                    assessment = assessment,
                    room = room,
                    ratings = ratings,
                    categories = PriorityCalculator.categorySummaries(rated),
                    priorities = PriorityCalculator.compute(rated, previous),
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    suspend fun addToTodo(priorities: List<Priority>): Int {
        val room = state.value?.room ?: return 0
        return repo.addPrioritiesToTodo(room, priorities)
    }

    fun delete(onDone: () -> Unit) = viewModelScope.launch {
        repo.deleteAssessment(assessmentId)
        onDone()
    }
}

private fun Priority.key() = "$category|$item"

@Composable
fun ResultScreen(
    onBack: () -> Unit,
    onOpenTodo: () -> Unit,
    vm: ResultViewModel = viewModel(factory = repoFactory { repo, h -> ResultViewModel(repo, h) }),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val selected = remember { mutableStateListOf<String>() }
    var initialized by remember { mutableStateOf(false) }
    var showAll by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Pre-select the top 3 priorities the first time results are shown.
    LaunchedEffect(state?.priorities) {
        val p = state?.priorities ?: return@LaunchedEffect
        if (!initialized) {
            selected.addAll(p.take(3).map { it.key() })
            initialized = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state?.room?.name?.let { "$it results" } ?: "Results") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
                actions = {
                    IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, "Delete assessment") }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            val s = state
            if (s != null && s.priorities.isNotEmpty()) {
                Surface(tonalElevation = 3.dp) {
                    Button(
                        enabled = selected.isNotEmpty(),
                        onClick = {
                            val chosen = s.priorities.filter { it.key() in selected }
                            scope.launch {
                                val added = vm.addToTodo(chosen)
                                selected.clear()
                                val msg = when {
                                    added == 0 -> "Already on your to-do list"
                                    added == 1 -> "Added 1 item to your to-do list"
                                    else -> "Added $added items to your to-do list"
                                }
                                val result = snackbar.showSnackbar(msg, "View", duration = SnackbarDuration.Short)
                                if (result == SnackbarResult.ActionPerformed) onOpenTodo()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                    ) {
                        Text("Add ${selected.size} to To-Do")
                    }
                }
            }
        },
    ) { padding ->
        val s = state ?: return@Scaffold
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        ) {
            item { Summary(s) }

            item { SectionHeader("Top priorities") }
            if (s.priorities.isEmpty()) {
                item { Text("Everything is green — nice work! 🎉") }
            } else {
                item {
                    Text(
                        "Ranked by rating, how much of the category needs work, and changes since last time. " +
                            "Select the ones to add to your to-do list.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                items(s.priorities, key = { "p-" + it.key() }) { p ->
                    PriorityRow(
                        priority = p,
                        checked = p.key() in selected,
                        onToggle = { if (p.key() in selected) selected.remove(p.key()) else selected.add(p.key()) },
                    )
                }
                item {
                    Row {
                        TextButton(onClick = { selected.clear(); selected.addAll(s.priorities.map { it.key() }) }) {
                            Text("Select all")
                        }
                        TextButton(onClick = { selected.clear() }) { Text("Clear") }
                    }
                }
            }

            item { SectionHeader("Categories") }
            items(s.categories, key = { "c-" + it.category }) { c ->
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    StatusDot(c.status)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(c.category)
                        RatingCounts(c.red, c.yellow, c.green)
                    }
                    ScoreBadge(c.score)
                }
            }

            item {
                TextButton(onClick = { showAll = !showAll }) {
                    Text(if (showAll) "Hide all ratings" else "Show all ${s.ratings.size} ratings")
                }
            }
            if (showAll) {
                items(s.ratings, key = { "r-" + it.id }) { r ->
                    Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        StatusDot(r.rating, 12.dp)
                        Spacer(Modifier.width(10.dp))
                        Text("${r.categoryName} · ${r.itemName}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "Delete assessment?",
            message = "This removes it from the room's history and trends.",
            onDismiss = { confirmDelete = false },
            onConfirm = {
                confirmDelete = false
                vm.delete(onBack)
            },
        )
    }
}

@Composable
private fun Summary(s: ResultState) {
    val red = s.ratings.count { it.rating == Rating.RED }
    val yellow = s.ratings.count { it.rating == Rating.YELLOW }
    val green = s.ratings.count { it.rating == Rating.GREEN }
    Card(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        Rating.fromScore(s.assessment.overallScore).label,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(formatDateTime(s.assessment.timestamp), style = MaterialTheme.typography.bodySmall)
                }
                ScoreBadge(s.assessment.overallScore)
            }
            RatingCounts(red, yellow, green)
            if (s.assessment.notes.isNotBlank()) {
                Text(s.assessment.notes, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun PriorityRow(priority: Priority, checked: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = { onToggle() })
        StatusDot(priority.rating)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(priority.item, style = MaterialTheme.typography.bodyLarge)
            Text(
                priority.category + " · " + priority.reasons.drop(1).ifEmpty { priority.reasons }.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
