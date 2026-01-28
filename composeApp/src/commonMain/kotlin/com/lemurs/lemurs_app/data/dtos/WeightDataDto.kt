package com.lemurs.lemurs_app.data.dtos

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class WeightDataDto(
  val userId: String,
  val type: String = "weight",
  val weight: Double,
  val timestamp: @Contextual LocalDateTime,
  val unit: String = "lbs",
  val appSource: String,
)

@Serializable
data class StepsDataDto(
  val userId: String,
  val type: String = "steps",
  val steps: Long,
  val start_timestamp: @Contextual LocalDateTime,
  val end_timestamp: @Contextual LocalDateTime,
  val appSource: String,
)

@Serializable
data class CaloriesDataDto(
  val userId: String,
  val type: String = "calories",
  val calories: Int,
  val start_timestamp: @Contextual LocalDateTime,
  val end_timestamp: @Contextual LocalDateTime,
  val appSource: String,
)

@Serializable
data class SleepDataDto(
  val userId: String,
  val type: String = "sleep",
  val sleep: String,
  val startTimestamp: @Contextual LocalDateTime,
  val endTimestamp: @Contextual LocalDateTime,
  val appSource: String,
)

@Serializable
data class DistanceDataDto(
  val userId: String,
  val type: String = "distance",
  val distance: Double,
  val start_timestamp: @Contextual LocalDateTime,
  val end_timestamp: @Contextual LocalDateTime,
  val appSource: String,
)

@Serializable
data class SpeedDataDto(
  val userId: String,
  val type: String = "speed",
  val speed: List<Double>,
  val start_timestamp: @Contextual LocalDateTime,
  val end_timestamp: @Contextual LocalDateTime,
  val unit: String = "m/s",
  val appSource: String,
)
