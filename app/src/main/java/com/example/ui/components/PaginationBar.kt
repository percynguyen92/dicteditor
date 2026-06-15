package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import com.example.ui.components.hazeGlassmorphism

@Composable
fun PaginationBar(
    hazeState: HazeState,
    currentPage: Int,
    totalPages: Int,
    pageSize: Int,
    filteredCount: Int,
    onFirstPage: () -> Unit,
    onPrevPage: () -> Unit,
    onNextPage: () -> Unit,
    onLastPage: () -> Unit,
    onJumpToPage: (Int) -> Unit
) {
    if (filteredCount > 0 && totalPages > 0) {
        Surface(
            color = androidx.compose.ui.graphics.Color.Transparent,
            modifier = Modifier.fillMaxWidth().hazeGlassmorphism(hazeState, cornerRadius = 0)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    FilledTonalButton(
                        onClick = onFirstPage,
                        enabled = currentPage > 0,
                        modifier = Modifier.testTag("first_page_button"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Đầu", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                    FilledTonalButton(
                        onClick = onPrevPage,
                        enabled = currentPage > 0,
                        modifier = Modifier.testTag("prev_page_button"),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.KeyboardArrowLeft,
                            contentDescription = "Trang trước",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("Trước", style = MaterialTheme.typography.bodySmall)
                    }
                }

                val currentStart = currentPage * pageSize + 1
                val currentEnd = minOf((currentPage + 1) * pageSize, filteredCount)
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "$currentStart-$currentEnd / $filteredCount từ",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    
                    var pageInputText by remember(currentPage) { mutableStateOf((currentPage + 1).toString()) }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text(
                            text = "Trang ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        BasicTextField(
                            value = pageInputText,
                            onValueChange = { newValue ->
                                val filtered = newValue.filter { it.isDigit() }
                                if (filtered.length <= 4) {
                                    pageInputText = filtered
                                    val pageNum = filtered.toIntOrNull()
                                    if (pageNum != null && pageNum in 1..totalPages) {
                                        onJumpToPage(pageNum - 1)
                                    }
                                }
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Go
                            ),
                            keyboardActions = KeyboardActions(
                                onGo = {
                                    val pageNum = pageInputText.toIntOrNull()
                                    if (pageNum != null && pageNum in 1..totalPages) {
                                        onJumpToPage(pageNum - 1)
                                    }
                                }
                            ),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            ),
                            modifier = Modifier
                                .width(42.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(vertical = 2.dp, horizontal = 4.dp)
                                .testTag("page_jump_input"),
                            decorationBox = { innerTextField ->
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (pageInputText.isEmpty()) {
                                        Text(
                                            text = "-",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                        Text(
                            text = "/$totalPages",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 2.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    FilledTonalButton(
                        onClick = onNextPage,
                        enabled = currentPage < totalPages - 1,
                        modifier = Modifier.testTag("next_page_button"),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("Sau", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.KeyboardArrowRight,
                            contentDescription = "Trang sau",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    FilledTonalButton(
                        onClick = onLastPage,
                        enabled = currentPage < totalPages - 1,
                        modifier = Modifier.testTag("last_page_button"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Cuối", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
