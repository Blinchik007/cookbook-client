package com.baranov.cookbook.data.database.remote

import com.baranov.cookbook.CurrentUser
import com.baranov.cookbook.data.database.remote.dto.AddProductToRecipeRequest
import com.baranov.cookbook.data.database.remote.dto.ChangePasswordRequest
import com.baranov.cookbook.data.database.remote.dto.CreateProductRequest
import com.baranov.cookbook.data.database.remote.dto.CreateRecipeRequest
import com.baranov.cookbook.data.database.remote.dto.CreateUserRequest
import com.baranov.cookbook.data.database.remote.dto.LoginRequest
import com.baranov.cookbook.data.database.remote.dto.ProductDto
import com.baranov.cookbook.data.database.remote.dto.RecipeDto
import com.baranov.cookbook.data.database.remote.dto.RecipeWithDetailsDto
import com.baranov.cookbook.data.database.remote.dto.UpdateUserRequest
import com.baranov.cookbook.data.database.remote.dto.UserProfileResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import io.ktor.client.plugins.HttpTimeout

object ApiClient {
    private const val BASE_URL = "http://192.168.1.3:8080/"

    val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
            connectTimeoutMillis = 5_000
            socketTimeoutMillis = 15_000
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

    suspend fun register(name: String, email: String, password: String, photo: String?): CurrentUser {
        val response = client.post("${BASE_URL}users") {
            contentType(ContentType.Application.Json)
            setBody(CreateUserRequest(name, email, password, photo))
        }
        if (response.status.isSuccess()) {
            val profile = response.body<UserProfileResponse>()
            return CurrentUser(profile.id, profile.name, profile.email, profile.photo)
        } else {
            val errorMsg = try { response.bodyAsText() } catch (e: Exception) { "Ошибка регистрации" }
            throw Exception(errorMsg)
        }
    }

    suspend fun login(email: String, password: String): CurrentUser {
        val response = client.post("${BASE_URL}users/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(email, password))
        }
        if (response.status.isSuccess()) {
            val profile = response.body<UserProfileResponse>()
            return CurrentUser(profile.id, profile.name, profile.email, profile.photo)
        } else {
            val errorMsg = try { response.bodyAsText() } catch (e: Exception) { "Неверный email или пароль" }
            throw Exception(errorMsg)
        }
    }

    suspend fun getAllUsers(): List<UserProfileResponse> =
        client.get("${BASE_URL}users").body()

    suspend fun getUserById(id: Int): UserProfileResponse? {
        return try {
            client.get("${BASE_URL}users/$id").body()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getUserByEmail(email: String): UserProfileResponse? {
        return try {
            client.get("${BASE_URL}users/by-email") {
                parameter("email", email)
            }.body()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun createProduct(request: CreateProductRequest): ProductDto? {
        val response = client.post("${BASE_URL}products") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        return if (response.status.isSuccess()) {
            response.body<ProductDto>()
        } else null
    }

    suspend fun getAllProducts(): List<ProductDto> =
        client.get("${BASE_URL}products").body()

    /**
     * Получить рецепты с сервера.
     * @param search подстрока в названии рецепта.
     * @param author подстрока в имени автора.
     * Оба null — все рецепты.
     */
    suspend fun getAllRecipes(search: String? = null, author: String? = null): List<RecipeDto> =
        client.get("${BASE_URL}recipes") {
            if (!search.isNullOrBlank()) parameter("search", search)
            if (!author.isNullOrBlank()) parameter("author", author)
        }.body()

    suspend fun getRecipeById(id: Int): RecipeWithDetailsDto? {
        return try {
            client.get("${BASE_URL}recipes/$id").body()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun createRecipe(request: CreateRecipeRequest): RecipeDto? {
        val response = client.post("${BASE_URL}recipes") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        return if (response.status.isSuccess()) {
            response.body<RecipeDto>()
        } else null
    }

    suspend fun addProductToRecipe(recipeId: Int, request: AddProductToRecipeRequest): Boolean {
        val response = client.post("${BASE_URL}recipes/$recipeId/products") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        return response.status.isSuccess()
    }

    suspend fun updateUser(userId: Int, request: UpdateUserRequest): CurrentUser? {
        return try {
            val response = client.put("${BASE_URL}users/$userId") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            if (response.status.isSuccess()) {
                val profile = response.body<UserProfileResponse>()
                CurrentUser(profile.id, profile.name, profile.email, profile.photo)
            } else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun changePassword(userId: Int, oldPassword: String, newPassword: String): Boolean {
        return try {
            val response = client.post("${BASE_URL}users/$userId/change-password") {
                contentType(ContentType.Application.Json)
                setBody(ChangePasswordRequest(oldPassword, newPassword))
            }
            response.status.isSuccess()
        } catch (e: Exception) {
            false
        }
    }
}