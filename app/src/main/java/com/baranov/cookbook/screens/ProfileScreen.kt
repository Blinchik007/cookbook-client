package com.baranov.cookbook.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.baranov.cookbook.AppContainer
import com.baranov.cookbook.CurrentUser
import com.baranov.cookbook.CurrentUserHolder
import com.baranov.cookbook.data.database.remote.ApiClient
import com.baranov.cookbook.data.database.remote.dto.UpdateUserRequest
import com.baranov.cookbook.ui.components.UserAvatar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onFinish: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentUser = CurrentUserHolder.currentUser

    // Если по какой-то причине вышли на экран без пользователя — закрываем
    LaunchedEffect(currentUser) {
        if (currentUser == null) onFinish()
    }
    if (currentUser == null) return

    var name by remember { mutableStateOf(currentUser.name) }
    var photoBase64 by remember { mutableStateOf(currentUser.photo) }
    var isSaving by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            photoBase64 = scaleAndEncodeImage(context, it)
        }
    }

    val hasChanges = name != currentUser.name || photoBase64 != currentUser.photo

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Профиль") },
                navigationIcon = {
                    IconButton(onClick = onFinish) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (!isSaving && hasChanges && name.isNotBlank()) {
                                scope.launch {
                                    isSaving = true
                                    val updated = ApiClient.updateUser(
                                        userId = currentUser.id,
                                        request = UpdateUserRequest(
                                            name = if (name != currentUser.name) name else null,
                                            photo = if (photoBase64 != currentUser.photo) photoBase64 else null
                                        )
                                    )
                                    if (updated != null) {
                                        CurrentUserHolder.currentUser = updated
                                        AppContainer.userPreferences.saveUser(updated)
                                        message = "Изменения сохранены"
                                    } else {
                                        message = "Не удалось сохранить"
                                    }
                                    isSaving = false
                                }
                            }
                        },
                        enabled = !isSaving && hasChanges && name.isNotBlank()
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Аватарка — кликабельная, открывает галерею
            Box(
                modifier = Modifier.clickable {
                    imagePickerLauncher.launch("image/*")
                }
            ) {
                UserAvatar(
                    photoBase64 = photoBase64,
                    size = 120.dp
                )
            }
            Text(
                text = "Нажмите на аватар, чтобы изменить",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Имя") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = currentUser.email,
                onValueChange = {},
                label = { Text("Email") },
                singleLine = true,
                enabled = false,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = { showPasswordDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Сменить пароль")
            }

            if (isSaving) {
                CircularProgressIndicator()
            }
        }
    }

    if (showPasswordDialog) {
        ChangePasswordDialog(
            userId = currentUser.id,
            onDismiss = { showPasswordDialog = false },
            onResult = { msg ->
                showPasswordDialog = false
                message = msg
            }
        )
    }

    // Простой снэкбар-сообщение
    message?.let { msg ->
        LaunchedEffect(msg) {
            delay(2000)
            message = null
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

@Composable
private fun ChangePasswordDialog(
    userId: Int,
    onDismiss: () -> Unit,
    onResult: (String) -> Unit
) {
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var newPasswordRepeat by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        title = { Text("Смена пароля") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = oldPassword,
                    onValueChange = { oldPassword = it },
                    label = { Text("Старый пароль") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("Новый пароль") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = newPasswordRepeat,
                    onValueChange = { newPasswordRepeat = it },
                    label = { Text("Повторите новый пароль") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when {
                        oldPassword.isBlank() -> errorMessage = "Введите старый пароль"
                        newPassword.isBlank() -> errorMessage = "Введите новый пароль"
                        newPassword != newPasswordRepeat -> errorMessage = "Пароли не совпадают"
                        else -> {
                            errorMessage = null
                            scope.launch {
                                isProcessing = true
                                val success = ApiClient.changePassword(userId, oldPassword, newPassword)
                                isProcessing = false
                                if (success) {
                                    onResult("Пароль изменён")
                                } else {
                                    errorMessage = "Неверный старый пароль"
                                }
                            }
                        }
                    }
                },
                enabled = !isProcessing
            ) {
                Text(if (isProcessing) "..." else "Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isProcessing) {
                Text("Отмена")
            }
        }
    )
}

private fun scaleAndEncodeImage(context: Context, uri: Uri): String? {
    return try {
        val input = context.contentResolver.openInputStream(uri) ?: return null
        val original = BitmapFactory.decodeStream(input)
        input.close()
        if (original == null) return null

        val maxSize = 256
        val scale = minOf(
            maxSize.toFloat() / original.width,
            maxSize.toFloat() / original.height,
            1f
        )
        val scaled = if (scale < 1f) {
            val newWidth = (original.width * scale).toInt()
            val newHeight = (original.height * scale).toInt()
            Bitmap.createScaledBitmap(original, newWidth, newHeight, true)
        } else {
            original
        }

        val output = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 80, output)
        val bytes = output.toByteArray()
        output.close()
        if (scaled != original) scaled.recycle()
        original.recycle()

        Base64.encodeToString(bytes, Base64.NO_WRAP)
    } catch (e: Exception) {
        null
    }
}