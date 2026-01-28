package com.lemurs.lemurs_app.data

import android.content.Context

object AndroidContextProvider {
    private var appContext: Context? = null

    val context: Context?
        get() = appContext

    fun setContext(context: Context) {
        appContext = context
    }
}