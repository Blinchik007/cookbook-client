package com.baranov.cookbook.screens

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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baranov.cookbook.AppContainer
import com.baranov.cookbook.CurrentUserHolder
import com.baranov.cookbook.data.database.remote.ApiClient
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onNavigateToMain: () -> Unit,
    onNavigateToRegistration: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val scheme = MaterialTheme.colorScheme

    // Радиальный градиент фона
    val bgBrush = Brush.radialGradient(
        colors = listOf(scheme.surfaceVariant, scheme.background, scheme.background),
        radius = 1400f
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 54.dp, bottom = 32.dp)
        ) {
            if (onBack != null) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .border(1.dp, scheme.primary.copy(alpha = 0.5f), CircleShape)
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Назад",
                            tint = scheme.primary
                        )
                    }
                }
            }

            Spacer(Modifier.height(73.dp))
            Text(
                text  = "Вход",
                color = scheme.primary,
                style = MaterialTheme.typography.displayMedium
            )

            Spacer(Modifier.height(32.dp))

            FieldLabel("Email")
            Spacer(Modifier.height(8.dp))
            UnderlinedField(
                value           = email,
                onValueChange   = { email = it },
                leadingIcon     = Icons.Outlined.Email,
                placeholder     = "your@email.com",
                keyboardType    = KeyboardType.Email
            )

            Spacer(Modifier.height(24.dp))

            FieldLabel("Пароль")
            Spacer(Modifier.height(8.dp))
            UnderlinedField(
                value         = password,
                onValueChange = { password = it },
                leadingIcon   = Icons.Outlined.Lock,
                placeholder   = "••••••••",
                keyboardType  = KeyboardType.Password,
                visualTransformation = if (passwordVisible) VisualTransformation.None
                else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff
                            else Icons.Outlined.Visibility,
                            contentDescription = null,
                            tint = scheme.onSurfaceVariant
                        )
                    }
                }
            )

            if (errorMessage != null) {
                Spacer(Modifier.height(12.dp))
                Text(errorMessage!!, color = scheme.error, fontSize = 14.sp)
            }

            Spacer(Modifier.height(40.dp))

            GoldButton(
                text    = "Войти",
                enabled = !isLoading && email.isNotBlank() && password.isNotBlank(),
                loading = isLoading,
                onClick = {
                    isLoading = true
                    errorMessage = null
                    scope.launch {
                        try {
                            val user = ApiClient.login(email, password)
                            CurrentUserHolder.currentUser = user
                            if (user != null) {
                                AppContainer.userPreferences.saveUser(user)
                                AppContainer.onAuthChanged()
                            }
                            onNavigateToMain()
                        } catch (e: Exception) {
                            errorMessage = "Неверный email или пароль"
                        } finally {
                            isLoading = false
                        }
                    }
                }
            )

            Spacer(Modifier.height(24.dp))

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text     = "Нет аккаунта?  ",
                    color    = scheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
                Text(
                    text     = "Регистрация",
                    color    = scheme.onBackground,
                    fontSize = 14.sp,
                    modifier = Modifier.clickable(onClick = onNavigateToRegistration)
                )
            }
        }
    }
}

/* ---------- Внутренние компоненты ---------- */

@Composable
private fun FieldLabel(text: String) {
    Text(
        text     = text,
        color    = MaterialTheme.colorScheme.onSurfaceVariant,
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
    trailingIcon: (@Composable () -> Unit)? = null
) {
    val scheme = MaterialTheme.colorScheme
    TextField(
        value         = value,
        onValueChange = onValueChange,
        modifier      = Modifier.fillMaxWidth(),
        singleLine    = true,
        textStyle     = TextStyle(color = scheme.onBackground, fontSize = 14.sp),
        placeholder   = {
            Text(placeholder, color = scheme.onSurfaceVariant.copy(alpha = 0.5f), fontSize = 14.sp)
        },
        leadingIcon   = { Icon(leadingIcon, contentDescription = null, tint = scheme.primary) },
        trailingIcon  = trailingIcon,
        visualTransformation = visualTransformation,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = TextFieldDefaults.colors(
            focusedContainerColor    = Color.Transparent,
            unfocusedContainerColor  = Color.Transparent,
            disabledContainerColor   = Color.Transparent,
            focusedIndicatorColor    = scheme.primary,
            unfocusedIndicatorColor  = scheme.outline,
            cursorColor              = scheme.primary
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
                color       = scheme.primary,
                strokeWidth = 2.dp,
                modifier    = Modifier.size(24.dp)
            )
        } else {
            // Кнопка "Войти" тоже мелком — это стиль макета
            Text(
                text  = text,
                color = scheme.primary,
                style = MaterialTheme.typography.headlineMedium
            )
        }
    }
}