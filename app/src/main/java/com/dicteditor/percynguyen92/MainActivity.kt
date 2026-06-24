package com.dicteditor.percynguyen92

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.activity.viewModels
import com.dicteditor.percynguyen92.ui.screens.main.MainScreen
import com.dicteditor.percynguyen92.ui.theme.MyApplicationTheme
import com.dicteditor.percynguyen92.viewmodel.DictionaryViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: DictionaryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )

        // Load recent files
        viewModel.loadRecentFiles(applicationContext)

        setContent {
            MyApplicationTheme {
                MainScreen(viewModel = viewModel, onExit = {
                    window.decorView.postDelayed({ finish() }, 100)
                })
            }
        }
    }
}
