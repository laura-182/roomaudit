@file:OptIn(ExperimentalMaterial3Api::class)

package com.roomcheck.app.ui.assess

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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.roomcheck.app.data.CategoryWithItems
import com.roomcheck.app.data.ChecklistItem
import com.roomcheck.app.data.Repository
import com.roomcheck.app.domain.Rating
import com.roomcheck.app.ui.repoFactory
import com.roomcheck.app.ui.theme.color
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AssessmentViewModel(private val repo: Repository, handle: SavedStateHandle) : ViewModel() {
    val roomId: Long = checkNotNull(handle["roomId"])

    /** Current in-progress ratings, keyed by checklist item id. */
    val ratings = mutableStateMapOf<Long, Rating>()
    var notes by mutableStateOf("")
    var saving by mutableStateOf(false)
        private set
    var hasPrevious by mutableStateOf(false)
        private set

    val checklist: StateFlow<List<CategoryWithItems>?> = repo.observeChecklist(roomId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val roomName: StateFlow<String> = repo.observeRoom(roomId).map { it?.name ?: "" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    init {
        viewModelScope.launch { hasPrevious = repo.latestRatingsByItem(roomId).isNotEmpty() }
    }

    fun rate(item: ChecklistItem, rating: Rating) {
        if (ratings[item.id] == rating) ratings.remove(item.id) else ratings[item.id] = rating
    }

    fun rateCategory(category: CategoryWithItems, rating: Rating) {
        category.items.forEach { ratings[it.id] = rating }
    }

    fun copyLast() = viewModelScope.launch {
        val valid = checklist.value.orEmpty().flatMap { c -> c.items.map { it.id } }.toSet()
        repo.latestRatingsByItem(roomId).filterKeys { it in valid }.forEach { (id, r) -> ratings[id] = r }
    }

    fun save(onSaved: (Long) -> Unit) {
        if (saving || ratings.isEmpty()) return
        saving = true
        viewModelScope.launch {
            val id = repo.saveAssessment(roomId, ratings.toMap(), notes)
            onSaved(id)
        }
    }
}

@Composable
fun AssessmentScreen(
    onBack: () -> Unit,
    onSaved: (Long) -> Unit,
    onEditChecklist: (Long) -> Unit,
    vm: AssessmentViewModel = viewModel(factory = repoFactory { repo, h -> AssessmentViewModel(repo, h) }),
) {
    val checklist by vm.checklist.collectAsStateWithLifecycle()
    val roomName by vm.roomName.collectAsStateWithLifecycle()
    val categories = checklist.orEmpty()
    val total = categories.sumOf { it.items.size }
    val rated = categories.sumOf { c -> c.items.count { vm.ratings.containsKey(it.id) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Assess $roomName", maxLines = 1)
                        Text("$rated of $total rated", style = MaterialTheme.typography.labelMedium)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
                actions = {
                    IconButton(onClick = { onEditChecklist(vm.roomId) }) { Icon(Icons.Filled.Edit, "Edit checklist") }
                },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    LinearProgressIndicator(
                        progress = { if (total == 0) 0f else rated.toFloat() / total },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        onClick = { vm.save(onSaved) },
                        enabled = rated > 0 && !vm.saving,
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    ) {
                        Text(if (rated < total && rated > 0) "Save ($rated of $total rated)" else "Save assessment")
                    }
                }
            }
        },
    ) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("assess-list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (vm.hasPrevious) {
                item {
                    OutlinedButton(onClick = { vm.copyLast() }, modifier = Modifier.fillMaxWidth()) {
                        Text("Start from last assessment's ratings")
                    }
                }
            }
            if (checklist != null && categories.isEmpty()) {
                item {
                    Text("This room has no checklist items yet. Tap the pencil icon to add some.")
                }
            }
            items(categories, key = { it.category.id }) { category ->
                CategoryCard(category, vm)
            }
            item {
                OutlinedTextField(
                    value = vm.notes,
                    onValueChange = { vm.notes = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                )
            }
        }
    }
}

@Composable
private fun CategoryCard(category: CategoryWithItems, vm: AssessmentViewModel) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    category.category.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                if (category.items.isNotEmpty()) {
                    TextButton(
                        onClick = { vm.rateCategory(category, Rating.GREEN) },
                        modifier = Modifier.testTag("allgood|${category.category.name}"),
                    ) { Text("All good") }
                }
            }
            if (category.items.isEmpty()) {
                Text("No items", style = MaterialTheme.typography.bodySmall)
            }
            category.sortedItems.forEach { item ->
                Column(Modifier.padding(vertical = 6.dp)) {
                    Text(item.name, style = MaterialTheme.typography.bodyLarge)
                    RatingSelector(
                        tagPrefix = "rate|${category.category.name}|${item.name}",
                        selected = vm.ratings[item.id],
                        onSelect = { vm.rate(item, it) },
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun RatingSelector(
    selected: Rating?,
    onSelect: (Rating) -> Unit,
    modifier: Modifier = Modifier,
    tagPrefix: String = "rate",
) {
    val options = listOf(Rating.RED, Rating.YELLOW, Rating.GREEN)
    SingleChoiceSegmentedButtonRow(modifier) {
        options.forEachIndexed { index, rating ->
            SegmentedButton(
                selected = selected == rating,
                onClick = { onSelect(rating) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                modifier = Modifier.testTag("$tagPrefix|${rating.name}"),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = rating.color,
                    activeContentColor = Color.White,
                ),
                icon = {},
                label = {
                    Text(
                        when (rating) {
                            Rating.RED -> "Red"
                            Rating.YELLOW -> "Yellow"
                            Rating.GREEN -> "Green"
                        },
                    )
                },
            )
        }
    }
}
