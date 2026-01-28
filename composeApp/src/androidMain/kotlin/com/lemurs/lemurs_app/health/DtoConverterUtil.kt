package com.lemurs.lemurs_app.health

import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WeightRecord
import com.lemurs.lemurs_app.data.dtos.CaloriesDataDto
import com.lemurs.lemurs_app.data.dtos.DistanceDataDto
import com.lemurs.lemurs_app.data.dtos.SleepDataDto
import com.lemurs.lemurs_app.data.dtos.SpeedDataDto
import com.lemurs.lemurs_app.data.dtos.StepsDataDto
import com.lemurs.lemurs_app.data.dtos.WeightDataDto
import kotlinx.datetime.toKotlinLocalDateTime
import java.time.ZoneId

fun WeightRecord.toDto(): WeightDataDto {
  return WeightDataDto(
    userId = "me",
    type = "weight",
    weight = this.weight.inPounds,
    timestamp = this.time.atZone(ZoneId.systemDefault()).toLocalDateTime().toKotlinLocalDateTime(),
    appSource = this.metadata.dataOrigin.toString()
  )
}


fun StepsRecord.toDto(): StepsDataDto {
  return StepsDataDto(
    userId = "me",
    type = "steps",
    steps = this.count,
    start_timestamp = this.startTime.atZone(ZoneId.systemDefault()).toLocalDateTime().toKotlinLocalDateTime(),
    end_timestamp = this.endTime.atZone(ZoneId.systemDefault()).toLocalDateTime().toKotlinLocalDateTime(),

    appSource = this.metadata.dataOrigin.toString()
  )
}

fun TotalCaloriesBurnedRecord.toDto(): CaloriesDataDto {
  return CaloriesDataDto(
    userId = "me",
    type = "calories",
    calories = this.energy.inCalories.toInt()/1000,
    start_timestamp = this.startTime.atZone(ZoneId.systemDefault()).toLocalDateTime().toKotlinLocalDateTime(),
    end_timestamp = this.endTime.atZone(ZoneId.systemDefault()).toLocalDateTime().toKotlinLocalDateTime(),
    appSource = this.metadata.dataOrigin.toString()
  )
}

fun SleepSessionRecord.toDto(): SleepDataDto {
  return SleepDataDto(
    userId = "me",
    type = "sleep",
    sleep = this.stages.toString(),
    startTimestamp = this.startTime.atZone(ZoneId.systemDefault()).toLocalDateTime().toKotlinLocalDateTime(),
    endTimestamp = this.endTime.atZone(ZoneId.systemDefault()).toLocalDateTime().toKotlinLocalDateTime(),
    appSource = this.metadata.dataOrigin.toString()
  )
}

fun DistanceRecord.toDto(): DistanceDataDto {
  return DistanceDataDto(
    userId = "me",
    type = "distance",
    distance = this.distance.inMeters,
    start_timestamp = this.startTime.atZone(ZoneId.systemDefault()).toLocalDateTime().toKotlinLocalDateTime(),
    end_timestamp = this.endTime.atZone(ZoneId.systemDefault()).toLocalDateTime().toKotlinLocalDateTime(),
    appSource = this.metadata.dataOrigin.toString()
  )
}

fun SpeedRecord.toDto(): SpeedDataDto {
  return SpeedDataDto(
    userId = "me",
    type = "speed",
    speed = this.samples.map { it.speed.inMetersPerSecond },
    start_timestamp = this.startTime.atZone(ZoneId.systemDefault()).toLocalDateTime().toKotlinLocalDateTime(),
    end_timestamp = this.endTime.atZone(ZoneId.systemDefault()).toLocalDateTime().toKotlinLocalDateTime(),
    unit = "m/s",
    appSource = this.metadata.dataOrigin.toString()
  )
}

