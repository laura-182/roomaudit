package com.roomcheck.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.roomcheck.app.domain.Rating
import com.roomcheck.app.ui.theme.color
import java.text.DateFormat
import java.util.Date

@Composable
fun StatusDot(rating: Rating?, size: Dp = 14.dp, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(size)
            .background(rating?.color ?: MaterialTheme.colorScheme.outlineVariant, CircleShape)
    )
}

/** A colored pill showing a 0-100 score. */
@Composable
fun ScoreBadge(score: Int?, modifier: Modifier = Modifier) {
    val rating = score?.let { Rating.fromScore(it) }
    Box(
        modifier
            .background(rating?.color ?: MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = score?.let { "$it%" } ?: "—",
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** Small "3 red · 2 yellow · 8 green" summary. */
@Composable
fun RatingCounts(red: Int, yellow: Int, green: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        listOf(Rating.RED to red, Rating.YELLOW to yellow, Rating.GREEN to green).forEach { (r, n) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusDot(r, 10.dp)
                Text(" $n", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun TextInputDialog(
    title: String,
    initial: String = "",
    label: String = "Name",
    confirmText: String = "Save",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(label) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(enabled = text.isNotBlank(), onClick = { onConfirm(text) }) { Text(confirmText) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String = "Delete",
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmText) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Column(modifier.padding(top = 16.dp, bottom = 6.dp)) {
        Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

fun formatDate(millis: Long): String = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(millis))
fun formatDateTime(millis: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(millis))
fun formatShortDate(millis: Long): String = DateFormat.getDateInstance(DateFormat.SHORT).format(Date(millis))
