package au.edu.jcu.assessment.utilityapp.data

import com.google.gson.Gson
import com.google.gson.JsonElement
import java.io.File
import java.io.IOException
import java.time.Clock
import java.time.Duration
import java.time.Instant

// Cached responses contain public football data only, never API credentials.
class FootballResponseCache(
    private val directory: File? = null,
    private val clock: Clock = Clock.systemUTC()
) {
    private val gson = Gson()
    private val memory = mutableMapOf<String, StoredResponse>()

    fun <T> read(key: String, type: Class<T>, maxAge: Duration): CachedResponse<T>? {
        val stored = memory[key] ?: readFile(key) ?: return null
        val fetchedAt = Instant.ofEpochMilli(stored.savedAt)
        val age = Duration.between(fetchedAt, clock.instant())
        if (age.isNegative || age >= maxAge) return null
        return try {
            val value = gson.fromJson(stored.payload, type) ?: return null
            CachedResponse(value, fetchedAt)
        } catch (error: RuntimeException) {
            null
        }
    }

    fun <T> write(key: String, value: T): CachedResponse<T> {
        val fetchedAt = Instant.ofEpochMilli(clock.millis())
        val stored = StoredResponse(fetchedAt.toEpochMilli(), gson.toJsonTree(value))
        memory[key] = stored
        if (directory != null) {
            try {
                directory.mkdirs()
                val temporary = File(directory, "$key.tmp")
                temporary.writeText(gson.toJson(stored))
                // The previous response remains intact until the replacement is complete.
                java.nio.file.Files.move(temporary.toPath(), File(directory, "$key.json").toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING)
            } catch (error: IOException) {
                // A disk-cache failure should not prevent showing a successful API response.
            }
        }
        return CachedResponse(value, fetchedAt)
    }

    private fun readFile(key: String): StoredResponse? = try {
        directory?.let { folder ->
            val file = File(folder, "$key.json")
            if (file.exists()) gson.fromJson(file.readText(), StoredResponse::class.java) else null
        }?.also { memory[key] = it }
    } catch (error: IOException) {
        null
    } catch (error: RuntimeException) {
        null
    }

    private data class StoredResponse(val savedAt: Long, val payload: JsonElement)
}

data class CachedResponse<T>(val value: T, val fetchedAt: Instant)
