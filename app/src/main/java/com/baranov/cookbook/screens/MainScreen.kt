package com.baranov.cookbook

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(rootNavController: NavController) {
    val innerNavController = rememberNavController()

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
                    onEditRecipe = { recipeId ->
                        rootNavController.navigate("recipe_editor/$recipeId")
                    }
                )
            }
            composable("public_recipes") {
                PublicRecipesScreen()
            }
        }
    }
}

@Composable
fun MyRecipesScreen(onEditRecipe: (Int) -> Unit) {
    val recipes = RecipeRepository.recipes
    var expandedRecipeIndex by remember { mutableStateOf<Int?>(null) }
    var showMenuForRecipeId by remember { mutableStateOf<Int?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        itemsIndexed(recipes) { index, recipe ->
            val isExpanded = expandedRecipeIndex == index
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { expandedRecipeIndex = if (isExpanded) null else index },
                        onLongClick = { showMenuForRecipeId = recipe.id }
                    ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = recipe.imageRes),
                        contentDescription = recipe.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Text(
                        text = recipe.title,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (isExpanded) {
                        Text(
                            text = recipe.description,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }

    if (showMenuForRecipeId != null) {
        AlertDialog(
            onDismissRequest = { showMenuForRecipeId = null },
            title = { Text("Действие") },
            text = { Text("Выберите действие для рецепта") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onEditRecipe(showMenuForRecipeId!!)
                        showMenuForRecipeId = null
                    }
                ) {
                    Text("Редактировать")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMenuForRecipeId = null }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
fun PublicRecipesScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Публичные рецепты")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeEditorScreen(recipeId: Int, onFinish: (Recipe) -> Unit) {
    val isNew = recipeId == -1
    val existingRecipe = if (!isNew) RecipeRepository.getRecipeById(recipeId) else null

    var title by remember { mutableStateOf(existingRecipe?.title ?: "") }
    var description by remember { mutableStateOf(existingRecipe?.description ?: "") }
    var longDescription by remember { mutableStateOf(existingRecipe?.longDescription ?: "") }
    var imageRes by remember { mutableStateOf(existingRecipe?.imageRes ?: R.drawable.placeholder1) }

    LaunchedEffect(existingRecipe) {
        if (existingRecipe != null) {
            title = existingRecipe.title
            description = existingRecipe.description
            longDescription = existingRecipe.longDescription
            imageRes = existingRecipe.imageRes
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isNew) "Новый рецепт" else "Редактирование") },
                navigationIcon = {
                    IconButton(onClick = { onFinish(Recipe(0, 0, "", "", "")) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (title.isNotBlank() && description.isNotBlank()) {
                                val newId = if (isNew) System.currentTimeMillis().toInt() else recipeId
                                val recipe = Recipe(
                                    id = newId,
                                    imageRes = imageRes,
                                    title = title,
                                    description = description,
                                    longDescription = longDescription
                                )
                                onFinish(recipe)
                            }
                        }
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Закончить")
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = "Фото блюда",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
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
                label = { Text("Короткое описание") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
            OutlinedTextField(
                value = longDescription,
                onValueChange = { longDescription = it },
                label = { Text("Полное описание (инструкция)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 6
            )
        }
    }
}