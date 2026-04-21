package com.lemurs.lemurs_app.data.screentime

import android.app.AppOpsManager
import android.app.AppOpsManager.MODE_ALLOWED
import android.app.AppOpsManager.OPSTR_GET_USAGE_STATS
import android.app.usage.UsageEvents
import android.app.usage.UsageStats
import android.os.Process
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Context.APP_OPS_SERVICE
import android.content.Context.USAGE_STATS_SERVICE
import android.os.Build
import androidx.annotation.RequiresApi
import co.touchlab.kermit.Logger
import com.lemurs.lemurs_app.data.AndroidContextProvider
import com.lemurs.lemurs_app.data.local.ScreentimeSyncStore
import com.lemurs.lemurs_app.data.local.UseCaseResult
import com.lemurs.lemurs_app.data.local.passiveData.Screentime
import com.lemurs.lemurs_app.data.local.passiveData.ScreentimeDAO
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import org.koin.core.component.KoinComponent

/**
 * Use case for getting and storing screen time data
 */
class ScreentimeUseCase(
    private val screentimeDAO: ScreentimeDAO,
    private val syncStore: ScreentimeSyncStore

) : KoinComponent {
    private val context : Context = requireNotNull(AndroidContextProvider.context)
    val logger = Logger.withTag("Scrgieentime")
    private val usageStatsManager =
        context.getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
    private val appOpsManager = context.getSystemService(APP_OPS_SERVICE)!! as AppOpsManager

    @RequiresApi(Build.VERSION_CODES.Q)
    suspend fun getScreenTime(): UseCaseResult<Any> {
        val screentimeDataList = if (checkPermissions(appOpsManager)) {
            getUsageStats(usageStatsManager)
        } else {
            return UseCaseResult.Failure()
        }

        CoroutineScope(Dispatchers.IO).launch {
            // Deduplicate based on appName, totalTime, and lastTimeUsed to prevent duplicates
            val uniqueData = screentimeDataList.distinctBy {
                Triple(it.appName, it.totalTime, it.lastTimeUsed)
            }
            logger.w("Collected ${screentimeDataList.size} entries, inserting ${uniqueData.size} unique entries")
            screentimeDAO.insertScreentimeListData(uniqueData)
        }
        logger.w("done adding screentime data")
        syncStore.saveLastRunTime(System.currentTimeMillis())
        return UseCaseResult.Success(Any())
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    suspend fun getUsageStats(usageStatsManager: UsageStatsManager): List<Screentime> {
//        val now = System.currentTimeMillis()
//        val fifteenMinAgo = now - (15 * 60 * 1000)
//
//        logger.w("stats from " + Instant.fromEpochMilliseconds(now).toString() + " to " + Instant.fromEpochMilliseconds(fifteenMinAgo).toString())
//        val events = usageStatsManager.queryEvents(fifteenMinAgo, now)

        val lastRun = syncStore.getLastRunTime()
        val nextRun = lastRun + (15 * 60 * 1000)
        val events = usageStatsManager.queryEvents(lastRun, nextRun)

        logger.w("stats from " + Instant.fromEpochMilliseconds(lastRun).toString() + " to " + Instant.fromEpochMilliseconds(nextRun).toString())

        val event = UsageEvents.Event()

        val appStartTimes = mutableMapOf<String, Long>()
        val appTotalDurations = mutableMapOf<String, Long>()
        val lastEventTimes = mutableMapOf<String, Long>()

        // Process raw events to get precise 15-minute delta
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg = event.packageName ?: continue

            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    appStartTimes[pkg] = event.timeStamp
                }
                UsageEvents.Event.ACTIVITY_PAUSED,
                UsageEvents.Event.ACTIVITY_STOPPED -> {
                    // If we see a pause/stop without a start, assume it started at the beginning of the 15-minute window
                    val rawStart = appStartTimes.remove(pkg) ?: lastRun

                    // never count usage time earlier than the start of a 15-minute window.
                    val chunkStart = maxOf(rawStart, lastRun)
                    val duration = event.timeStamp - chunkStart

                    if (duration > 0) {
//                        val duration = event.timeStamp - startTime
                        appTotalDurations[pkg] = (appTotalDurations[pkg] ?: 0L) + duration
                    } else {
                        // If an app never started in that chunk but began in an earlier one,
                        // then startTime would not be recorded on that chunk.
                        // so we subtract the duration of time the app ran for that chunk.
                        val duration = event.timeStamp - fifteenMinAgo
                        appTotalDurations[pkg] = (appTotalDurations[pkg] ?: 0L) + duration
                    }
                }
            }
            lastEventTimes[pkg] = event.timeStamp
        }

        // Handle app currently being used
        appStartTimes.forEach { (pkg, startTime) ->
            val ongoingDuration = now - startTime
            appTotalDurations[pkg] = (appTotalDurations[pkg] ?: 0L) + ongoingDuration
        }

        val screentimeDataList: MutableList<Screentime> = mutableListOf()

        for ((pkgName, totalTime) in appTotalDurations) {
            if (totalTime > 0) {
                val lastUsed = lastEventTimes[pkgName] ?: 0L
                // If the last event for this app is ACTIVITY_RESUMED, set lastTimeUsed to now
                val lastEventType = appStartTimes[pkgName]?.let { _ -> UsageEvents.Event.ACTIVITY_RESUMED } // If app is still active
                val lastTimeUsedDate = if (lastEventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                    Instant.fromEpochMilliseconds(now).toString()
                } else {
                    Instant.fromEpochMilliseconds(lastUsed).toString()
                }

                val screentimeData = Screentime(
                    System.currentTimeMillis().toString(),
                    Instant.fromEpochMilliseconds(lastRun).toString(),
                    Instant.fromEpochMilliseconds(now).toString(),
                    pkgName,
                    totalTime,
                    lastTimeUsedDate
                )

                logger.w("package name: $pkgName total time: $totalTime last time used: $lastTimeUsedDate")
                screentimeDataList.add(screentimeData)
            }
        }

        // If no screentime was detected, insert a meaningless entry to indicate
        // data collection was successful but user wasn't using their phone.
        // distingush betwween no activity and no lemurs app running
        if (screentimeDataList.isEmpty()) {
            val heartbeatEntry = Screentime(
                System.currentTimeMillis().toString(),
                Instant.fromEpochMilliseconds(lastRun).toString(),
                Instant.fromEpochMilliseconds(now).toString(),
                "com.lemurs.no_screentime_detected",
                0L,  // Zero usage time
                Instant.fromEpochMilliseconds(now).toString()
            )
            logger.w("No screentime detected in interval, inserting heartbeat entry")
            screentimeDataList.add(heartbeatEntry)
        }

        return screentimeDataList
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun checkPermissions(appOpsManager: AppOpsManager): Boolean {
        var mode = 0
        mode =
            appOpsManager.unsafeCheckOpNoThrow(OPSTR_GET_USAGE_STATS, Process.myUid(), "com.lemurs")
        logger.w("permissions: " + (mode==MODE_ALLOWED))
        return mode == MODE_ALLOWED
    }

}