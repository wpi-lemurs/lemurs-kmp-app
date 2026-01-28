package com.lemurs.lemurs_app.util

import java.util.Locale
import co.touchlab.kermit.Logger
import com.lemurs.lemurs_app.data.datastore.NotificationTimesImpl
import com.lemurs.lemurs_app.data.local.UseCaseResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.TimeZone
import kotlin.text.format

class
NotificationUseCase : KoinComponent {

    val logger = Logger.withTag("NotificationsUseCase")
    fun randomize(): UseCaseResult<Any>{
        val calendar = java.util.Calendar.getInstance()
        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val minute = calendar.get(java.util.Calendar.MINUTE)
        val currentInstant = Clock.System.now()
        val timezone : TimeZone = calendar.getTimeZone()

        val date : String = (calendar.get(java.util.Calendar.MONTH)+1).toString() + "-" +
                            calendar.get(java.util.Calendar.DAY_OF_MONTH).toString() + "-" +
                            calendar.get(java.util.Calendar.YEAR).toString()
        logger.w("hour: $hour minute: $minute date $date")
        val notificationTimesImpl: NotificationTimesImpl by inject()
        var dateFromDataStore : String
        runBlocking {
            dateFromDataStore = notificationTimesImpl.getDate().first().toString()
        }
        //if there are not random times selected for today, pick new random times:
        if(!dateFromDataStore.equals(date)) {
            val notificationScheduler = NotificationScheduler("")
            
            // MODIFIED: Morning survey logic - only schedule reminders if initial notification hasn't been sent yet
            if(hour < 11) {
                // Check if morning initial notification has already been sent today
                // If not, the DailyNotificationSetupWorker will handle it
                logger.w("Morning survey initial notification will be handled by DailyNotificationSetupWorker")
            }
            //less than 2 hours left:
            else if(hour == 11 || (hour == 12 && minute < 45)){
                val totalMinLeft = (13 - hour)* 60  - minute - 14
                // Schedule reminders with adjusted timing for late morning
                notificationScheduler.scheduleReminder((totalMinLeft/2).toLong())
                notificationScheduler.scheduleFinalReminder(totalMinLeft.toLong(), totalMinLeft.toString())
            }

            // MODIFIED: Afternoon survey logic - only schedule reminders if initial notification hasn't been sent yet
            if(hour < 18) {
                // Check if afternoon initial notification has already been sent today
                // If not, the DailyNotificationSetupWorker will handle it
                logger.w("Afternoon survey initial notification will be handled by DailyNotificationSetupWorker")
            }
            //less than 2 hours left:
            else if(hour == 18 || (hour == 19 && minute < 45)){
                val totalMinLeft = (13 - hour)* 60  - minute - 14
                // Schedule reminders with adjusted timing for late afternoon
                notificationScheduler.scheduleReminder((totalMinLeft/2).toLong())
                notificationScheduler.scheduleFinalReminder(totalMinLeft.toLong(), totalMinLeft.toString())
            }

            //now update in datastore:
            // MODIFIED: Since we're not calculating specific times here anymore, just update the date
            logger.w("date: $date")
            runBlocking {
                notificationTimesImpl.updateDate(date)
            }
        }

        return UseCaseResult.Success(Any())
    }


    /**
     * schedules all three notifications together
     * time - string either "morning" or "afternoon"
     * delayOne- how many minutes after first notification the first reminder is sent
     * delayTwo- how many minutes after first notification the final reminder is sent
     * timeLeft - how many minutes left in the survey after the final reminder
     */
    fun schedule(time: String, delayOne: Long, delayTwo: Long, timeLeft: Int): UseCaseResult<Any> {
        logger.w("scheduleing $time notification with delay $delayOne and $delayTwo and $timeLeft minutes left")
        val notificationScheduler = NotificationScheduler(time)
        notificationScheduler.scheduleNotify()
        notificationScheduler.scheduleReminder(delayOne)
        notificationScheduler.scheduleFinalReminder(delayTwo, timeLeft.toString())
        return UseCaseResult.Success(Any())
    }
}