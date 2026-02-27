package com.lemurs.lemurs_app.util

/**
 * Demo Mode Manager for App Store Review
 *
 * This allows Apple App Store reviewers to bypass MSAL (Microsoft Authentication Library)
 * authentication when testing the app, since they don't have WPI Microsoft accounts.
 *
 * ## How to Activate Demo Mode:
 * On the Login screen, tap the Lemur icon 5 times quickly (within 3 seconds).
 * The app will automatically navigate to the main screen in demo mode.
 *
 * ## What Demo Mode Does:
 * - Bypasses Microsoft authentication (no WPI account needed)
 * - Returns mock progress data (earnings, survey completions)
 * - Makes all surveys appear available
 * - API calls are made without authentication tokens (may fail gracefully)
 *
 * ## Important Notes:
 * - Demo mode is session-only; it resets when the app is closed
 * - Data submitted in demo mode will fail (no auth tokens)
 * - This is intended ONLY for App Store review purposes
 *
 * @see LoginScreen for the hidden tap gesture implementation
 */
object DemoMode {
    /**
     * Whether demo mode is currently active
     */
    var isActive: Boolean = false
        private set

    /**
     * Activate demo mode
     */
    fun activate() {
        isActive = true
        println("🔵 (DemoMode) Demo mode activated for App Review")
    }

    /**
     * Deactivate demo mode
     */
    fun deactivate() {
        isActive = false
        println("🔵 (DemoMode) Demo mode deactivated")
    }

    /**
     * Demo access token for mock API calls (not used for actual auth)
     */
    const val DEMO_ACCESS_TOKEN = "demo_access_token_for_review"

    /**
     * Demo refresh token for mock API calls (not used for actual auth)
     */
    const val DEMO_REFRESH_TOKEN = "demo_refresh_token_for_review"
}

