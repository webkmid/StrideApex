package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AeroCyan
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TelemetryMonoFontFamily
import com.example.ui.theme.TextSecondary
import java.util.regex.Pattern

data class StravaSplitItem(
    val km: Int,
    val paceStr: String,
    val paceSeconds: Long,
    val deltaFromAvgSeconds: Long = 0L
)

fun parseSplitsSummary(splitsSummary: String, avgPaceSecondsPerKm: Long = 0L): List<StravaSplitItem> {
    if (splitsSummary.isBlank()) return emptyList()

    val items = mutableListOf<StravaSplitItem>()
    // Match "KM 1: 7:28/km" or "1 7:28" or "1: 7:28"
    val pattern = Pattern.compile("(?:KM\\s*)?([1-9][0-9]?)\\s*[:.]?\\s*([0-9]{1,2}:[0-5][0-9])(?:/km)?", Pattern.CASE_INSENSITIVE)
    val matcher = pattern.matcher(splitsSummary)

    while (matcher.find()) {
        val kmNum = matcher.group(1)?.toIntOrNull() ?: continue
        val paceStr = matcher.group(2).orEmpty()
        val parts = paceStr.split(":")
        val sec = if (parts.size == 2) {
            (parts[0].toLongOrNull() ?: 0L) * 60L + (parts[1].toLongOrNull() ?: 0L)
        } else {
            0L
        }
        if (sec in 120L..900L) {
            items.add(StravaSplitItem(km = kmNum, paceStr = paceStr, paceSeconds = sec))
        }
    }

    if (items.isEmpty()) return emptyList()

    val computedAvg = if (avgPaceSecondsPerKm > 0L) {
        avgPaceSecondsPerKm
    } else {
        items.map { it.paceSeconds }.average().toLong()
    }

    return items.map { item ->
        item.copy(deltaFromAvgSeconds = item.paceSeconds - computedAvg)
    }
}

@Composable
fun StravaSplitsBarChart(
    splitsSummary: String,
    avgPaceSecondsPerKm: Long = 0L,
    modifier: Modifier = Modifier
) {
    val splits = parseSplitsSummary(splitsSummary, avgPaceSecondsPerKm)
    if (splits.isEmpty()) return

    val minSec = splits.minOf { it.paceSeconds }.toFloat().coerceAtLeast(60f)
    val maxSec = splits.maxOf { it.paceSeconds }.toFloat().coerceAtLeast(minSec + 1f)

    Surface(
        color = ObsidianBackground,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "GRAFIK SPLIT (STRAVA STYLE)",
                    style = MaterialTheme.typography.labelSmall,
                    color = AeroCyan,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "${splits.size} KM",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = TelemetryMonoFontFamily,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            splits.forEach { split ->
                // Normalize bar: faster pace (lower seconds) has fuller bar in Strava
                val spread = (maxSec - minSec).coerceAtLeast(10f)
                val normalized = 1f - ((split.paceSeconds - minSec) / spread)
                val barFraction = (0.35f + normalized * 0.65f).coerceIn(0.2f, 1f)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Split kilometer index
                    Text(
                        text = "${split.km}",
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = TelemetryMonoFontFamily,
                        color = Color.White,
                        modifier = Modifier.width(22.dp)
                    )

                    // Split pace
                    Text(
                        text = split.paceStr,
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = TelemetryMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.width(46.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Strava signature electric blue progress bar
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(14.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(barFraction)
                                .height(14.dp)
                                .background(
                                    color = Color(0xFF2979FF),
                                    shape = RoundedCornerShape(4.dp)
                                )
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Relative pace delta
                    val delta = split.deltaFromAvgSeconds
                    val deltaText = when {
                        delta == 0L -> "-0"
                        delta < 0L -> "-${kotlin.math.abs(delta)}s"
                        else -> "+${delta}s"
                    }
                    val deltaColor = when {
                        delta < 0L -> SuccessGreen
                        delta == 0L -> TextSecondary
                        else -> Color(0xFFFF8A65)
                    }

                    Text(
                        text = deltaText,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = TelemetryMonoFontFamily,
                        color = deltaColor,
                        modifier = Modifier.width(36.dp)
                    )
                }
            }
        }
    }
}
