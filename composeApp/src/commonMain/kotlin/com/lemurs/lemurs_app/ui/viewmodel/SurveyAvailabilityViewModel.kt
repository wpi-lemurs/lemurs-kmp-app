package com.lemurs.lemurs_app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.lemurs.lemurs_app.survey.SurveyStatus
import com.lemurs.lemurs_app.survey.SurveyWindowState
import com.lemurs.lemurs_app.survey.SurveyWindows
import com.lemurs.lemurs_app.survey.SurveysApi
import com.lemurs.lemurs_app.survey.fetchSurveyStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.until

/**
 * Holds the survey status and re-evaluates it against the phone's clock.
 *
 * The server states what the windows are and what has already been submitted today; every decision
 * about whether something is *open* is made here, in the participant's own timezone. That split is
 * what makes the app behave correctly outside Eastern.
 */
class SurveyAvailabilityViewModel(
    private val api: SurveysApi
) : ViewModel() {

    private val logger = Logger.withTag("SurveyAvailability")

    private val _status = MutableStateFlow<SurveyStatus?>(null)
    val status: StateFlow<SurveyStatus?> = _status.asStateFlow()

    /** Advances on a timer so countdowns tick and windows open without needing a refresh. */
    private val _now = MutableStateFlow(Clock.System.now())

    private val _dailyState = MutableStateFlow<SurveyWindowState?>(null)

    /** The last non-empty set of window names the server reported. See [knownWindowNames]. */
    private var lastKnownWindowNames: List<String> = emptyList()

    /** Null until the first successful fetch, which the UI shows as a spinner. */
    val dailyState: StateFlow<SurveyWindowState?> = _dailyState.asStateFlow()

    init {
        viewModelScope.launch {
            while (true) {
                delay(TICK_MILLIS)
                _now.value = Clock.System.now()
                recompute()
            }
        }
    }

    /**
     * The window names last seen from the server.
     *
     * Needed to cancel per-window alarms at the end of the study, since the windows come from the
     * database rather than being known at build time.
     *
     * Held separately from [status] rather than read off it, because the server reports no windows
     * once the study has concluded -- which is exactly when the names are needed. Reading the live
     * status would hand the teardown an empty list and leave any already-armed per-window alarms
     * running: the 06:30 setup can have armed them from cache before the app learned the study was
     * over.
     *
     * Still empty if this process has never seen a window -- a fresh install, or the view model
     * recreated after process death, opened for the first time on day 29. The setup worker covers
     * that case, since it resolves names from the DataStore cache rather than from memory.
     */
    fun knownWindowNames(): List<String> = lastKnownWindowNames

    /** Fetches in the background; the UI keeps showing the previous state meanwhile. */
    fun refresh() {
        viewModelScope.launch { refreshAndWait() }
    }

    /** Fetches and returns only once the state has been updated. */
    suspend fun refreshAndWait() {
        val fetched = fetchSurveyStatus(api)
        if (fetched == null) {
            // Keep whatever we last knew. Dropping to "closed" on a flaky network
            // would lock a participant out of a survey that is genuinely open.
            logger.w("Status fetch failed; keeping the previous state")
            return
        }
        _status.value = fetched
        if (fetched.windows.isNotEmpty()) {
            lastKnownWindowNames = fetched.windows.map { it.name }
        }
        recompute()
        logger.d("Status refreshed: $fetched")
    }

    private fun recompute() {
        val current = _status.value ?: return
        _dailyState.value = SurveyWindows.evaluate(
            windows = current.windows,
            completedWindowNames = current.completedWindows,
            studyConcluded = current.studyConcluded,
            at = _now.value
        )
    }

    /**
     * Seconds until the weekly survey opens, or a non-positive value if it is open now.
     *
     * The weekly survey is gated on elapsed days rather than time of day, so unlike the daily
     * windows it genuinely is an absolute instant and needs no timezone reasoning.
     */
    fun secondsUntilWeekly(at: Instant = Clock.System.now()): Long? {
        val next = _status.value?.weeklyNextAvailable ?: return null
        return at.until(next, DateTimeUnit.SECOND)
    }

    private companion object {
        const val TICK_MILLIS = 30_000L
    }
}
