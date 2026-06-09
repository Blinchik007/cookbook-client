package com.baranov.cookbook.screens.recipeEditor


import android.os.Build
import android.util.Base64
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.baranov.cookbook.AppContainer
import com.baranov.cookbook.CurrentUserHolder
import com.baranov.cookbook.data.database.local.entity.LocalProductEntity
import com.baranov.cookbook.data.database.remote.ApiClient
import com.baranov.cookbook.data.database.remote.dto.CreateProductRequest
import com.baranov.cookbook.data.database.remote.dto.RecipeProductDto
import com.baranov.cookbook.util.scaleAndEncodeImage
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
    val scheme = MaterialTheme.colorScheme

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var cookingInstructions by remember { mutableStateOf("") }
    var photoBase64 by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val ingredients = remember { mutableStateListOf<RecipeIngredient>() }
    var pending by remember { mutableStateOf<PendingIngredient?>(null) }
    var suggestions by remember { mutableStateOf<List<LocalProductEntity>>(emptyList()) }

    var showCreateProductSheet by remember { mutableStateOf(false) }
    var createProductError by remember { mutableStateOf<String?>(null) }
    var isCreatingProduct by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            photoBase64 = scaleAndEncodeImage(context, it, maxSize = 1024)
            Log.d("PhotoDebug", "Base64 length: ${photoBase64?.length}, starts with: ${photoBase64?.take(30)}")
        }
    }

    LaunchedEffect(Unit) {
        repository.syncProductsFromServer()
    }

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

    val bgBrush = Brush.radialGradient(
        colors = listOf(scheme.surfaceVariant, scheme.background, scheme.background),
        radius = 1400f
    )

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.background(bgBrush),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = scheme.primary,
                    navigationIconContentColor = scheme.primary,
                    actionIconContentColor = scheme.primary
                ),
                title = {
                    Text(
                        text = if (isNew) "Новый рецепт" else "Редактирование",
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                navigationIcon = {
                    CircleIconButton(onClick = onFinish) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                            tint = scheme.primary
                        )
                    }
                },
                actions = {
                    CircleIconButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                        Icon(
                            Icons.Default.PhotoCamera,
                            contentDescription = "Выбрать фото",
                            tint = scheme.primary
                        )
                    }
                    val canSave = !isLoading && pending == null && title.isNotBlank()
                    CircleIconButton(
                        onClick = {
                            if (canSave) {
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
                        enabled = canSave,
                        dim = !canSave
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Сохранить",
                            tint = if (canSave) scheme.primary
                            else scheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
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
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ====== Фото ======
            PhotoBlock(
                photoBase64 = photoBase64,
                onPick = { imagePickerLauncher.launch("image/*") }
            )

            // ====== Поля Название и Описание ======
            FieldLabel("Название")
            UnderlinedField(
                value = title,
                onValueChange = { title = it },
                placeholder = "Например: Блины"
            )

            FieldLabel("Краткое описание")
            UnderlinedField(
                value = description,
                onValueChange = { description = it },
                placeholder = "Несколько слов о рецепте",
                minLines = 2,
                singleLine = false
            )

            // ====== Секция ингредиентов ======
            SectionTitle("Ингредиенты")

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(scheme.surface)
                    .border(1.dp, scheme.primary.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
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
                    // Кнопка Добавить ингредиент
                    AddIngredientButton(onClick = { pending = PendingIngredient() })
                }
            }

            // ====== Инструкция ======
            SectionTitle("Приготовление")
            UnderlinedField(
                value = cookingInstructions,
                onValueChange = { cookingInstructions = it },
                placeholder = "Опишите шаги приготовления…",
                minLines = 6,
                singleLine = false
            )

            if (isLoading) {
                CircularProgressIndicator(
                    color = scheme.primary,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            Spacer(Modifier.height(8.dp))
        }
    }

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

/* ---------- Внутренние стилизованные компоненты ---------- */

@Composable
private fun PhotoBlock(
    photoBase64: String?,
    onPick: () -> Unit
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val photoBytes = remember(photoBase64) {
        if (photoBase64.isNullOrBlank()) null
        else runCatching { Base64.decode(photoBase64, Base64.DEFAULT) }.getOrNull()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(scheme.surface)
            .border(1.dp, scheme.primary.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            .clickable(onClick = onPick),
        contentAlignment = Alignment.Center
    ) {
        if (photoBytes != null) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(photoBytes).crossfade(true).build(),
                contentDescription = "Фото рецепта",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoCamera,
                    contentDescription = null,
                    tint = scheme.primary.copy(alpha = 0.7f),
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = "Нажмите, чтобы добавить фото",
                    color = scheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 16.sp,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.primary
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnderlinedField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean = true,
    minLines: Int = 1
) {
    val scheme = MaterialTheme.colorScheme
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = singleLine,
        minLines = minLines,
        textStyle = TextStyle(color = scheme.onBackground, fontSize = 14.sp),
        placeholder = {
            Text(placeholder, color = scheme.onSurfaceVariant.copy(alpha = 0.5f), fontSize = 14.sp)
        },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            focusedIndicatorColor = scheme.primary,
            unfocusedIndicatorColor = scheme.outline,
            cursorColor = scheme.primary
        )
    )
}

@Composable
private fun AddIngredientButton(onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val borderBrush = Brush.horizontalGradient(listOf(scheme.primary, scheme.secondary))
    val fillBrush = Brush.horizontalGradient(
        listOf(scheme.primary.copy(alpha = 0.08f), scheme.secondary.copy(alpha = 0.08f))
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(fillBrush)
            .border(1.dp, borderBrush, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Add,
            contentDescription = null,
            tint = scheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            "Добавить ингредиент",
            color = scheme.primary,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun CircleIconButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    dim: Boolean = false,
    content: @Composable () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val borderColor = if (dim) scheme.onSurfaceVariant.copy(alpha = 0.3f)
    else scheme.primary.copy(alpha = 0.5f)
    Box(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .size(40.dp)
            .clip(CircleShape)
            .border(1.dp, borderColor, CircleShape)
    ) {
        IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxSize()) {
            content()
        }
    }
}

private fun formatQuantityForEdit(quantity: Double): String {
    return if (quantity % 1.0 == 0.0) {
        quantity.toInt().toString()
    } else {
        quantity.toString()
    }
}