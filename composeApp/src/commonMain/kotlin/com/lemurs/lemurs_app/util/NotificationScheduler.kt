package com.lemurs.lemurs_app.util

import kotlinx.datetime.LocalTime

/**
 * Schedules survey notifications.
 *
 * Every method takes the window name rather than being duplicated per window, so the set of windows
 * can change in the database without an app release.
 */
expect class NotificationScheduler() {

    /** Arms the pre-dawn alarms that plan each day's notifications. */
    fun scheduleDailyNotificationSetup()

    /** Re-arms tomorrow's setup alarms, since they are one-shot. */
    fun rescheduleDailySetupsForTomorrow()

    /**
     * Schedules [windowName]'s opening nudge for [atLocalTime] today.
     *
     * [forceToday] keeps a time that has already passed today rather than rolling it to tomorrow,
     * which is what a setup that ran late needs.
     */
    fun scheduleInitialNotification(windowName: String, atLocalTime: LocalTime, forceToday: Boolean)

    /** Schedules a single "last call" for [windowName], with no follow-up reminders. */
    fun scheduleLastChanceNotification(windowName: String)

    /** Schedules a reminder for [windowName] to fire [delayMinutes] from now. */
    fun scheduleReminder(
        windowName: String,
        delayMinutes: Long,
        title: String,
        body: String,
        isFinal: Boolean
    )

    /** Schedules the weekly survey notification. */
    fun scheduleWeeklySurveyNotification()

    /**
     * Cancels every scheduled notification permanently.
     *
     * For the end of the study, not for a completed survey. On Android the pre-dawn setup alarms
     * re-arm themselves each morning, so stopping means cancelling them rather than simply
     * declining to schedule anything: otherwise the chain plans a new day forever.
     *
     * [windowNames] are needed because per-window alarms and registrations are keyed by name, and
     * the set of windows comes from the database rather than being known at build time.
     */
    fun cancelAll(windowNames: List<String>)
}
