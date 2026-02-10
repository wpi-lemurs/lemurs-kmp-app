package com.lemurs.lemurs_app.data.screentime

import android.app.AppOpsManager
import android.app.AppOpsManager.MODE_ALLOWED
import android.app.AppOpsManager.OPSTR_GET_USAGE_STATS
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
        //        get a 15 minute interval
        val now = System.currentTimeMillis()
        val fifteenMinAgo = now - (15 * 60 * 1000)
        val queryUsageStats: List<UsageStats> = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_BEST,
            fifteenMinAgo,
            now
        )
        logger.w("stats from "+Instant.fromEpochMilliseconds(now).toString()+" to " + Instant.fromEpochMilliseconds(fifteenMinAgo).toString())
        val screentimeDataList: MutableList<Screentime> = mutableListOf()
        for (i in queryUsageStats.indices) {
            // Only include apps that were actually used in the 15-minute window
            if (queryUsageStats[i].lastTimeUsed >= fifteenMinAgo && queryUsageStats[i].totalTimeVisible > 0) {
                val stats = queryUsageStats.get(i)
//                val firstTimeStamp = stats.firstTimeStamp
//                val firstTimeStampDate = Instant.fromEpochMilliseconds(firstTimeStamp).toString()
//                val lastTimeStamp = stats.lastTimeStamp
//                val lastTimeStampDate = Instant.fromEpochMilliseconds(lastTimeStamp).toString()
                val appName = stats.packageName
                val lastTimeUsed = stats.lastTimeUsed
                val lastTimeUsedDate = Instant.fromEpochMilliseconds(lastTimeUsed).toString()
                val totalTime: Long = stats.totalTimeVisible
                //TODO- make dates into date format
                val screentimeData = Screentime(
                    //0,
                    System.currentTimeMillis().toString(),
                    Instant.fromEpochMilliseconds(fifteenMinAgo).toString(),
                    Instant.fromEpochMilliseconds(now).toString(),
                    appName,
                    totalTime,
                    lastTimeUsedDate
                )
                logger.w("package name: " +appName+" total time: "+totalTime+" last time used: "+lastTimeUsedDate)
                screentimeDataList.add(screentimeData)
            }
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