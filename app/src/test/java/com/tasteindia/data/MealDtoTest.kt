package com.tasteindia.data

import com.tasteindia.domain.isIndianRecipe
import org.junit.Assert.*
import org.junit.Test

class MealDtoTest {
    @Test fun `fixture serializes and normalizes ordered ingredient pairs`() {
        val json = checkNotNull(javaClass.classLoader?.getResourceAsStream("fixtures/indian-meals.json"))
            .bufferedReader().use { it.readText() }
        val response = AppJson.decodeFromString<MealsResponse>(json)
        assertEquals(15, response.meals!!.size)
        val dto = response.meals.first { it.value("idMeal") == "52795" }
        val recipe = dto.recipe()!!

        assertEquals("Chicken Handi", recipe.name)
        assertEquals("Chicken", recipe.ingredients.first().name)
        assertEquals("1.2 kg", recipe.ingredients.first().measure)
        assertTrue(recipe.ingredients.none { it.name.isBlank() })
        assertEquals(16, recipe.ingredients.size)
        assertEquals("India", recipe.area)
    }

    @Test fun `country keeps authoritative India record when legacy area is missing`() {
        val recipe = MealDto(mapOf("idMeal" to "53580", "strMeal" to "Modak", "strCountry" to "India")).recipe()!!
        assertTrue(recipe.isIndianRecipe())
    }

    @Test fun `malformed links and blank values are omitted`() {
        val dto = MealDto(mapOf(
            "idMeal" to "10", "strMeal" to "Test", "strMealThumb" to "javascript:bad()",
            "strArea" to "India", "strIngredient1" to "  Rice  ", "strMeasure1" to " 2 cups ",
            "strIngredient2" to "  ", "strSource" to "file:///secret", "strYoutube" to "https://youtube.com/watch?v=test"
        ))
        val recipe = dto.recipe()!!
        assertNull(recipe.image)
        assertNull(recipe.source)
        assertNotNull(recipe.video)
        assertEquals(listOf("Rice"), recipe.ingredients.map { it.name })
    }
}
