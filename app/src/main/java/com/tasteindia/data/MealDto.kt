package com.tasteindia.data

import com.tasteindia.domain.*
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.*

@Serializable
data class MealsResponse(val meals: List<MealDto>? = null)

/** Preserve new V1 fields instead of silently discarding meaningful future additions. */
@Serializable(with = MealDtoSerializer::class)
data class MealDto(val fields: Map<String, String?>) {
    fun value(key: String): String? = fields[key]?.trim()?.takeIf { it.isNotEmpty() && !it.equals("null", true) }

    fun summary(): MealSummary? {
        val id = value("idMeal") ?: return null
        val name = value("strMeal") ?: return null
        return MealSummary(id, name, safeWebUrl(value("strMealThumb")), value("strCategory"))
    }

    fun recipe(): Recipe? {
        val summary = summary() ?: return null
        val ingredients = (1..20).mapNotNull { i ->
            value("strIngredient$i")?.let { Ingredient(it, value("strMeasure$i").orEmpty()) }
        }
        val mapped = setOf("idMeal", "strMeal", "strMealThumb", "strCategory", "strArea", "strInstructions", "strTags", "strSource", "strYoutube")
        val extras = fields.keys.filter { it !in mapped && !it.matches(Regex("str(?:Ingredient|Measure)\\d+")) }
            .mapNotNull { key -> value(key)?.let { fieldLabel(key) to it } }.toMap()
        return Recipe(summary.id, summary.name, summary.image, summary.category, value("strArea"), ingredients,
            value("strInstructions")?.replace("\r\n", "\n"),
            value("strTags")?.split(',')?.map(String::trim)?.filter(String::isNotEmpty)?.distinct().orEmpty(),
            safeWebUrl(value("strSource")), safeWebUrl(value("strYoutube")), extras)
    }
}

fun fieldLabel(key: String): String = when (key) {
    "strMealAlternate" -> "Also known as"
    "strDrinkAlternate" -> "Suggested drink"
    "strImageSource" -> "Image source"
    "strCreativeCommonsConfirmed" -> "Creative Commons confirmed"
    "dateModified" -> "Last updated"
    "strCountry" -> "Country"
    else -> key.removePrefix("str").replace(Regex("([a-z])([A-Z])"), "$1 $2").replaceFirstChar(Char::uppercase)
}

object MealDtoSerializer : KSerializer<MealDto> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("MealDto")
    override fun deserialize(decoder: Decoder): MealDto {
        val obj = (decoder as JsonDecoder).decodeJsonElement().jsonObject
        return MealDto(obj.mapValues { (_, value) -> (value as? JsonPrimitive)?.contentOrNull })
    }
    override fun serialize(encoder: Encoder, value: MealDto) {
        (encoder as JsonEncoder).encodeJsonElement(buildJsonObject { value.fields.forEach { (k, v) -> put(k, v?.let(::JsonPrimitive) ?: JsonNull) } })
    }
}
