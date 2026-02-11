package com.lemurs.lemurs_app.ui.screens

import com.lemurs.lemurs_app.util.NotificationUtil

actual fun scheduleWeeklySurveyNotificationIos(nextWeeklySurvey: String) {
    NotificationUtil().scheduleWeeklySurveyNotification(nextWeeklySurvey)
}
