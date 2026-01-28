package com.lemurs.lemurs_app.data

import android.content.Context
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import co.touchlab.kermit.Logger

object AndroidActivityLauncherProvider {
    private var appActivityLauncherMultiple: ActivityResultLauncher<Array<String>>? = null

    val activityLauncherMultiple: ActivityResultLauncher<Array<String>>?
        get() = appActivityLauncherMultiple
    val logger = Logger.withTag("AndroidActivityLauncherProvider")


    fun setActivityMultiple(activity: ComponentActivity, context: Context) {
        if (appActivityLauncherMultiple == null) {
            appActivityLauncherMultiple =
                activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissionsMap ->
                    val grantedPermissions = permissionsMap.filterValues { it }.keys
                    val deniedPermissions = permissionsMap.filterValues { !it }.keys
                    val message =
                        when {
                            deniedPermissions.isEmpty() ->
                                "All permissions granted"

                            grantedPermissions.isEmpty() ->
                                "No permissions granted yet"

                            else ->
                                "Permissions not yet granted: ${deniedPermissions.joinToString(", ")}"
                        }
                    logger.w(message)
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
        }
    }
}