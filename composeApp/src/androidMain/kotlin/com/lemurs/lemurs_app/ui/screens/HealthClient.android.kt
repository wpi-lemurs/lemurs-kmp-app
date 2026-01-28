package com.lemurs.lemurs_app.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewModelScope
import com.lemurs.lemurs_app.data.AndroidActivityLauncherProvider.logger
import com.lemurs.lemurs_app.health.HealthConnectAvailability
import com.lemurs.lemurs_app.health.HealthConnectViewModel
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import androidx.core.net.toUri
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
actual fun HealthScreen(onNavigateTo: (String) -> Unit) {
  val viewModel: HealthConnectViewModel = koinInject()
  val onPermissionsResult = { viewModel.initialLoad() }
  val permissionsLauncher =
    rememberLauncherForActivityResult(viewModel.requestPermissionsActivityContract()) {
      onPermissionsResult()
    }
  val onPermissionsLaunch = { permissionsLauncher.launch(viewModel.permissions) }
  val lifecycleOwner = LocalLifecycleOwner.current

  if (viewModel.checkAvailability() == HealthConnectAvailability.NOT_INSTALLED) {
    InstallHealthConnectApp()
  }
    else if (viewModel.checkAvailability() == HealthConnectAvailability.NOT_SUPPORTED) {
        Text(text = "Health Connect is not supported on this device")
    return
    }


  LaunchedEffect(Unit) {
    viewModel.viewModelScope.launch {
      if (!viewModel.hasAllPermissions(viewModel.permissions)) {
        logger.d("Permissions not granted")
        onPermissionsLaunch()
      } else {
        // Initialize changes tokens if permissions are already granted
        logger.d("Permissions already granted, initializing changes tokens")
        try {
          viewModel.initializeChangesTokens()
          logger.d("Successfully initialized changes tokens")
        } catch (e: Exception) {
          logger.e("Error initializing changes tokens: ${e.message}", e)
        }
      }
    }
  }

  // Add lifecycle observer to re-check permissions on resume
  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        viewModel.viewModelScope.launch {
          if (!viewModel.hasAllPermissions(viewModel.permissions)) {
            onPermissionsLaunch()
          } else {
            // Initialize changes tokens if permissions are now granted
            logger.d("Permissions granted on resume, initializing changes tokens")
            try {
              viewModel.initializeChangesTokens()
              logger.d("Successfully initialized changes tokens on resume")
            } catch (e: Exception) {
              logger.e("Error initializing changes tokens on resume: ${e.message}", e)
            }
          }
        }
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
    }
  }

  Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
    // Removed Button for reading weight inputs
    // You can add other health data buttons here if needed
  }
}

@Composable
fun InstallHealthConnectApp() {
  val context = LocalContext.current
  Button(
    onClick = {
      context.startActivity(
        Intent(
          Intent.ACTION_VIEW,
            "market://details?id=com.google.android.apps.healthdata".toUri(),
        )
      )
    }
  ) {
    Text(text = "Install health connect app")
  }
}
