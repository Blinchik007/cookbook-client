package com.baranov.cookbook.screens

import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PhotoCamera
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
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var cookingInstructions by remember { mutableStateOf("") }
    var photoBase64 by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            photoBase64 = uriToBase64(context, it)
            android.util.Log.d("PhotoDebug", "Base64 length: ${photoBase64?.length}, starts with: ${photoBase64?.take(30)}")
        }
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
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isNew) "Новый рецепт" else "Редактирование") },
                navigationIcon = {
                    IconButton(onClick = onFinish) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
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
                                        if (isNew) {
                                            val currentUserId = CurrentUserHolder.currentUser?.id
                                            repository.createRecipeLocal(
                                                ownerUserId = currentUserId,
                                                authorId = currentUserId,
                                                title = title,
                                                description = description.ifBlank { null },
                                                cookingInstructions = cookingInstructions,
                                                photo = photoBase64,
                                                products = emptyList()
                                            )
                                        } else {
                                            repository.updateRecipe(
                                                localId = recipeLocalId,
                                                title = title,
                                                description = description.ifBlank { null },
                                                cookingInstructions = cookingInstructions,
                                                photo = photoBase64,
                                                products = null
                                            )
                                        }
                                        onFinish()
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            }
                        },
                        enabled = !isLoading
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
                    runCatching { android.util.Base64.decode(photoBase64, android.util.Base64.DEFAULT) }
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
}

private fun uriToBase64(context: Context, uri: Uri): String {
    val inputStream = context.contentResolver.openInputStream(uri) ?: return ""
    val bytes = inputStream.readBytes()
    inputStream.close()
    return Base64.encodeToString(bytes, Base64.NO_WRAP)
}