package com.lemurs.lemurs_app.data

import android.app.Activity

object AndroidActivityProvider {
    private var appActivity: Activity? = null

    val activity: Activity?
        get() = appActivity

    fun setActivity(activity: Activity) {
        appActivity = activity
    }
}