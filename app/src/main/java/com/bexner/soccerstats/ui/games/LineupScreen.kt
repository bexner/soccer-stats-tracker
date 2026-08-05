package com.bexner.soccerstats.ui.games

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bexner.soccerstats.data.entity.ShapePhase
import com.bexner.soccerstats.ui.AppViewModelProvider
import com.bexner.soccerstats.ui.components.PitchMarker
import com.bexner.soccerstats.ui.components.PitchView
import com.bexner.soccerstats.ui.formations.markerColor

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LineupScreen(
    onBack: () -> Unit,
    viewModel: LineupViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val formation = uiState.formations.firstOrNull { it.formation.id == viewModel.selectedFormationId }
    val playersById = uiState.available.associateBy { it.id }

    val markers = formation?.slotsFor(viewModel.phase)?.map { slot ->
        val assigned = viewModel.assignments[slot.slotIndex]?.let { playersById[it] }
        PitchMarker(
            id = slot.slotIndex.toLong(),
            x = slot.x,
            y = slot.y,
            label = assigned?.displayNumber ?: slot.displayLabel,
            color = if (assigned != null) slot.role.markerColor() else Color(0x66000000)
        )
    }.orEmpty()

    val filled = formation?.slotsFor(ShapePhase.DEFENDING)
        ?.count { viewModel.assignments[it.slotIndex] != null } ?: 0
    val required = formation?.formation?.format?.playersOnField ?: 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Lineup")
                        uiState.game?.let {
                            Text(
                                "vs ${it.opponent} · ${uiState.available.size} available",
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
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Formation", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                uiState.formations.forEach { entry ->
                    FilterChip(
                        selected = viewModel.selectedFormationId == entry.formation.id,
                        onClick = { viewModel.onFormationSelected(entry.formation.id) },
                        label = { Text("${entry.formation.name} (${entry.formation.format.label})") }
                    )
                }
            }

            if (formation == null) {
                Text(
                    "Pick a formation to start placing players.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                if (formation.hasBothPhases) {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        ShapePhase.all.forEachIndexed { index, p ->
                            SegmentedButton(
                                selected = viewModel.phase == p,
                                onClick = { viewModel.onPhaseChange(p) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = ShapePhase.all.size
                                ),
                                label = { Text(p.label) }
                            )
                        }
                    }
                }

                Text(
                    text = "Tap a position to assign a player. Numbers shown are jersey numbers " +
                        "once filled.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                PitchView(
                    markers = markers,
                    onMarkerTapped = { id -> viewModel.onSlotTapped(id.toInt()) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.66f)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { viewModel.autoFill(formation) },
                        modifier = Modifier.weight(1f)
                    ) { Text("Auto-fill") }
                    OutlinedButton(
                        onClick = viewModel::clearAll,
                        modifier = Modifier.weight(1f)
                    ) { Text("Clear") }
                }

                Text(
                    text = "$filled of $required positions filled",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (filled == required) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )

                val benched = uiState.available.filter { it.id !in viewModel.assignments.values }
                if (benched.isNotEmpty()) {
                    Text("Bench", style = MaterialTheme.typography.labelLarge)
                    Text(
                        text = benched.joinToString(", ") { "${it.displayNumber} ${it.firstName}" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = { viewModel.save(onBack) },
                    enabled = filled > 0,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Save lineup") }

                Spacer(Modifier.height(8.dp))
            }
        }
    }

    val pickingSlot = viewModel.pickingForSlot
    if (pickingSlot != null && formation != null) {
        val slot = formation.slotsFor(viewModel.phase).firstOrNull { it.slotIndex == pickingSlot }
        AlertDialog(
            onDismissRequest = viewModel::dismissPicker,
            title = { Text("Who plays ${slot?.displayLabel ?: "this position"}?") },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 340.dp)) {
                    items(uiState.available, key = { it.id }) { player ->
                        val takenSlot = viewModel.assignments.entries
                            .firstOrNull { it.value == player.id }?.key
                        Text(
                            text = buildString {
                                append("${player.displayNumber}  ${player.fullName}")
                                if (takenSlot != null && takenSlot != pickingSlot) {
                                    append("  (moving from another spot)")
                                }
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (takenSlot != null && takenSlot != pickingSlot) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.assignPlayer(pickingSlot, player.id) }
                                .padding(vertical = 12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.clearSlot(pickingSlot) }) { Text("Leave empty") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissPicker) { Text("Cancel") }
            }
        )
    }
}
