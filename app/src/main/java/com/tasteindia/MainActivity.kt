package com.tasteindia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.tasteindia.ui.TasteIndiaApp
import com.tasteindia.ui.theme.TasteIndiaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = (application as TasteIndiaApplication).container.repository
        setContent { TasteIndiaTheme { TasteIndiaApp(repository) } }
    }
}
