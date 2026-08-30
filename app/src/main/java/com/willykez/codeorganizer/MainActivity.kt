package com.willykez.codeorganizer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.willykez.codeorganizer.navigation.CodeOrganizerNavGraph
import com.willykez.codeorganizer.ui.theme.CodeOrganizerTheme
import com.willykez.codeorganizer.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            CodeOrganizerTheme {
                val viewModel: MainViewModel = viewModel(
                    factory = MainViewModel.factory(application)
                )
                CodeOrganizerNavGraph(viewModel = viewModel)
            }
        }
    }
}
