package com.baranov.cookbook.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.baranov.cookbook.screens.recipeEditor.MeasurementUnits

/**
 * BottomSheet для создания нового продукта.
 *
 * @param initialName предзаполненное имя (то, что пользователь набирал в автокомплите)
 * @param onDismiss закрытие без создания
 * @param onCreate создать продукт (название, единица). Внешний код делает запрос.
 * @param errorMessage сообщение об ошибке, если предыдущая попытка не прошла
 * @param isCreating флаг "создаётся" (для блокировки кнопки и спиннера)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateProductBottomSheet(
    initialName: String,
    onDismiss: () -> Unit,
    onCreate: (name: String, measurementUnit: String) -> Unit,
    errorMessage: String? = null,
    isCreating: Boolean = false
) {
    var name by remember { mutableStateOf(initialName) }
    var unit by remember { mutableStateOf(MeasurementUnits.GRAMS) }
    var unitMenuExpanded by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = { if (!isCreating) onDismiss() },
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Новый продукт",
                style = MaterialTheme.typography.titleLarge
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Название") },
                singleLine = true,
                enabled = !isCreating,
                modifier = Modifier.fillMaxWidth()
            )

            // Выпадашка единицы измерения
            ExposedDropdownMenuBox(
                expanded = unitMenuExpanded,
                onExpandedChange = { if (!isCreating) unitMenuExpanded = !unitMenuExpanded }
            ) {
                OutlinedTextField(
                    value = unit,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Единица измерения") },
                    trailingIcon = {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    },
                    enabled = !isCreating,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = unitMenuExpanded,
                    onDismissRequest = { unitMenuExpanded = false }
                ) {
                    MeasurementUnits.all.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                unit = option
                                unitMenuExpanded = false
                            }
                        )
                    }
                }
            }

            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isCreating) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                }
                TextButton(
                    onClick = onDismiss,
                    enabled = !isCreating
                ) {
                    Text("Отмена")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { onCreate(name.trim(), unit) },
                    enabled = !isCreating && name.isNotBlank()
                ) {
                    Text("Создать")
                }
            }
        }
    }
}