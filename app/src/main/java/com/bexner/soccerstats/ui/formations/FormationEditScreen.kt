package com.bexner.soccerstats.ui.formations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bexner.soccerstats.data.entity.MatchFormat
import com.bexner.soccerstats.data.entity.Position
import com.bexner.soccerstats.ui.AppViewModelProvider
import com.bexner.soccerstats.ui.components.PitchMarker
import com.bexner.soccerstats.ui.components.PitchView

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FormationEditScreen(
    onDone: () -> Unit,
    viewModel: FormationEditViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state = viewModel.state

    // Marker ids are slot indexes here, so drag callbacks map straight back to slots.
    val markers = state.slots.map { slot ->
        PitchMarker(
            id = slot.slotIndex.toLong(),
            x = slot.x,
            y = slot.y,
            label = slot.displayLabel,
            color = slot.role.markerColor()
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.isNew) "New Formation" else "Edit Formation") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = { Text("Formation name") },
                placeholder = { Text("2-3-1") },
                singleLine = true,
                isError = state.nameError != null,
                supportingText = state.nameError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Text("Format", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MatchFormat.all.forEach { format ->
                    FilterChip(
                        selected = state.format == format,
                        onClick = { viewModel.onFormatChange(format) },
                        label = { Text(format.label) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Goalkeeper", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Switching this resets the shape to an even spread.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = state.hasKeeper,
                    onCheckedChange = viewModel::onKeeperChange
                )
            }

            Text(
                text = "Drag the markers to shape your formation. Tap one to change its role or label.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            PitchView(
                markers = markers,
                selectedMarkerId = state.selectedSlotIndex?.toLong(),
                onMarkerMoved = { id, x, y -> viewModel.onSlotMoved(id.toInt(), x, y) },
                onMarkerTapped = { id -> viewModel.onSlotTapped(id.toInt()) },
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.66f)
            )

            state.selectedSlotIndex?.let { index ->
                val slot = state.slots.firstOrNull { it.slotIndex == index }
                if (slot != null) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                "Selected marker",
                                style = MaterialTheme.typography.titleSmall
                            )
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(
                                    Position.GOALKEEPER,
                                    Position.DEFENDER,
                                    Position.MIDFIELDER,
                                    Position.FORWARD
                                ).forEach { role ->
                                    FilterChip(
                                        selected = slot.role == role,
                                        onClick = { viewModel.onSelectedRoleChange(role) },
                                        label = { Text(role.abbreviation) }
                                    )
                                }
                            }
                            OutlinedTextField(
                                value = slot.label,
                                onValueChange = viewModel::onSelectedLabelChange,
                                label = { Text("Custom label (optional)") },
                                placeholder = { Text("LB") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            Text(
                text = "${state.placed} of ${state.required} players placed",
                style = MaterialTheme.typography.bodyMedium,
                color = if (state.isComplete) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.error
                }
            )

            Button(
                onClick = { viewModel.save(onDone) },
                enabled = state.isValid,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (viewModel.isNew) "Save formation" else "Save changes")
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}
