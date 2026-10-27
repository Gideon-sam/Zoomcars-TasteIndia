package com.tasteindia.data

import com.tasteindia.domain.Recipe
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class JsonFileStoreTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun `favourite snapshot survives store recreation without network`() = runTest {
        val directory = temporary.newFolder("tasteindia")
        val recipe = Recipe("42", "Saved meal", null, "Vegetarian", "India", emptyList(), "Cook it")
        JsonFileStore(directory).writeFavourites(mapOf(recipe.id to recipe))

        val restored = JsonFileStore(directory).readFavourites()
        assertEquals(recipe, restored[recipe.id])
    }
}
