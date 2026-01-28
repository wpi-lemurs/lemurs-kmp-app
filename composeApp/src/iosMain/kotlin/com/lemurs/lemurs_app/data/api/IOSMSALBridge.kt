package com.lemurs.lemurs_app.data.api

/**
 * Protocol interface for iOS MSAL authentication bridge.
 * This interface matches the MSALKotlinBridge Swift class.
 *
 * The actual implementation is in Swift (MSALKotlinBridge.swift).
 * Kotlin calls these methods via Objective-C interop.
 */
interface IOSMSALBridge {
    fun initialize(
        onAccountLoaded: (Boolean) -> Unit,
        onError: (String) -> Unit
    )

    fun acquireToken(
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    )

    fun acquireTokenSilently(
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    )

    fun signOut(
        onComplete: () -> Unit,
        onError: (String) -> Unit
    )

    fun isSignedIn(): Boolean
}

/**
 * Provider for accessing the Swift MSAL bridge.
 * This is set from Swift side during app initialization.
 */
object IOSMSALBridgeProvider {
    var bridge: IOSMSALBridge? = null
}
