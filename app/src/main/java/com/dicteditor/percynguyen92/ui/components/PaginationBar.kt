package com.dicteditor.percynguyen92.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardDoubleArrowLeft
import androidx.compose.material.icons.filled.KeyboardDoubleArrowRight
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
import com.dicteditor.percynguyen92.ui.components.hazeGlassmorphism
import com.dicteditor.percynguyen92.ui.components.staticGlassmorphism
import kotlinx.coroutines.delay

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
        val isKeyboardVisible = WindowInsets.ime.asPaddingValues().calculateBottomPadding() > 0.dp
        Surface(
            color = androidx.compose.ui.graphics.Color.Transparent,
            modifier = Modifier
                .fillMaxWidth()
                .hazeGlassmorphism(
                    state = hazeState,
                    shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 6.dp,
                        bottom = if (isKeyboardVisible) 16.dp else 6.dp
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    OutlinedButton(
                        onClick = onFirstPage,
                        enabled = currentPage > 0,
                        modifier = Modifier.testTag("first_page_button"),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardDoubleArrowLeft,
                            contentDescription = "Đầu trang",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    OutlinedButton(
                        onClick = onPrevPage,
                        enabled = currentPage > 0,
                        modifier = Modifier.testTag("prev_page_button"),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.KeyboardArrowLeft,
                            contentDescription = "Trang trước",
                            modifier = Modifier.size(18.dp)
                        )
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
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.Center
                    )
                    
                    var pageInputText by remember(currentPage) { mutableStateOf((currentPage + 1).toString()) }

                    LaunchedEffect(pageInputText) {
                        // Debounce jump to avoid jumping mid-typing
                        delay(300)
                        val pageNum = pageInputText.toIntOrNull()
                        if (pageNum != null && pageNum in 1..totalPages && pageNum != currentPage + 1) {
                            onJumpToPage(pageNum - 1)
                        }
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text(
                            text = "Trang ",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        BasicTextField(
                            value = pageInputText,
                            onValueChange = { newValue ->
                                val filtered = newValue.filter { it.isDigit() }
                                if (filtered.length <= 4) {
                                    pageInputText = filtered
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
                            textStyle = MaterialTheme.typography.labelMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            ),
                            modifier = Modifier
                                .width(42.dp)
                                .staticGlassmorphism(cornerRadius = 4)
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
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                        Text(
                            text = "/$totalPages",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 2.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    OutlinedButton(
                        onClick = onNextPage,
                        enabled = currentPage < totalPages - 1,
                        modifier = Modifier.testTag("next_page_button"),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.KeyboardArrowRight,
                            contentDescription = "Trang sau",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    OutlinedButton(
                        onClick = onLastPage,
                        enabled = currentPage < totalPages - 1,
                        modifier = Modifier.testTag("last_page_button"),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardDoubleArrowRight,
                            contentDescription = "Trang cuối",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
