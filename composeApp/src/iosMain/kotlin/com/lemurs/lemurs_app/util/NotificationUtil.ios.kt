package com.lemurs.lemurs_app.util

import com.lemurs.lemurs_app.data.local.UseCaseResult


actual class NotificationUtil {
    actual fun checkSurveyCompleted(): Boolean{
        return true
    }
    actual fun sendNotificationText(
        title: String,
        body: String
    ): UseCaseResult<Any> {
        // iOS implementation - TODO
        return UseCaseResult.Success(Unit)
    }
    actual fun sendNotificationWithoutCheck(
        title: String,
        body: String
    ): UseCaseResult<Any> {
        // iOS implementation - TODO
        return UseCaseResult.Success(Unit)
    }
}
