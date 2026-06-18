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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dicteditor.percynguyen92.aitranslateportal.AiPortalConnectionManager
import com.dicteditor.percynguyen92.aitranslateportal.AiSuggestionParcel
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
    var meaningsStr by remember { mutableStateOf(initialMeanings.joinToString("/")) }
    
    var aiResult by remember { mutableStateOf<AiSuggestionParcel?>(null) }
    var isTranslating by remember { mutableStateOf(false) }
    var aiError by remember { mutableStateOf<String?>(null) }
    
    val isAtpConnected by atpConnectionManager.isConnected.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val hazeState = remember { HazeState() }

    Box(modifier = Modifier.fillMaxSize().appBackground().hazeSource(hazeState)) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(if (editMode) "Sửa từ" else "Thêm từ mới") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    ),
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Trở về")
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
                OutlinedTextField(
                    value = chinese,
                    onValueChange = { chinese = it },
                    label = { Text("Từ tiếng Trung") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = glassTextFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = meaningsStr,
                    onValueChange = { meaningsStr = it },
                    label = { Text("Nghĩa ") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = glassTextFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 3
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
                                Toast.makeText(context, "Vui lòng nhập từ tiếng Trung trước.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val meaningsList = meaningsStr.split("/").map { it.trim() }.filter { it.isNotEmpty() }
                            onSave(chinese.trim(), meaningsList)
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
                        Text("Lưu", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            fetchAiSuggestion(
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
                            Text("Gợi ý AI", maxLines = 1)
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
                        onUseMeanings = { meaningsStr = it },
                        onClearCacheAndRefresh = {
                            if (chinese.isBlank()) {
                                aiError = "Vui lòng nhập từ tiếng Trung trước."
                                return@AiSuggestionCard
                            }
                            if (!isAtpConnected) {
                                aiError = "AI chưa kết nối. Đang thử kết nối lại..."
                                atpConnectionManager.bindService()
                                return@AiSuggestionCard
                            }
                            scope.launch {
                                isTranslating = true
                                aiResult = null
                                aiError = null
                                
                                val clearResult = atpConnectionManager.clearCache(chinese.trim())
                                if (clearResult.isFailure) {
                                    aiError = "Lỗi xoá cache: ${clearResult.exceptionOrNull()?.message}"
                                    isTranslating = false
                                    return@launch
                                }
                                
                                fetchAiSuggestion(
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
    onUseMeanings: (String) -> Unit,
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
                Text("Kết quả từ AI Translate Portal:", fontWeight = FontWeight.Bold)
                
                if (parcel.meanings.isNotEmpty()) {
                    Text("Các nghĩa:", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                    parcel.meanings.forEachIndexed { index, meaning ->
                        Text("${index + 1}. ${meaning.meaning}")
                        Text(
                            text = "    Cách dùng: ${meaning.usage}",
                            style = MaterialTheme.typography.bodySmall,
                            color = LocalContentColor.current.copy(alpha = 0.7f)
                        )
                    }

                    if (parcel.note.isNotBlank()) {
                        Text(
                            text = "Ghi chú: ${parcel.note}",
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
                                onUseMeanings(suggestedMeanings.joinToString("/"))
                            },
                            modifier = Modifier
                                .hazeGlassmorphism(hazeState),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Sử dụng các nghĩa này", color = MaterialTheme.colorScheme.primary)
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
                            Text("Xoá cache", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                
                Text(
                    text = if (parcel.fromCache) "Cache" else "",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 8.dp),
                    color = LocalContentColor.current.copy(alpha = 0.5f)
                )
            }
        }
    }
}

private fun fetchAiSuggestion(
    scope: CoroutineScope,
    chinese: String,
    isAtpConnected: Boolean,
    atpConnectionManager: AiPortalConnectionManager,
    onStart: () -> Unit,
    onSuccess: (AiSuggestionParcel) -> Unit,
    onFailure: (String) -> Unit
) {
    if (chinese.isBlank()) {
        onFailure("Vui lòng nhập từ tiếng Trung trước.")
        return
    }
    if (!isAtpConnected) {
        onFailure("AI chưa kết nối. Đang thử kết nối lại...")
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
                onFailure("Lỗi kết nối: ${err.message}")
            }
        } catch (t: Throwable) {
            onFailure("Crash (Đã catch): ${t.javaClass.simpleName} - ${t.message}")
            t.printStackTrace()
        }
    }
}
