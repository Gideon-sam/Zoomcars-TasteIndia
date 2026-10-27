package com.tasteindia.data

import android.content.res.AssetManager
import com.tasteindia.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.ConcurrentHashMap

enum class DataSource { LIVE, CACHE, REVIEW_FIXTURE }
data class MealCollection(
    val meals: List<MealSummary>,
    val details: Map<String, Recipe>,
    val source: DataSource,
    val notice: String? = null,
)

interface MealsRepository {
    val favourites: StateFlow<Map<String, Recipe>>
    suspend fun initialise()
    suspend fun loadCollection(forceNetwork: Boolean = false): MealCollection
    suspend fun recipe(id: String): Recipe
    suspend fun toggleFavourite(meal: MealSummary)
    suspend fun toggleFavourite(recipe: Recipe)
}

class DefaultMealsRepository(
    private val service: MealService,
    private val store: LocalStore,
    private val assets: AssetManager,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : MealsRepository {
    private val _favourites = MutableStateFlow<Map<String, Recipe>>(emptyMap())
    override val favourites = _favourites.asStateFlow()
    private val details = ConcurrentHashMap<String, Recipe>()
    private val inFlight = mutableMapOf<String, Deferred<Recipe>>()
    private val requestLock = Mutex()
    private val persistenceLock = Mutex()
    private val initLock = Mutex()
    private var initialised = false
    private val requestScope = CoroutineScope(SupervisorJob() + io)

    override suspend fun initialise() {
        initLock.withLock {
            if (initialised) return
            val saved = store.readFavourites()
            _favourites.value = saved
            details.putAll(saved)
            store.readCache()?.details?.let(details::putAll)
            initialised = true
        }
    }

    override suspend fun loadCollection(forceNetwork: Boolean): MealCollection = withContext(io) {
        if (!forceNetwork) store.readCache()?.takeIf { it.meals.isNotEmpty() }?.let { cached ->
            details.putAll(cached.details)
            // A cache is shown immediately, but each explicit retry always demonstrates the real GET.
            val source = if (cached.fromReviewFixture) DataSource.REVIEW_FIXTURE else DataSource.CACHE
            val notice = if (cached.fromReviewFixture)
                "Showing checked-in review data. Tap retry to refresh from the public service."
            else "Showing saved recipes. Tap retry to refresh."
            return@withContext MealCollection(cached.meals, cached.details, source, notice)
        }
        try {
            loadLive()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            val cached = store.readCache()?.takeIf { it.meals.isNotEmpty() }
            if (cached != null) {
                details.putAll(cached.details)
                MealCollection(cached.meals, cached.details, DataSource.CACHE, friendlyMessage(error))
            } else {
                val fixture = readFixture()
                if (fixture.meals.isEmpty()) throw error
                details.putAll(fixture.details)
                try {
                    store.writeCache(StoredCollection(fixture.meals, fixture.details, System.currentTimeMillis(), fromReviewFixture = true))
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Throwable) {
                    // The fixture remains usable in-memory even when the device is out of storage.
                }
                MealCollection(fixture.meals, fixture.details, DataSource.REVIEW_FIXTURE,
                    "The public service is unavailable. Showing checked-in review data; retry when online.")
            }
        }
    }

    private suspend fun loadLive(): MealCollection = coroutineScope {
        // The provider historically used "Indian"; its current V1 response uses "India".
        val legacyRequest = async { service.byArea("Indian").checkedBody().meals.orEmpty() }
        val currentRequest = async { service.byArea("India").checkedBody().meals.orEmpty() }
        val legacy = legacyRequest.await()
        val current = currentRequest.await()
        // During the provider's label migration both endpoints may return different subsets.
        val baseDtos = if (current.size >= legacy.size) current else legacy
        val base = baseDtos.mapNotNull(MealDto::summary).distinctBy { it.id }
        if (base.isEmpty()) throw ServiceException("No Indian recipes were returned. Your saved review data is still available.")
        val gate = Semaphore(4)
        val enriched = base.map { summary -> async { gate.withPermit {
            try { recipe(summary.id) } catch (error: CancellationException) { throw error } catch (_: Throwable) { null }
        } } }
            .awaitAll().filterNotNull().filter { it.isIndianRecipe() }
        // The IDs returned by the area endpoint are authoritative. Details only enrich them.
        val allowed = base.map { it.id }.toSet()
        val byId = enriched.filter { it.id in allowed }.associateBy { it.id }
        // A partial detail failure must not make a valid base meal disappear.
        val display = base.map { byId[it.id]?.summary() ?: it }
        val stored = StoredCollection(display, byId, System.currentTimeMillis())
        store.writeCache(stored)
        MealCollection(display, byId, DataSource.LIVE)
    }

    override suspend fun recipe(id: String): Recipe {
        require(id.matches(Regex("[0-9]{1,20}"))) { "Invalid recipe ID" }
        details[id]?.let { return it }
        _favourites.value[id]?.let { return it }
        val deferred = requestLock.withLock {
            inFlight[id] ?: requestScope.async {
                val value = service.lookup(id).checkedBody().meals.orEmpty().firstOrNull()?.recipe()
                    ?: throw ServiceException("This recipe is no longer available.")
                if (!value.isIndianRecipe()) throw ServiceException("This recipe is outside the TasteIndia collection.")
                details[id] = value
                value
            }.also { inFlight[id] = it }
        }
        return try { deferred.await() } finally { requestLock.withLock { if (inFlight[id] === deferred) inFlight.remove(id) } }
    }

    override suspend fun toggleFavourite(meal: MealSummary) {
        val snapshot = details[meal.id] ?: Recipe(meal.id, meal.name, meal.image, meal.category, "India", emptyList(), null)
        toggleFavourite(snapshot)
    }

    override suspend fun toggleFavourite(recipe: Recipe) = persistenceLock.withLock {
        val next = _favourites.value.toMutableMap().apply {
            if (containsKey(recipe.id)) remove(recipe.id) else put(recipe.id, recipe)
        }
        store.writeFavourites(next)
        _favourites.value = next
    }

    private fun readFixture(): MealCollection {
        val json = assets.open("fixtures/indian-meals.json").bufferedReader().use { it.readText() }
        val recipes = AppJson.decodeFromString<MealsResponse>(json).meals.orEmpty().mapNotNull(MealDto::recipe)
            .filter { it.isIndianRecipe() }
        return MealCollection(recipes.map(Recipe::summary), recipes.associateBy(Recipe::id), DataSource.REVIEW_FIXTURE)
    }

    private fun friendlyMessage(error: Throwable): String = when (error) {
        is SocketTimeoutException -> "The recipe service timed out. Showing saved recipes."
        is IOException -> "You appear to be offline. Showing saved recipes."
        else -> error.message ?: "Recipes could not be refreshed. Showing saved recipes."
    }
}
