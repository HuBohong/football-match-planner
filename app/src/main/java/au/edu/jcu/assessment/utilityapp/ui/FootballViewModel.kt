package au.edu.jcu.assessment.utilityapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import au.edu.jcu.assessment.utilityapp.data.FootballRepository
import au.edu.jcu.assessment.utilityapp.data.FootballSchedule
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import com.google.gson.JsonParseException
import retrofit2.HttpException

data class FootballUiState(
    val schedule: FootballSchedule? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class FootballViewModel(
    private val repository: FootballRepository,
    private val hasToken: Boolean
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(FootballUiState())
    val uiState = mutableUiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadSchedule()
    }

    fun loadSchedule() {
        if (loadJob?.isActive == true) return
        if (!hasToken) {
            mutableUiState.value = FootballUiState(isLoading = false,
                errorMessage = "Add FOOTBALL_API_TOKEN to local.properties, then rebuild the app.")
            return
        }
        mutableUiState.value = FootballUiState()
        loadJob = viewModelScope.launch {
            try {
                mutableUiState.value = FootballUiState(schedule = repository.loadPremierLeague(), isLoading = false)
            } catch (error: HttpException) {
                val message = when (error.code()) {
                    401, 403 -> "API access denied. Check your token and competition access."
                    429 -> "API request limit reached. Wait a minute, then retry."
                    else -> "Football service unavailable. Please retry later."
                }
                mutableUiState.value = FootballUiState(isLoading = false, errorMessage = message)
            } catch (error: JsonParseException) {
                mutableUiState.value = FootballUiState(isLoading = false,
                    errorMessage = "Could not read the football service response. Please retry later.")
            } catch (error: IOException) {
                mutableUiState.value = FootballUiState(isLoading = false,
                    errorMessage = "Could not load matches. Check your connection and retry.")
            }
        }
    }

    class Factory(
        private val repository: FootballRepository,
        private val hasToken: Boolean
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(FootballViewModel::class.java))
            @Suppress("UNCHECKED_CAST")
            return FootballViewModel(repository, hasToken) as T
        }
    }
}
