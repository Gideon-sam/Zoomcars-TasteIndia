package com.tasteindia.data

import com.tasteindia.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import java.io.File

@Serializable
data class StoredCollection(val meals: List<MealSummary> = emptyList(), val details: Map<String, Recipe> = emptyMap(), val savedAt: Long = 0)

/** Replaceable persistence boundary. Favourite snapshots are independent of the response cache. */
interface LocalStore {
    suspend fun readCache(): StoredCollection?
    suspend fun writeCache(value: StoredCollection)
    suspend fun readFavourites(): Map<String, Recipe>
    suspend fun writeFavourites(value: Map<String, Recipe>)
}

class JsonFileStore(private val directory: File) : LocalStore {
    private val lock = Mutex()
    private suspend fun read(name: String): String? = withContext(Dispatchers.IO) {
        lock.withLock {
            val file = File(directory, name)
            val backup = File(directory, "$name.bak")
            if (!file.exists() && backup.exists()) backup.renameTo(file)
            file.takeIf(File::exists)?.readText()
        }
    }
    private suspend fun write(name: String, text: String): Unit = withContext(Dispatchers.IO) {
        lock.withLock {
            directory.mkdirs()
            val file = File(directory, name)
            val temp = File(directory, "$name.tmp")
            val backup = File(directory, "$name.bak")
            temp.outputStream().use { out -> out.write(text.toByteArray(Charsets.UTF_8)); out.fd.sync() }
            if (file.exists() && !file.renameTo(backup)) error("Could not preserve saved data")
            if (!temp.renameTo(file)) { backup.renameTo(file); error("Could not save recipes") }
            backup.delete()
            Unit
        }
    }
    override suspend fun readCache() = read("cache.json")?.let { runCatching { AppJson.decodeFromString<StoredCollection>(it) }.getOrNull() }
    override suspend fun writeCache(value: StoredCollection) = write("cache.json", AppJson.encodeToString(value))
    override suspend fun readFavourites() = read("favourites.json")?.let { AppJson.decodeFromString<Map<String, Recipe>>(it) }.orEmpty()
    override suspend fun writeFavourites(value: Map<String, Recipe>) = write("favourites.json", AppJson.encodeToString(value))
}
