package com.dicteditor.percynguyen92.ui.screens.import_screen

import android.annotation.SuppressLint
import android.widget.Space
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dicteditor.percynguyen92.R
import com.dicteditor.percynguyen92.data.model.DictEntry
import com.dicteditor.percynguyen92.data.model.InvalidLine
import com.dicteditor.percynguyen92.data.model.ParseResult
import com.dicteditor.percynguyen92.data.repository.dictionary.ImportMergeMode
import com.dicteditor.percynguyen92.ui.components.appBackground
import com.dicteditor.percynguyen92.ui.components.hazeGlassmorphism
import com.dicteditor.percynguyen92.ui.theme.MyApplicationTheme
import com.dicteditor.percynguyen92.ui.screens.import_screen.components.ImportFilePicker
import com.dicteditor.percynguyen92.ui.screens.import_screen.components.ImportTextField
import com.dicteditor.percynguyen92.ui.screens.import_screen.components.MergeModeSelector
import com.dicteditor.percynguyen92.ui.screens.import_screen.components.importInvalidLinesSection
import com.dicteditor.percynguyen92.ui.screens.import_screen.components.SelectedFileCard
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("NewApi")
@Composable
fun ImportScreen(
    onBack: () -> Unit,
    onImport: (entries: List<DictEntry>, mergeMode: ImportMergeMode) -> Unit,
) {
    val context = LocalContext.current
    val hazeState = remember { HazeState() }

    var rawText by remember { mutableStateOf("") }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var fileContentText by remember { mutableStateOf("") }
    var parsedEntries by remember { mutableStateOf<List<DictEntry>>(emptyList()) }
    var invalidLines by remember { mutableStateOf<List<InvalidLine>>(emptyList()) }
    var selectedMergeMode by remember { mutableStateOf(ImportMergeMode.INSERT) }

    val mergeModes = remember {
        listOf(
            ImportMergeMode.INSERT,
            ImportMergeMode.APPEND,
            ImportMergeMode.REPLACE
        )
    }

    val textToParse = remember(rawText, fileContentText, selectedFileName) {
        if (selectedFileName != null) fileContentText else rawText
    }

    LaunchedEffect(textToParse) {
        if (textToParse.isBlank()) {
            parsedEntries = emptyList()
            invalidLines = emptyList()
        } else {
            val parsed = withContext(Dispatchers.Default) {
                parseEntries(textToParse)
            }
            parsedEntries = parsed.first
            invalidLines = parsed.second
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .appBackground()
            .hazeSource(hazeState)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.title_import),
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    ),
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back_description)
                            )
                        }
                    }
                )
            },
            bottomBar = {
                BottomAppBar(
                    containerColor = Color.Transparent,
                    modifier = Modifier.hazeGlassmorphism(
                        hazeState,
                        cornerRadius = 0,
                        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, alignment = Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                rawText = ""
                                selectedFileName = null
                                fileContentText = ""
                            },
                            modifier = Modifier.testTag("import_clear_button")
                        ) {
                            Text(
                                text = stringResource(R.string.import_button_clear),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }

                        Button(
                            onClick = {
                                if (parsedEntries.isNotEmpty()) {
                                    onImport(parsedEntries, selectedMergeMode)
                                }
                            },
                            enabled = parsedEntries.isNotEmpty(),
                            modifier = Modifier.testTag("import_save_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.import_button_confirm, parsedEntries.size),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                // --- File Picker & Paste Button ---
                item(key = "file_picker") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ImportFilePicker(
                            modifier = Modifier.weight(1f),
                            hazeState = hazeState,
                            onFileSelected = { name, content ->
                                selectedFileName = name
                                fileContentText = content
                                rawText = ""
                            }
                        )

                        val clipboard = LocalClipboard.current
                        val scope = rememberCoroutineScope()

                        TextButton(
                            onClick = {
                                scope.launch {
                                    val clipEntry = clipboard.getClipEntry()
                                    val text = clipEntry?.clipData?.getItemAt(0)?.text?.toString()
                                    if (!text.isNullOrEmpty()) {
                                        rawText = text
                                        selectedFileName = null
                                        fileContentText = ""
                                    }
                                }
                            },
                            modifier = Modifier
                                .hazeGlassmorphism(hazeState)
                                .testTag("import_paste_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = stringResource(R.string.button_paste),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.button_paste),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }

                // --- Text Field with Paste button / Selected File Card ---
                item(key = "text_field") {
                    if (selectedFileName != null) {
                        SelectedFileCard(
                            fileName = selectedFileName!!,
                            hazeState = hazeState,
                            onClear = {
                                selectedFileName = null
                                fileContentText = ""
                            }
                        )
                    } else {
                        ImportTextField(
                            value = rawText,
                            onValueChange = {
                                rawText = it
                                selectedFileName = null
                                fileContentText = ""
                            }
                        )
                    }
                }

                // --- Merge Mode Selector ---
                item(key = "merge_mode") {
                    MergeModeSelector(
                        hazeState = hazeState,
                        selectedMode = selectedMergeMode,
                        modes = mergeModes,
                        onModeSelected = { selectedMergeMode = it }
                    )
                }

                // --- Scanned Count Info ---
                item(key = "preview_info") {
                    if (parsedEntries.isEmpty() && invalidLines.isEmpty()) {
                        Text(
                            text = stringResource(R.string.import_preview_empty),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    } else {
                        Row (
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.import_file_loaded, parsedEntries.size),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant

                            )
                            if (invalidLines.isNotEmpty()){
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.import_invalid_lines_title, invalidLines.size),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // --- Invalid Lines Warning & Copy ---
                importInvalidLinesSection(
                    invalidLines = invalidLines,
                    context = context
                )
            }
        }
    }
}

// =============================================================================
// Utility functions
// =============================================================================

private suspend fun parseEntries(rawText: String): Pair<List<DictEntry>, List<InvalidLine>> {
    val lines = rawText.lines()
    val entries = ArrayList<DictEntry>(lines.size.coerceAtMost(1000))
    val invalidLines = ArrayList<InvalidLine>()

    lines.forEachIndexed { index, line ->
        currentCoroutineContext().ensureActive()
        if (line.trim().isEmpty()) {
            return@forEachIndexed
        }
        val result = DictEntry.parse(line)
        when (result) {
            is ParseResult.Success -> {
                entries.add(result.entry)
            }
            is ParseResult.Failure -> {
                invalidLines.add(
                    InvalidLine(
                        id = UUID.randomUUID().toString(),
                        lineNumber = index + 1,
                        lineContent = line,
                        error = result.error
                    )
                )
            }
        }
    }

    return entries to invalidLines
}

@Preview(showBackground = true)
@Composable
fun ImportScreenPreview() {
    MyApplicationTheme {
        ImportScreen(
            onBack = {},
            onImport = { _, _ -> }
        )
    }
}
