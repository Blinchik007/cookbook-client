package com.baranov.cookbook.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecipeCard(
    title: String,
    description: String?,
    photoBase64: String?,
    badge: String? = null,
    expanded: Boolean,
    onClick: () -> Unit,
    onToggleExpand: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    loadIngredients: (suspend () -> List<IngredientDisplayItem>)? = null
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme

    val photoBytes = remember(photoBase64) {
        if (photoBase64.isNullOrBlank()) null
        else runCatching {
            android.util.Base64.decode(photoBase64, android.util.Base64.DEFAULT)
        }.getOrNull()
    }

    var ingredients by remember(loadIngredients) { mutableStateOf<List<IngredientDisplayItem>?>(null) }
    var ingredientsLoading by remember(loadIngredients) { mutableStateOf(false) }

    LaunchedEffect(expanded, loadIngredients) {
        if (expanded && ingredients == null && loadIngredients != null && !ingredientsLoading) {
            ingredientsLoading = true
            ingredients = runCatching { loadIngredients() }.getOrDefault(emptyList())
            ingredientsLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(scheme.surface)
            .border(1.dp, scheme.primary.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        // ====== Фото с плашкой названия поверх ======
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            if (photoBytes != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(photoBytes)
                        .size(800, 400)
                        .crossfade(true)
                        .build(),
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(scheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Restaurant,
                        contentDescription = null,
                        tint = scheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(64.dp)
                    )
                }
            }

            // Градиент по низу фото — чтобы плашка с названием читалась
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.5f)
                            )
                        )
                    )
            )

            // Плашка названия в правом нижнем углу
            Surface(
                color = Color.Black.copy(alpha = 0.55f),
                shape = RoundedCornerShape(topStart = 12.dp, bottomEnd = 16.dp),
                modifier = Modifier.align(Alignment.BottomEnd)
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 22.sp),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Бейдж "скачано" в левом верхнем углу
            if (badge != null) {
                Surface(
                    color = scheme.primary.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(bottomEnd = 12.dp, topStart = 16.dp),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Text(
                        text = badge,
                        color = scheme.onPrimary,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // ====== Развёрнутая часть (описание + ингредиенты) ======
        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier.padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 12.dp,
                    bottom = 4.dp
                )
            ) {
                if (!description.isNullOrBlank()) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurface
                    )
                    Spacer(Modifier.height(8.dp))
                }
                when {
                    ingredientsLoading -> {
                        Text(
                            text = "Загрузка ингредиентов…",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant
                        )
                    }
                    ingredients != null -> {
                        IngredientsCompactRow(items = ingredients!!)
                    }
                }
            }
        }

        // ====== Кнопка-стрелочка для разворота ======
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onToggleExpand,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess
                    else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Свернуть" else "Развернуть",
                    tint = scheme.primary
                )
            }
        }
    }
}