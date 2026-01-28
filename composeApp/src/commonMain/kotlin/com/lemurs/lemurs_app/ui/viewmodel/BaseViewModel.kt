package com.lemurs.lemurs_app.ui.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

open class BaseViewModel : CoroutineScope {
    override val coroutineContext = SupervisorJob() + Dispatchers.Main

    fun clear() {
        coroutineContext.cancel()
    }
}
