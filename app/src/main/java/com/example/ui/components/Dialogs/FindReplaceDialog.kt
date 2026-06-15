package com.example.ui.components.Dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.glassTextFieldColors

@Composable
fun FindReplaceDialog(
    onDismiss: () -> Unit,
    onReplaceAll: (find: String, replace: String, useRegex: Boolean) -> Unit
) {
    var findText by remember { mutableStateOf("") }
    var replaceText by remember { mutableStateOf("") }
    var useRegex by remember { mutableStateOf(false) }
    var findError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Tìm & Thay thế",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = findText,
                    onValueChange = {
                        findText = it
                        if (it.isNotEmpty()) findError = null
                    },
                    label = { Text(if (useRegex) "Biểu thức Regex" else "Tìm chuỗi") },
                    placeholder = { Text(if (useRegex) "E.g. ^rồng$" else "E.g. rồng") },
                    singleLine = true,
                    isError = findError != null,
                    supportingText = {
                        if (findError != null) {
                            Text(findError!!, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    colors = glassTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = replaceText,
                    onValueChange = { replaceText = it },
                    label = { Text("Thay thế bằng") },
                    placeholder = { Text("E.g. long vương") },
                    singleLine = true,
                    colors = glassTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { useRegex = !useRegex }
                ) {
                    Checkbox(
                        checked = useRegex,
                        onCheckedChange = { useRegex = it }
                    )
                    Text("Sử dụng Regular Expression (Regex)", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (findText.isEmpty()) {
                        findError = "Chuỗi tìm kiếm không được để trống"
                    } else if (useRegex) {
                        try {
                            Regex(findText)
                            onReplaceAll(findText, replaceText, true)
                        } catch (e: Exception) {
                            findError = "Regex không hợp lệ"
                        }
                    } else {
                        onReplaceAll(findText, replaceText, false)
                    }
                }
            ) {
                Text("Replace All")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}
