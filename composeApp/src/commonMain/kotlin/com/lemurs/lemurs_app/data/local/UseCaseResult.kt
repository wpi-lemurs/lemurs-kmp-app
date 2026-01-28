package com.lemurs.lemurs_app.data.local

/**
 * Result class for Successes and Failures in Use Case Classes
 */
sealed interface UseCaseResult<T> {
    data class Success<T>(val resultObject: T) : UseCaseResult<T>
    class Failure<T> : UseCaseResult<T>
}