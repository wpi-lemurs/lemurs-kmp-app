package com.lemurs.lemurs_app.data.local

interface SendDataUseCase {

    suspend fun call(): UseCaseResult<Any>

}
