package com.baranov.cookbook.screens.recipeEditor

import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Base64
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.baranov.cookbook.AppContainer
import com.baranov.cookbook.CurrentUserHolder
import com.baranov.cookbook.data.database.local.entity.LocalProductEntity
import com.baranov.cookbook.data.database.remote.ApiClient
import com.baranov.cookbook.data.database.remote.dto.CreateProductRequest
import com.baranov.cookbook.data.database.remote.dto.RecipeProductDto
import com.baranov.cookbook.ui.components.CreateProductBottomSheet
import com.baranov.cookbook.ui.components.IngredientEditorRow
import com.baranov.cookbook.ui.components.IngredientRow
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeEditorScreen(
    recipeLocalId: Long,
    onFinish: () -> Unit
) {
    val repository = AppContainer.repository
    val isNew = recipeLocalId == -1L
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var cookingInstructions by remember { mutableStateOf("") }
    var photoBase64 by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // Ингредиенты
    val ingredients = remember { mutableStateListOf<RecipeIngredient>() }
    var pending by remember { mutableStateOf<PendingIngredient?>(null) }
    var suggestions by remember { mutableStateOf<List<LocalProductEntity>>(emptyList()) }

    // BottomSheet создания продукта
    var showCreateProductSheet by remember { mutableStateOf(false) }
    var createProductError by remember { mutableStateOf<String?>(null) }
    var isCreatingProduct by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            photoBase64 = uriToBase64(context, it)
            Log.d("PhotoDebug", "Base64 length: ${photoBase64?.length}, starts with: ${photoBase64?.take(30)}")
        }
    }

    // Синк продуктов при открытии редактора (если кеш старше часа)
    LaunchedEffect(Unit) {
        repository.syncProductsFromServer()
    }

    // Загрузка существующего рецепта
    LaunchedEffect(recipeLocalId) {
        if (!isNew) {
            val recipeWithProducts = repository.getRecipeWithProducts(recipeLocalId)
            if (recipeWithProducts != null) {
                val r = recipeWithProducts.recipe
                title = r.title
                description = r.description ?: ""
                cookingInstructions = r.cookingInstructions
                photoBase64 = r.photo

                ingredients.clear()
                recipeWithProducts.products.forEachIndexed { idx, dto ->
                    val productEntity = recipeWithProducts.productEntities[idx]
                    ingredients.add(
                        RecipeIngredient(
                            productLocalId = productEntity.localId,
                            productServerId = productEntity.serverId,
                            productName = productEntity.name,
                            measurementUnit = productEntity.measurementUnit,
                            quantity = dto.quantity
                        )
                    )
                }
            }
        }
    }

    // Обновление подсказок при изменении query
    LaunchedEffect(pending?.nameQuery, pending?.selectedProductLocalId) {
        val p = pending
        if (p != null && p.selectedProductLocalId == null && p.nameQuery.isNotBlank()) {
            val usedIds = ingredients
                .filterIndexed { idx, _ -> idx != p.editingIndex }
                .map { it.productLocalId }
                .toSet()
            suggestions = repository.searchLocalProducts(p.nameQuery)
                .filter { it.localId !in usedIds }
        } else {
            suggestions = emptyList()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isNew) "Новый рецепт" else "Редактирование") },
                navigationIcon = {
                    IconButton(onClick = onFinish) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = "Выбрать фото")
                    }
                    IconButton(
                        onClick = {
                            if (title.isNotBlank()) {
                                scope.launch {
                                    isLoading = true
                                    try {
                                        val productsForDb = ingredients.map {
                                            RecipeProductDto(
                                                productId = it.productServerId,
                                                quantity = it.quantity
                                            )
                                        }
                                        if (isNew) {
                                            val currentUserId = CurrentUserHolder.currentUser?.id
                                            repository.createRecipeLocal(
                                                ownerUserId = currentUserId,
                                                authorId = currentUserId,
                                                title = title,
                                                description = description.ifBlank { null },
                                                cookingInstructions = cookingInstructions,
                                                photo = photoBase64,
                                                products = productsForDb
                                            )
                                        } else {
                                            repository.updateRecipe(
                                                localId = recipeLocalId,
                                                title = title,
                                                description = description.ifBlank { null },
                                                cookingInstructions = cookingInstructions,
                                                photo = photoBase64,
                                                products = productsForDb
                                            )
                                        }
                                        onFinish()
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            }
                        },
                        enabled = !isLoading && pending == null
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Сохранить")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!photoBase64.isNullOrBlank()) {
                val photoBytes = remember(photoBase64) {
                    runCatching { Base64.decode(photoBase64, Base64.DEFAULT) }
                        .getOrNull()
                }
                if (photoBytes != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(photoBytes).crossfade(true).build(),
                        contentDescription = "Фото рецепта",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                    )
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Название") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Краткое описание") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            // === СЕКЦИЯ ИНГРЕДИЕНТОВ ===
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Ингредиенты",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )

                ingredients.forEachIndexed { index, ingredient ->
                    val isBeingEdited = pending?.editingIndex == index
                    if (!isBeingEdited) {
                        IngredientRow(
                            name = ingredient.productName,
                            measurementUnit = ingredient.measurementUnit,
                            quantity = ingredient.quantity,
                            onEdit = {
                                if (pending == null) {
                                    pending = PendingIngredient(
                                        editingIndex = index,
                                        nameQuery = ingredient.productName,
                                        selectedProductLocalId = ingredient.productLocalId,
                                        selectedProductServerId = ingredient.productServerId,
                                        selectedProductName = ingredient.productName,
                                        selectedMeasurementUnit = ingredient.measurementUnit,
                                        quantityText = formatQuantityForEdit(ingredient.quantity)
                                    )
                                }
                            },
                            onRemove = { ingredients.removeAt(index) }
                        )
                    }
                }

                val p = pending
                if (p != null) {
                    IngredientEditorRow(
                        query = p.nameQuery,
                        onQueryChange = { newQuery ->
                            pending = p.copy(
                                nameQuery = newQuery,
                                selectedProductLocalId = null,
                                selectedProductServerId = null,
                                selectedProductName = null,
                                selectedMeasurementUnit = null,
                                quantityText = ""
                            )
                        },
                        suggestions = suggestions,
                        selectedProductName = p.selectedProductName,
                        selectedMeasurementUnit = p.selectedMeasurementUnit,
                        quantityText = p.quantityText,
                        onQuantityChange = { pending = p.copy(quantityText = it) },
                        canConfirm = p.isReady,
                        onSelectSuggestion = { product ->
                            pending = p.copy(
                                nameQuery = product.name,
                                selectedProductLocalId = product.localId,
                                selectedProductServerId = product.serverId,
                                selectedProductName = product.name,
                                selectedMeasurementUnit = product.measurementUnit
                            )
                        },
                        onCreateNew = {
                            showCreateProductSheet = true
                            createProductError = null
                        },
                        onConfirm = {
                            val quantity = p.quantityText.toDoubleOrNull()
                            if (quantity != null && quantity > 0 &&
                                p.selectedProductLocalId != null &&
                                p.selectedProductServerId != null &&
                                p.selectedProductName != null &&
                                p.selectedMeasurementUnit != null) {
                                val newIngredient = RecipeIngredient(
                                    productLocalId = p.selectedProductLocalId,
                                    productServerId = p.selectedProductServerId,
                                    productName = p.selectedProductName,
                                    measurementUnit = p.selectedMeasurementUnit,
                                    quantity = quantity
                                )
                                if (p.editingIndex >= 0) {
                                    ingredients[p.editingIndex] = newIngredient
                                } else {
                                    ingredients.add(newIngredient)
                                }
                                pending = null
                            }
                        },
                        onCancel = { pending = null }
                    )
                } else {
                    OutlinedButton(
                        onClick = { pending = PendingIngredient() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Добавить ингредиент")
                    }
                }
            }
            // === КОНЕЦ СЕКЦИИ ИНГРЕДИЕНТОВ ===

            OutlinedTextField(
                value = cookingInstructions,
                onValueChange = { cookingInstructions = it },
                label = { Text("Инструкция приготовления") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 6
            )

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }
        }
    }

    // BottomSheet создания нового продукта
    if (showCreateProductSheet) {
        val p = pending
        CreateProductBottomSheet(
            initialName = p?.nameQuery ?: "",
            isCreating = isCreatingProduct,
            errorMessage = createProductError,
            onDismiss = {
                if (!isCreatingProduct) {
                    showCreateProductSheet = false
                    createProductError = null
                }
            },
            onCreate = { name, unit ->
                scope.launch {
                    isCreatingProduct = true
                    createProductError = null
                    val created = try {
                        ApiClient.createProduct(CreateProductRequest(name, unit))
                    } catch (e: Exception) {
                        null
                    }
                    if (created != null) {
                        repository.syncProductsFromServer(force = true)
                        val localProduct = repository.findLocalProductByServerId(created.id)
                        if (localProduct != null && p != null) {
                            pending = p.copy(
                                nameQuery = localProduct.name,
                                selectedProductLocalId = localProduct.localId,
                                selectedProductServerId = localProduct.serverId,
                                selectedProductName = localProduct.name,
                                selectedMeasurementUnit = localProduct.measurementUnit
                            )
                        }
                        showCreateProductSheet = false
                    } else {
                        createProductError = "Не удалось создать продукт. Проверьте подключение к сети."
                    }
                    isCreatingProduct = false
                }
            }
        )
    }
}

private fun uriToBase64(context: Context, uri: Uri): String {
    val inputStream = context.contentResolver.openInputStream(uri) ?: return ""
    val bytes = inputStream.readBytes()
    inputStream.close()
    return Base64.encodeToString(bytes, Base64.NO_WRAP)
}

private fun formatQuantityForEdit(quantity: Double): String {
    return if (quantity % 1.0 == 0.0) {
        quantity.toInt().toString()
    } else {
        quantity.toString()
    }
}