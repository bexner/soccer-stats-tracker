package com.bexner.soccerstats.ui.teams

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bexner.soccerstats.ui.AppViewModelProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamEditScreen(
    onDone: () -> Unit,
    viewModel: TeamEditViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val form = viewModel.form

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.isNewTeam) "New Team" else "Edit Team") },
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
                value = form.name,
                onValueChange = viewModel::onNameChange,
                label = { Text("Team name") },
                placeholder = { Text("Riverside Rovers") },
                singleLine = true,
                isError = form.nameError != null,
                supportingText = form.nameError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = form.ageGroup,
                onValueChange = viewModel::onAgeGroupChange,
                label = { Text("Age group (optional)") },
                placeholder = { Text("U12") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = form.season,
                onValueChange = viewModel::onSeasonChange,
                label = { Text("Season (optional)") },
                placeholder = { Text("Fall 2026") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = { viewModel.save(onDone) },
                enabled = form.isValid,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (viewModel.isNewTeam) "Create team" else "Save changes")
            }
        }
    }
}
