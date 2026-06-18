package com.dicteditor.percynguyen92.ui.components

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.dicteditor.percynguyen92.utils.getFileName
import dev.chrisbanes.haze.HazeState

@Composable
fun AppTopBar(
    hazeState: HazeState,
    openedFileUri: Uri?,
    totalWords: Int,
    hasUnsavedChanges: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    searchQuery: String,
    searchUseRegex: Boolean,
    searchMatchCase: Boolean,
    statusMessage: String,
    isReplaceMode: Boolean,
    replaceQuery: String,
    onUndoClick: () -> Unit,
    onRedoClick: () -> Unit,
    onSaveClick: () -> Unit,
    onOpenFileClick: () -> Unit,
    onToggleRegex: () -> Unit,
    onToggleMatchCase: () -> Unit,
    onSortDefaultLengthDescending: () -> Unit,
    onSortLengthAscending: () -> Unit,
    onFindReplaceClick: () -> Unit,
    onBatchImportClick: () -> Unit,
    onCheckAiConnectionClick: () -> Unit,
    onExitClick: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onReplaceQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onReplaceClick: () -> Unit,
    onCloseReplaceMode: () -> Unit
) {
    val context = LocalContext.current
    var menuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        // High-fidelity integrated Toolbar and Stat indicators
        Surface(
            color = Color.Transparent,
            modifier = Modifier
                .fillMaxWidth()
                .hazeGlassmorphism(
                    state = hazeState,
                    shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "DictEditor",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val fileName by produceState(initialValue = "...", key1 = openedFileUri) {
                            value = if (openedFileUri != null) {
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                    getFileName(context, openedFileUri)
                                }
                            } else {
                                "Chưa chọn file"
                            }
                        }

                        val titleStyle = MaterialTheme.typography.titleMedium.toSpanStyle().copy(
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val wordCountStyle = MaterialTheme.typography.labelSmall.toSpanStyle().copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = buildAnnotatedString {
                                withStyle(titleStyle) {
                                    append(fileName)
                                }
                                if (openedFileUri != null) {
                                    withStyle(wordCountStyle) {
                                        append(" • $totalWords từ")
                                    }
                                }
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // State Status Badges
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (hasUnsavedChanges) {
                            Surface(
                                color = com.dicteditor.percynguyen92.ui.theme.DarkColors.UnsavedBadge,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Text(
                                    text = "Chưa lưu",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        if (openedFileUri != null) {
                            IconButton(
                                onClick = onUndoClick,
                                enabled = canUndo,
                                modifier = Modifier.testTag("undo_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Hoàn tác",
                                    tint = if (canUndo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                )
                            }
                            IconButton(
                                onClick = onRedoClick,
                                enabled = canRedo,
                                modifier = Modifier.testTag("redo_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = "Làm lại",
                                    tint = if (canRedo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                )
                            }
                            IconButton(
                                onClick = onSaveClick,
                                enabled = hasUnsavedChanges,
                                modifier = Modifier.testTag("save_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Lưu file",
                                    tint = if (hasUnsavedChanges) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                )
                            }
                        }

                        // Overflow menu
                        Box {
                            IconButton(
                                onClick = { menuExpanded = true },
                                modifier = Modifier.testTag("overflow_menu_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Thao tác menu"
                                )
                            }

                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false },
                                containerColor = Color.Transparent,
                                modifier = Modifier.hazeGlassmorphism(hazeState, cornerRadius = 12)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Mở file") },
                                    leadingIcon = { Icon(Icons.Default.List, contentDescription = "Open file") },
                                    onClick = {
                                        menuExpanded = false
                                        onOpenFileClick()
                                    },
                                    modifier = Modifier.testTag("open_file_menu")
                                )
                                HorizontalDivider()
                                if (openedFileUri != null) {
                                    DropdownMenuItem(
                                        text = { Text("Sắp xếp: Dài → Ngắn") },
                                        leadingIcon = { Icon(Icons.Default.List, contentDescription = "Sort Long to Short") },
                                        onClick = {
                                            menuExpanded = false
                                            onSortDefaultLengthDescending()
                                        },
                                        modifier = Modifier.testTag("sort_by_length_desc_menu")
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Sắp xếp: Ngắn → Dài") },
                                        leadingIcon = { Icon(Icons.Default.List, contentDescription = "Sort Short to Long") },
                                        onClick = {
                                            menuExpanded = false
                                            onSortLengthAscending()
                                        },
                                        modifier = Modifier.testTag("sort_by_length_asc_menu")
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Tìm & Thay thế") },
                                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = "Find replace") },
                                        onClick = {
                                            menuExpanded = false
                                            onFindReplaceClick()
                                        },
                                        modifier = Modifier.testTag("find_replace_menu")
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Import") },
                                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = "Batch import") },
                                        onClick = {
                                            menuExpanded = false
                                            onBatchImportClick()
                                        },
                                        modifier = Modifier.testTag("batch_import_menu")
                                    )
                                }
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("Kiểm tra kết nối AI") },
                                    leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = "Check AI Connection") },
                                    onClick = {
                                        menuExpanded = false
                                        onCheckAiConnectionClick()
                                    },
                                    modifier = Modifier.testTag("check_ai_connection_menu")
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Status Bar and Counters
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = statusMessage.ifEmpty { if (openedFileUri == null) "Hãy chọn một file để bắt đầu" else "Sẵn sàng" },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (openedFileUri != null) {
                    SearchBar(
                        searchQuery = searchQuery,
                        searchUseRegex = searchUseRegex,
                        searchMatchCase = searchMatchCase,
                        isReplaceMode = isReplaceMode,
                        replaceQuery = replaceQuery,
                        onSearchQueryChange = onSearchQueryChange,
                        onReplaceQueryChange = onReplaceQueryChange,
                        onClearSearch = onClearSearch,
                        onToggleRegex = onToggleRegex,
                        onToggleMatchCase = onToggleMatchCase,
                        onReplaceClick = onReplaceClick,
                        onCloseReplaceMode = onCloseReplaceMode
                    )
                }
            }
        }
    }
}
