@file:OptIn(ExperimentalMaterial3Api::class)

package com.roomcheck.app.ui.todo

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.roomcheck.app.data.Repository
import com.roomcheck.app.data.TodoItem
import com.roomcheck.app.ui.components.SectionHeader
import com.roomcheck.app.ui.components.StatusDot
import com.roomcheck.app.ui.components.formatDate
import com.roomcheck.app.ui.repoFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TodoViewModel(private val repo: Repository) : ViewModel() {
    val todos: StateFlow<List<TodoItem>> = repo.observeTodos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun toggle(item: TodoItem) = viewModelScope.launch { repo.setTodoDone(item, !item.done) }
    fun delete(item: TodoItem) = viewModelScope.launch { repo.deleteTodo(item) }
    fun clearCompleted() = viewModelScope.launch { repo.clearCompletedTodos() }
    fun add(title: String, room: String) = viewModelScope.launch { repo.addCustomTodo(title, room) }
}

@Composable
fun TodoScreen(vm: TodoViewModel = viewModel(factory = repoFactory { repo, _ -> TodoViewModel(repo) })) {
    val todos by vm.todos.collectAsStateWithLifecycle()
    val open = todos.filter { !it.done }
    val done = todos.filter { it.done }.sortedByDescending { it.completedAt ?: 0 }
    var showAdd by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("To-Do") },
                actions = {
                    if (done.isNotEmpty()) TextButton(onClick = { vm.clearCompleted() }) { Text("Clear done") }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) { Icon(Icons.Filled.Add, "Add to-do") }
        },
    ) { padding ->
        if (todos.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(padding).padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Nothing to do yet", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Finish a room assessment and add its top priorities here, or tap + to add your own.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Scaffold
        }
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
        ) {
            open.groupBy { it.roomName.ifBlank { "General" } }.forEach { (room, list) ->
                item(key = "h-$room") { SectionHeader("$room (${list.size})") }
                items(list, key = { it.id }) { TodoRow(it, vm) }
            }
            if (done.isNotEmpty()) {
                item(key = "h-done") { SectionHeader("Completed (${done.size})") }
                items(done, key = { it.id }) { TodoRow(it, vm) }
            }
        }
    }

    if (showAdd) AddTodoDialog(onDismiss = { showAdd = false }, onAdd = { t, r -> vm.add(t, r); showAdd = false })
}

@Composable
private fun TodoRow(item: TodoItem, vm: TodoViewModel) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = item.done, onCheckedChange = { vm.toggle(item) })
        if (item.rating != null) {
            StatusDot(item.rating, 10.dp)
            Spacer(Modifier.width(10.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                item.title,
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (item.done) TextDecoration.LineThrough else null,
                color = if (item.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            )
            val sub = listOfNotNull(
                item.categoryName.ifBlank { null },
                if (item.done) item.completedAt?.let { "done ${formatDate(it)}" } else "added ${formatDate(item.createdAt)}",
            ).joinToString(" · ")
            Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = { vm.delete(item) }) { Icon(Icons.Filled.Close, "Delete") }
    }
    HorizontalDivider()
}

@Composable
private fun AddTodoDialog(onDismiss: () -> Unit, onAdd: (String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var room by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to-do") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Task") }, singleLine = true)
                OutlinedTextField(room, { room = it }, label = { Text("Room (optional)") }, singleLine = true)
            }
        },
        confirmButton = { TextButton(enabled = title.isNotBlank(), onClick = { onAdd(title, room) }) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
