package com.inter.calc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.inter.calc.ui.HomeScreen
import com.inter.calc.ui.InterViewModel
import com.inter.calc.ui.theme.InterTheme

class MainActivity : ComponentActivity() {
    private val viewModel: InterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by viewModel.uiState.collectAsState()
            LaunchedEffect(state.darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = if (state.darkTheme) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                    },
                    navigationBarStyle = if (state.darkTheme) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                    },
                )
            }
            InterTheme(dark = state.darkTheme) {
                HomeScreen(viewModel = viewModel)
            }
        }
    }
}
