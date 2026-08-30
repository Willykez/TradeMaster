package com.trademaster.pro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.trademaster.pro.ui.nav.TradeMasterRoot
import com.trademaster.pro.ui.theme.TradeMasterProTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as TradeMasterApplication
        setContent {
            TradeMasterProTheme {
                var mode by remember { mutableStateOf(AppMode.CLIENT) }
                TradeMasterRoot(
                    repository = app.repository,
                    adminAuth = app.adminAuth,
                    mode = mode,
                    onModeChange = { mode = it }
                )
            }
        }
    }
}
