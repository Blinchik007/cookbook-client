package com.baranov.cookbook

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.baranov.cookbook.network.ApiClient
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onNavigateToMain: () -> Unit,  // больше не передаём параметры, сессия уже установлена
    onNavigateToRegistration: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Пароль") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            if (errorMessage != null) {
                Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(8.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    if (email.isNotBlank() && password.isNotBlank()) {
                        isLoading = true
                        errorMessage = null
                        scope.launch {
                            try {
                                val user = ApiClient.getUserByEmail(email)
                                if (user != null) {
                                    val passwordHash = ApiClient.hashPassword(password)
                                    if (user.passwordHash == passwordHash) {
                                        Session.userId = user.id
                                        Session.userName = user.name
                                        onNavigateToMain()
                                    } else {
                                        errorMessage = "Неверный пароль"
                                    }
                                } else {
                                    errorMessage = "Пользователь не найден"
                                }
                            } catch (e: Exception) {
                                errorMessage = e.message ?: "Ошибка сети"
                            } finally {
                                isLoading = false
                            }
                        }
                    }
                },
                enabled = email.isNotBlank() && password.isNotBlank() && !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Text("Вход")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = onNavigateToRegistration,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Нет аккаунта? Зарегистрироваться")
            }
        }
    }
}