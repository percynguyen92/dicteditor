package com.dicteditor.percynguyen92

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import android.graphics.Color
import com.dicteditor.percynguyen92.ui.screens.export.ExportScreen
import com.dicteditor.percynguyen92.ui.theme.MyApplicationTheme

class ExportActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        
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


