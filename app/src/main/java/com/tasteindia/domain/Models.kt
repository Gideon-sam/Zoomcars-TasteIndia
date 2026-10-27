package com.tasteindia.domain

import kotlinx.serialization.Serializable
import java.net.URI
import java.util.Locale

@Serializable
data class Ingredient(val name: String, val measure: String)

@Serializable
data class MealSummary(val id: String, val name: String, val image: String?, val category: String? = null)

@Serializable
data class Recipe(
    val id: String,
    val name: String,
    val image: String?,
    val category: String?,
    val area: String?,
    val ingredients: List<Ingredient>,
    val instructions: String?,
    val tags: List<String> = emptyList(),
    val source: String? = null,
    val video: String? = null,
    val extras: Map<String, String> = emptyMap(),
) {
    fun summary() = MealSummary(id, name, image, category)
    val mainIngredient: String? get() = ingredients.firstOrNull()?.name
}

enum class SortOrder { AZ, ZA }

data class RecipeFilters(
    val query: String = "",
    val category: String? = null,
    val ingredient: String? = null,
    val favouritesOnly: Boolean = false,
    val sort: SortOrder = SortOrder.AZ,
) {
    val activeCount: Int get() = listOf(query.isNotBlank(), category != null, ingredient != null, favouritesOnly, sort != SortOrder.AZ).count { it }
}

/** Every criterion is intersected with the authoritative area IDs, never unioned. */
fun filterMeals(
    indianMeals: List<MealSummary>,
    details: Map<String, Recipe>,
    favourites: Set<String>,
    filters: RecipeFilters,
): List<MealSummary> {
    val categoryIds = filters.category?.let { chosen -> details.values.filter { it.category.equals(chosen, true) }.map { it.id }.toSet() }
    val ingredientIds = filters.ingredient?.let { chosen -> details.values.filter { it.mainIngredient.equals(chosen, true) }.map { it.id }.toSet() }
    val result = indianMeals.distinctBy { it.id }.filter { meal ->
        meal.name.contains(filters.query.trim(), ignoreCase = true) &&
            (categoryIds == null || meal.id in categoryIds) &&
            (ingredientIds == null || meal.id in ingredientIds) &&
            (!filters.favouritesOnly || meal.id in favourites)
    }.sortedBy { it.name.lowercase(Locale.ROOT) }
    return if (filters.sort == SortOrder.ZA) result.reversed() else result
}

fun safeWebUrl(value: String?): String? = value?.trim()?.takeIf { text ->
    runCatching {
        val uri = URI(text)
        uri.scheme?.lowercase(Locale.ROOT) in setOf("https", "http") &&
            !uri.host.isNullOrBlank() && uri.userInfo == null
    }.getOrDefault(false)
}

fun isIndianArea(area: String?) = area.equals("Indian", true) || area.equals("India", true)

fun Recipe.isIndianRecipe() = isIndianArea(area) || extras["Country"].equals("India", true)
