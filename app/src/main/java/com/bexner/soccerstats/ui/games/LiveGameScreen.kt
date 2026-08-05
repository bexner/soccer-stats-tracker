package com.bexner.soccerstats.ui.games

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bexner.soccerstats.data.entity.EventSide
import com.bexner.soccerstats.data.entity.EventType
import com.bexner.soccerstats.data.entity.GameStatus
import com.bexner.soccerstats.ui.AppViewModelProvider

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LiveGameScreen(
    onBack: () -> Unit,
    viewModel: LiveGameViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val elapsedMs by viewModel.elapsedMs.collectAsStateWithLifecycle()
    val game = uiState.game
    var confirmFinish by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        game?.let { "${it.goalsFor} – ${it.goalsAgainst}  vs ${it.opponent}" }
                            ?: "Match"
                    )
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
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ---- Clock ----
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = GameFormat.clock(elapsedMs),
                        fontSize = 54.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = game?.let {
                            "Half ${it.currentPeriod} of ${it.periodCount} · " +
                                "${it.periodMinutes} min halves"
                        } ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (game?.isClockRunning == true) {
                            Button(
                                onClick = viewModel::stopClock,
                                modifier = Modifier.weight(1f)
                            ) { Text("Stop") }
                        } else {
                            Button(
                                onClick = viewModel::startClock,
                                modifier = Modifier.weight(1f)
                            ) { Text(if (elapsedMs == 0L) "Kick off" else "Resume") }
                        }
                        OutlinedButton(
                            onClick = viewModel::endPeriod,
                            modifier = Modifier.weight(1f)
                        ) { Text("End half") }
                    }
                }
            }

            if (game?.status == GameStatus.FINAL) {
                Text(
                    "This game is marked final.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // ---- Side toggle ----
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                EventSide.all.forEachIndexed { index, s ->
                    SegmentedButton(
                        selected = viewModel.side == s,
                        onClick = { viewModel.onSideChange(s) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = EventSide.all.size
                        ),
                        label = { Text(if (s == EventSide.US) "Us" else "Them") }
                    )
                }
            }
            Text(
                text = if (viewModel.side == EventSide.US) {
                    "Logging for your team — you'll be asked who."
                } else {
                    "Logging against you — recorded with one tap, no player needed."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // ---- Event buttons ----
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EventType.loggable.forEach { type ->
                    FilledTonalButton(onClick = { viewModel.onEventTapped(type) }) {
                        Text(type.label)
                    }
                }
            }

            HorizontalDivider()

            // ---- On the pitch ----
            Text("On the pitch", style = MaterialTheme.typography.titleSmall)
            if (uiState.onPitch.isEmpty()) {
                Text(
                    "No one is on yet. Set a lineup, then kick off to start tracking minutes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                uiState.onPitch.forEach { stint ->
                    val player = uiState.player(stint.playerId)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.beginSubstitution(stint.playerId) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${player?.displayNumber ?: "-"}  ${player?.fullName ?: "Unknown"}",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = GameFormat.minutes(
                                uiState.minutesFor(stint.playerId, elapsedMs)
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(10.dp))
                        AssistChip(
                            onClick = { viewModel.beginSubstitution(stint.playerId) },
                            label = { Text("Sub") }
                        )
                    }
                }
            }

            HorizontalDivider()

            // ---- Timeline ----
            Text("Timeline", style = MaterialTheme.typography.titleSmall)
            if (uiState.events.isEmpty()) {
                Text(
                    "Nothing logged yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                uiState.events.take(30).forEach { event ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = event.clockLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = buildString {
                                append(event.type.label)
                                if (event.side == EventSide.THEM) append(" (them)")
                                uiState.player(event.playerId)?.let { append(" — ${it.fullName}") }
                                uiState.player(event.secondaryPlayerId)?.let {
                                    append(" ▸ ${it.fullName}")
                                }
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { viewModel.undo(event) }) { Text("Undo") }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            OutlinedButton(
                onClick = { confirmFinish = true },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Finish game") }
            Spacer(Modifier.height(12.dp))
        }
    }

    // ---- Who did it? ----
    viewModel.pendingEvent?.let { pending ->
        AlertDialog(
            onDismissRequest = viewModel::dismissPendingEvent,
            title = { Text("${pending.type.label} — who?") },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                    items(uiState.onPitch, key = { it.id }) { stint ->
                        val player = uiState.player(stint.playerId)
                        Text(
                            text = "${player?.displayNumber ?: "-"}  ${player?.fullName ?: ""}",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.confirmPendingEvent(stint.playerId) }
                                .padding(vertical = 12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmPendingEvent(null) }) {
                    Text("Skip player")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissPendingEvent) { Text("Cancel") }
            }
        )
    }

    // ---- Substitution ----
    viewModel.substitutingOut?.let { outId ->
        val outPlayer = uiState.player(outId)
        AlertDialog(
            onDismissRequest = viewModel::cancelSubstitution,
            title = { Text("Who comes on for ${outPlayer?.firstName ?: "this player"}?") },
            text = {
                if (uiState.bench.isEmpty()) {
                    Text("Nobody is on the bench.")
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                        items(uiState.bench, key = { it.id }) { player ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.completeSubstitution(player.id) }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${player.displayNumber}  ${player.fullName}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = GameFormat.minutes(
                                        uiState.minutesFor(player.id, elapsedMs)
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = viewModel::cancelSubstitution) { Text("Cancel") }
            }
        )
    }

    if (confirmFinish) {
        AlertDialog(
            onDismissRequest = { confirmFinish = false },
            title = { Text("Finish game?") },
            text = {
                Text(
                    "This stops the clock and closes everyone's minutes. You can still " +
                        "review the timeline afterwards."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmFinish = false
                    viewModel.finishGame(onBack)
                }) { Text("Finish") }
            },
            dismissButton = {
                TextButton(onClick = { confirmFinish = false }) { Text("Cancel") }
            }
        )
    }
}
