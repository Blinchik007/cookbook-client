package com.baranov.cookbook.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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

/**
 * Экран просмотра рецепта. Поддерживает два режима:
 *  - [RecipeViewMode.Local] — данные читаются из Room. Работает офлайн.
 *  - [RecipeViewMode.Server] — данные тянутся с сервера через ApiClient. Требует интернет.
 *
 * Действия в TopAppBar зависят от контекста рецепта:
 *  - Свой локальный (не опубликован): Редактировать, Опубликовать, Удалить.
 *  - Свой опубликованный: Редактировать, Удалить.
 *  - Скачанный (чужой авторства, лежит у меня): Удалить.
 *  - Публичный с сервера (не скачан): Скачать (только если залогинен).
 */
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
    val context = LocalContext.current
    val repository = AppContainer.repository
    val currentUserId = CurrentUserHolder.currentUser?.id
    val scope = rememberCoroutineScope()

    // Унифицированная модель отображения. Поля nullable там, где данные могут отсутствовать
    // в одном из режимов (например, localId есть только для Local; authorId есть всегда).
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

    // Загрузка данных при первом открытии или смене mode.
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = viewData?.title ?: "Рецепт",
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                },
                actions = {
                    val data = viewData
                    if (data != null) {
                        // Определяем доступные действия по контексту
                        val isLocal = mode is RecipeViewMode.Local
                        val localId = (mode as? RecipeViewMode.Local)?.localId
                        val isOwn = isLocal && data.authorId == data.ownerUserId
                        val isUnpublished = isLocal && (data.serverId == null || data.serverId == -1)
                        val canEdit = isLocal && isOwn
                        val canPublish = isLocal && isOwn && isUnpublished
                        val canDelete = isLocal
                        val canDownload = mode is RecipeViewMode.Server

                        // Скачать — отдельная кнопка для серверного режима (главное действие)
                        if (canDownload) {
                            IconButton(onClick = {
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
                                    contentDescription = "Скачать"
                                )
                            }
                        }

                        // Экспорт ингредиентов в шоппинг-лист — отдельная иконка для серверного режима.
                        // Для локального режима этот пункт лежит в DropdownMenu ниже.
                        if (mode is RecipeViewMode.Server) {
                            IconButton(onClick = {
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
                                    contentDescription = "Экспортировать в список покупок"
                                )
                            }
                        }

                        // Меню три точки — для локального
                        if (canEdit || canPublish || canDelete) {
                            Box {
                                IconButton(onClick = { menuExpanded = true }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "Действия")
                                }
                                DropdownMenu(
                                    expanded = menuExpanded,
                                    onDismissRequest = { menuExpanded = false }
                                ) {
                                    if (canEdit && localId != null) {
                                        DropdownMenuItem(
                                            text = { Text("Редактировать") },
                                            leadingIcon = { Icon(Icons.Default.Edit, null) },
                                            onClick = {
                                                menuExpanded = false
                                                onEdit(localId)
                                            }
                                        )
                                    }
                                    if (canPublish && localId != null) {
                                        DropdownMenuItem(
                                            text = { Text("Опубликовать") },
                                            leadingIcon = { Icon(Icons.Default.Publish, null) },
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
                                    // Экспорт в шоп-лист — всегда доступен в локальном режиме,
                                    // независимо от авторства и статуса публикации.
                                    DropdownMenuItem(
                                        text = { Text("Экспортировать в список") },
                                        leadingIcon = { Icon(Icons.Default.AddShoppingCart, null) },
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
                                            text = { Text("Удалить") },
                                            leadingIcon = { Icon(Icons.Default.Delete, null) },
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
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        when {
            loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            loadError != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = loadError!!,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
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
            title = { Text("Удалить рецепт?") },
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
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Отмена")
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
        // 1. Большое фото на всю ширину
        if (photoBytes != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(photoBytes)
                    .size(1200, 800)
                    .crossfade(true)
                    .build(),
                contentDescription = data.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
            )
        }

        Column(modifier = Modifier.padding(16.dp)) {
            // 2. Заголовок
            Text(
                text = data.title,
                style = MaterialTheme.typography.headlineSmall
            )

            // 3. Описание (если есть)
            if (!data.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = data.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 4. Ингредиенты
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Ингредиенты",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (data.ingredients.isEmpty()) {
                Text(
                    text = "Список пуст",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            } else {
                // Переиспользуем IngredientRow, но без действий редактирования.
                // onEdit/onRemove тут no-op, потому что это view-only экран.
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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

            // 5. Инструкция по приготовлению
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Приготовление",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = data.cookingInstructions.ifBlank { "Инструкция не указана" },
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Унифицированная модель данных для экрана, общая для обоих режимов.
 */
private data class RecipeViewData(
    val title: String,
    val description: String?,
    val cookingInstructions: String,
    val photoBase64: String?,
    val ingredients: List<IngredientView>,
    // Контекстные поля — могут быть null в серверном режиме
    val serverId: Int?,
    val authorId: Int?,
    val ownerUserId: Int?
)

private data class IngredientView(
    val name: String,
    val measurementUnit: String,
    val quantity: Double
)

/**
 * Загружает данные локального рецепта из Room.
 */
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

/**
 * Загружает данные публичного рецепта с сервера.
 * Имена продуктов резолвятся из локального кеша по serverId.
 * Если продукт не в кеше — показываем "Продукт #N".
 */
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

/**
 * Экспорт ингредиентов рецепта в шоппинг-лист.
 * Каждый ингредиент превращается в строку "Название количество единица" (например "Мука 100 г")
 * и добавляется как отдельная запись. Без объединения с существующими — даже если в списке
 * уже есть "Мука 100 г", новая строка с тем же текстом добавится отдельно (по требованию).
 *
 * Возвращает количество фактически добавленных строк (0 если ингредиентов нет).
 */
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
    com.baranov.cookbook.AppContainer.shoppingListRepository.addItems(ownerUserId, texts)
    return texts.size
}

/**
 * Форматирует количество для строки экспорта: целое — без точки, дробное — до 2 знаков без хвостовых нулей.
 * Логика та же, что в IngredientRow.formatQuantity, но локальная — чтобы не плодить зависимости.
 */
private fun formatExportQuantity(quantity: Double): String {
    return if (quantity % 1.0 == 0.0) {
        quantity.toInt().toString()
    } else {
        "%.2f".format(quantity).trimEnd('0').trimEnd(',').trimEnd('.')
    }
}