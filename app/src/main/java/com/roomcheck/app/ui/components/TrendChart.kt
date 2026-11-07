package com.roomcheck.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.roomcheck.app.domain.Rating
import com.roomcheck.app.domain.TrendPoint
import com.roomcheck.app.ui.theme.StatusGreen
import com.roomcheck.app.ui.theme.StatusRed
import com.roomcheck.app.ui.theme.StatusYellow
import com.roomcheck.app.ui.theme.color

/**
 * Line chart of 0-100 scores over time, drawn over faint red / yellow / green bands.
 * Points are spaced evenly (one per assessment) so frequent and infrequent checks both read clearly.
 */
@Composable
fun TrendChart(
    points: List<TrendPoint>,
    modifier: Modifier = Modifier,
    height: Dp = 160.dp,
    showBands: Boolean = true,
    showDates: Boolean = true,
) {
    val sorted = points.sortedBy { it.timestamp }
    val lineColor = MaterialTheme.colorScheme.primary
    Column(modifier) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(height)
        ) {
            if (showBands) {
                // 75-100 green, 40-75 yellow, 0-40 red (matches Rating.fromScore)
                fun yOf(score: Float) = size.height * (1f - score / 100f)
                drawRect(StatusGreen.copy(alpha = 0.10f), Offset(0f, yOf(100f)), Size(size.width, yOf(75f) - yOf(100f)))
                drawRect(StatusYellow.copy(alpha = 0.10f), Offset(0f, yOf(75f)), Size(size.width, yOf(40f) - yOf(75f)))
                drawRect(StatusRed.copy(alpha = 0.10f), Offset(0f, yOf(40f)), Size(size.width, yOf(0f) - yOf(40f)))
            }
            if (sorted.isEmpty()) return@Canvas

            val pad = 8.dp.toPx()
            val w = size.width - pad * 2
            val h = size.height - pad * 2
            val offsets = sorted.mapIndexed { i, p ->
                val x = if (sorted.size == 1) size.width / 2 else pad + w * i / (sorted.size - 1)
                val y = pad + h * (1f - p.score / 100f)
                Offset(x, y)
            }
            if (offsets.size > 1) {
                val path = Path().apply {
                    moveTo(offsets.first().x, offsets.first().y)
                    offsets.drop(1).forEach { lineTo(it.x, it.y) }
                }
                drawPath(path, lineColor, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
            }
            val r = if (height < 60.dp) 3.dp.toPx() else 5.dp.toPx()
            sorted.zip(offsets).forEach { (p, o) ->
                drawCircle(Rating.fromScore(p.score).color, radius = r, center = o)
            }
        }
        if (showDates && sorted.isNotEmpty()) {
            Row(Modifier.fillMaxWidth()) {
                Text(formatShortDate(sorted.first().timestamp), style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.weight(1f))
                if (sorted.size > 1) {
                    Text(formatShortDate(sorted.last().timestamp), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
