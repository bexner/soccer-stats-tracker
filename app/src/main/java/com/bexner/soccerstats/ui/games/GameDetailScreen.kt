package com.bexner.soccerstats.ui.games

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bexner.soccerstats.data.entity.GameStatus
import com.bexner.soccerstats.data.entity.Venue
import com.bexner.soccerstats.ui.AppViewModelProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameDetailScreen(
    onBack: () -> Unit,
    onEditGame: () -> Unit,
    onAttendance: () -> Unit,
    onLineup: () -> Unit,
    onLive: () -> Unit,
    onStats: () -> Unit,
    viewModel: GameDetailViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val game = uiState.game

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        game?.let {
                            when (it.venue) {
                                Venue.HOME -> "vs ${it.opponent}"
                                Venue.AWAY -> "at ${it.opponent}"
                                Venue.NEUTRAL -> "vs ${it.opponent}"
                            }
                        } ?: "Game"
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onEditGame) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit game")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            game?.let {
                Text(GameFormat.full(it.kickoffAt), style = MaterialTheme.typography.titleMedium)
                if (it.location.isNotBlank()) {
                    Text(
                        it.location,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (it.status != GameStatus.SCHEDULED) {
                    Text(
                        "${it.status.label} · ${it.goalsFor}–${it.goalsAgainst}",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            StepCard(
                title = "Attendance",
                subtitle = "${uiState.yesCount} in · ${uiState.maybeCount} maybe · " +
                    "${uiState.noCount} out",
                onClick = onAttendance
            )

            StepCard(
                title = "Lineup",
                subtitle = if (uiState.lineup.isEmpty()) {
                    "Not set — pick a formation and place players"
                } else {
                    "${uiState.lineup.size} positions filled"
                },
                onClick = onLineup
            )

            StepCard(
                title = "Stats",
                subtitle = "Breakdown for this game, and season totals",
                onClick = onStats
            )

            Button(
                onClick = onLive,
                enabled = uiState.lineup.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    when (game?.status) {
                        GameStatus.IN_PROGRESS -> "Back to live match"
                        GameStatus.FINAL -> "Review match"
                        else -> "Start match"
                    }
                )
            }
            if (uiState.lineup.isEmpty()) {
                Text(
                    "Set a lineup first — the clock needs to know who started to track minutes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StepCard(title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
