package com.dicteditor.percynguyen92.ui.screens.import_screen.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.dicteditor.percynguyen92.R
import com.dicteditor.percynguyen92.data.model.DictEntry
import com.dicteditor.percynguyen92.data.model.InvalidLine
import com.dicteditor.percynguyen92.data.model.ParseResult

// ---------------------------------------------------------------------------
// State model (private, internal to this section)
// ---------------------------------------------------------------------------

private data class EditableInvalidLine(
    val original: InvalidLine,
    val currentText: String = original.lineContent,
    val parsedEntry: DictEntry? = null,   // non-null → dòng này đã hợp lệ
)

private fun EditableInvalidLine.isFixed() = parsedEntry != null

// ---------------------------------------------------------------------------
// Public extension on LazyListScope
// ---------------------------------------------------------------------------

/**
 * Hiển thị danh sách dòng lỗi, mỗi dòng có thể sửa inline.
 *
 * [onFixedEntryChanged] được gọi mỗi khi user chỉnh sửa một dòng:
 * - entry != null → dòng lineId đã parse thành công → upsert vào map
 * - entry == null → dòng lineId vẫn lỗi → remove khỏi map
 *
 * ImportScreen duy trì một `Map<lineId, DictEntry>` và merge vào parsedEntries
 * khi tính tổng số entry để enable nút Import.
 */
fun LazyListScope.importInvalidLinesSection(
    invalidLines: List<InvalidLine>,
    onFixedEntryChanged: (lineId: String, entry: DictEntry?) -> Unit,
) {
    if (invalidLines.isEmpty()) return

    // Mỗi dòng lỗi = 1 lazy item riêng (key ổn định theo line.id)
    itemsIndexed(
        items = invalidLines,
        key = { _, line -> "invalid_${line.id}" }
    ) { _, line ->

        // State local cho mỗi dòng — reset tự động khi line.id đổi
        var editState by remember(line.id) {
            mutableStateOf(EditableInvalidLine(original = line))
        }

        InvalidLineEditItem(
            lineNumber = line.lineNumber,
            editState = editState,
            onTextChange = { newText ->
                val fixedEntry = when (val parsed = DictEntry.parse(newText)) {
                    is ParseResult.Success -> parsed.entry
                    is ParseResult.Failure -> null
                }
                editState = editState.copy(
                    currentText = newText,
                    parsedEntry = fixedEntry
                )
                // Mỗi dòng chỉ báo về chính nó → ImportScreen tự upsert/remove vào map
                onFixedEntryChanged(line.id, fixedEntry)
            }
        )
    }
}

// ---------------------------------------------------------------------------
// Private composable cho mỗi dòng
// ---------------------------------------------------------------------------

@Composable
private fun InvalidLineEditItem(
    lineNumber: Int,
    editState: EditableInvalidLine,
    onTextChange: (String) -> Unit,
) {
    val isFixed = editState.isFixed()

    val borderColor = if (isFixed)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.error.copy(alpha = 0.6f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        OutlinedTextField(
            value = editState.currentText,
            onValueChange = onTextChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace
            ),
            label = {
                Text(
                    text = stringResource(R.string.import_invalid_line_label, lineNumber),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isFixed)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.error
                )
            },
            trailingIcon = {
                Icon(
                    imageVector = if (isFixed) Icons.Default.CheckCircle else Icons.Default.Error,
                    contentDescription = null,
                    tint = if (isFixed)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = borderColor,
                unfocusedBorderColor = borderColor.copy(alpha = 0.5f),
            ),
            isError = !isFixed,
        )

        // Hint dưới TextField: lỗi parse gốc khi chưa fix, preview word|meaning khi đã fix
        AnimatedVisibility(visible = !isFixed, enter = fadeIn(), exit = fadeOut()) {
            Text(
                text = stringResource(editState.original.error.resId),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                modifier = Modifier.padding(start = 12.dp, top = 2.dp)
            )
        }
        AnimatedVisibility(visible = isFixed, enter = fadeIn(), exit = fadeOut()) {
            editState.parsedEntry?.let { entry ->
                Text(
                    text = stringResource(
                        R.string.import_fixed_line_preview,
                        entry.chinese,
                        entry.meanings.joinToString("/")
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    modifier = Modifier.padding(start = 12.dp, top = 2.dp)
                )
            }
        }
    }
}