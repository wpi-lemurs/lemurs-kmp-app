package com.lemurs.lemurs_app.util

import com.lemurs.lemurs_app.survey.SurveyStatus
import com.lemurs.lemurs_app.survey.SurveyWindow

/**
 * What the morning setup should do with today.
 *
 * Extracted from `DailyNotificationSetupWorker` so the decision can be tested without WorkManager,
 * Koin or a DataStore. The worker keeps the IO; this holds the reasoning.
 */
sealed interface DailyWindowSource {

    /** Plan the day from these windows. */
    data class Plan(val windows: List<SurveyWindow>, val fromCache: Boolean) : DailyWindowSource

    /**
     * The study is over. Nothing should be scheduled, and standing work should be torn down.
     *
     * [knownWindowNames] carries every window name seen from either the server or the cache, since
     * cancelling per-window alarms needs the names and they come from the database rather than
     * being known at build time.
     */
    data class StudyConcluded(val knownWindowNames: List<String>) : DailyWindowSource

    /**
     * Nothing is known about today.
     *
     * Distinct from [StudyConcluded]: this means "ask again tomorrow", not "stop".
     */
    data object Unknown : DailyWindowSource
}

object DailyWindowResolver {

    /**
     * Decides where today's windows come from.
     *
     * [status] is null when the status could not be fetched at all, which is expected — this runs
     * before dawn, when connectivity is least reliable. That is what [cachedWindows] is for.
     */
    fun resolve(status: SurveyStatus?, cachedWindows: List<SurveyWindow>): DailyWindowSource {
        // Checked before anything else, and only ever from a live status. The server empties
        // `windows` when the study ends, which is indistinguishable from a failed fetch -- so
        // without this the cache below would resurrect a study that is over. A fetch that failed
        // outright leaves `status` null and cannot conclude anything.
        if (status != null && status.studyConcluded) {
            return DailyWindowSource.StudyConcluded(
                (status.windows + cachedWindows).map { it.name }.distinct()
            )
        }

        if (status != null && status.windows.isNotEmpty()) {
            return DailyWindowSource.Plan(status.windows, fromCache = false)
        }

        return if (cachedWindows.isEmpty()) {
            DailyWindowSource.Unknown
        } else {
            DailyWindowSource.Plan(cachedWindows, fromCache = true)
        }
    }
}
