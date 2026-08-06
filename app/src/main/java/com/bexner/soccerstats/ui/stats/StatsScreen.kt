package com.bexner.soccerstats.ui.stats

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bexner.soccerstats.data.entity.EventSide
import com.bexner.soccerstats.data.entity.EventType
import com.bexner.soccerstats.data.entity.Position
import com.bexner.soccerstats.stats.PlayerStats
import com.bexner.soccerstats.stats.TeamTotals
import com.bexner.soccerstats.ui.AppViewModelProvider
import com.bexner.soccerstats.ui.components.GoalMark
import com.bexner.soccerstats.ui.components.GoalMouthView
import com.bexner.soccerstats.ui.components.PitchMarker
import com.bexner.soccerstats.ui.components.PitchView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    onBack: () -> Unit,
    viewModel: StatsViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel.message) {
        viewModel.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Stats") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.exportCurrent(context) }) {
                        Icon(Icons.Default.Share, contentDescription = "Export to Excel")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        if (viewModel.isLoading) {
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (viewModel.gameStats != null) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    StatsScope.entries.forEachIndexed { index, s ->
                        SegmentedButton(
                            selected = viewModel.scope == s,
                            onClick = { viewModel.onScopeChange(s) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = StatsScope.entries.size
                            ),
                            label = { Text(s.label) }
                        )
                    }
                }
            }

            if (viewModel.scope == StatsScope.GAME) {
                val stats = viewModel.gameStats
                if (stats == null) {
                    Text("No game selected.")
                } else {
                    Text(
                        "vs ${stats.game.opponent} · ${stats.result} " +
                            "${stats.totals.goalsFor}–${stats.totals.goalsAgainst}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    TotalsCard(stats.totals)
                    PlayerTable(stats.players.filter { it.minutesMs > 0 })
                    ShotMap(stats.events.filter { it.hasPitchPosition }.map {
                        Triple(it.pitchX!!, it.pitchY!!, it.type to it.side)
                    })
                    GoalPlacement(
                        stats.events.filter { it.hasGoalPlacement }.map {
                            Triple(it.goalX!!, it.goalY!!, it.type to it.side)
                        }
                    )
                }
            } else {
                val season = viewModel.seasonStats
                if (season == null || season.games.isEmpty()) {
                    Text("No games played yet.")
                } else {
                    Text(
                        "${season.teamName} · ${season.games.size} games · ${season.record}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    TotalsCard(season.totals)
                    PlayerTable(season.players)
                    MinutesByPosition(season.players)
                    ShotMap(
                        season.games.flatMap { g ->
                            g.events.filter { it.hasPitchPosition }.map {
                                Triple(it.pitchX!!, it.pitchY!!, it.type to it.side)
                            }
                        }
                    )
                    GoalPlacement(
                        season.games.flatMap { g ->
                            g.events.filter { it.hasGoalPlacement }.map {
                                Triple(it.goalX!!, it.goalY!!, it.type to it.side)
                            }
                        }
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun TotalsCard(totals: TeamTotals) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("Team", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(6.dp))
            listOf(
                "Goals" to "${totals.goalsFor} – ${totals.goalsAgainst}",
                "Shots" to "${totals.shotsFor} – ${totals.shotsAgainst}",
                "On target" to "${totals.shotsOnFor} – ${totals.shotsOnAgainst}",
                "Corners" to "${totals.cornersFor} – ${totals.cornersAgainst}",
                "Fouls" to "${totals.foulsFor} – ${totals.foulsAgainst}",
                "Saves" to "${totals.savesFor}"
            ).forEach { (label, value) ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Text(
                        label,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(value, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

/** Horizontally scrollable so the column set can grow without squashing. */
@Composable
private fun PlayerTable(players: List<PlayerStats>) {
    if (players.isEmpty()) {
        Text(
            "No player minutes recorded.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    val headers = listOf("#", "Player", "GP", "Min", "G", "A", "Sh", "SOT", "Tkl", "50/50")
    val widths = listOf(34, 140, 34, 46, 30, 30, 34, 38, 38, 50)

    Column {
        Text("Players", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(6.dp))
        Column(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            Row {
                headers.forEachIndexed { i, h ->
                    Text(
                        h,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(widths[i].dp).padding(vertical = 4.dp)
                    )
                }
            }
            HorizontalDivider()
            players.forEach { p ->
                val cells = listOf(
                    p.player.displayNumber,
                    p.player.fullName,
                    p.gamesPlayed.toString(),
                    p.minutes.toString(),
                    p.goals.toString(),
                    p.assists.toString(),
                    p.shots.toString(),
                    p.shotsOn.toString(),
                    p.tackles.toString(),
                    p.fiftyFifties.toString()
                )
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

@Composable
private fun MinutesByPosition(players: List<PlayerStats>) {
    val roles = listOf(
        Position.GOALKEEPER, Position.DEFENDER, Position.MIDFIELDER, Position.FORWARD
    )
    Column {
        Text("Minutes by position", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(6.dp))
        Column(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            Row {
                (listOf("Player") + roles.map { it.abbreviation } + "Total").forEachIndexed { i, h ->
                    Text(
                        h,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(if (i == 0) 140.dp else 52.dp)
                            .padding(vertical = 4.dp)
                    )
                }
            }
            HorizontalDivider()
            players.forEach { p ->
                Row {
                    Text(
                        p.player.fullName,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        modifier = Modifier.width(140.dp).padding(vertical = 5.dp)
                    )
                    roles.forEach { role ->
                        Text(
                            ((p.minutesByPosition[role] ?: 0L) / 60000).toString(),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.width(52.dp).padding(vertical = 5.dp)
                        )
                    }
                    Text(
                        p.minutes.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.width(52.dp).padding(vertical = 5.dp)
                    )
                }
            }
        }
    }
}

private fun markerColor(type: EventType, side: EventSide): Color = when {
    side == EventSide.THEM -> Color(0xFFC62828)
    type == EventType.GOAL -> Color(0xFF2E7D32)
    type == EventType.SHOT_ON -> Color(0xFF1565C0)
    else -> Color(0xFF6A1B9A)
}

@Composable
private fun ShotMap(points: List<Triple<Float, Float, Pair<EventType, EventSide>>>) {
    if (points.isEmpty()) return
    Column {
        Text("Where events happened", style = MaterialTheme.typography.titleSmall)
        Text(
            "Attacking upward. Green goals, blue shots on target, red against you.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        PitchView(
            markers = points.mapIndexed { index, (x, y, meta) ->
                PitchMarker(
                    id = index.toLong(),
                    x = x,
                    y = y,
                    label = meta.first.short,
                    color = markerColor(meta.first, meta.second)
                )
            },
            markerSize = 24.dp,
            modifier = Modifier.fillMaxWidth().aspectRatio(0.66f)
        )
    }
}

@Composable
private fun GoalPlacement(points: List<Triple<Float, Float, Pair<EventType, EventSide>>>) {
    if (points.isEmpty()) return
    Column {
        Text("Goal placement", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(6.dp))
        GoalMouthView(
            marks = points.map { (x, y, meta) ->
                GoalMark(x = x, y = y, color = markerColor(meta.first, meta.second))
            },
            modifier = Modifier.fillMaxWidth().aspectRatio(1.6f)
        )
    }
}
