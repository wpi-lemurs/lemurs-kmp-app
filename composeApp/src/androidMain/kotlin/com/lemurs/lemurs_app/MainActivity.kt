package com.lemurs.lemurs_app

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat
import com.lemurs.lemurs_app.data.AndroidActivityProvider
import com.lemurs.lemurs_app.di.OnCreateService
import com.lemurs.lemurs_app.ui.navigation.AppNavigator
import com.lemurs.lemurs_app.ui.screens.LemurScreen
import com.lemurs.lemurs_app.ui.theme.LemursAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set the activity for MSAL authentication
        AndroidActivityProvider.setActivity(this)

        val onCreateService = OnCreateService(this, this.application)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            onCreateService.setupPermissionLaunchers(this)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            onCreateService.onCreate(this)
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)
        @Suppress("DEPRECATION")
        window.navigationBarColor = Color.Transparent.toArgb()
        @Suppress("DEPRECATION")
        window.statusBarColor = Color(0xFF1C5F6E).toArgb()

        enableEdgeToEdge()

        setContent {
            LemursAppTheme {
                // create navigator in Android composable scope so we can intercept system back
                val navigator = remember { AppNavigator(LemurScreen.Login.name) }

                // Intercept hardware/system back presses. If the navigator can go back, navigate back
                // in-app instead of finishing the Activity. Otherwise finish the Activity.
                BackHandler {
                    if (navigator.canGoBack()) {
                        navigator.goBack()
                    } else {
                        this@MainActivity.finish()
                    }
                }

                // pass navigator into the multiplatform App composable
                App(navigator)
            }
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}