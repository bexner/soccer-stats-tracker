package com.bexner.soccerstats.ui.stats

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bexner.soccerstats.stats.PlayerTrend
import com.bexner.soccerstats.stats.RateMode
import com.bexner.soccerstats.stats.TrendMetric
import com.bexner.soccerstats.ui.AppViewModelProvider
import com.bexner.soccerstats.ui.components.TrendChart
import com.bexner.soccerstats.ui.components.TrendPoint
import com.bexner.soccerstats.ui.components.formatCompact
import com.bexner.soccerstats.ui.games.GameFormat

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PlayerStatsScreen(
    onBack: () -> Unit,
    viewModel: PlayerStatsViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val trend = viewModel.trend

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(trend?.playerName ?: "Player")
                        trend?.let {
                            Text(
                                buildString {
                                    it.jerseyNumber?.let { n -> append("#$n · ") }
                                    append("${it.playedLines.size} games · ${it.total.minutes} min")
                                    if (it.keptGoal) append(" · ${it.total.keeperMinutes} in goal")
                                },
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        if (viewModel.isLoading) {
            Box(
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
            return@Scaffold
        }
        if (trend == null || trend.playedLines.isEmpty()) {
            Box(
                modifier = Modifier.padding(innerPadding).fillMaxSize().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No games played yet for this player.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SeasonCard(trend)

            Text("Chart", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TrendMetric.availableFor(trend.total).forEach { m ->
                    FilterChip(
                        selected = viewModel.metric == m,
                        onClick = { viewModel.onMetricChange(m) },
                        label = { Text(m.label) }
                    )
                }
            }

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                RateMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = viewModel.rateMode == mode,
                        onClick = { viewModel.onRateModeChange(mode) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = RateMode.entries.size
                        ),
                        enabled = !(mode == RateMode.PER_60 && viewModel.metric == TrendMetric.MINUTES),
                        label = { Text(mode.label) }
                    )
                }
            }

            val series = trend.series(viewModel.metric, viewModel.rateMode)
            val overall = trend.overall(viewModel.metric, viewModel.rateMode)

            Text(
                text = buildString {
                    append(viewModel.metric.label)
                    append(" · season ")
                    append(overall?.let { formatCompact(it) } ?: "—")
                    if (viewModel.rateMode == RateMode.PER_60) {
                        append(
                            if (viewModel.metric.keeperOnly) " per 60 min in goal"
                            else " per 60 min played"
                        )
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            TrendChart(
                points = series.map { (line, value) ->
                    TrendPoint(label = shortLabel(line.game.opponent), value = value)
                },
                modifier = Modifier.fillMaxWidth().height(190.dp)
            )
            Text(
                "Bars are each game; the line is the running average. " +
                    "Gaps mean no minutes to measure against.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider()
            GameLog(trend)
            Spacer(Modifier.height(20.dp))
        }
    }
}

/** Opponent names get long; four characters is enough to tell games apart. */
private fun shortLabel(opponent: String): String =
    opponent.trim().take(4).ifBlank { "?" }

@Composable
private fun SeasonCard(trend: PlayerTrend) {
    val t = trend.total
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("Season", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(6.dp))
            val rows = buildList {
                add("Games" to trend.playedLines.size.toString())
                add("Minutes" to t.minutes.toString())
                add("Goals" to t.goals.toString())
                add("Assists" to t.assists.toString())
                add("Shots (on target)" to "${t.shots} (${t.shotsOn})")
                t.shotAccuracy?.let { add("Shot accuracy" to "${(it * 100).toInt()}%") }
                add("Tackles / 50-50s" to "${t.tackles} / ${t.fiftyFifties}")
                if (t.playedInGoal) {
                    add("Minutes in goal" to t.keeperMinutes.toString())
                    add("Saves" to t.saves.toString())
                    add("Goals conceded" to t.goalsConceded.toString())
                    t.savePercentage?.let { add("Save rate" to "${(it * 100).toInt()}%") }
                }
            }
            rows.forEach { (label, value) ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Text(
                        label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Text(value, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun GameLog(trend: PlayerTrend) {
    val keeper = trend.keptGoal
    val headers = buildList {
        addAll(listOf("Date", "Opponent", "Min", "G", "A", "Sh", "SOG", "Tkl"))
        if (keeper) addAll(listOf("GKm", "Sv", "GA"))
    }
    val widths = buildList {
        addAll(listOf(64, 96, 40, 28, 28, 30, 36, 34))
        if (keeper) addAll(listOf(40, 30, 30))
    }

    Column {
        Text("Game by game", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(6.dp))
        Column(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            Row {
                headers.forEachIndexed { i, h ->
                    Text(
                        h,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier.width(widths[i].dp).padding(vertical = 4.dp)
                    )
                }
            }
            HorizontalDivider()
            trend.playedLines.forEach { line ->
                val s = line.stats
                val cells = buildList {
                    addAll(
                        listOf(
                            GameFormat.day(line.game.kickoffAt),
                            line.game.opponent,
                            s.minutes.toString(),
                            s.goals.toString(),
                            s.assists.toString(),
                            s.shots.toString(),
                            s.shotsOn.toString(),
                            s.tackles.toString()
                        )
                    )
                    if (keeper) {
                        addAll(
                            listOf(
                                s.keeperMinutes.toString(),
                                if (s.playedInGoal) s.saves.toString() else "–",
                                if (s.playedInGoal) s.goalsConceded.toString() else "–"
                            )
                        )
                    }
                }
                Row {
                    cells.forEachIndexed { i, value ->
                        Text(
                            value,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            modifier = Modifier.width(widths[i].dp).padding(vertical = 5.dp)
                        )
                    }
                }
            }
        }
    }
}
