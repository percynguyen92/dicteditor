package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.components.appBackground
import com.example.ui.components.hazeGlassmorphism
import com.example.ui.components.glassTextFieldColors
import com.example.ui.theme.MyApplicationTheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ExportActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val initialText = ExportSession.exportText
        ExportSession.exportText = "" // clear it
        
        setContent {
            MyApplicationTheme {
                ExportScreen(
                    initialText = initialText,
                    onBack = { finish() },
                    context = this
                )
            }
        }
    }
}

object ExportSession {
    var exportText: String = ""
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    initialText: String,
    onBack: () -> Unit,
    context: Context
) {
    var text by remember { mutableStateOf(initialText) }
    val scope = rememberCoroutineScope()
    val hazeState = remember { HazeState() }
    
    val exportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openOutputStream(uri, "w")?.use { stream ->
                        stream.bufferedWriter().use { writer ->
                            writer.write(text)
                        }
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Xuất file thành công", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Lỗi xuất file", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().appBackground().haze(hazeState)) {
        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Xuất tệp") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = androidx.compose.ui.graphics.Color.Transparent
                    ),
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Trở về")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    colors = glassTextFieldColors(),
                    textStyle = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Đã sao chép", text)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Đã sao chép vào bộ nhớ tạm", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.hazeGlassmorphism(hazeState, cornerRadius = 12),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp)
                    ) {
                        Text("Copy tất cả", color = MaterialTheme.colorScheme.primary)
                    }
                    
                    Button(
                        onClick = {
                            exportFileLauncher.launch("exported_entries.txt")
                        },
                        modifier = Modifier.hazeGlassmorphism(hazeState, cornerRadius = 12),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp)
                    ) {
                        Text("Export to txt", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}
