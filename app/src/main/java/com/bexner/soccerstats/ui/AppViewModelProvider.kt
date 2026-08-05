package com.bexner.soccerstats.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.bexner.soccerstats.SoccerStatsApplication
import com.bexner.soccerstats.ui.formations.FormationEditViewModel
import com.bexner.soccerstats.ui.games.GameDetailViewModel
import com.bexner.soccerstats.ui.games.GameEditViewModel
import com.bexner.soccerstats.ui.games.GameListViewModel
import com.bexner.soccerstats.ui.games.LineupViewModel
import com.bexner.soccerstats.ui.games.LiveGameViewModel
import com.bexner.soccerstats.ui.formations.FormationListViewModel
import com.bexner.soccerstats.ui.roster.PlayerEditViewModel
import com.bexner.soccerstats.ui.roster.RosterViewModel
import com.bexner.soccerstats.ui.teams.TeamEditViewModel
import com.bexner.soccerstats.ui.teams.TeamListViewModel

/** Wires ViewModels to the repository held by the Application. */
object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer { TeamListViewModel(soccerApplication().repository) }
        initializer { TeamEditViewModel(createSavedStateHandle(), soccerApplication().repository) }
        initializer { RosterViewModel(createSavedStateHandle(), soccerApplication().repository) }
        initializer { PlayerEditViewModel(createSavedStateHandle(), soccerApplication().repository) }
        initializer { FormationListViewModel(soccerApplication().repository) }
        initializer { FormationEditViewModel(createSavedStateHandle(), soccerApplication().repository) }
        initializer { GameListViewModel(createSavedStateHandle(), soccerApplication().repository) }
        initializer { GameEditViewModel(createSavedStateHandle(), soccerApplication().repository) }
        initializer { GameDetailViewModel(createSavedStateHandle(), soccerApplication().repository) }
        initializer { LineupViewModel(createSavedStateHandle(), soccerApplication().repository) }
        initializer { LiveGameViewModel(createSavedStateHandle(), soccerApplication().repository) }
    }
}

private fun CreationExtras.soccerApplication(): SoccerStatsApplication =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as SoccerStatsApplication)
