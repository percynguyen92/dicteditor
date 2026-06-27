package com.dicteditor.percynguyen92.ui.components.DictEntriesList

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import com.dicteditor.percynguyen92.R
import com.dicteditor.percynguyen92.data.model.DictEntry
import com.dicteditor.percynguyen92.data.model.InvalidLine
import dev.chrisbanes.haze.HazeState

@Composable
fun DictEntriesList(
    hazeState: HazeState,
    entries: LazyPagingItems<DictEntry>,
    invalidLines: List<InvalidLine>,
    selectedIds: Set<String>,
    highlightedIds: Set<String>,
    isBulkMode: Boolean,
    onEditClick: (DictEntry) -> Unit,
    onDeleteEntryConfirm: (String) -> Unit,
    onEntrySelectedChange: (id: String, isChecked: Boolean) -> Unit,
    onInvalidLineContentChange: (id: String, newContent: String) -> Unit,
    onDeleteInvalidLine: (id: String) -> Unit,
    onInvalidLineSelectedChange: (id: String, isChecked: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 88.dp
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            count = entries.itemCount,
            key = entries.itemKey { it.id }
        ) { index ->
            val entry = entries[index] ?: return@items
            DictEntryItemRow(
                hazeState = hazeState,
                entry = entry,
                onEditClick = { onEditClick(entry) },
                onDeleteConfirm = { onDeleteEntryConfirm(entry.id) },
                selected = selectedIds.contains(entry.id),
                onSelectedChange = { isChecked -> onEntrySelectedChange(entry.id, isChecked) },
                isHighlighted = highlightedIds.contains(entry.id),
                isBulkMode = isBulkMode
            )
        }

        if (invalidLines.isNotEmpty()) {
            item(key = "invalid_section_header") {
                InvalidLinesSectionHeader(count = invalidLines.size)
            }

            items(
                count = invalidLines.size,
                key = { index -> invalidLines[index].id }
            ) { index ->
                val invalidLine = invalidLines[index]
                InvalidDictEntryRow(
                    hazeState = hazeState,
                    invalidLine = invalidLine,
                    onContentChange = { newContent ->
                        onInvalidLineContentChange(invalidLine.id, newContent)
                    },
                    onDeleteClick = { onDeleteInvalidLine(invalidLine.id) },
                    selected = selectedIds.contains(invalidLine.id),
                    onSelectedChange = { isChecked ->
                        onInvalidLineSelectedChange(invalidLine.id, isChecked)
                    }
                )
            }
        }
    }
}

@Composable
private fun InvalidLinesSectionHeader(count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f))
        Text(
            text = stringResource(R.string.label_invalid_lines_section, count),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        HorizontalDivider(modifier = Modifier.weight(1f))
    }
}
