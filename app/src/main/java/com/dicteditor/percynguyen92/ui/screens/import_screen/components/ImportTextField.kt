package com.dicteditor.percynguyen92.ui.screens.import_screen.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dicteditor.percynguyen92.R
import com.dicteditor.percynguyen92.ui.components.glassTextFieldColors

@Composable
fun ImportTextField(
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                text = stringResource(R.string.import_hint),
                style = MaterialTheme.typography.bodySmall
            )
        },
        minLines = 4,
        maxLines = 8,
        textStyle = MaterialTheme.typography.bodyMedium,
        colors = glassTextFieldColors(),
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .testTag("import_text_field")
    )
}

