package com.baranov.cookbook.screens

import android.os.Build
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baranov.cookbook.AppContainer
import com.baranov.cookbook.CurrentUserHolder
import com.baranov.cookbook.data.database.remote.ApiClient
import com.baranov.cookbook.data.database.remote.dto.UpdateUserRequest
import com.baranov.cookbook.ui.components.UserAvatar
import com.baranov.cookbook.util.scaleAndEncodeImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onFinish: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentUser = CurrentUserHolder.currentUser
    val scheme = MaterialTheme.colorScheme

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
            photoBase64 = scaleAndEncodeImage(context, uri, maxSize = 256)
        }
    }

    val hasChanges = name != currentUser.name || photoBase64 != currentUser.photo

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
                        "Профиль",
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                navigationIcon = {
                    Box(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .border(1.dp, scheme.primary.copy(alpha = 0.5f), CircleShape)
                    ) {
                        IconButton(onClick = onFinish, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Назад",
                                tint = scheme.primary
                            )
                        }
                    }
                },
                actions = {
                    val canSave = !isSaving && hasChanges && name.isNotBlank()
                    Box(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .border(
                                1.dp,
                                if (canSave) scheme.primary.copy(alpha = 0.5f)
                                else scheme.onSurfaceVariant.copy(alpha = 0.3f),
                                CircleShape
                            )
                    ) {
                        IconButton(
                            onClick = {
                                if (canSave) {
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
                            enabled = canSave,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Сохранить",
                                tint = if (canSave) scheme.primary
                                else scheme.onSurfaceVariant.copy(alpha = 0.3f)
                            )
                        }
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
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(132.dp)
                    .clip(CircleShape)
                    .border(2.dp, scheme.primary.copy(alpha = 0.6f), CircleShape)
                    .padding(6.dp)
                    .clickable {
                        imagePickerLauncher.launch("image/*")
                    },
                contentAlignment = Alignment.Center
            ) {
                UserAvatar(
                    photoBase64 = photoBase64,
                    size = 120.dp
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Нажмите на аватар, чтобы изменить",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant
            )

            Spacer(Modifier.height(32.dp))

            FieldLabel("Имя")
            Spacer(Modifier.height(8.dp))
            UnderlinedField(
                value = name,
                onValueChange = { name = it },
                leadingIcon = Icons.Outlined.Person,
                placeholder = "Ваше имя"
            )

            Spacer(Modifier.height(24.dp))

            FieldLabel("Email")
            Spacer(Modifier.height(8.dp))
            UnderlinedField(
                value = currentUser.email,
                onValueChange = {},
                leadingIcon = Icons.Outlined.Email,
                placeholder = "",
                enabled = false
            )

            Spacer(Modifier.height(40.dp))

            GoldButton(
                text = "Сменить пароль",
                enabled = !isSaving,
                loading = false,
                onClick = { showPasswordDialog = true }
            )

            if (isSaving) {
                Spacer(Modifier.height(16.dp))
                CircularProgressIndicator(color = scheme.primary)
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
                shape = RoundedCornerShape(12.dp),
                color = scheme.surface,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    scheme.primary.copy(alpha = 0.5f)
                )
            ) {
                Text(
                    text = msg,
                    color = scheme.onSurface,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
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
    val scheme = MaterialTheme.colorScheme

    AlertDialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        containerColor = scheme.surface,
        titleContentColor = scheme.primary,
        textContentColor = scheme.onSurface,
        title = {
            Text(
                "Смена пароля",
                style = MaterialTheme.typography.headlineMedium
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                UnderlinedField(
                    value = oldPassword,
                    onValueChange = { oldPassword = it },
                    leadingIcon = Icons.Outlined.Lock,
                    placeholder = "Старый пароль",
                    visualTransformation = PasswordVisualTransformation()
                )
                UnderlinedField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    leadingIcon = Icons.Outlined.Lock,
                    placeholder = "Новый пароль",
                    visualTransformation = PasswordVisualTransformation()
                )
                UnderlinedField(
                    value = newPasswordRepeat,
                    onValueChange = { newPasswordRepeat = it },
                    leadingIcon = Icons.Outlined.Lock,
                    placeholder = "Повторите новый пароль",
                    visualTransformation = PasswordVisualTransformation()
                )
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = scheme.error,
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
                Text(
                    if (isProcessing) "..." else "Сохранить",
                    color = scheme.primary
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isProcessing) {
                Text("Отмена", color = scheme.onSurfaceVariant)
            }
        }
    )
}

/* ---------- Внутренние компоненты ---------- */

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 16.sp
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnderlinedField(
    value: String,
    onValueChange: (String) -> Unit,
    leadingIcon: ImageVector,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: (@Composable () -> Unit)? = null,
    enabled: Boolean = true
) {
    val scheme = MaterialTheme.colorScheme
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        enabled = enabled,
        textStyle = TextStyle(
            color = if (enabled) scheme.onBackground else scheme.onSurfaceVariant,
            fontSize = 14.sp
        ),
        placeholder = {
            Text(placeholder, color = scheme.onSurfaceVariant.copy(alpha = 0.5f), fontSize = 14.sp)
        },
        leadingIcon = { Icon(leadingIcon, contentDescription = null, tint = scheme.primary) },
        trailingIcon = trailingIcon,
        visualTransformation = visualTransformation,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            focusedIndicatorColor = scheme.primary,
            unfocusedIndicatorColor = scheme.outline,
            disabledIndicatorColor = scheme.outline.copy(alpha = 0.5f),
            cursorColor = scheme.primary,
            disabledTextColor = scheme.onSurfaceVariant,
            disabledLeadingIconColor = scheme.primary.copy(alpha = 0.6f)
        )
    )
}

@Composable
private fun GoldButton(
    text: String,
    enabled: Boolean,
    loading: Boolean,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val borderBrush = Brush.horizontalGradient(listOf(scheme.primary, scheme.secondary))
    val fillBrush = Brush.horizontalGradient(
        listOf(scheme.primary.copy(alpha = 0.10f), scheme.secondary.copy(alpha = 0.10f))
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(fillBrush)
            .border(1.dp, borderBrush, RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            CircularProgressIndicator(
                color = scheme.primary,
                strokeWidth = 2.dp,
                modifier = Modifier.size(24.dp)
            )
        } else {
            Text(
                text = text,
                color = scheme.primary,
                style = MaterialTheme.typography.headlineMedium
            )
        }
    }
}