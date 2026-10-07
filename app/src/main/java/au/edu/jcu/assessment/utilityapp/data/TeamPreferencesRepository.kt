package au.edu.jcu.assessment.utilityapp.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// A single DataStore instance for this file, shared across Activity recreations.
private val Context.teamPreferencesDataStore by preferencesDataStore(name = "followed_teams")

class TeamPreferencesRepository(private val dataStore: DataStore<Preferences>) {
    private val followedTeamsKey = stringSetPreferencesKey("followed_teams")

    val followedTeams: Flow<List<String>> = dataStore.data.map { preferences ->
        preferences[followedTeamsKey].orEmpty().sorted()
    }

    suspend fun saveFollowedTeams(teams: List<String>) {
        dataStore.edit { preferences ->
            preferences[followedTeamsKey] = teams.toSet()
        }
    }

    companion object {
        fun from(context: Context) =
            TeamPreferencesRepository(context.applicationContext.teamPreferencesDataStore)
    }
}
