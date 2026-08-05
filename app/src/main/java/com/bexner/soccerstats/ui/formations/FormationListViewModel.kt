package com.bexner.soccerstats.ui.formations

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bexner.soccerstats.data.SoccerRepository
import com.bexner.soccerstats.data.entity.FormationWithSlots
import com.bexner.soccerstats.data.entity.MatchFormat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FormationListViewModel(
    private val repository: SoccerRepository
) : ViewModel() {

    private val selectedFormat = MutableStateFlow(MatchFormat.SEVEN_V_SEVEN)

    var format by mutableStateOf(MatchFormat.SEVEN_V_SEVEN)
        private set

    @OptIn(ExperimentalCoroutinesApi::class)
    val formations: StateFlow<List<FormationWithSlots>> =
        selectedFormat
            .flatMapLatest { repository.observeFormations(it) }
            .map { list -> list.sortedWith(compareByDescending<FormationWithSlots> { it.formation.isPreset }.thenBy { it.formation.name }) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    fun onFormatSelected(value: MatchFormat) {
        format = value
        selectedFormat.value = value
    }

    fun duplicate(source: FormationWithSlots, onDone: (Long) -> Unit) {
        viewModelScope.launch {
            val id = repository.duplicateFormation(source, "${source.formation.name} (copy)")
            onDone(id)
        }
    }

    fun delete(source: FormationWithSlots) {
        viewModelScope.launch {
            repository.deleteFormation(source.formation)
        }
    }
}
