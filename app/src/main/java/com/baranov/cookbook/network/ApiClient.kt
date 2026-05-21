package com.baranov.cookbook.network

import com.baranov.cookbook.network.dto.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.security.MessageDigest

object ApiClient {
    // Замените на актуальный IP вашего сервера
    private const val BASE_URL = "http://192.168.1.3:8080/"

    val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        install(Logging) {
            level = LogLevel.BODY
            logger = object : Logger {
                override fun log(message: String) {
                    println("Ktor: $message")
                }
            }
        }
    }

    // ============================
    //   Users
    // ============================
    suspend fun createUser(request: CreateUserRequest): Int? {
        val response = client.post("${BASE_URL}users") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        return if (response.status.isSuccess()) {
            response.body<Map<String, Int>>()["id"]
        } else null
    }

    suspend fun getAllUsers(): List<UserDto> =
        client.get("${BASE_URL}users").body()

    suspend fun getUserById(id: Int): UserDto? {
        return try {
            client.get("${BASE_URL}users/$id").body()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getUserByEmail(email: String): UserDto? {
        return try {
            client.get("${BASE_URL}users/by-email") {
                parameter("email", email)
            }.body()
        } catch (e: Exception) {
            null
        }
    }

    // ============================
    //   Products
    // ============================
    suspend fun createProduct(request: CreateProductRequest): Int? {
        val response = client.post("${BASE_URL}products") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        return if (response.status.isSuccess()) {
            response.body<Map<String, Int>>()["id"]
        } else null
    }

    suspend fun getAllProducts(): List<ProductDto> =
        client.get("${BASE_URL}products").body()

    // ============================
    //   Recipes
    // ============================
    suspend fun getAllRecipes(): List<RecipeDto> =
        client.get("${BASE_URL}recipes").body()

    suspend fun getRecipeById(id: Int): RecipeWithDetailsDto? {
        return try {
            client.get("${BASE_URL}recipes/$id").body()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun createRecipe(request: CreateRecipeRequest): Int? {
        val response = client.post("${BASE_URL}recipes") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        return if (response.status.isSuccess()) {
            response.body<Map<String, Int>>()["id"]
        } else null
    }

    suspend fun addProductToRecipe(recipeId: Int, request: AddProductToRecipeRequest): Boolean {
        val response = client.post("${BASE_URL}recipes/$recipeId/products") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        return response.status.isSuccess()
    }

    // ============================
    //   Utility
    // ============================
    fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(password.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}