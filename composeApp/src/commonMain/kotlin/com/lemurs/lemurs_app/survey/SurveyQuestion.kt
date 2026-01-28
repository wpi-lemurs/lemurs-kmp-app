package com.lemurs.lemurs_app.survey

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable

data class SurveyQuestion(
    val id: Int,
    val question: String,
    val style: String, // "open-response", "5-scale", "4-scale", "yes-no", "dropdown", "time-picker"
    var options: List<String?>? = null, // Only applicable for "dropdown" or 5-scale or 4 scale
    var selectedAnswer: @Contextual Any? = null // This can be String, Int, etc. based on the question style
)
