package com.bexner.soccerstats.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.bexner.soccerstats.SoccerStatsApplication
import com.bexner.soccerstats.ui.formations.FormationEditViewModel
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
    }
}

private fun CreationExtras.soccerApplication(): SoccerStatsApplication =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as SoccerStatsApplication)
