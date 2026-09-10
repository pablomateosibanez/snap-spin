package com.example.snapspin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.snapspin.presentation.AppRoot
import com.example.snapspin.ui.theme.SnapSpinTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SnapSpinTheme {
                AppRoot()
            }
        }
    }
}
