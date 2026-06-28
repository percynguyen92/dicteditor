package com.dicteditor.percynguyen92.ui.components.DictEntriesList

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dicteditor.percynguyen92.R
import com.dicteditor.percynguyen92.data.model.InvalidLine
import dev.chrisbanes.haze.HazeState
import com.dicteditor.percynguyen92.ui.components.hazeGlassmorphism
import com.dicteditor.percynguyen92.ui.components.glassTextFieldColors

@Composable
fun InvalidDictEntryRow(
    hazeState: HazeState,
    invalidLine: InvalidLine,
    onContentChange: (String) -> Unit,
    onDeleteClick: () -> Unit,
    selected: Boolean = false,
    onSelectedChange: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.elevatedCardElevation(0.dp),
        modifier = modifier
            .fillMaxWidth()
            .hazeGlassmorphism(hazeState, cornerRadius = 12)
            .testTag("invalid_entry_item_${invalidLine.lineNumber}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onSelectedChange != null) {
                Checkbox(
                    checked = selected,
                    onCheckedChange = { onSelectedChange(it) },
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .testTag("invalid_checkbox_${invalidLine.id}")
                )
            }

            Text(
                text = stringResource(R.string.label_line_number, invalidLine.lineNumber),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(end = 8.dp)
            )

            OutlinedTextField(
                value = invalidLine.lineContent,
                onValueChange = onContentChange,
                modifier = Modifier
                    .weight(1f)
                    .testTag("invalid_textfield_${invalidLine.id}"),
                textStyle = MaterialTheme.typography.bodyMedium,
                singleLine = false,
                shape = RoundedCornerShape(12.dp),
                colors = glassTextFieldColors(),
                placeholder = {
                    Text(
                        text = stringResource(R.string.error_parse_empty_line),
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                trailingIcon = {
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.testTag("invalid_delete_btn_${invalidLine.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = stringResource(R.string.description_remove),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                supportingText = {
                    Text(
                        text = stringResource(invalidLine.error.resId),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            )
        }
    }
}
