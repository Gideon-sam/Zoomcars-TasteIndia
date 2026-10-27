package com.tasteindia.data

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit

interface MealService {
    @GET("filter.php") suspend fun byArea(@Query("a") area: String): Response<MealsResponse>
    @GET("lookup.php") suspend fun lookup(@Query("i") id: String): Response<MealsResponse>
    @GET("list.php") suspend fun areas(@Query("a") list: String = "list"): Response<MealsResponse>
}

val AppJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

fun createMealService(baseUrl: String = "https://www.themealdb.com/api/json/v1/1/"): MealService {
    val client = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS).callTimeout(25, TimeUnit.SECONDS)
        .retryOnConnectionFailure(false).build()
    return Retrofit.Builder().baseUrl(baseUrl).client(client)
        .addConverterFactory(AppJson.asConverterFactory("application/json".toMediaType())).build().create(MealService::class.java)
}

fun <T> Response<T>.checkedBody(): T {
    if (!isSuccessful) throw ServiceException(if (code() == 429) "The recipe service is busy. Please try again shortly." else "The recipe service returned HTTP ${code()}. Try again.")
    return body() ?: throw ServiceException("The recipe service sent an empty response.")
}

class ServiceException(message: String) : Exception(message)
