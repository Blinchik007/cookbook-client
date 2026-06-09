package com.baranov.cookbook.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Publish
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.baranov.cookbook.AppContainer
import com.baranov.cookbook.CurrentUserHolder
import com.baranov.cookbook.data.database.local.LocalRecipeWithProducts
import com.baranov.cookbook.data.database.remote.ApiClient
import com.baranov.cookbook.data.database.remote.dto.RecipeWithDetailsDto
import com.baranov.cookbook.ui.components.IngredientRow
import com.baranov.cookbook.ui.components.LoginRequiredDialog
import kotlinx.coroutines.launch

sealed class RecipeViewMode {
    data class Local(val localId: Long) : RecipeViewMode()
    data class Server(val serverId: Int) : RecipeViewMode()
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeViewScreen(
    mode: RecipeViewMode,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val repository = AppContainer.repository
    val currentUserId = CurrentUserHolder.currentUser?.id
    val scope = rememberCoroutineScope()
    val scheme = MaterialTheme.colorScheme

    var showLoginRequiredFor by remember { mutableStateOf<String?>(null) }
    var viewData by remember(mode) { mutableStateOf<RecipeViewData?>(null) }
    var loading by remember(mode) { mutableStateOf(true) }
    var loadError by remember(mode) { mutableStateOf<String?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            snackbarMessage = null
        }
    }

    LaunchedEffect(mode) {
        loading = true
        loadError = null
        viewData = null
        try {
            viewData = when (mode) {
                is RecipeViewMode.Local -> loadLocal(mode.localId, repository::getRecipeWithProducts)
                is RecipeViewMode.Server -> loadServer(mode.serverId, repository::findLocalProductByServerId)
            }
            if (viewData == null) {
                loadError = "Рецепт не найден"
            }
        } catch (e: Exception) {
            loadError = "Не удалось загрузить рецепт: ${e.message ?: "ошибка сети"}"
        } finally {
            loading = false
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
                        text = viewData?.title ?: "Рецепт",
                        style = MaterialTheme.typography.headlineMedium,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    CircleIconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                            tint = scheme.primary
                        )
                    }
                },
                actions = {
                    val data = viewData
                    if (data != null) {
                        val isLocal = mode is RecipeViewMode.Local
                        val localId = (mode as? RecipeViewMode.Local)?.localId
                        val isOwn = isLocal && data.authorId == data.ownerUserId
                        val isUnpublished = isLocal && (data.serverId == null || data.serverId == -1)
                        val canEdit = isLocal && isOwn
                        val canPublish = isLocal && isOwn && isUnpublished
                        val canDelete = isLocal
                        val canDownload = mode is RecipeViewMode.Server

                        if (canDownload) {
                            CircleIconButton(onClick = {
                                if (currentUserId == null) {
                                    showLoginRequiredFor = "скачать рецепт"
                                } else {
                                    val serverId = (mode as RecipeViewMode.Server).serverId
                                    scope.launch {
                                        val result = repository.downloadPublicRecipe(serverId, currentUserId)
                                        snackbarMessage = if (result != null) "Рецепт сохранён в ваши"
                                        else "Не удалось скачать рецепт"
                                    }
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = "Скачать",
                                    tint = scheme.primary
                                )
                            }
                        }

                        if (mode is RecipeViewMode.Server) {
                            CircleIconButton(onClick = {
                                scope.launch {
                                    val added = exportIngredientsToShoppingList(
                                        ingredients = data.ingredients,
                                        ownerUserId = currentUserId
                                    )
                                    snackbarMessage = if (added > 0) "Добавлено в список: $added"
                                    else "В рецепте нет ингредиентов"
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.AddShoppingCart,
                                    contentDescription = "Экспортировать в список покупок",
                                    tint = scheme.primary
                                )
                            }
                        }

                        if (canEdit || canPublish || canDelete) {
                            Box {
                                CircleIconButton(onClick = { menuExpanded = true }) {
                                    Icon(
                                        Icons.Default.MoreVert,
                                        contentDescription = "Действия",
                                        tint = scheme.primary
                                    )
                                }
                                DropdownMenu(
                                    expanded = menuExpanded,
                                    onDismissRequest = { menuExpanded = false },
                                    containerColor = scheme.surface
                                ) {
                                    if (canEdit && localId != null) {
                                        DropdownMenuItem(
                                            text = { Text("Редактировать", color = scheme.onSurface) },
                                            leadingIcon = { Icon(Icons.Default.Edit, null, tint = scheme.primary) },
                                            onClick = {
                                                menuExpanded = false
                                                onEdit(localId)
                                            }
                                        )
                                    }
                                    if (canPublish && localId != null) {
                                        DropdownMenuItem(
                                            text = { Text("Опубликовать", color = scheme.onSurface) },
                                            leadingIcon = { Icon(Icons.Default.Publish, null, tint = scheme.primary) },
                                            onClick = {
                                                menuExpanded = false
                                                if (currentUserId == null) {
                                                    showLoginRequiredFor = "опубликовать рецепт"
                                                } else {
                                                    scope.launch {
                                                        try {
                                                            repository.publishRecipe(localId)
                                                            snackbarMessage = "Рецепт опубликован"
                                                            viewData = loadLocal(localId, repository::getRecipeWithProducts)
                                                        } catch (e: Exception) {
                                                            snackbarMessage = "Не удалось опубликовать"
                                                        }
                                                    }
                                                }
                                            }
                                        )
                                    }
                                    DropdownMenuItem(
                                        text = { Text("Экспортировать в список", color = scheme.onSurface) },
                                        leadingIcon = { Icon(Icons.Default.AddShoppingCart, null, tint = scheme.primary) },
                                        onClick = {
                                            menuExpanded = false
                                            scope.launch {
                                                val added = exportIngredientsToShoppingList(
                                                    ingredients = data.ingredients,
                                                    ownerUserId = currentUserId
                                                )
                                                snackbarMessage = if (added > 0) "Добавлено в список: $added"
                                                else "В рецепте нет ингредиентов"
                                            }
                                        }
                                    )
                                    if (canDelete && localId != null) {
                                        DropdownMenuItem(
                                            text = { Text("Удалить", color = scheme.error) },
                                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = scheme.error) },
                                            onClick = {
                                                menuExpanded = false
                                                showDeleteConfirm = true
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    actionColor = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    ) { innerPadding ->
        when {
            loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = scheme.primary)
                }
            }
            loadError != null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = loadError!!,
                        style = MaterialTheme.typography.bodyLarge,
                        color = scheme.error
                    )
                }
            }
            viewData != null -> {
                RecipeViewContent(
                    data = viewData!!,
                    contentPadding = innerPadding
                )
            }
        }
    }

    if (showDeleteConfirm) {
        val localId = (mode as? RecipeViewMode.Local)?.localId
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = scheme.surface,
            titleContentColor = scheme.primary,
            textContentColor = scheme.onSurface,
            title = {
                Text(
                    "Удалить рецепт?",
                    style = MaterialTheme.typography.headlineMedium
                )
            },
            text = { Text("Это действие нельзя отменить.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    if (localId != null) {
                        scope.launch {
                            repository.deleteRecipe(localId)
                            onBack()
                        }
                    }
                }) {
                    Text("Удалить", color = scheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Отмена", color = scheme.onSurfaceVariant)
                }
            }
        )
    }

    showLoginRequiredFor?.let { action ->
        LoginRequiredDialog(
            actionDescription = action,
            onDismiss = { showLoginRequiredFor = null },
            onLogin = {
                showLoginRequiredFor = null
                onNavigateToLogin()
            }
        )
    }
}

@Composable
private fun RecipeViewContent(
    data: RecipeViewData,
    contentPadding: PaddingValues
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val photoBytes = remember(data.photoBase64) {
        if (data.photoBase64.isNullOrBlank()) null
        else runCatching {
            android.util.Base64.decode(data.photoBase64, android.util.Base64.DEFAULT)
        }.getOrNull()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
    ) {
        //  Фото с названием поверх
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(scheme.surfaceVariant)
                .border(1.dp, scheme.primary.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
        ) {
            if (photoBytes != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(photoBytes)
                        .size(1200, 800)
                        .crossfade(true)
                        .build(),
                    contentDescription = data.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Restaurant,
                        contentDescription = null,
                        tint = scheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(80.dp)
                    )
                }
            }

            // Градиент по низу
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.55f)
                            )
                        )
                    )
            )

            // Плашка с названием
            Surface(
                color = Color.Black.copy(alpha = 0.55f),
                shape = RoundedCornerShape(topStart = 12.dp, bottomEnd = 20.dp),
                modifier = Modifier.align(Alignment.BottomEnd)
            ) {
                Text(
                    text = data.title,
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 22.sp),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            // Описание
            if (!data.description.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = data.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = scheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(24.dp))

            SectionTitle("Ингредиенты")
            Spacer(Modifier.height(8.dp))

            if (data.ingredients.isEmpty()) {
                Text(
                    text = "Список пуст",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(scheme.surface)
                        .border(1.dp, scheme.primary.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    data.ingredients.forEach { ingredient ->
                        IngredientRow(
                            name = ingredient.name,
                            measurementUnit = ingredient.measurementUnit,
                            quantity = ingredient.quantity,
                            onEdit = { /* no-op */ },
                            onRemove = { /* no-op */ }
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            SectionTitle("Приготовление")
            Spacer(Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(scheme.surface)
                    .border(1.dp, scheme.primary.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Text(
                    text = data.cookingInstructions.ifBlank { "Инструкция не указана" },
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (data.cookingInstructions.isBlank()) scheme.onSurfaceVariant
                    else scheme.onSurface
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}


@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun CircleIconButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .size(40.dp)
            .clip(CircleShape)
            .border(1.dp, scheme.primary.copy(alpha = 0.5f), CircleShape)
    ) {
        IconButton(onClick = onClick, modifier = Modifier.fillMaxSize()) {
            content()
        }
    }
}

private data class RecipeViewData(
    val title: String,
    val description: String?,
    val cookingInstructions: String,
    val photoBase64: String?,
    val ingredients: List<IngredientView>,
    val serverId: Int?,
    val authorId: Int?,
    val ownerUserId: Int?
)

private data class IngredientView(
    val name: String,
    val measurementUnit: String,
    val quantity: Double
)

private suspend fun loadLocal(
    localId: Long,
    getRecipeWithProducts: suspend (Long) -> LocalRecipeWithProducts?
): RecipeViewData? {
    val withProducts = getRecipeWithProducts(localId) ?: return null
    val ingredients = withProducts.products.zip(withProducts.productEntities) { rp, product ->
        IngredientView(
            name = product.name.ifBlank { "Продукт #${product.serverId}" },
            measurementUnit = product.measurementUnit,
            quantity = rp.quantity
        )
    }
    return RecipeViewData(
        title = withProducts.recipe.title,
        description = withProducts.recipe.description,
        cookingInstructions = withProducts.recipe.cookingInstructions,
        photoBase64 = withProducts.recipe.photo,
        ingredients = ingredients,
        serverId = withProducts.recipe.serverId,
        authorId = withProducts.recipe.authorId,
        ownerUserId = withProducts.recipe.ownerUserId
    )
}

private suspend fun loadServer(
    serverId: Int,
    findLocalProduct: suspend (Int) -> com.baranov.cookbook.data.database.local.entity.LocalProductEntity?
): RecipeViewData? {
    val details: RecipeWithDetailsDto = ApiClient.getRecipeById(serverId) ?: return null
    val ingredients = details.products.map { rp ->
        val product = findLocalProduct(rp.productId)
        IngredientView(
            name = product?.name?.ifBlank { null } ?: "Продукт #${rp.productId}",
            measurementUnit = product?.measurementUnit ?: "",
            quantity = rp.quantity
        )
    }
    return RecipeViewData(
        title = details.recipe.title,
        description = details.recipe.description,
        cookingInstructions = details.recipe.cookingInstructions,
        photoBase64 = details.recipe.photo,
        ingredients = ingredients,
        serverId = details.recipe.id,
        authorId = details.recipe.authorId,
        ownerUserId = null
    )
}

private suspend fun exportIngredientsToShoppingList(
    ingredients: List<IngredientView>,
    ownerUserId: Int?
): Int {
    if (ingredients.isEmpty()) return 0
    val texts = ingredients.map { ing ->
        val qty = formatExportQuantity(ing.quantity)
        listOfNotNull(ing.name.ifBlank { null }, qty, ing.measurementUnit.ifBlank { null })
            .joinToString(" ")
    }
    AppContainer.shoppingListRepository.addItems(ownerUserId, texts)
    return texts.size
}

private fun formatExportQuantity(quantity: Double): String {
    return if (quantity % 1.0 == 0.0) {
        quantity.toInt().toString()
    } else {
        "%.2f".format(quantity).trimEnd('0').trimEnd(',').trimEnd('.')
    }
}