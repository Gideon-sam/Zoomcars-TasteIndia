package com.tasteindia.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.RestaurantMenu
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.tasteindia.data.MealsRepository

private enum class MainDestination(val route: String, val label: String, val icon: ImageVector, val selected: ImageVector) {
    RECIPES("recipes", "Recipes", Icons.Outlined.RestaurantMenu, Icons.Outlined.RestaurantMenu),
    FAVOURITES("favourites", "Favourites", Icons.Outlined.FavoriteBorder, Icons.Rounded.Favorite),
}

private object Routes {
    const val DETAILS = "details/{mealId}"
    fun details(mealId: String) = "details/$mealId"
}

@Composable
fun TasteIndiaApp(repository: MealsRepository) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val current = entry?.destination?.route
    val showNav = current in MainDestination.entries.map { it.route }
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showNav) NavigationBar {
                MainDestination.entries.forEach { destination ->
                    val selected = current == destination.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            nav.navigate(destination.route) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(if (selected) destination.selected else destination.icon, null) },
                        label = { Text(destination.label) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = MainDestination.RECIPES.route) {
            composable(MainDestination.RECIPES.route) {
                val vm: ListViewModel = viewModel(factory = listViewModelFactory(repository, false))
                val state by vm.uiState.collectAsStateWithLifecycle()
                RecipeListScreen(state, padding, "Recipes", "Stories from Indian kitchens", vm::setQuery, vm::apply,
                    vm::clearAll, vm::retry, vm::dismissNotice, vm::toggle) { nav.navigate(Routes.details(it)) }
            }
            composable(MainDestination.FAVOURITES.route) {
                val vm: ListViewModel = viewModel(factory = listViewModelFactory(repository, true))
                val state by vm.uiState.collectAsStateWithLifecycle()
                RecipeListScreen(state, padding, "Your favourites", "Saved for the next feast", vm::setQuery, vm::apply,
                    vm::clearAll, vm::retry, vm::dismissNotice, vm::toggle) { nav.navigate(Routes.details(it)) }
            }
            composable(Routes.DETAILS) {
                val vm: DetailsViewModel = viewModel(factory = detailsViewModelFactory(repository))
                val state by vm.uiState.collectAsStateWithLifecycle()
                RecipeDetailsScreen(state, onBack = nav::navigateUp, onRetry = vm::retry, onToggle = vm::toggleFavourite)
            }
        }
    }
}
