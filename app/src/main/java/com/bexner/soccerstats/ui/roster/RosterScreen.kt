package com.bexner.soccerstats.ui.roster

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bexner.soccerstats.data.entity.Player
import com.bexner.soccerstats.data.entity.Position
import com.bexner.soccerstats.ui.AppViewModelProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RosterScreen(
    onBack: () -> Unit,
    onEditTeam: () -> Unit,
    onAddPlayer: () -> Unit,
    onEditPlayer: (Long) -> Unit,
    onTeamDeleted: () -> Unit,
    viewModel: RosterViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var menuOpen by remember { mutableStateOf(false) }
    var confirmDeleteTeam by remember { mutableStateOf(false) }
    var playerPendingDelete by remember { mutableStateOf<Player?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(uiState.team?.name ?: "Roster")
                        val sub = listOfNotNull(
                            uiState.team?.ageGroup?.takeIf { it.isNotBlank() },
                            uiState.team?.season?.takeIf { it.isNotBlank() }
                        ).joinToString(" · ")
                        if (sub.isNotBlank()) {
                            Text(text = sub, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Team options")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Edit team details") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                onEditTeam()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete team") },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                confirmDeleteTeam = true
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddPlayer) {
                Icon(Icons.Default.Add, contentDescription = "Add player")
            }
        }
    ) { innerPadding ->
        if (!uiState.isLoading && uiState.players.isEmpty()) {
            EmptyRoster(modifier = Modifier.padding(innerPadding))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = innerPadding.calculateTopPadding() + 12.dp,
                    bottom = innerPadding.calculateBottomPadding() + 88.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        text = "${uiState.players.size} on roster · ${uiState.activeCount} active",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                items(uiState.players, key = { it.id }) { player ->
                    PlayerRow(
                        player = player,
                        onClick = { onEditPlayer(player.id) },
                        onToggleActive = { viewModel.togglePlayerActive(player) },
                        onDelete = { playerPendingDelete = player }
                    )
                }
            }
        }
    }

    if (confirmDeleteTeam) {
        AlertDialog(
            onDismissRequest = { confirmDeleteTeam = false },
            title = { Text("Delete team?") },
            text = {
                Text("This removes ${uiState.team?.name ?: "the team"} and all ${uiState.players.size} players on its roster. This cannot be undone.")
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDeleteTeam = false
                    viewModel.deleteTeam(onTeamDeleted)
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteTeam = false }) { Text("Cancel") }
            }
        )
    }

    playerPendingDelete?.let { player ->
        AlertDialog(
            onDismissRequest = { playerPendingDelete = null },
            title = { Text("Remove ${player.fullName}?") },
            text = { Text("This player will be removed from the roster.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deletePlayer(player)
                    playerPendingDelete = null
                }) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { playerPendingDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun PlayerRow(
    player: Player,
    onClick: () -> Unit,
    onToggleActive: () -> Unit,
    onDelete: () -> Unit
) {
    var rowMenuOpen by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .alpha(if (player.isActive) 1f else 0.45f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            JerseyBadge(number = player.displayNumber)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = player.fullName,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = if (player.isActive) player.position.label
                    else "${player.position.label} · inactive",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            PositionChip(position = player.position)
            Box {
                IconButton(onClick = { rowMenuOpen = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Player options")
                }
                DropdownMenu(expanded = rowMenuOpen, onDismissRequest = { rowMenuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = {
                            rowMenuOpen = false
                            onClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(if (player.isActive) "Mark inactive" else "Mark active") },
                        onClick = {
                            rowMenuOpen = false
                            onToggleActive()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Remove from roster") },
                        onClick = {
                            rowMenuOpen = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun JerseyBadge(number: String) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(MaterialTheme.colorScheme.primary, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = number,
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun PositionChip(position: Position) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = position.abbreviation,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun EmptyRoster(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.PersonAdd,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(16.dp))
        Text(text = "Roster is empty", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Tap + to add players. Name, jersey number and position are all you need to start tracking stats.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
