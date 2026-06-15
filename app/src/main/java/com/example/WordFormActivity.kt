package com.example

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.aitranslateportal.AiPortalConnectionManager
import com.example.aitranslateportal.AiSuggestionParcel
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.components.appBackground
import com.example.ui.components.hazeGlassmorphism
import com.example.ui.components.glassTextFieldColors
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.launch

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

    Box(modifier = Modifier.fillMaxSize().appBackground().haze(hazeState)) {
        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(if (editMode) "Sửa từ" else "Thêm từ mới") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = androidx.compose.ui.graphics.Color.Transparent
                    ),
                    navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Trở về")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            if (chinese.isBlank()) return@TextButton
                            val meaningsList = meaningsStr.split("/").map { it.trim() }.filter { it.isNotEmpty() }
                            onSave(chinese.trim(), meaningsList)
                        }
                    ) {
                        Text("Lưu")
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
                singleLine = true
            )

            OutlinedTextField(
                value = meaningsStr,
                onValueChange = { meaningsStr = it },
                label = { Text("Nghĩa ") },
                modifier = Modifier.fillMaxWidth(),
                colors = glassTextFieldColors(),
                minLines = 3
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (chinese.isBlank()) {
                        aiError = "Vui lòng nhập từ tiếng Trung trước."
                        return@Button
                    }
                    if (!isAtpConnected) {
                        aiError = "AI chưa kết nối. Đang thử kết nối lại..."
                        atpConnectionManager.bindService()
                        return@Button
                    }
                    scope.launch {
                        isTranslating = true
                        aiError = null
                        aiResult = null
                        
                        try {
                            val result = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                atpConnectionManager.getSuggestion(chinese.trim())
                            }
                            result.onSuccess { parcel ->
                                aiResult = parcel
                            }.onFailure { err ->
                                aiError = "Lỗi kết nối: ${err.message}"
                            }
                        } catch (t: Throwable) {
                            aiError = "Crash (Đã catch): ${t.javaClass.simpleName} - ${t.message}"
                            t.printStackTrace()
                        } finally {
                            isTranslating = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .hazeGlassmorphism(hazeState, cornerRadius = 12),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp)
            ) {
                if (isTranslating) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Đang lấy gợi ý AI...")
                } else {
                    Icon(Icons.Default.Star, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Lấy gợi ý AI")
                }
            }
            
            if (aiError != null) {
                Surface(
                    color = androidx.compose.ui.graphics.Color.Transparent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .hazeGlassmorphism(hazeState, cornerRadius = 16)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = aiError!!, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }

            if (aiResult != null) {
                val parcel = aiResult!!
                Surface(
                    color = androidx.compose.ui.graphics.Color.Transparent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .hazeGlassmorphism(hazeState, cornerRadius = 16)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Kết quả từ AI Translate Portal:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        
                        if (parcel.meanings.isNotEmpty()) {
                            Text("Các nghĩa:", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                            parcel.meanings.forEachIndexed { index, meaning ->
                                Text("${index + 1}. ${meaning.meaning}")
                                Text("    Cách dùng: ${meaning.usage}", style = MaterialTheme.typography.bodySmall)
                            }
                            
                            Button(
                                onClick = {
                                    val suggestedMeanings = parcel.meanings.map { it.meaning }
                                    meaningsStr = suggestedMeanings.joinToString("/")
                                },
                                modifier = Modifier
                                    .padding(top = 8.dp)
                                    .hazeGlassmorphism(hazeState, cornerRadius = 12),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp)
                            ) {
                                Text("Sử dụng các nghĩa này", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        
                        if (parcel.note.isNotBlank()) {
                            Text("Ghi chú: ${parcel.note}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                        }
                        
                        Text(
                            text = if (parcel.fromCache) "Đã lấy từ Cache" else "Lấy từ AI mới",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(top = 8.dp),
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}
}
