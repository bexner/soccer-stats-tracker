package com.bexner.soccerstats.ui.roster

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bexner.soccerstats.data.entity.Position
import com.bexner.soccerstats.ui.AppViewModelProvider

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PlayerEditScreen(
    onDone: () -> Unit,
    viewModel: PlayerEditViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val form = viewModel.form

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.isNewPlayer) "Add Player" else "Edit Player") },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = form.firstName,
                onValueChange = viewModel::onFirstNameChange,
                label = { Text("First name") },
                singleLine = true,
                isError = form.firstNameError != null,
                supportingText = form.firstNameError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = form.lastName,
                onValueChange = viewModel::onLastNameChange,
                label = { Text("Last name (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = form.jerseyNumber,
                onValueChange = viewModel::onJerseyChange,
                label = { Text("Jersey number") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = form.jerseyError != null,
                supportingText = form.jerseyError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth()
            )

            Text(text = "Primary position", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Position.selectable.forEach { position ->
                    FilterChip(
                        selected = form.position == position,
                        onClick = { viewModel.onPositionChange(position) },
                        label = { Text(position.label) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Active", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Inactive players stay on the roster but are left out of game lineups.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = form.isActive,
                    onCheckedChange = viewModel::onActiveChange
                )
            }

            Button(
                onClick = { viewModel.save(onDone) },
                enabled = form.isValid,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (viewModel.isNewPlayer) "Add to roster" else "Save changes")
            }
        }
    }
}
