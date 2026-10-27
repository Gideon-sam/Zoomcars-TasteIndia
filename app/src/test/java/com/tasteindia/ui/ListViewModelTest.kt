package com.tasteindia.ui

import androidx.lifecycle.SavedStateHandle
import com.tasteindia.data.*
import com.tasteindia.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class ListViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun `rapid queries expose only latest matching list`() = runTest(dispatcher.scheduler) {
        val vm = ListViewModel(FakeRepository(), SavedStateHandle(), false)
        val states = mutableListOf<ListUiState>()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.toList(states) }
        advanceUntilIdle()
        vm.setQuery("Chicken")
        advanceTimeBy(100)
        vm.setQuery("Lamb")
        advanceUntilIdle()
        assertEquals(listOf("Lamb Biryani"), vm.uiState.value.meals.map { it.name })
        job.cancel()
    }

    @Test fun `saved filter state is restored when destination is recreated`() = runTest(dispatcher.scheduler) {
        val saved = SavedStateHandle(mapOf("query" to "Baingan", "sort" to "ZA", "category" to "Vegetarian"))
        val vm = ListViewModel(FakeRepository(), saved, false)
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
        advanceUntilIdle()
        assertEquals("Baingan", vm.uiState.value.filters.query)
        assertEquals(SortOrder.ZA, vm.uiState.value.filters.sort)
        assertEquals(listOf("Baingan Bharta"), vm.uiState.value.meals.map { it.name })
        job.cancel()
    }
}

private class FakeRepository : MealsRepository {
    private val recipes = listOf(
        Recipe("1", "Chicken Handi", null, "Chicken", "India", listOf(Ingredient("Chicken", "1")), "Cook"),
        Recipe("2", "Lamb Biryani", null, "Lamb", "India", listOf(Ingredient("Lamb", "1")), "Cook"),
        Recipe("3", "Baingan Bharta", null, "Vegetarian", "India", listOf(Ingredient("Aubergine", "1")), "Cook"),
    )
    override val favourites = MutableStateFlow<Map<String, Recipe>>(emptyMap())
    override suspend fun initialise() = Unit
    override suspend fun loadCollection(forceNetwork: Boolean) = MealCollection(recipes.map(Recipe::summary), recipes.associateBy(Recipe::id), DataSource.LIVE)
    override suspend fun recipe(id: String) = recipes.first { it.id == id }
    override suspend fun toggleFavourite(meal: MealSummary) { favourites.value = favourites.value + (meal.id to recipe(meal.id)) }
    override suspend fun toggleFavourite(recipe: Recipe) { favourites.value = favourites.value + (recipe.id to recipe) }
}
