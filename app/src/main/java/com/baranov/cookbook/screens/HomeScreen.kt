package com.baranov.cookbook.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.baranov.cookbook.AppContainer
import com.baranov.cookbook.CurrentUserHolder
import com.baranov.cookbook.data.database.local.LocalRecipesRepository
import com.baranov.cookbook.data.database.remote.ApiClient
import com.baranov.cookbook.data.database.remote.dto.RecipeDto
import com.baranov.cookbook.ui.components.IngredientDisplayItem
import com.baranov.cookbook.ui.components.RecipeCard
import com.baranov.cookbook.ui.components.UserAvatar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(rootNavController: NavController) {
    val repository = AppContainer.repository
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })
    val scope = rememberCoroutineScope()
    val currentUser = CurrentUserHolder.currentUser

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.fillMaxWidth(0.5f)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    UserAvatar(
                        photoBase64 = currentUser?.photo,
                        size = 72.dp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = currentUser?.name ?: "Гость",
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (currentUser != null) {
                        Text(
                            text = currentUser.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                HorizontalDivider()

                if (currentUser != null) {
                    NavigationDrawerItem(
                        label = { Text("Профиль") },
                        icon = { Icon(Icons.Default.Person, contentDescription = null) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            rootNavController.navigate("profile_screen")
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    NavigationDrawerItem(
                        label = { Text("Выйти") },
                        icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null) },
                        selected = false,
                        onClick = {
                            scope.launch {
                                drawerState.close()
                                AppContainer.userPreferences.clearUser()
                                CurrentUserHolder.currentUser = null
                            }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                } else {
                    NavigationDrawerItem(
                        label = { Text("Войти") },
                        icon = { Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            rootNavController.navigate("login_screen")
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    scope.launch { drawerState.open() }
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            UserAvatar(
                                photoBase64 = currentUser?.photo,
                                size = 36.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(currentUser?.name ?: "Гость")
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.List, contentDescription = "Мои рецепты") },
                        label = { Text("Мои рецепты") },
                        selected = pagerState.currentPage == 0,
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(0) }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Public, contentDescription = "Публичные рецепты") },
                        label = { Text("Публичные рецепты") },
                        selected = pagerState.currentPage == 1,
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(1) }
                        }
                    )
                }
            }
        ) { innerPadding ->
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) { page ->
                when (page) {
                    0 -> MyRecipesScreen(
                        repository = repository,
                        onEditRecipe = { localId ->
                            rootNavController.navigate("recipe_editor/$localId")
                        },
                        onOpenRecipe = { localId ->
                            rootNavController.navigate("recipe_view/local/$localId")
                        }
                    )
                    1 -> PublicRecipesScreen(
                        onOpenRecipe = { serverId ->
                            rootNavController.navigate("recipe_view/server/$serverId")
                        }
                    )
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MyRecipesScreen(
    repository: LocalRecipesRepository,
    onEditRecipe: (Long) -> Unit,
    onOpenRecipe: (Long) -> Unit
) {
    val currentUserId = CurrentUserHolder.currentUser?.id
    val recipes by repository.getRecipesForUser(currentUserId)
        .collectAsStateWithLifecycle(initialValue = emptyList())
    var expandedRecipeId by remember { mutableStateOf<Long?>(null) }
    var showMenuForRecipeId by remember { mutableStateOf<Long?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<Long?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { onEditRecipe(-1L) }) {
                Icon(Icons.Default.Add, contentDescription = "Добавить рецепт")
            }
        }
    ) { innerPadding ->
        if (recipes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("У вас пока нет рецептов")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(recipes, key = { it.localId }) { recipe ->
                    val isExpanded = expandedRecipeId == recipe.localId
                    val isOwn = recipe.authorId == recipe.ownerUserId
                    RecipeCard(
                        title = recipe.title,
                        description = recipe.description,
                        photoBase64 = recipe.photo,
                        badge = if (!isOwn) "скачано" else null,
                        expanded = isExpanded,
                        onClick = { onOpenRecipe(recipe.localId) },
                        onToggleExpand = {
                            expandedRecipeId = if (isExpanded) null else recipe.localId
                        },
                        onLongClick = { showMenuForRecipeId = recipe.localId },
                        loadIngredients = {
                            val data = repository.getRecipeWithProducts(recipe.localId)
                            data?.products?.zip(data.productEntities) { rp, product ->
                                IngredientDisplayItem(
                                    name = product.name.ifBlank { "Продукт #${product.serverId}" },
                                    quantity = rp.quantity,
                                    measurementUnit = product.measurementUnit
                                )
                            }.orEmpty()
                        }
                    )
                }
            }
        }
    }

    if (showMenuForRecipeId != null) {
        val localId = showMenuForRecipeId!!
        val recipe = recipes.find { it.localId == localId }
        val isLocal = recipe?.serverId == null || recipe.serverId == -1
        val isOwn = recipe?.authorId == recipe?.ownerUserId
        val canPublish = isLocal && isOwn && CurrentUserHolder.currentUser != null

        AlertDialog(
            onDismissRequest = { showMenuForRecipeId = null },
            title = { Text("Действие с рецептом") },
            text = { Text("Выберите действие") },
            confirmButton = {
                // Для своих — основное действие "Редактировать".
                // Для скачанных — единственное доступное действие "Удалить" (редактировать чужой нельзя).
                if (isOwn) {
                    TextButton(onClick = {
                        showMenuForRecipeId = null
                        onEditRecipe(localId)
                    }) {
                        Text("Редактировать")
                    }
                } else {
                    TextButton(onClick = {
                        showMenuForRecipeId = null
                        showDeleteConfirm = localId
                    }) {
                        Text("Удалить")
                    }
                }
            },
            dismissButton = {
                // dismissButton показываем только для своих — там есть дополнительные действия.
                // Для скачанных confirmButton уже содержит Удалить, других действий нет.
                if (isOwn) {
                    Row {
                        if (canPublish) {
                            TextButton(onClick = {
                                showMenuForRecipeId = null
                                scope.launch {
                                    repository.publishRecipe(localId)
                                }
                            }) {
                                Text("Опубликовать")
                            }
                        }
                        TextButton(onClick = {
                            showMenuForRecipeId = null
                            showDeleteConfirm = localId
                        }) {
                            Text("Удалить")
                        }
                    }
                }
            }
        )
    }

    if (showDeleteConfirm != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("Удалить рецепт?") },
            text = { Text("Это действие нельзя отменить.") },
            confirmButton = {
                TextButton(onClick = {
                    val id = showDeleteConfirm!!
                    showDeleteConfirm = null
                    scope.launch {
                        repository.deleteRecipe(id)
                    }
                }) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun PublicRecipesScreen(
    onOpenRecipe: (Int) -> Unit
) {
    val repository = AppContainer.repository
    val recipes = remember { mutableStateListOf<RecipeDto>() }
    var isLoading by remember { mutableStateOf(false) }
    var expandedRecipeId by remember { mutableStateOf<Int?>(null) }
    var showDownloadConfirm by remember { mutableStateOf<RecipeDto?>(null) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val currentUserId = CurrentUserHolder.currentUser?.id

    LaunchedEffect(Unit) {
        isLoading = true
        try {
            val serverRecipes = ApiClient.getAllRecipes()
            recipes.clear()
            recipes.addAll(serverRecipes)
        } catch (e: Exception) {
            // обработка ошибок
        } finally {
            isLoading = false
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (recipes.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Публичных рецептов пока нет")
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(recipes, key = { it.id }) { recipe ->
                val isExpanded = expandedRecipeId == recipe.id
                RecipeCard(
                    title = recipe.title,
                    description = recipe.description,
                    photoBase64 = recipe.photo,
                    expanded = isExpanded,
                    onClick = { onOpenRecipe(recipe.id) },
                    onToggleExpand = {
                        expandedRecipeId = if (isExpanded) null else recipe.id
                    },
                    onLongClick = if (currentUserId != null) {
                        { showDownloadConfirm = recipe }
                    } else null,
                    loadIngredients = {
                        // Для публичного рецепта тянем детали с сервера.
                        // Имена продуктов резолвим из локального кеша; если нет — заглушка.
                        val details = ApiClient.getRecipeById(recipe.id) ?: return@RecipeCard emptyList()
                        details.products.map { rp ->
                            val product = repository.findLocalProductByServerId(rp.productId)
                            IngredientDisplayItem(
                                name = product?.name?.ifBlank { null } ?: "Продукт #${rp.productId}",
                                quantity = rp.quantity,
                                measurementUnit = product?.measurementUnit ?: ""
                            )
                        }
                    }
                )
            }
        }
    }

    if (showDownloadConfirm != null) {
        val recipe = showDownloadConfirm!!
        AlertDialog(
            onDismissRequest = { showDownloadConfirm = null },
            title = { Text("Скачать рецепт?") },
            text = { Text("«${recipe.title}» будет сохранён в ваши рецепты.") },
            confirmButton = {
                TextButton(onClick = {
                    showDownloadConfirm = null
                    scope.launch {
                        val ownerId = currentUserId ?: return@launch
                        val result = repository.downloadPublicRecipe(recipe.id, ownerId)
                        snackbarMessage = if (result != null) "Рецепт сохранён"
                        else "Не удалось скачать рецепт"
                    }
                }) {
                    Text("Скачать")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDownloadConfirm = null }) {
                    Text("Отмена")
                }
            }
        )
    }

    snackbarMessage?.let { msg ->
        LaunchedEffect(msg) {
            delay(2000)
            snackbarMessage = null
        }
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = Modifier.padding(16.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.inverseSurface
            ) {
                Text(
                    text = msg,
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}