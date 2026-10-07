package au.edu.jcu.assessment.utilityapp.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.nio.file.Files
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class TeamPreferencesRepositoryTest {
    @Test
    fun teamsSurviveStoreRecreationAndClearing() = runBlocking {
        val folder = Files.createTempDirectory("team-preferences-test").toFile()
        val file = folder.resolve("test.preferences_pb")
        var job = SupervisorJob()

        fun openRepository(): TeamPreferencesRepository {
            val scope = CoroutineScope(Dispatchers.IO + job)
            return TeamPreferencesRepository(
                PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
            )
        }

        try {
            var repository = openRepository()
            assertEquals(emptyList<String>(), repository.followedTeams.first())
            repository.saveFollowedTeams(listOf("Chelsea", "Arsenal", "Chelsea"))
            job.cancelAndJoin()

            job = SupervisorJob()
            repository = openRepository()
            assertEquals(listOf("Arsenal", "Chelsea"), repository.followedTeams.first())
            repository.saveFollowedTeams(emptyList())
            job.cancelAndJoin()

            job = SupervisorJob()
            repository = openRepository()
            assertEquals(emptyList<String>(), repository.followedTeams.first())
        } finally {
            job.cancelAndJoin()
            folder.deleteRecursively()
        }
    }
}
