package com.dicteditor.percynguyen92.ui.screens.import_screen.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dicteditor.percynguyen92.R
import com.dicteditor.percynguyen92.data.model.InvalidLine

fun LazyListScope.importInvalidLinesSection(
    invalidLines: List<InvalidLine>,
    context: Context
) {
    if (invalidLines.isEmpty()) return

    item(key = "invalid_lines_container") {
        val errorText = invalidLines.joinToString("\n") {
            context.getString(R.string.import_invalid_line_format, it.lineNumber, it.lineContent)
        }
        
        SelectionContainer {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(12.dp)
            ) {
                val horizontalScrollState = rememberScrollState()
                val verticalScrollState = rememberScrollState()
                
                Text(
                    text = errorText,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.85f)
                    ),
                    softWrap = false,
                    modifier = Modifier
                        .fillMaxSize()
                        .horizontalScroll(horizontalScrollState)
                        .verticalScroll(verticalScrollState)
                )
            }
        }
        TextButton(
            onClick = {
                val clipText = invalidLines.joinToString("\n") { it.lineContent }
                val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("Invalid Lines", clipText)
                clipboardManager.setPrimaryClip(clip)
            }
        ) {
            Text(
                text = stringResource(R.string.import_copy_invalid_lines),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}
