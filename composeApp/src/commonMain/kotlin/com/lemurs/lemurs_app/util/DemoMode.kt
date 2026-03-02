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
 * - Returns hardcoded daily survey questions (5 mood-related questions)
 * - Returns hardcoded weekly survey questions (PHQ-9 style, 10 questions)
 * - Simulates successful survey submissions (no actual API calls made)
 * - API calls are made without authentication tokens (may fail gracefully)
 *
 * ## Hardcoded Surveys in Demo Mode:
 * ### Daily Survey:
 * - How are you feeling right now?
 * - How stressed do you feel right now?
 * - How well did you sleep last night?
 * - How motivated do you feel today?
 * - How connected do you feel to others right now?
 *
 * ### Weekly Survey (PHQ-9):
 * - Little interest or pleasure in doing things
 * - Feeling down, depressed, or hopeless
 * - Trouble falling or staying asleep, or sleeping too much
 * - Feeling tired or having little energy
 * - Poor appetite or overeating
 * - Feeling bad about yourself
 * - Trouble concentrating on things
 * - Moving or speaking slowly (or too fast)
 * - Thoughts that you would be better off dead (trigger question)
 * - Difficulty assessment question
 *
 * ## Important Notes:
 * - Demo mode is session-only; it resets when the app is closed
 * - Data submitted in demo mode will NOT be sent to the server
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

