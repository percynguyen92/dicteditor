package com.dicteditor.percynguyen92

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import java.util.Collections
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import com.dicteditor.percynguyen92.aitranslateportal.AiPortalConnectionManager
import com.dicteditor.percynguyen92.aitranslateportal.AiSuggestionParcel
import com.dicteditor.percynguyen92.utils.HanVietDictionary
import com.dicteditor.percynguyen92.ui.components.appBackground
import com.dicteditor.percynguyen92.ui.components.glassTextFieldColors
import com.dicteditor.percynguyen92.ui.components.hazeGlassmorphism
import com.dicteditor.percynguyen92.ui.theme.MyApplicationTheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WordFormActivity : ComponentActivity() {

    private lateinit var atpConnectionManager: AiPortalConnectionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        atpConnectionManager = AiPortalConnectionManager(this)
        
        val editMode = intent.getBooleanExtra("EXTRA_EDIT_MODE", false)
        val entryId = intent.getStringExtra("EXTRA_ENTRY_ID")
        val initialChinese = intent.getStringExtra("EXTRA_CHINESE") ?: ""
        val initialMeanings = intent.getStringArrayExtra("EXTRA_MEANINGS")?.toList() ?: emptyList()

        setContent {
            MyApplicationTheme {
                WordFormScreen(
                    editMode = editMode,
                    initialChinese = initialChinese,
                    initialMeanings = initialMeanings,
                    atpConnectionManager = atpConnectionManager,
                    onBack = { finish() },
                    onSave = { chinese, meanings ->
                        val data = Intent().apply {
                            putExtra("RESULT_ENTRY_ID", entryId)
                            putExtra("RESULT_CHINESE", chinese)
                            putExtra("RESULT_MEANINGS", meanings.toTypedArray())
                        }
                        setResult(Activity.RESULT_OK, data)
                        finish()
                    }
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        atpConnectionManager.bindService()
    }

    override fun onStop() {
        super.onStop()
        atpConnectionManager.unbindService()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordFormScreen(
    editMode: Boolean,
    initialChinese: String,
    initialMeanings: List<String>,
    atpConnectionManager: AiPortalConnectionManager,
    onBack: () -> Unit,
    onSave: (String, List<String>) -> Unit
) {
    var chinese by remember { mutableStateOf(initialChinese) }
    var meaningsList by remember { mutableStateOf(initialMeanings) }
    
    var aiResult by remember { mutableStateOf<AiSuggestionParcel?>(null) }
    var isTranslating by remember { mutableStateOf(false) }
    var aiError by remember { mutableStateOf<String?>(null) }
    
    val isAtpConnected by atpConnectionManager.isConnected.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val hazeState = remember { HazeState() }
    
    var isDictLoaded by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        HanVietDictionary.load(context)
        isDictLoaded = true
    }
    
    val hanVietText = remember(chinese, isDictLoaded) {
        if (isDictLoaded && chinese.isNotBlank()) HanVietDictionary.translate(chinese) else ""
    }
    
    val errorInputChineseFirst = stringResource(R.string.error_input_chinese_first)
    val errorAiNotConnected = stringResource(R.string.error_ai_not_connected)

    Box(modifier = Modifier.fillMaxSize().appBackground().hazeSource(hazeState)) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(if (editMode) stringResource(R.string.title_edit_word) else stringResource(R.string.title_add_word)) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    ),
                    navigationIcon = {
                        IconButton(onClick = {
                            keyboardController?.hide()
                            onBack()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_description))
                        }
                    }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
                    .imePadding()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = chinese,
                        onValueChange = { chinese = it },
                        label = { Text(stringResource(R.string.label_chinese_word)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = glassTextFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    
                    if (hanVietText.isNotBlank()) {
                        Text(
                            text = hanVietText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                        )
                    }
                }

                MeaningList(
                    meanings = meaningsList,
                    onMeaningsChange = { meaningsList = it }
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            if (chinese.isBlank()) {
                                Toast.makeText(context, errorInputChineseFirst, Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            keyboardController?.hide()
                            val filteredMeanings = meaningsList.map { it.trim() }.filter { it.isNotEmpty() }
                            onSave(chinese.trim(), filteredMeanings)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp)
                    ) {
                        Icon(
                            Icons.Default.Save,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.button_save), fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            fetchAiSuggestion(
                                context = context,
                                scope = scope,
                                chinese = chinese,
                                isAtpConnected = isAtpConnected,
                                atpConnectionManager = atpConnectionManager,
                                onStart = {
                                    isTranslating = true
                                    aiError = null
                                    aiResult = null
                                },
                                onSuccess = { parcel ->
                                    aiResult = parcel
                                    isTranslating = false
                                },
                                onFailure = { err ->
                                    aiError = err
                                    isTranslating = false
                                }
                            )
                        },
                        enabled = isAtpConnected,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp)
                    ) {
                        if (isTranslating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        } else {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isAtpConnected) stringResource(R.string.button_ai_suggestion) else stringResource(R.string.label_atp_offline), maxLines = 1)
                        }
                    }
                }
                
                if (aiError != null) {
                    AiErrorCard(error = aiError!!, hazeState = hazeState)
                }

                if (aiResult != null) {
                    AiSuggestionCard(
                        parcel = aiResult!!,
                        hazeState = hazeState,
                        onUseMeanings = { meaningsList = it },
                        onClearCacheAndRefresh = {
                            if (chinese.isBlank()) {
                                aiError = errorInputChineseFirst
                                return@AiSuggestionCard
                            }
                            if (!isAtpConnected) {
                                aiError = errorAiNotConnected
                                atpConnectionManager.bindService()
                                return@AiSuggestionCard
                            }
                            scope.launch {
                                isTranslating = true
                                aiResult = null
                                aiError = null
                                
                                val clearResult = atpConnectionManager.clearCache(chinese.trim())
                                if (clearResult.isFailure) {
                                    aiError = context.getString(R.string.error_clear_cache_failed, clearResult.exceptionOrNull()?.message ?: "")
                                    isTranslating = false
                                    return@launch
                                }
                                
                                fetchAiSuggestion(
                                    context = context,
                                    scope = scope,
                                    chinese = chinese,
                                    isAtpConnected = isAtpConnected,
                                    atpConnectionManager = atpConnectionManager,
                                    onStart = {
                                        isTranslating = true
                                        aiError = null
                                        aiResult = null
                                    },
                                    onSuccess = { parcel ->
                                        aiResult = parcel
                                        isTranslating = false
                                    },
                                    onFailure = { err ->
                                        aiError = err
                                        isTranslating = false
                                    }
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AiErrorCard(
    error: String,
    hazeState: HazeState,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.Transparent,
        modifier = modifier
            .fillMaxWidth()
            .hazeGlassmorphism(hazeState, cornerRadius = 12)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = error, color = MaterialTheme.colorScheme.onErrorContainer)
        }
    }
}

@Composable
fun AiSuggestionCard(
    parcel: AiSuggestionParcel,
    hazeState: HazeState,
    onUseMeanings: (List<String>) -> Unit,
    onClearCacheAndRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.Transparent,
        modifier = modifier
            .fillMaxWidth()
            .hazeGlassmorphism(hazeState, cornerRadius = 12)
    ) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(stringResource(R.string.ai_result_title), fontWeight = FontWeight.Bold)
                
                if (parcel.meanings.isNotEmpty()) {
                    Text(stringResource(R.string.ai_meanings_header), fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                    parcel.meanings.forEachIndexed { index, meaning ->
                        Text(stringResource(R.string.ai_meaning_item, index + 1, meaning.meaning))
                        Text(
                            text = stringResource(R.string.ai_usage_format, meaning.usage),
                            style = MaterialTheme.typography.bodySmall,
                            color = LocalContentColor.current.copy(alpha = 0.7f)
                        )
                    }

                    if (parcel.note.isNotBlank()) {
                        Text(
                            text = stringResource(R.string.ai_note_format, parcel.note),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp),
                            color = LocalContentColor.current.copy(alpha = 0.7f)
                        )
                    }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Button(
                            onClick = {
                                val suggestedMeanings = parcel.meanings.map { it.meaning }
                                onUseMeanings(suggestedMeanings)
                            },
                            modifier = Modifier
                                .hazeGlassmorphism(hazeState),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(stringResource(R.string.button_use_meanings), color = MaterialTheme.colorScheme.primary)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = onClearCacheAndRefresh,
                            modifier = Modifier
                                .hazeGlassmorphism(hazeState, borderColor = MaterialTheme.colorScheme.error),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(stringResource(R.string.button_clear_cache), color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                
                Text(
                    text = if (parcel.fromCache) stringResource(R.string.label_cache) else "",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 8.dp),
                    color = LocalContentColor.current.copy(alpha = 0.5f)
                )
            }
        }
    }
}

private fun fetchAiSuggestion(
    context: Context,
    scope: CoroutineScope,
    chinese: String,
    isAtpConnected: Boolean,
    atpConnectionManager: AiPortalConnectionManager,
    onStart: () -> Unit,
    onSuccess: (AiSuggestionParcel) -> Unit,
    onFailure: (String) -> Unit
) {
    if (chinese.isBlank()) {
        onFailure(context.getString(R.string.error_input_chinese_first))
        return
    }
    if (!isAtpConnected) {
        onFailure(context.getString(R.string.error_ai_not_connected))
        atpConnectionManager.bindService()
        return
    }
    scope.launch {
        onStart()
        try {
            val result = withContext(Dispatchers.IO) {
                atpConnectionManager.getSuggestion(chinese.trim())
            }
            result.onSuccess { parcel ->
                onSuccess(parcel)
            }.onFailure { err ->
                onFailure(context.getString(R.string.error_connection, err.message ?: ""))
            }
        } catch (t: Throwable) {
            onFailure(context.getString(R.string.error_crash, t.javaClass.simpleName, t.message ?: ""))
            t.printStackTrace()
        }
    }
}

@Composable
fun MeaningList(
    meanings: List<String>,
    onMeaningsChange: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    var draggedIndex by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<Int?>(null) }
    var dragOffset by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(0f) }
    var newMeaning by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    
    var editingIndex by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<Int?>(null) }
    var editingText by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    
    val density = androidx.compose.ui.platform.LocalDensity.current
    val itemHeightPx = with(density) { 60.dp.toPx() }

    androidx.compose.foundation.layout.Column(modifier = modifier.fillMaxWidth(), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
        androidx.compose.material3.Text(stringResource(R.string.label_meanings), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(bottom = 4.dp))
        
        meanings.forEachIndexed { index, meaning ->
            val isDragged = index == draggedIndex
            val zIndex = if (isDragged) 1f else 0f
            val translationY = if (isDragged) dragOffset else 0f
            
            androidx.compose.foundation.layout.Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(zIndex)
                    .graphicsLayer { this.translationY = translationY }
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material3.Icon(
                    Icons.Default.DragHandle,
                    contentDescription = stringResource(R.string.description_drag_to_reorder),
                    modifier = Modifier
                        .pointerInput(Unit) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { draggedIndex = index; dragOffset = 0f },
                                onDragEnd = { draggedIndex = null; dragOffset = 0f },
                                onDragCancel = { draggedIndex = null; dragOffset = 0f },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffset += dragAmount.y
                                    
                                    if (dragOffset > itemHeightPx && index < meanings.size - 1) {
                                        val newList = meanings.toMutableList()
                                        java.util.Collections.swap(newList, index, index + 1)
                                        onMeaningsChange(newList)
                                        draggedIndex = index + 1
                                        dragOffset -= itemHeightPx
                                    } else if (dragOffset < -itemHeightPx && index > 0) {
                                        val newList = meanings.toMutableList()
                                        java.util.Collections.swap(newList, index, index - 1)
                                        onMeaningsChange(newList)
                                        draggedIndex = index - 1
                                        dragOffset += itemHeightPx
                                    }
                                }
                            )
                        }
                        .padding(8.dp)
                )
                
                if (editingIndex == index) {
                    androidx.compose.material3.OutlinedTextField(
                        value = editingText,
                        onValueChange = { editingText = it },
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                        singleLine = true,
                        colors = com.dicteditor.percynguyen92.ui.components.glassTextFieldColors(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            if (editingText.isNotBlank()) {
                                val newList = meanings.toMutableList()
                                newList[index] = editingText.trim()
                                onMeaningsChange(newList)
                            }
                            editingIndex = null
                        })
                    )
                    androidx.compose.material3.IconButton(onClick = {
                        if (editingText.isNotBlank()) {
                            val newList = meanings.toMutableList()
                            newList[index] = editingText.trim()
                            onMeaningsChange(newList)
                        }
                        editingIndex = null
                    }) {
                        androidx.compose.material3.Icon(Icons.Default.Check, contentDescription = "Done")
                    }
                } else {
                    androidx.compose.material3.Text(
                        text = meaning,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    
                    androidx.compose.material3.IconButton(onClick = {
                        editingIndex = index
                        editingText = meaning
                    }) {
                        androidx.compose.material3.Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.description_edit_meaning))
                    }
                    
                    androidx.compose.material3.IconButton(onClick = {
                        val newList = meanings.toMutableList().apply { removeAt(index) }
                        onMeaningsChange(newList)
                        if (editingIndex == index) {
                            editingIndex = null
                        } else if (editingIndex != null && editingIndex!! > index) {
                            editingIndex = editingIndex!! - 1
                        }
                    }) {
                        androidx.compose.material3.Icon(Icons.Default.Close, contentDescription = stringResource(R.string.description_remove))
                    }
                }
            }
        }
        
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.material3.OutlinedTextField(
                value = newMeaning,
                onValueChange = { newMeaning = it },
                label = { androidx.compose.material3.Text(stringResource(R.string.label_add_meaning)) },
                modifier = Modifier.weight(1f),
                colors = com.dicteditor.percynguyen92.ui.components.glassTextFieldColors(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (newMeaning.isNotBlank()) {
                        onMeaningsChange(meanings + newMeaning.trim())
                        newMeaning = ""
                    }
                })
            )
            
            androidx.compose.material3.IconButton(
                onClick = {
                    if (newMeaning.isNotBlank()) {
                        onMeaningsChange(meanings + newMeaning.trim())
                        newMeaning = ""
                    }
                },
                modifier = Modifier.padding(start = 8.dp)
            ) {
                androidx.compose.material3.Icon(Icons.Default.Add, contentDescription = stringResource(R.string.label_add_meaning))
            }
        }
    }
}
