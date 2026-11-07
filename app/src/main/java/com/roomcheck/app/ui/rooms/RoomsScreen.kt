@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.roomcheck.app.ui.rooms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.roomcheck.app.data.Assessment
import com.roomcheck.app.data.HomeRoom
import com.roomcheck.app.data.Repository
import com.roomcheck.app.domain.DefaultTemplates
import com.roomcheck.app.domain.Rating
import com.roomcheck.app.ui.components.ScoreBadge
import com.roomcheck.app.ui.components.StatusDot
import com.roomcheck.app.ui.components.formatDate
import com.roomcheck.app.ui.repoFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RoomRow(val room: HomeRoom, val latest: Assessment?)

class RoomsViewModel(private val repo: Repository) : ViewModel() {
    val rows: StateFlow<List<RoomRow>?> = combine(repo.observeRooms(), repo.observeLatestPerRoom()) { rooms, latest ->
        val byRoom = latest.associateBy { it.roomId }
        rooms.map { RoomRow(it, byRoom[it.id]) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun addRoom(name: String, type: String) = viewModelScope.launch { repo.addRoom(name, type) }
}

@Composable
fun RoomsScreen(
    onOpenRoom: (Long) -> Unit,
    vm: RoomsViewModel = viewModel(factory = repoFactory { repo, _ -> RoomsViewModel(repo) }),
) {
    val rows by vm.rows.collectAsStateWithLifecycle()
    var showAdd by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("My Home") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { showAdd = true }) {
                // Content-slot overload keeps the label in the accessibility tree (screen readers, UI tests).
                Icon(Icons.Filled.Add, null)
                Spacer(Modifier.width(12.dp))
                Text("Add room")
            }
        },
    ) { padding ->
        val list = rows
        if (list != null && list.isEmpty()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("No rooms yet", style = MaterialTheme.typography.titleLarge)
                Text("Tap \"Add room\" to start your first checklist.")
            }
        } else {
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (list != null && list.any { it.latest != null }) {
                    item { HomeSummary(list) }
                }
                items(list.orEmpty(), key = { it.room.id }) { row ->
                    RoomCard(row, onClick = { onOpenRoom(row.room.id) })
                }
            }
        }
    }

    if (showAdd) {
        AddRoomDialog(
            onDismiss = { showAdd = false },
            onAdd = { name, type ->
                vm.addRoom(name, type)
                showAdd = false
            },
        )
    }
}

@Composable
private fun HomeSummary(rows: List<RoomRow>) {
    val assessed = rows.mapNotNull { it.latest }
    val avg = assessed.map { it.overallScore }.average().toInt()
    val red = assessed.count { Rating.fromScore(it.overallScore) == Rating.RED }
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Whole home", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "${assessed.size} of ${rows.size} rooms assessed" +
                        if (red > 0) " · $red need work" else "",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            ScoreBadge(avg)
        }
    }
}

@Composable
private fun RoomCard(row: RoomRow, onClick: () -> Unit) {
    val latest = row.latest
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            StatusDot(latest?.let { Rating.fromScore(it.overallScore) }, 18.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(row.room.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    latest?.let { "Last checked ${formatDate(it.timestamp)}" } ?: "Not assessed yet",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (latest != null) ScoreBadge(latest.overallScore)
        }
    }
}

@Composable
private fun AddRoomDialog(onDismiss: () -> Unit, onAdd: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(DefaultTemplates.all.first().type) }
    var nameEdited by remember { mutableStateOf(false) }
    val types = DefaultTemplates.all.map { it.type } + DefaultTemplates.custom.type

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add a room") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Starting checklist", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    types.forEach { t ->
                        FilterChip(
                            selected = t == type,
                            onClick = {
                                type = t
                                if (!nameEdited) name = if (t == "Custom") "" else t
                            },
                            label = { Text(t) },
                        )
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; nameEdited = true },
                    label = { Text("Room name (e.g. Upstairs Bathroom)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "You can edit categories and items any time.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            val finalName = name.ifBlank { if (type == "Custom") "" else type }
            TextButton(enabled = finalName.isNotBlank(), onClick = { onAdd(finalName, type) }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
