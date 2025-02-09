package com.example.websocketintegration

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.websocketintegration.ui.screen.MainRoute
import com.example.websocketintegration.ui.screen.MainScreenVm
import com.example.websocketintegration.ui.theme.WebSocketIntegrationTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WebSocketIntegrationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val viewModel = viewModel<MainScreenVm>()
                    MainRoute(
                        modifier = Modifier.padding(innerPadding),
                        vm = viewModel,
                    )
                }
            }
        }
    }
}
