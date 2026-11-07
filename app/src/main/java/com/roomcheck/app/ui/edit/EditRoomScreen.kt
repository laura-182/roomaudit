@file:OptIn(ExperimentalMaterial3Api::class)

package com.roomcheck.app.ui.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.roomcheck.app.data.Category
import com.roomcheck.app.data.CategoryWithItems
import com.roomcheck.app.data.ChecklistItem
import com.roomcheck.app.data.HomeRoom
import com.roomcheck.app.data.Repository
import com.roomcheck.app.ui.components.ConfirmDialog
import com.roomcheck.app.ui.components.TextInputDialog
import com.roomcheck.app.ui.repoFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EditRoomViewModel(private val repo: Repository, handle: SavedStateHandle) : ViewModel() {
    val roomId: Long = checkNotNull(handle["roomId"])

    val room: StateFlow<HomeRoom?> = repo.observeRoom(roomId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val checklist: StateFlow<List<CategoryWithItems>> = repo.observeChecklist(roomId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private fun io(block: suspend Repository.() -> Unit) = viewModelScope.launch { repo.block() }

    fun renameRoom(name: String) = room.value?.let { r -> io { renameRoom(r, name) } }
    fun deleteRoom(onDone: () -> Unit) = room.value?.let { r ->
        viewModelScope.launch { repo.deleteRoom(r); onDone() }
    }
    fun resetChecklist() = room.value?.let { r -> io { resetChecklist(r) } }

    fun addCategory(name: String) = io { addCategory(roomId, name) }
    fun renameCategory(c: Category, name: String) = io { renameCategory(c, name) }
    fun deleteCategory(c: Category) = io { deleteCategory(c) }
    fun moveCategory(c: Category, delta: Int) = io { moveCategory(roomId, c.id, delta) }

    fun addItem(c: CategoryWithItems, name: String) = io { addItem(c, name) }
    fun renameItem(i: ChecklistItem, name: String) = io { renameItem(i, name) }
    fun deleteItem(i: ChecklistItem) = io { deleteItem(i) }
    fun moveItem(c: CategoryWithItems, i: ChecklistItem, delta: Int) = io { moveItem(c, i.id, delta) }
}

/** Which text dialog is open, if any. */
private sealed interface EditDialog {
    data object RenameRoom : EditDialog
    data object AddCategory : EditDialog
    data class RenameCategory(val category: Category) : EditDialog
    data class AddItem(val category: CategoryWithItems) : EditDialog
    data class RenameItem(val item: ChecklistItem) : EditDialog
    data class DeleteCategory(val category: Category) : EditDialog
    data object DeleteRoom : EditDialog
    data object ResetChecklist : EditDialog
}

@Composable
fun EditRoomScreen(
    onBack: () -> Unit,
    onRoomDeleted: () -> Unit,
    vm: EditRoomViewModel = viewModel(factory = repoFactory { repo, h -> EditRoomViewModel(repo, h) }),
) {
    val room by vm.room.collectAsStateWithLifecycle()
    val checklist by vm.checklist.collectAsStateWithLifecycle()
    var dialog by remember { mutableStateOf<EditDialog?>(null) }
    var menuOpen by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit ${room?.name ?: ""}", maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
                actions = {
                    IconButton(onClick = { dialog = EditDialog.RenameRoom }) { Icon(Icons.Filled.Edit, "Rename room") }
                    IconButton(onClick = { menuOpen = true }) { Icon(Icons.Filled.MoreVert, "More") }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Reset to default checklist") },
                            onClick = { menuOpen = false; dialog = EditDialog.ResetChecklist },
                        )
                        DropdownMenuItem(
                            text = { Text("Delete room") },
                            onClick = { menuOpen = false; dialog = EditDialog.DeleteRoom },
                        )
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    "Changes apply to future assessments. Past assessments keep the names they were rated with.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(checklist, key = { it.category.id }) { cwi ->
                val index = checklist.indexOf(cwi)
                CategoryEditor(
                    cwi = cwi,
                    canMoveUp = index > 0,
                    canMoveDown = index < checklist.lastIndex,
                    vm = vm,
                    onRename = { dialog = EditDialog.RenameCategory(cwi.category) },
                    onDelete = { dialog = EditDialog.DeleteCategory(cwi.category) },
                    onAddItem = { dialog = EditDialog.AddItem(cwi) },
                    onRenameItem = { dialog = EditDialog.RenameItem(it) },
                )
            }
            item {
                OutlinedButton(onClick = { dialog = EditDialog.AddCategory }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Add, null)
                    Text("  Add category")
                }
            }
        }
    }

    val close = { dialog = null }
    when (val d = dialog) {
        null -> Unit
        EditDialog.RenameRoom -> TextInputDialog(
            title = "Rename room", initial = room?.name ?: "", onDismiss = close,
            onConfirm = { vm.renameRoom(it); close() },
        )
        EditDialog.AddCategory -> TextInputDialog(
            title = "Add category", label = "Category (e.g. Clutter)", confirmText = "Add", onDismiss = close,
            onConfirm = { vm.addCategory(it); close() },
        )
        is EditDialog.RenameCategory -> TextInputDialog(
            title = "Rename category", initial = d.category.name, onDismiss = close,
            onConfirm = { vm.renameCategory(d.category, it); close() },
        )
        is EditDialog.AddItem -> TextInputDialog(
            title = "Add item to ${d.category.category.name}", label = "Item (e.g. Mirror)", confirmText = "Add",
            onDismiss = close,
            onConfirm = { vm.addItem(d.category, it); close() },
        )
        is EditDialog.RenameItem -> TextInputDialog(
            title = "Rename item", initial = d.item.name, onDismiss = close,
            onConfirm = { vm.renameItem(d.item, it); close() },
        )
        is EditDialog.DeleteCategory -> ConfirmDialog(
            title = "Delete \"${d.category.name}\"?",
            message = "The category and all of its items will be removed from this room's checklist.",
            onDismiss = close,
            onConfirm = { vm.deleteCategory(d.category); close() },
        )
        EditDialog.DeleteRoom -> ConfirmDialog(
            title = "Delete ${room?.name ?: "room"}?",
            message = "This deletes the room, its checklist and all of its assessment history.",
            onDismiss = close,
            onConfirm = { close(); vm.deleteRoom(onRoomDeleted) },
        )
        EditDialog.ResetChecklist -> ConfirmDialog(
            title = "Reset checklist?",
            message = "Replaces this room's categories and items with the default ${room?.type ?: ""} list. " +
                "Past assessments are kept.",
            confirmText = "Reset",
            onDismiss = close,
            onConfirm = { vm.resetChecklist(); close() },
        )
    }
}

@Composable
private fun CategoryEditor(
    cwi: CategoryWithItems,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    vm: EditRoomViewModel,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onAddItem: () -> Unit,
    onRenameItem: (ChecklistItem) -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    cwi.category.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { vm.moveCategory(cwi.category, -1) }, enabled = canMoveUp) {
                    Icon(Icons.Filled.KeyboardArrowUp, "Move category up")
                }
                IconButton(onClick = { vm.moveCategory(cwi.category, 1) }, enabled = canMoveDown) {
                    Icon(Icons.Filled.KeyboardArrowDown, "Move category down")
                }
                IconButton(onClick = onRename) { Icon(Icons.Filled.Edit, "Rename category") }
                IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, "Delete category") }
            }
            HorizontalDivider()
            val items = cwi.sortedItems
            items.forEachIndexed { i, item ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { onRenameItem(item) }, modifier = Modifier.weight(1f)) {
                        Text(
                            item.name,
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    IconButton(onClick = { vm.moveItem(cwi, item, -1) }, enabled = i > 0) {
                        Icon(Icons.Filled.KeyboardArrowUp, "Move item up")
                    }
                    IconButton(onClick = { vm.moveItem(cwi, item, 1) }, enabled = i < items.lastIndex) {
                        Icon(Icons.Filled.KeyboardArrowDown, "Move item down")
                    }
                    IconButton(onClick = { vm.deleteItem(item) }) { Icon(Icons.Filled.Close, "Delete item") }
                }
            }
            TextButton(onClick = onAddItem) {
                Icon(Icons.Filled.Add, null)
                Text(" Add item")
            }
        }
    }
}
