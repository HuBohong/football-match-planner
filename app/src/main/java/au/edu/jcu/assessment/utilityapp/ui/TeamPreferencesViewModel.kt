package au.edu.jcu.assessment.utilityapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import au.edu.jcu.assessment.utilityapp.data.TeamPreferencesRepository
import java.io.IOException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TeamPreferencesUiState(
    // Null means loading; an empty list means the user has not followed any teams.
    val followedTeams: List<String>? = null,
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)

class TeamPreferencesViewModel(
    private val repository: TeamPreferencesRepository
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(TeamPreferencesUiState())
    val uiState = mutableUiState.asStateFlow()
    private var loadJob: Job? = null

    init {
        loadTeams()
    }

    fun loadTeams() {
        loadJob?.cancel()
        mutableUiState.update { it.copy(errorMessage = null) }
        loadJob = viewModelScope.launch {
            try {
                repository.followedTeams.collect { teams ->
                    mutableUiState.update { it.copy(followedTeams = teams) }
                }
            } catch (error: IOException) {
                mutableUiState.update {
                    it.copy(errorMessage = "Could not read saved teams. Please retry.")
                }
            }
        }
    }

    fun saveTeams(teams: List<String>) {
        if (mutableUiState.value.isSaving) return
        mutableUiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                repository.saveFollowedTeams(teams)
            } catch (error: IOException) {
                mutableUiState.update {
                    it.copy(errorMessage = "Could not save teams. Open My teams and try again.")
                }
            } finally {
                mutableUiState.update { it.copy(isSaving = false) }
            }
        }
    }

    class Factory(private val repository: TeamPreferencesRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(TeamPreferencesViewModel::class.java))
            @Suppress("UNCHECKED_CAST")
            return TeamPreferencesViewModel(repository) as T
        }
    }
}
