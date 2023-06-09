package com.kiko.kflashbot.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.kiko.kflashbot.ui.screens.NavGraphs
import com.kiko.kflashbot.ui.screens.destinations.StartScreenDestination
import com.kiko.kflashbot.ui.theme.KflashbotTheme
import com.ramcosta.composedestinations.DestinationsNavHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KflashbotTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    DestinationsNavHost(
                        navGraph = NavGraphs.root,
                        startRoute = StartScreenDestination
                    )
                }
            }
        }
    }
}