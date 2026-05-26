package com.baranov.cookbook.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.baranov.cookbook.AppContainer
import com.baranov.cookbook.CurrentUserHolder
import com.baranov.cookbook.data.database.local.LocalRecipesRepository
import com.baranov.cookbook.data.database.remote.ApiClient
import com.baranov.cookbook.data.database.remote.dto.RecipeDto
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.baranov.cookbook.ui.components.RecipeCard

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(rootNavController: NavController) {
    val innerNavController = rememberNavController()
    val repository = AppContainer.repository

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(CurrentUserHolder.currentUser?.name ?: "Гость") })
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.List, contentDescription = "Мои рецепты") },
                    label = { Text("Мои рецепты") },
                    selected = innerNavController.currentDestination?.route == "my_recipes",
                    onClick = {
                        innerNavController.navigate("my_recipes") {
                            popUpTo(innerNavController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Public, contentDescription = "Публичные рецепты") },
                    label = { Text("Публичные рецепты") },
                    selected = innerNavController.currentDestination?.route == "public_recipes",
                    onClick = {
                        innerNavController.navigate("public_recipes") {
                            popUpTo(innerNavController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = innerNavController,
            startDestination = "my_recipes",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("my_recipes") {
                MyRecipesScreen(
                    repository = repository,
                    onEditRecipe = { localId ->
                        rootNavController.navigate("recipe_editor/$localId")
                    }
                )
            }
            composable("public_recipes") {
                PublicRecipesScreen()
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MyRecipesScreen(
    repository: LocalRecipesRepository,
    onEditRecipe: (Long) -> Unit
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
                        onClick = {
                            expandedRecipeId = if (isExpanded) null else recipe.localId
                        },
                        onLongClick = { showMenuForRecipeId = recipe.localId }
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
                TextButton(onClick = {
                    showMenuForRecipeId = null
                    onEditRecipe(localId)
                }) {
                    Text("Редактировать")
                }
            },
            dismissButton = {
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
fun PublicRecipesScreen() {
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
                    onClick = {
                        expandedRecipeId = if (isExpanded) null else recipe.id
                    },
                    onLongClick = if (currentUserId != null) {
                        { showDownloadConfirm = recipe }
                    } else null
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