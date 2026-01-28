package com.lemurs.lemurs_app.data.api

import org.koin.core.component.KoinComponent

expect class MicrosoftApiAuthorizationService(
    webApiService: WebAPIAuthorizationService
) : KoinComponent {
    fun initClient(navigate: (String) -> Unit): Boolean
    fun acquireToken()
    fun removeAccount()
}
