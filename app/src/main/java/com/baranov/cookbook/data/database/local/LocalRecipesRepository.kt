package com.baranov.cookbook.data.database.local

import android.os.Build
import androidx.annotation.RequiresApi
import com.baranov.cookbook.data.database.local.dao.ProductDao
import com.baranov.cookbook.data.database.local.dao.RecipeDao
import com.baranov.cookbook.data.database.local.dao.RecipeProductDao
import com.baranov.cookbook.data.database.local.entity.LocalProductEntity
import com.baranov.cookbook.data.database.local.entity.LocalRecipeEntity
import com.baranov.cookbook.data.database.local.entity.LocalRecipeProductEntity
import com.baranov.cookbook.data.database.remote.ApiClient
import com.baranov.cookbook.data.database.remote.dto.CreateRecipeRequest
import com.baranov.cookbook.data.database.remote.dto.ProductInRecipeRequest
import com.baranov.cookbook.data.database.remote.dto.RecipeProductDto
import kotlinx.coroutines.flow.Flow
import java.time.Instant

class LocalRecipesRepository(
    private val recipeDao: RecipeDao,
    private val productDao: ProductDao,
    private val recipeProductDao: RecipeProductDao,
    private val apiClient: ApiClient = ApiClient
) {
    fun getRecipesForUser(ownerUserId: Int?): Flow<List<LocalRecipeEntity>> =
        recipeDao.getRecipesByOwner(ownerUserId)

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun createRecipeLocal(
        ownerUserId: Int?,
        authorId: Int?,
        title: String,
        description: String?,
        cookingInstructions: String,
        photo: String?,
        products: List<RecipeProductDto>
    ): Long {
        val now = Instant.now().toEpochMilli()
        val recipe = LocalRecipeEntity(
            ownerUserId = ownerUserId,
            authorId = authorId,
            title = title,
            description = description,
            cookingInstructions = cookingInstructions,
            photo = photo,
            createdAt = now,
            updatedAt = now,
            syncedAt = 0
        )
        val localId = recipeDao.insertRecipe(recipe)

        for (productDto in products) {
            val productLocalId = ensureLocalProduct(productDto.productId)
            recipeProductDao.insert(
                LocalRecipeProductEntity(
                    recipeLocalId = localId,
                    productLocalId = productLocalId,
                    quantity = productDto.quantity
                )
            )
        }
        return localId
    }

    suspend fun getRecipeWithProducts(localId: Long): LocalRecipeWithProducts? {
        val recipe = recipeDao.getRecipeByLocalId(localId) ?: return null
        val productLinks = recipeProductDao.getProductsForRecipe(localId)
        val products = productLinks.mapNotNull { link ->
            val product = productDao.getProductByLocalId(link.productLocalId)
            if (product != null) {
                RecipeProductDto(
                    productId = product.serverId,
                    quantity = link.quantity
                ) to product
            } else null
        }
        return LocalRecipeWithProducts(
            recipe = recipe,
            products = products.map { it.first },
            productEntities = products.map { it.second }
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun updateRecipe(
        localId: Long,
        title: String?,
        description: String?,
        cookingInstructions: String?,
        photo: String?,
        products: List<RecipeProductDto>?
    ) {
        val recipe = recipeDao.getRecipeByLocalId(localId) ?: return
        val updatedRecipe = recipe.copy(
            title = title ?: recipe.title,
            description = description ?: recipe.description,
            cookingInstructions = cookingInstructions ?: recipe.cookingInstructions,
            photo = photo ?: recipe.photo,
            updatedAt = Instant.now().toEpochMilli()
        )
        recipeDao.updateRecipe(updatedRecipe)

        if (products != null) {
            recipeProductDao.deleteByRecipeId(localId)
            for (productDto in products) {
                val productLocalId = ensureLocalProduct(productDto.productId)
                recipeProductDao.insert(
                    LocalRecipeProductEntity(
                        recipeLocalId = localId,
                        productLocalId = productLocalId,
                        quantity = productDto.quantity
                    )
                )
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun publishRecipe(localId: Long) {
        val recipe = recipeDao.getRecipeByLocalId(localId) ?: return
        if (recipe.serverId != null && recipe.serverId != -1) return

        val authorId = recipe.ownerUserId ?: return  // гостевой нельзя публиковать

        val products = recipeProductDao.getProductsForRecipe(localId).map { link ->
            val product = productDao.getProductByLocalId(link.productLocalId)!!
            ProductInRecipeRequest(
                productId = product.serverId,
                quantity = link.quantity
            )
        }
        val createdRecipe = apiClient.createRecipe(
            CreateRecipeRequest(
                authorId = authorId,
                title = recipe.title,
                description = recipe.description,
                cookingInstructions = recipe.cookingInstructions,
                photo = recipe.photo,
                products = products,
                accessibleUserIds = emptyList()
            )
        )
        if (createdRecipe != null) {
            val now = Instant.now().toEpochMilli()
            recipeDao.updateRecipe(
                recipe.copy(
                    serverId = createdRecipe.id,
                    authorId = createdRecipe.authorId,
                    photo = createdRecipe.photo,
                    syncedAt = now,
                    updatedAt = now
                )
            )
        }
    }

    /**
     * Скачать публичный рецепт с сервера в локалку для текущего пользователя.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun downloadPublicRecipe(serverRecipeId: Int, ownerUserId: Int): Long? {
        val server = apiClient.getRecipeById(serverRecipeId) ?: return null
        val existing = recipeDao.getRecipeByServerId(serverRecipeId, ownerUserId)
        if (existing != null) return existing.localId  // уже скачан

        val now = Instant.now().toEpochMilli()
        val createdAt = runCatching { Instant.parse(server.recipe.createdAt).toEpochMilli() }.getOrDefault(now)
        val updatedAt = runCatching { Instant.parse(server.recipe.updatedAt).toEpochMilli() }.getOrDefault(now)

        val localId = recipeDao.insertRecipe(
            LocalRecipeEntity(
                serverId = server.recipe.id,
                ownerUserId = ownerUserId,
                authorId = server.recipe.authorId,
                title = server.recipe.title,
                description = server.recipe.description,
                cookingInstructions = server.recipe.cookingInstructions,
                photo = server.recipe.photo,
                createdAt = createdAt,
                updatedAt = updatedAt,
                syncedAt = now
            )
        )
        for (productDto in server.products) {
            val productLocalId = ensureLocalProduct(productDto.productId)
            recipeProductDao.insert(
                LocalRecipeProductEntity(
                    recipeLocalId = localId,
                    productLocalId = productLocalId,
                    quantity = productDto.quantity
                )
            )
        }
        return localId
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun syncRecipe(localId: Long) {
        val recipe = recipeDao.getRecipeByLocalId(localId) ?: return
        val serverId = recipe.serverId ?: return
        val serverRecipe = apiClient.getRecipeById(serverId) ?: return
        val serverUpdatedAt = Instant.parse(serverRecipe.recipe.updatedAt).toEpochMilli()
        if (serverUpdatedAt > recipe.syncedAt) {
            val now = Instant.now().toEpochMilli()
            recipeDao.updateRecipe(
                recipe.copy(
                    title = serverRecipe.recipe.title,
                    description = serverRecipe.recipe.description,
                    cookingInstructions = serverRecipe.recipe.cookingInstructions,
                    photo = serverRecipe.recipe.photo,
                    authorId = serverRecipe.recipe.authorId,
                    updatedAt = serverUpdatedAt,
                    syncedAt = now
                )
            )
            recipeProductDao.deleteByRecipeId(localId)
            for (productDto in serverRecipe.products) {
                val productLocalId = ensureLocalProduct(productDto.productId)
                recipeProductDao.insert(
                    LocalRecipeProductEntity(
                        recipeLocalId = localId,
                        productLocalId = productLocalId,
                        quantity = productDto.quantity
                    )
                )
            }
        }
    }

    suspend fun deleteRecipe(localId: Long) {
        val recipe = recipeDao.getRecipeByLocalId(localId) ?: return
        recipeProductDao.deleteByRecipeId(localId)
        recipeDao.deleteRecipe(recipe)
    }

    /**
     * Перенести все гостевые рецепты на указанного пользователя.
     * Вызывается после логина, если пользователь согласился присвоить гостевые записи.
     */
    suspend fun claimGuestRecipes(newOwnerId: Int) {
        recipeDao.reassignGuestRecipes(newOwnerId)
    }

    /**
     * Гарантирует, что в local_products есть запись с нужным serverId,
     * и возвращает её localId. Если записи нет — создаёт пустую заглушку.
     * (TODO: дотягивать полную инфу с сервера, когда появится /products/{id}.)
     */
    private suspend fun ensureLocalProduct(serverProductId: Int): Long {
        val existing = productDao.getProductByServerId(serverProductId)
        if (existing != null) return existing.localId
        productDao.insertProduct(
            LocalProductEntity(
                serverId = serverProductId,
                name = "",
                measurementUnit = "",
                updatedAt = 0,
                syncedAt = 0
            )
        )
        // после insertIgnore запись точно есть — либо вставили, либо уже была
        return productDao.getProductByServerId(serverProductId)!!.localId
    }
}

data class LocalRecipeWithProducts(
    val recipe: LocalRecipeEntity,
    val products: List<RecipeProductDto>,
    val productEntities: List<LocalProductEntity>
)