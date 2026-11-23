package com.example.oraclepredictanything

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.example.oraclepredictanything.ui.theme.OraclePredictAnythingTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install modern SplashScreen API before super.onCreate
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as OracleApp
        val preferences = app.preferences

        setContent {
            val themeMode by preferences.themeMode.collectAsState(initial = "DARK")
            val onboardingCompleted by preferences.onboardingCompleted.collectAsState(initial = true)

            val isDark = when (themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemInDarkTheme()
            }

            OraclePredictAnythingTheme(darkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainNavigation(
                        isOnboardingCompleted = onboardingCompleted,
                        onCompleteOnboarding = {
                            lifecycleScope.launch {
                                preferences.setOnboardingCompleted(true)
                            }
                        }
                    )
                }
            }
        }
    }
}
