package com.tasteindia.ui

import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.CreationExtras
import com.tasteindia.data.*
import com.tasteindia.domain.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ListUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val meals: List<MealSummary> = emptyList(),
    val allMeals: List<MealSummary> = emptyList(),
    val details: Map<String, Recipe> = emptyMap(),
    val favourites: Set<String> = emptySet(),
    val filters: RecipeFilters = RecipeFilters(),
    val categories: List<String> = emptyList(),
    val ingredients: List<String> = emptyList(),
    val source: DataSource? = null,
    val notice: String? = null,
    val error: String? = null,
    val favouritesDestination: Boolean = false,
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class ListViewModel(
    private val repository: MealsRepository,
    private val savedState: SavedStateHandle,
    private val destinationFavourites: Boolean,
) : ViewModel() {
    private val collection = MutableStateFlow<MealCollection?>(null)
    private val loading = MutableStateFlow(true)
    private val refreshing = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)
    private val filters = MutableStateFlow(
        RecipeFilters(
            query = savedState["query"] ?: "",
            category = savedState["category"],
            ingredient = savedState["ingredient"],
            favouritesOnly = if (destinationFavourites) false else (savedState["favouritesOnly"] ?: false),
            sort = runCatching { SortOrder.valueOf(savedState["sort"] ?: "AZ") }.getOrDefault(SortOrder.AZ),
        )
    )

    private val appliedFilters = filters.debounce(250).distinctUntilChanged()
    private val loadState = combine(loading, refreshing, error) { initial, refresh, failure -> Triple(initial, refresh, failure) }

    val uiState: StateFlow<ListUiState> = combine(
        collection, repository.favourites, appliedFilters, loadState
    ) { loaded, saved, active, progress ->
        val (isLoading, isRefreshing, failure) = progress
        val all = if (destinationFavourites) saved.values.map(Recipe::summary) else loaded?.meals.orEmpty()
        val knownDetails = loaded?.details.orEmpty() + saved
        val effective = if (destinationFavourites) active.copy(favouritesOnly = false) else active
        ListUiState(
            loading = isLoading,
            refreshing = isRefreshing,
            meals = filterMeals(all, knownDetails, saved.keys, effective),
            allMeals = all,
            details = knownDetails,
            favourites = saved.keys,
            filters = effective,
            categories = knownDetails.values.mapNotNull { it.category }.distinct().sorted(),
            ingredients = knownDetails.values.mapNotNull { it.mainIngredient }.distinct().sorted(),
            source = loaded?.source,
            notice = loaded?.notice,
            error = failure,
            favouritesDestination = destinationFavourites,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ListUiState())

    init {
        viewModelScope.launch {
            repository.initialise()
            if (destinationFavourites) {
                collection.value = MealCollection(repository.favourites.value.values.map(Recipe::summary), repository.favourites.value, DataSource.CACHE)
                loading.value = false
            } else load(false)
        }
    }

    fun setQuery(value: String) = update(filters.value.copy(query = value))
    fun apply(category: String?, ingredient: String?, favouritesOnly: Boolean, sort: SortOrder) =
        update(filters.value.copy(category = category, ingredient = ingredient, favouritesOnly = !destinationFavourites && favouritesOnly, sort = sort))
    fun clearAll() = update(RecipeFilters())
    fun retry() { viewModelScope.launch { load(true) } }
    fun dismissNotice() { collection.update { it?.copy(notice = null) } }
    fun toggle(meal: MealSummary) { viewModelScope.launch { repository.toggleFavourite(meal) } }

    private fun update(value: RecipeFilters) {
        filters.value = value
        savedState["query"] = value.query
        savedState["category"] = value.category
        savedState["ingredient"] = value.ingredient
        savedState["favouritesOnly"] = value.favouritesOnly
        savedState["sort"] = value.sort.name
    }

    private suspend fun load(force: Boolean) {
        error.value = null
        if (collection.value == null) loading.value = true else refreshing.value = true
        try {
            collection.value = repository.loadCollection(force)
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (failure: Throwable) {
            error.value = failure.message ?: "Recipes could not be loaded. Check your connection and retry."
        } finally {
            loading.value = false
            refreshing.value = false
        }
    }
}

fun listViewModelFactory(repository: MealsRepository, favourites: Boolean) = object : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
        ListViewModel(repository, extras.createSavedStateHandle(), favourites) as T
}
