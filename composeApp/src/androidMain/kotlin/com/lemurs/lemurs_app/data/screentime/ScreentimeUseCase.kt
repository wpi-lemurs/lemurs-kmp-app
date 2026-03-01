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
    private val screentimeDAO: ScreentimeDAO
) : KoinComponent {
    private val context : Context = requireNotNull(AndroidContextProvider.context)
    val logger = Logger.withTag("Screentime")
    private val usageStatsManager =
        context.getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
    private val appOpsManager = context.getSystemService(APP_OPS_SERVICE)!! as AppOpsManager

    @RequiresApi(Build.VERSION_CODES.Q)
    fun getScreenTime(): UseCaseResult<Any> {
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
        return UseCaseResult.Success(Any())
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun getUsageStats(usageStatsManager: UsageStatsManager): List<Screentime> {
        val now = System.currentTimeMillis()
        val fifteenMinAgo = now - (15 * 60 * 1000)

        logger.w("stats from " + Instant.fromEpochMilliseconds(now).toString() + " to " + Instant.fromEpochMilliseconds(fifteenMinAgo).toString())

        val events = usageStatsManager.queryEvents(fifteenMinAgo, now)
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
                    val startTime = appStartTimes.remove(pkg)
                    if (startTime != null) {
                        val duration = event.timeStamp - startTime
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
                val lastTimeUsedDate = Instant.fromEpochMilliseconds(lastUsed).toString()

                val screentimeData = Screentime(
                    System.currentTimeMillis().toString(),
                    Instant.fromEpochMilliseconds(fifteenMinAgo).toString(),
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
                Instant.fromEpochMilliseconds(fifteenMinAgo).toString(),
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