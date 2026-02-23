package com.lemurs.lemurs_app.util

import kotlin.jvm.JvmField

object Constants {
    const val DATABASE_NAME = "lemurs.db"
    const val DEV_API_URL = "http://10.0.2.2:8080"
    const val IS_DEV = false
    const val debugModeEnabled = false
    const val LEMURS_DEV_API_URL = "lemurs-dev.wpi.edu"
    const val LEMURS_PROD_API_URL = "lemurs.wpi.edu"
    @JvmField val LEMURS_API_URL = if (IS_DEV) LEMURS_DEV_API_URL else LEMURS_PROD_API_URL
}