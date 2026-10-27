package com.tasteindia.ui

import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.CreationExtras
import com.tasteindia.data.MealsRepository
import com.tasteindia.domain.Recipe
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class DetailsUiState(
    val loading: Boolean = true,
    val recipe: Recipe? = null,
    val favourite: Boolean = false,
    val error: String? = null,
)

class DetailsViewModel(
    private val repository: MealsRepository,
    savedState: SavedStateHandle,
) : ViewModel() {
    private val mealId: String = checkNotNull(savedState["mealId"])
    private val recipe = MutableStateFlow<Recipe?>(null)
    private val loading = MutableStateFlow(true)
    private val error = MutableStateFlow<String?>(null)

    val uiState = combine(recipe, loading, error, repository.favourites) { item, busy, failure, saved ->
        DetailsUiState(busy, item, mealId in saved, failure)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailsUiState())

    init { load() }
    fun retry() = load()
    fun toggleFavourite() { recipe.value?.let { viewModelScope.launch { repository.toggleFavourite(it) } } }

    private fun load() {
        viewModelScope.launch {
            loading.value = true; error.value = null
            try {
                recipe.value = repository.recipe(mealId)
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (failure: Throwable) {
                error.value = failure.message ?: "This recipe could not be loaded."
            } finally {
                loading.value = false
            }
        }
    }
}

fun detailsViewModelFactory(repository: MealsRepository) = object : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
        DetailsViewModel(repository, extras.createSavedStateHandle()) as T
}
