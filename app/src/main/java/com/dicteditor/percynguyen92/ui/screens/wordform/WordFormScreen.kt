package com.dicteditor.percynguyen92.ui.screens.wordform

import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dicteditor.percynguyen92.R
import com.dicteditor.percynguyen92.utils.UiText
import kotlin.time.Duration.Companion.seconds
import com.dicteditor.percynguyen92.aitranslateportal.AiPortalConnectionManager
import com.dicteditor.percynguyen92.aitranslateportal.AiSuggestionParcel
import com.dicteditor.percynguyen92.ui.screens.wordform.components.AiErrorCard
import com.dicteditor.percynguyen92.ui.screens.wordform.components.AiSuggestionCard
import com.dicteditor.percynguyen92.ui.screens.wordform.components.MeaningList
import com.dicteditor.percynguyen92.ui.components.appBackground
import com.dicteditor.percynguyen92.ui.components.glassTextFieldColors
import com.dicteditor.percynguyen92.ui.components.hazeGlassmorphism
import com.dicteditor.percynguyen92.utils.HanVietDictionary
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    val clipboard = LocalClipboard.current
    val hazeState = remember { HazeState() }
    val snackbarHostState = remember { SnackbarHostState() }
    var undoJob by remember { mutableStateOf<Job?>(null) }
    
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
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState)
            },
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
                        singleLine = true,
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        val clipEntry = clipboard.getClipEntry()
                                        val text = clipEntry?.clipData?.getItemAt(0)?.text?.toString()
                                        if (!text.isNullOrEmpty()) {
                                            chinese = text
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentPaste,
                                    contentDescription = stringResource(R.string.button_paste),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (hanVietText.isNotBlank()) {
                        OutlinedTextField(
                            value = hanVietText,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.label_han_viet)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .hazeGlassmorphism(hazeState, cornerRadius = 12, borderColor = Color.Transparent, tint = Color.Transparent),
                            colors = glassTextFieldColors(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                MeaningList(
                    hazeState = hazeState,
                    meanings = meaningsList,
                    onMeaningsChange = { meaningsList = it },
                    onDeleteMeaning = { index, deletedMeaning ->
                        val newList = meaningsList.toMutableList().apply { removeAt(index) }
                        meaningsList = newList
                        
                        undoJob?.cancel()
                        undoJob = scope.launch {
                            val dismissJob = launch {
                                delay(3.seconds)
                                snackbarHostState.currentSnackbarData?.dismiss()
                            }
                            val message = UiText.StringResource(
                                R.string.snackbar_deleted_meaning,
                                listOf(deletedMeaning)
                            ).asString(context)
                            val actionLabel = UiText.StringResource(
                                R.string.button_undo
                            ).asString(context)
                            val result = snackbarHostState.showSnackbar(
                                message = message,
                                actionLabel = actionLabel,
                                duration = SnackbarDuration.Indefinite
                            )
                            dismissJob.cancel()
                            if (result == SnackbarResult.ActionPerformed) {
                                meaningsList = meaningsList.toMutableList().apply {
                                    add(index, deletedMeaning)
                                }
                            }
                        }
                    }
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
                                    val errorMsg = UiText.StringResource(
                                        R.string.error_clear_cache_failed,
                                        listOf(clearResult.exceptionOrNull()?.message ?: "")
                                    ).asString(context)
                                    aiError = errorMsg
                                    isTranslating = false
                                    return@launch
                                }
                                
                                fetchAiSuggestion(
                                    context = context,
                                    scope = scope,
                                    chinese = chinese,
                                    isAtpConnected = true,
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
        onFailure(UiText.StringResource(R.string.error_input_chinese_first).asString(context))
        return
    }
    if (!isAtpConnected) {
        onFailure(UiText.StringResource(R.string.error_ai_not_connected).asString(context))
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
                onFailure(
                    UiText.StringResource(
                        R.string.error_connection,
                        listOf(err.message ?: "")
                    ).asString(context)
                )
            }
        } catch (t: Throwable) {
            onFailure(
                UiText.StringResource(
                    R.string.error_crash,
                    listOf(t.javaClass.simpleName, t.message ?: "")
                ).asString(context)
            )
            t.printStackTrace()
        }
    }
}
