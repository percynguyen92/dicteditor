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
import androidx.compose.ui.res.stringResource
import com.dicteditor.percynguyen92.R
import com.dicteditor.percynguyen92.utils.getFileName
import dev.chrisbanes.haze.HazeState
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape

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
    onCloseReplaceMode: () -> Unit,
    searchError: String? = null,
    isAtpConnected: Boolean = false
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
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val noFileSelected = stringResource(R.string.label_no_file_selected)
                        val fileName by produceState(initialValue = "...", key1 = openedFileUri) {
                            value = if (openedFileUri != null) {
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                    getFileName(context, openedFileUri)
                                }
                            } else {
                                noFileSelected
                            }
                        }

                        val titleStyle = MaterialTheme.typography.titleMedium.toSpanStyle().copy(
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val wordCountStyle = MaterialTheme.typography.labelSmall.toSpanStyle().copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        val wordCountFormat = stringResource(R.string.label_word_count_format, totalWords)

                        Text(
                            text = buildAnnotatedString {
                                withStyle(titleStyle) {
                                    append(fileName)
                                }
                                if (openedFileUri != null) {
                                    withStyle(wordCountStyle) {
                                        append(wordCountFormat)
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
                                    text = stringResource(R.string.badge_unsaved),
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
                                    contentDescription = stringResource(R.string.description_undo),
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
                                    contentDescription = stringResource(R.string.description_redo),
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
                                    contentDescription = stringResource(R.string.description_save_file),
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
                                    contentDescription = stringResource(R.string.description_menu)
                                )
                            }

                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false },
                                containerColor = Color.Transparent,
                                modifier = Modifier.hazeGlassmorphism(hazeState, cornerRadius = 12)
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.menu_open_file)) },
                                    leadingIcon = { Icon(Icons.Default.List, contentDescription = stringResource(R.string.menu_open_file)) },
                                    onClick = {
                                        menuExpanded = false
                                        onOpenFileClick()
                                    },
                                    modifier = Modifier.testTag("open_file_menu")
                                )
                                HorizontalDivider()
                                if (openedFileUri != null) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.menu_sort_desc)) },
                                        leadingIcon = { Icon(Icons.Default.List, contentDescription = stringResource(R.string.menu_sort_desc)) },
                                        onClick = {
                                            menuExpanded = false
                                            onSortDefaultLengthDescending()
                                        },
                                        modifier = Modifier.testTag("sort_by_length_desc_menu")
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.menu_sort_asc)) },
                                        leadingIcon = { Icon(Icons.Default.List, contentDescription = stringResource(R.string.menu_sort_asc)) },
                                        onClick = {
                                            menuExpanded = false
                                            onSortLengthAscending()
                                        },
                                        modifier = Modifier.testTag("sort_by_length_asc_menu")
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.menu_find_replace)) },
                                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.menu_find_replace)) },
                                        onClick = {
                                            menuExpanded = false
                                            onFindReplaceClick()
                                        },
                                        modifier = Modifier.testTag("find_replace_menu")
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.menu_import)) },
                                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = stringResource(R.string.menu_import)) },
                                        onClick = {
                                            menuExpanded = false
                                            onBatchImportClick()
                                        },
                                        modifier = Modifier.testTag("batch_import_menu")
                                    )
                                }
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .background(
                                                        color = if (isAtpConnected) Color(0xFF4CAF50) else Color.Gray,
                                                        shape = CircleShape
                                                    )
                                            )
                                            Text(if (isAtpConnected) stringResource(R.string.label_atp_online) else stringResource(R.string.label_atp_offline))
                                        }
                                    },
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
                    val defaultStatus = if (openedFileUri == null) stringResource(R.string.status_pick_file) else stringResource(R.string.status_ready)
                    Text(
                        text = statusMessage.ifEmpty { defaultStatus },
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
                        onCloseReplaceMode = onCloseReplaceMode,
                        searchError = searchError
                    )
                }
            }
        }
    }
}
