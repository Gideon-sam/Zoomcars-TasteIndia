package com.tasteindia.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class FilterMealsTest {
    private val indian = listOf(
        MealSummary("1", "Aloo Test", null),
        MealSummary("2", "Biryani Test", null),
    )
    private val details = mapOf(
        "1" to Recipe("1", "Aloo Test", null, "Vegetarian", "India", listOf(Ingredient("Potato", "2")), null),
        "2" to Recipe("2", "Biryani Test", null, "Lamb", "India", listOf(Ingredient("Lamb", "500 g")), null),
        "999" to Recipe("999", "Outside", null, "Lamb", "British", listOf(Ingredient("Lamb", "1")), null),
    )

    @Test fun `combined filters intersect IDs and retain Indian base boundary`() {
        val result = filterMeals(indian, details, setOf("2", "999"), RecipeFilters(category = "Lamb", ingredient = "Lamb", favouritesOnly = true))
        assertEquals(listOf("2"), result.map { it.id })
    }

    @Test fun `sorting uses names and never list positions as identity`() {
        assertEquals(listOf("2", "1"), filterMeals(indian, details, emptySet(), RecipeFilters(sort = SortOrder.ZA)).map { it.id })
    }
}
