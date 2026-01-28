package com.lemurs.lemurs_app.util

import com.lemurs.lemurs_app.data.local.UseCaseResult

expect class NotificationUtil {
    fun checkSurveyCompleted(): Boolean
    fun sendNotificationText(title: String, body: String) : UseCaseResult<Any>
    fun sendNotificationWithoutCheck(title: String, body: String) : UseCaseResult<Any>
}