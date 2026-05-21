package com.baranov.cookbook

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.baranov.cookbook.network.ApiClient
import com.baranov.cookbook.network.dto.CreateUserRequest
import kotlinx.coroutines.launch

@Composable
fun AuthorizationScreen(onNavigateToMain: () -> Unit) {
    var name by remember { mutableStateOf("") }
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
                value = name,
                onValueChange = { name = it },
                label = { Text("Имя") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
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
                    if (name.isNotBlank() && email.isNotBlank() && password.isNotBlank()) {
                        isLoading = true
                        errorMessage = null
                        scope.launch {
                            try {
                                val passwordHash = ApiClient.hashPassword(password)
                                val userId = ApiClient.createUser(
                                    CreateUserRequest(
                                        name = name,
                                        email = email,
                                        passwordHash = passwordHash,
                                        photo = null
                                    )
                                )
                                if (userId != null) {
                                    Session.userId = userId
                                    Session.userName = name
                                    onNavigateToMain()
                                } else {
                                    errorMessage = "Ошибка регистрации"
                                }
                            } catch (e: Exception) {
                                errorMessage = e.message ?: "Неизвестная ошибка"
                            } finally {
                                isLoading = false
                            }
                        }
                    }
                },
                enabled = name.isNotBlank() && email.isNotBlank() && password.isNotBlank() && !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Text("Зарегистрироваться")
                }
            }
        }
    }
}