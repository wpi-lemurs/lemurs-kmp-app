package com.lemurs.lemurs_app.data.api

import co.touchlab.kermit.Logger
import com.lemurs.lemurs_app.data.datastore.JwtTokenResponseImpl
import com.lemurs.lemurs_app.survey.fetchDemographic
import com.lemurs.lemurs_app.ui.screens.LemurScreen
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * iOS implementation of MicrosoftApiAuthorizationService.
 *
 * This implementation uses Swift/Kotlin interop to call the native MSAL iOS SDK.
 * The Swift side (MSALKotlinBridge) handles the actual MSAL authentication,
 * and this Kotlin class provides the expect/actual interface for the common code.
 *
 * Architecture:
 * - Kotlin (this class) <-> Swift (MSALKotlinBridge) <-> MSAL iOS SDK
 */
actual class MicrosoftApiAuthorizationService actual constructor(
    private val webApiService: WebAPIAuthorizationService
) : KoinComponent {

    private val logger = Logger.withTag("MSAuth-iOS")
    private var navigationCallback: ((String) -> Unit)? = null
    private val mainScope = MainScope()

    // Inject the token storage
    private val jwtTokenResponseImpl: JwtTokenResponseImpl by inject()

    // Scopes are configured in Swift's MSALAuthHelper, kept here for reference
    @Suppress("unused")
    private val scopes = listOf("api://b00e7cc0-f93d-4caf-9f9c-c97d8d6f6a0d/lemurs")

    // Track authentication state
    private var isInitialized = false
    private var hasAccount = false

    /**
     * Initialize the MSAL client via Swift bridge.
     *
     * @param navigate Callback function for navigation after auth events
     * @return true if an account is already loaded, false otherwise
     */
    actual fun initClient(navigate: (String) -> Unit): Boolean {
        navigationCallback = navigate
        logger.i("Initializing MSAL client via Swift bridge")

        val bridge = IOSMSALBridgeProvider.bridge
        if (bridge == null) {
            logger.w("Swift MSAL bridge not available - ensure MSALKotlinBridge is initialized in Swift")
            return false
        }

        bridge.initialize(
            onAccountLoaded = { accountExists ->
                logger.i("Account loaded from MSAL: hasAccount=$accountExists")
                hasAccount = accountExists
                isInitialized = true

                if (accountExists) {
                    // User is already signed in, try to acquire token silently
                    silentlyAcquireToken()
                }
            },
            onError = { error ->
                logger.e("MSAL initialization failed: $error")
                isInitialized = false
                hasAccount = false
            }
        )

        // Return false since initialization is async
        return false
    }

    /**
     * Acquire authentication token interactively.
     * This will present the Microsoft login UI to the user.
     */
    actual fun acquireToken() {
        logger.i("Acquiring token interactively")

        val bridge = IOSMSALBridgeProvider.bridge
        if (bridge == null) {
            logger.e("Swift MSAL bridge not available")
            return
        }

        // Check if already signed in, use silent acquisition
        if (bridge.isSignedIn()) {
            logger.i("User already signed in, attempting silent token acquisition")
            silentlyAcquireToken()
            return
        }

        bridge.acquireToken(
            onSuccess = { accessToken ->
                logger.w("Successfully interactively authenticated")
                hasAccount = true
                handleAuthSuccess(accessToken)
            },
            onError = { error ->
                logger.e("Interactive token acquisition failed: $error")
                if (error.contains("User cancelled", ignoreCase = true)) {
                    // User cancelled - don't show error, just stay on login screen
                    logger.i("User cancelled login")
                }
            }
        )
    }

    /**
     * Attempt to acquire token silently for already signed-in users.
     * If silent acquisition fails with interaction required, fall back to interactive.
     */
    private fun silentlyAcquireToken() {
        logger.i("Attempting silent token acquisition")

        val bridge = IOSMSALBridgeProvider.bridge ?: return

        bridge.acquireTokenSilently(
            onSuccess = { accessToken ->
                logger.w("Successfully silently authenticated")
                hasAccount = true
                handleAuthSuccess(accessToken)
            },
            onError = { error ->
                logger.w("Silent token acquisition failed: $error")

                if (error == "INTERACTION_REQUIRED") {
                    // Token expired or needs re-auth, will require interactive login
                    logger.i("Interaction required - user will need to login again")
                    acquireToken()
                }
            }
        )
    }

    /**
     * Remove the current account / sign out.
     */
    actual fun removeAccount() {
        logger.i("Signing out")

        val bridge = IOSMSALBridgeProvider.bridge
        if (bridge == null) {
            logger.w("Swift MSAL bridge not available")
            // Still navigate to login even if bridge not available
            navigationCallback?.invoke(LemurScreen.Login.name)
            return
        }

        bridge.signOut(
            onComplete = {
                logger.i("Sign out successful")
                hasAccount = false
                navigationCallback?.invoke(LemurScreen.Login.name)
            },
            onError = { error ->
                logger.e("Sign out failed: $error")
                // Navigate to login anyway
                hasAccount = false
                navigationCallback?.invoke(LemurScreen.Login.name)
            }
        )
    }

    private fun handleAuthSuccess(microsoftToken: String) {
        logger.w("Microsoft Access token: $microsoftToken")
        mainScope.launch {
            val jwtTokenResponseJson = webApiService.accessWebApi(microsoftToken)
            jwtTokenResponseImpl.updateLemursAccessToken(
                jwtTokenResponseJson.jsonObject["accessToken"]!!.jsonPrimitive.content
            )
            jwtTokenResponseImpl.updateRefreshToken(
                jwtTokenResponseJson.jsonObject["refreshToken"]!!.jsonPrimitive.content
            )

            logger.w("navigating out of login")
            if (!checkDemographicsEmpty()) {
                logger.w("navigating to home")
                navigationCallback?.invoke(LemurScreen.Main.name)
            } else {
                logger.w("navigating to demographics page")
                navigationCallback?.invoke(LemurScreen.Demographics.name)
            }
        }
    }

    private suspend fun checkDemographicsEmpty(): Boolean {
        val demographics = fetchDemographic()
        logger.w("fetched demographics: $demographics")
        return demographics.isEmpty()
    }
}
