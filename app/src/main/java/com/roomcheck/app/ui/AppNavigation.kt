package com.roomcheck.app.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.roomcheck.app.ui.assess.AssessmentScreen
import com.roomcheck.app.ui.edit.EditRoomScreen
import com.roomcheck.app.ui.result.ResultScreen
import com.roomcheck.app.ui.room.RoomDetailScreen
import com.roomcheck.app.ui.rooms.RoomsScreen
import com.roomcheck.app.ui.todo.TodoScreen

object Routes {
    const val ROOMS = "rooms"
    const val TODO = "todo"
    const val ROOM = "room/{roomId}"
    const val ASSESS = "assess/{roomId}"
    const val RESULT = "result/{assessmentId}"
    const val EDIT = "edit/{roomId}"

    fun room(id: Long) = "room/$id"
    fun assess(id: Long) = "assess/$id"
    fun result(id: Long) = "result/$id"
    fun edit(id: Long) = "edit/$id"
}

@Composable
fun AppNavigation() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val showBottomBar = route == Routes.ROOMS || route == Routes.TODO

    fun switchTab(dest: String) = nav.navigate(dest) {
        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = route == Routes.ROOMS,
                        onClick = { switchTab(Routes.ROOMS) },
                        icon = { Icon(Icons.Filled.Home, null) },
                        label = { Text("Rooms") },
                    )
                    NavigationBarItem(
                        selected = route == Routes.TODO,
                        onClick = { switchTab(Routes.TODO) },
                        icon = { Icon(Icons.AutoMirrored.Filled.List, null) },
                        label = { Text("To-Do") },
                    )
                }
            }
        },
    ) { outer ->
        val idArg = { name: String -> listOf(navArgument(name) { type = NavType.LongType }) }
        NavHost(
            nav,
            startDestination = Routes.ROOMS,
            modifier = Modifier
                .padding(bottom = outer.calculateBottomPadding())
                .consumeWindowInsets(PaddingValues(bottom = outer.calculateBottomPadding())),
        ) {
            composable(Routes.ROOMS) {
                RoomsScreen(onOpenRoom = { nav.navigate(Routes.room(it)) })
            }
            composable(Routes.TODO) {
                TodoScreen()
            }
            composable(Routes.ROOM, arguments = idArg("roomId")) {
                RoomDetailScreen(
                    onBack = { nav.popBackStack() },
                    onAssess = { nav.navigate(Routes.assess(it)) },
                    onEdit = { nav.navigate(Routes.edit(it)) },
                    onOpenAssessment = { nav.navigate(Routes.result(it)) },
                )
            }
            composable(Routes.ASSESS, arguments = idArg("roomId")) {
                AssessmentScreen(
                    onBack = { nav.popBackStack() },
                    onSaved = { assessmentId ->
                        nav.navigate(Routes.result(assessmentId)) {
                            popUpTo(Routes.ASSESS) { inclusive = true }
                        }
                    },
                    onEditChecklist = { nav.navigate(Routes.edit(it)) },
                )
            }
            composable(Routes.RESULT, arguments = idArg("assessmentId")) {
                ResultScreen(
                    onBack = { nav.popBackStack() },
                    onOpenTodo = { switchTab(Routes.TODO) },
                )
            }
            composable(Routes.EDIT, arguments = idArg("roomId")) {
                EditRoomScreen(
                    onBack = { nav.popBackStack() },
                    onRoomDeleted = { nav.popBackStack(Routes.ROOMS, inclusive = false) },
                )
            }
        }
    }
}
