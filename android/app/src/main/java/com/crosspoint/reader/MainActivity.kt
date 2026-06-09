package com.crosspoint.reader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.crosspoint.reader.navigation.NavGraph
import com.crosspoint.reader.ui.theme.CrossPointTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val openUri = intent.data

        setContent {
            CrossPointTheme {
                NavGraph(openUri = openUri)
            }
        }
    }
}
