@file:OptIn(ExperimentalMaterial3Api::class)

package com.roomcheck.app.ui.room

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.roomcheck.app.data.HomeRoom
import com.roomcheck.app.data.Repository
import com.roomcheck.app.domain.Rating
import com.roomcheck.app.domain.TrendDirection
import com.roomcheck.app.domain.TrendPoint
import com.roomcheck.app.domain.Trends
import com.roomcheck.app.ui.components.ScoreBadge
import com.roomcheck.app.ui.components.SectionHeader
import com.roomcheck.app.ui.components.StatusDot
import com.roomcheck.app.ui.components.TrendChart
import com.roomcheck.app.ui.components.formatDateTime
import com.roomcheck.app.ui.repoFactory
import com.roomcheck.app.ui.theme.StatusGreen
import com.roomcheck.app.ui.theme.StatusRed
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class RoomDetailState(
    val room: HomeRoom?,
    val assessments: List<Assessment>,
    val overallTrend: List<TrendPoint>,
    val categoryTrends: Map<String, List<TrendPoint>>,
    val direction: TrendDirection,
)

class RoomDetailViewModel(repo: Repository, handle: SavedStateHandle) : ViewModel() {
    val roomId: Long = checkNotNull(handle["roomId"])

    val state: StateFlow<RoomDetailState?> = combine(
        repo.observeRoom(roomId),
        repo.observeAssessments(roomId),
        repo.observeRatingHistory(roomId),
        repo.observeChecklist(roomId),
    ) { room, assessments, history, checklist ->
        val overall = assessments.map { TrendPoint(it.timestamp, it.overallScore) }
        RoomDetailState(
            room = room,
            assessments = assessments,
            overallTrend = overall,
            categoryTrends = Trends.byCategory(
                history.map { Triple(it.timestamp, it.categoryName, it.rating) },
                order = checklist.map { it.category.name },
            ),
            direction = Trends.direction(overall),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@Composable
fun RoomDetailScreen(
    onBack: () -> Unit,
    onAssess: (Long) -> Unit,
    onEdit: (Long) -> Unit,
    onOpenAssessment: (Long) -> Unit,
    vm: RoomDetailViewModel = viewModel(factory = repoFactory { repo, h -> RoomDetailViewModel(repo, h) }),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state?.room?.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
                actions = {
                    IconButton(onClick = { onEdit(vm.roomId) }) { Icon(Icons.Filled.Edit, "Edit checklist") }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { onAssess(vm.roomId) }) {
                // Content-slot overload keeps the label in the accessibility tree (screen readers, UI tests).
                Icon(Icons.Filled.Check, null)
                Spacer(Modifier.width(12.dp))
                Text("New assessment")
            }
        },
    ) { padding ->
        val s = state ?: return@Scaffold
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
        ) {
            item { StatusCard(s) }

            if (s.assessments.size >= 2) {
                item {
                    SectionHeader("Overall trend")
                    Card(Modifier.fillMaxWidth()) {
                        TrendChart(s.overallTrend, Modifier.padding(12.dp))
                    }
                }
            }

            if (s.categoryTrends.isNotEmpty()) {
                item { SectionHeader("By category") }
                items(s.categoryTrends.entries.toList(), key = { "cat-" + it.key }) { (name, points) ->
                    CategoryTrendRow(name, points)
                }
            }

            item { SectionHeader("History") }
            if (s.assessments.isEmpty()) {
                item {
                    Text(
                        "No assessments yet. Tap \"New assessment\" to rate this room.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(s.assessments, key = { it.id }) { a ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onOpenAssessment(a.id) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StatusDot(Rating.fromScore(a.overallScore))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(formatDateTime(a.timestamp))
                        if (a.notes.isNotBlank()) {
                            Text(a.notes, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                        }
                    }
                    ScoreBadge(a.overallScore)
                }
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun StatusCard(s: RoomDetailState) {
    val latest = s.assessments.firstOrNull()
    Card(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    latest?.let { Rating.fromScore(it.overallScore).label } ?: "Not assessed yet",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                latest?.let {
                    Text("Last checked ${formatDateTime(it.timestamp)}", style = MaterialTheme.typography.bodySmall)
                }
                val (text, color) = when (s.direction) {
                    TrendDirection.IMPROVING -> "▲ Improving" to StatusGreen
                    TrendDirection.DECLINING -> "▼ Slipping" to StatusRed
                    TrendDirection.STEADY -> "● Holding steady" to MaterialTheme.colorScheme.onSurfaceVariant
                    TrendDirection.NOT_ENOUGH_DATA -> null to null
                }
                if (text != null && color != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(text, color = color, style = MaterialTheme.typography.labelLarge)
                }
            }
            ScoreBadge(latest?.overallScore)
        }
    }
}

@Composable
private fun CategoryTrendRow(name: String, points: List<TrendPoint>) {
    val latest = points.lastOrNull()
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.width(110.dp)) {
            Text(name, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
            latest?.let {
                Text(Rating.fromScore(it.score).label, style = MaterialTheme.typography.labelSmall)
            }
        }
        TrendChart(
            points.takeLast(12),
            Modifier.weight(1f).padding(horizontal = 8.dp),
            height = 36.dp,
            showDates = false,
        )
        ScoreBadge(latest?.score)
    }
}
