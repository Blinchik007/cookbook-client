package com.baranov.cookbook.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
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
import androidx.compose.runtime.saveable.rememberSaveable
import com.baranov.cookbook.ui.components.SearchBar
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.baranov.cookbook.AppContainer
import com.baranov.cookbook.CurrentUserHolder
import com.baranov.cookbook.data.database.local.LocalRecipesRepository
import com.baranov.cookbook.data.database.remote.ApiClient
import com.baranov.cookbook.data.database.remote.dto.RecipeDto
import com.baranov.cookbook.ui.components.IngredientDisplayItem
import com.baranov.cookbook.ui.components.LoginRequiredDialog
import com.baranov.cookbook.ui.components.RecipeCard
import com.baranov.cookbook.ui.components.UserAvatar
import com.baranov.cookbook.util.SearchQueryParser
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(rootNavController: NavController) {
    val repository = AppContainer.repository
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { 3 })
    val scope = rememberCoroutineScope()
    val currentUser = CurrentUserHolder.currentUser
    val scheme = MaterialTheme.colorScheme

    // Радиальный фон-градиент
    val bgBrush = Brush.radialGradient(
        colors = listOf(scheme.surfaceVariant, scheme.background, scheme.background),
        radius = 1400f
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.fillMaxWidth(0.55f),
                drawerContainerColor = scheme.surface
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    UserAvatar(photoBase64 = currentUser?.photo, size = 72.dp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = currentUser?.name ?: "Гость",
                        style = MaterialTheme.typography.headlineMedium,
                        color = scheme.primary
                    )
                    if (currentUser != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = currentUser.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant
                        )
                    }
                }
                HorizontalDivider(color = scheme.outline.copy(alpha = 0.5f))

                if (currentUser != null) {
                    DrawerLink(Icons.Default.Person, "Профиль") {
                        scope.launch { drawerState.close() }
                        rootNavController.navigate("profile_screen")
                    }
                    DrawerLink(Icons.Default.Settings, "Настройки") {
                        scope.launch { drawerState.close() }
                        rootNavController.navigate("settings_screen")
                    }
                    DrawerLink(Icons.AutoMirrored.Filled.ExitToApp, "Выйти") {
                        scope.launch {
                            drawerState.close()
                            AppContainer.userPreferences.clearUser()
                            CurrentUserHolder.currentUser = null
                            AppContainer.onAuthChanged()
                        }
                    }
                } else {
                    DrawerLink(Icons.AutoMirrored.Filled.Login, "Войти") {
                        scope.launch { drawerState.close() }
                        rootNavController.navigate("login_screen")
                    }
                }
            }
        }
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            modifier = Modifier.background(bgBrush),
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = scheme.onBackground
                    ),
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { scope.launch { drawerState.open() } }
                                .padding(vertical = 4.dp)
                        ) {
                            UserAvatar(
                                photoBase64 = currentUser?.photo,
                                size = 44.dp
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = currentUser?.name ?: "Гость",
                                style = MaterialTheme.typography.headlineMedium,
                                color = scheme.primary
                            )
                        }
                    },
                    actions = {
                        // Кнопка добавить рецепт
                        if (pagerState.currentPage == 1) {
                            IconButton(
                                onClick = {
                                    rootNavController.navigate("recipe_editor/-1")
                                }
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Добавить рецепт",
                                    tint = scheme.primary
                                )
                            }
                        }
                        // На странице "Список покупок" — кнопка удалить отмеченные.
                        if (pagerState.currentPage == 2) {
                            ShoppingListDeleteCheckedAction()
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = scheme.surface,
                    contentColor = scheme.onSurface
                ) {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Public, contentDescription = "Публичные рецепты") },
                        label = {
                            Text(
                                "Публичные",
                                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 12.sp)
                            )
                        },
                        selected = pagerState.currentPage == 0,
                        onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = scheme.primary,
                            selectedTextColor = scheme.primary,
                            indicatorColor = scheme.primary.copy(alpha = 0.15f),
                            unselectedIconColor = scheme.onSurfaceVariant,
                            unselectedTextColor = scheme.onSurfaceVariant
                        )
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.List, contentDescription = "Мои рецепты") },
                        label = {
                            Text(
                                "Мои рецепты",
                                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 12.sp)
                            )
                        },
                        selected = pagerState.currentPage == 1,
                        onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = scheme.primary,
                            selectedTextColor = scheme.primary,
                            indicatorColor = scheme.primary.copy(alpha = 0.15f),
                            unselectedIconColor = scheme.onSurfaceVariant,
                            unselectedTextColor = scheme.onSurfaceVariant
                        )
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "Список покупок") },
                        label = {
                            Text(
                                "Список",
                                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 12.sp)
                            )
                        },
                        selected = pagerState.currentPage == 2,
                        onClick = { scope.launch { pagerState.animateScrollToPage(2) } },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = scheme.primary,
                            selectedTextColor = scheme.primary,
                            indicatorColor = scheme.primary.copy(alpha = 0.15f),
                            unselectedIconColor = scheme.onSurfaceVariant,
                            unselectedTextColor = scheme.onSurfaceVariant
                        )
                    )
                }
            },
        ) { innerPadding ->
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) { page ->
                when (page) {
                    0 -> PublicRecipesScreen(
                        onOpenRecipe = { serverId ->
                            rootNavController.navigate("recipe_view/server/$serverId")
                        },
                        onNavigateToLogin = {
                            rootNavController.navigate("login_screen")
                        }
                    )
                    1 -> MyRecipesScreen(
                        repository = repository,
                        onEditRecipe = { localId ->
                            rootNavController.navigate("recipe_editor/$localId")
                        },
                        onOpenRecipe = { localId ->
                            rootNavController.navigate("recipe_view/local/$localId")
                        }
                    )
                    2 -> ShoppingListScreen()
                }
            }
        }
    }
}


@Composable
private fun DrawerLink(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = scheme.primary)
        Spacer(Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = scheme.onBackground
        )
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
    val scheme = MaterialTheme.colorScheme

    Scaffold(
        containerColor = Color.Transparent
    ) { innerPadding ->
        if (recipes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "У вас пока нет рецептов",
                    style = MaterialTheme.typography.headlineMedium,
                    color = scheme.onSurfaceVariant
                )
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
                if (isOwn) {
                    TextButton(onClick = {
                        showMenuForRecipeId = null
                        onEditRecipe(localId)
                    }) { Text("Редактировать") }
                } else {
                    TextButton(onClick = {
                        showMenuForRecipeId = null
                        showDeleteConfirm = localId
                    }) { Text("Удалить") }
                }
            },
            dismissButton = {
                if (isOwn) {
                    Row {
                        if (canPublish) {
                            TextButton(onClick = {
                                showMenuForRecipeId = null
                                scope.launch { repository.publishRecipe(localId) }
                            }) { Text("Опубликовать") }
                        }
                        TextButton(onClick = {
                            showMenuForRecipeId = null
                            showDeleteConfirm = localId
                        }) { Text("Удалить") }
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
                    scope.launch { repository.deleteRecipe(id) }
                }) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) { Text("Отмена") }
            }
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun PublicRecipesScreen(
    onOpenRecipe: (Int) -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val repository = AppContainer.repository
    val recipes = remember { mutableStateListOf<RecipeDto>() }
    var isLoading by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var expandedRecipeId by remember { mutableStateOf<Int?>(null) }
    var showDownloadConfirm by remember { mutableStateOf<RecipeDto?>(null) }
    var showLoginRequired by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val currentUserId = CurrentUserHolder.currentUser?.id
    var retryTrigger by remember { mutableIntStateOf(0) }
    val scheme = MaterialTheme.colorScheme

    var searchQuery by rememberSaveable { mutableStateOf("") }

    val historyItems by AppContainer.searchHistory.historyFlow
        .collectAsStateWithLifecycle(initialValue = emptyList())

    LaunchedEffect(searchQuery, retryTrigger) {
        delay(300)
        isLoading = true
        loadError = null
        try {
            val parts = SearchQueryParser.parse(searchQuery)
            val serverRecipes = ApiClient.getAllRecipes(
                search = parts.recipeName,
                author = parts.authorName
            )
            recipes.clear()
            recipes.addAll(serverRecipes)
        } catch (e: Exception) {
            loadError = "Не удалось загрузить рецепты"
        } finally {
            isLoading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            placeholder = "Поиск по названию",
            historyItems = historyItems,
            onPickHistory = { picked -> searchQuery = picked },
            onClearHistory = {
                scope.launch { AppContainer.searchHistory.clear() }
            }
        )

        when {
            isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = scheme.primary)
                }
            }
            loadError != null -> {
                Box(
                    Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(loadError!!, color = scheme.error)
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { retryTrigger++ },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = scheme.primary,
                                contentColor = scheme.onPrimary
                            )
                        ) { Text("Обновить") }
                    }
                }
            }
            recipes.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (searchQuery.isBlank()) "Публичных рецептов пока нет"
                        else "Ничего не найдено",
                        style = MaterialTheme.typography.headlineMedium,
                        color = scheme.onSurfaceVariant
                    )
                }
            }
            else -> {
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
                            onClick = {
                                val queryToSave = searchQuery
                                if (queryToSave.isNotBlank()) {
                                    scope.launch {
                                        AppContainer.searchHistory.addQuery(queryToSave)
                                    }
                                }
                                onOpenRecipe(recipe.id)
                            },
                            onToggleExpand = {
                                expandedRecipeId = if (isExpanded) null else recipe.id
                            },
                            onLongClick = { showDownloadConfirm = recipe },
                            loadIngredients = {
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
                    if (currentUserId == null) {
                        showDownloadConfirm = null
                        showLoginRequired = true
                    } else {
                        showDownloadConfirm = null
                        scope.launch {
                            val result = repository.downloadPublicRecipe(recipe.id, currentUserId)
                            snackbarMessage = if (result != null) "Рецепт сохранён"
                            else "Не удалось скачать рецепт"
                        }
                    }
                }) { Text("Скачать") }
            },
            dismissButton = {
                TextButton(onClick = { showDownloadConfirm = null }) { Text("Отмена") }
            }
        )
    }

    if (showLoginRequired) {
        LoginRequiredDialog(
            actionDescription = "скачать рецепт",
            onDismiss = { showLoginRequired = false },
            onLogin = {
                showLoginRequired = false
                onNavigateToLogin()
            }
        )
    }

    snackbarMessage?.let { msg ->
        LaunchedEffect(msg) {
            delay(2000)
            snackbarMessage = null
        }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
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