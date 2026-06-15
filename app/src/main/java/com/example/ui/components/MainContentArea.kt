package com.example.ui.components

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.DictEntry
import dev.chrisbanes.haze.HazeState

@Composable
fun MainContentArea(
    hazeState: HazeState,
    openedFileUri: Uri?,
    isLoading: Boolean,
    displayEntries: List<DictEntry>,
    recentFiles: List<Uri>,
    searchQuery: String,
    selectedIds: Map<String, Boolean>,
    highlightedIds: Set<String>,
    isBulkMode: Boolean,
    onFileClick: (Uri) -> Unit,
    onOpenNewClick: () -> Unit,
    onSearchClear: () -> Unit,
    onAddWordClick: () -> Unit,
    onEditClick: (DictEntry) -> Unit,
    onDeleteConfirm: (String) -> Unit,
    onSelectedChange: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        if (openedFileUri == null) {
            RecentFilesView(
                hazeState = hazeState,
                recentFiles = recentFiles,
                onFileClick = onFileClick,
                onOpenNewClick = onOpenNewClick
            )
        } else if (displayEntries.isEmpty() && !isLoading) {
            if (searchQuery.isNotEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.Search,
                    title = "Không tìm thấy kết quả",
                    description = "Không có từ nào chứa cụm từ '$searchQuery' trong file từ điển của bạn.",
                    buttonText = "Xóa lọc",
                    onButtonClick = onSearchClear
                )
            } else {
                EmptyStateView(
                    icon = Icons.Default.Info,
                    title = "Danh sách trống",
                    description = "File từ điển của bạn không chứa dữ liệu hợp lệ hoặc chưa thêm từ nào.",
                    buttonText = "Thêm từ mới",
                    onButtonClick = onAddWordClick
                )
            }
        } else {
            // Lazy vertical dictionary elements list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(displayEntries, key = { it.id }) { entry ->
                    DictEntryItemRow(
                        hazeState = hazeState,
                        entry = entry,
                        onEditClick = { onEditClick(entry) },
                        onDeleteConfirm = { onDeleteConfirm(entry.id) },
                        selected = selectedIds[entry.id] == true,
                        onSelectedChange = { isChecked ->
                            onSelectedChange(entry.id, isChecked)
                        },
                        isHighlighted = highlightedIds.contains(entry.id),
                        isBulkMode = isBulkMode
                    )
                }
            }
        }

        // Loader block
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}
