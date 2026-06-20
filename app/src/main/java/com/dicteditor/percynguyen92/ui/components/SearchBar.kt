package com.dicteditor.percynguyen92.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.dicteditor.percynguyen92.ui.components.glassTextFieldColors

@Composable
fun SearchBar(
    searchQuery: String,
    searchUseRegex: Boolean,
    searchMatchCase: Boolean,
    isReplaceMode: Boolean,
    replaceQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onReplaceQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onToggleRegex: () -> Unit,
    onToggleMatchCase: () -> Unit,
    onReplaceClick: () -> Unit,
    onCloseReplaceMode: () -> Unit,
    searchError: String? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))
        // Search input
        CustomOutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            isError = searchError != null,
            placeholder = { Text("Tìm kiếm...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Tìm kiếm") },
            trailingIcon = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = onClearSearch,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Xóa tìm kiếm",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onToggleMatchCase,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("toggle_match_case_button")
                    ) {
                        Text(
                            text = "Aa",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = if (searchMatchCase) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = onToggleRegex,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("toggle_regex_button")
                    ) {
                        Text(
                            text = ".*",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = if (searchUseRegex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            shape = RoundedCornerShape(12.dp),
            colors = glassTextFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_view_input")
        )
        // Replace row — hiển thị khi isReplaceMode = true
        AnimatedVisibility(
            visible = isReplaceMode,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CustomOutlinedTextField(
                    value = replaceQuery,
                    onValueChange = onReplaceQueryChange,
                    placeholder = { Text("Thay thế bằng...") },
                    shape = RoundedCornerShape(12.dp),
                    colors = glassTextFieldColors(),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("replace_view_input")
                )

                Button(
                    onClick = onReplaceClick,
                    enabled = searchQuery.isNotEmpty(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("replace_button").fillMaxHeight()
                ) {
                    Text(
                        text = "Thay thế",
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                IconButton(
                    onClick = onCloseReplaceMode,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("close_replace_mode_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Đóng chế độ thay thế",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean = false,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(12.dp),
    colors: TextFieldColors = glassTextFieldColors(),
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.height(44.dp),
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onSurface
        ),
        singleLine = true,
        interactionSource = interactionSource,
        decorationBox = @Composable { innerTextField ->
            OutlinedTextFieldDefaults.DecorationBox(
                value = value,
                innerTextField = innerTextField,
                enabled = true,
                singleLine = true,
                visualTransformation = VisualTransformation.None,
                interactionSource = interactionSource,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                placeholder = placeholder,
                leadingIcon = leadingIcon,
                trailingIcon = trailingIcon,
                container = {
                    OutlinedTextFieldDefaults.Container(
                        enabled = true,
                        isError = isError,
                        interactionSource = interactionSource,
                        colors = colors,
                        shape = shape,
                    )
                }
            )
        }
    )
}
