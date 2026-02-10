# HealthKit Integration for iOS

This directory contains the Swift bridge for Apple HealthKit integration in the LEMURS app.

## Overview

HealthKit is Apple's framework for accessing health and fitness data on iOS devices. This bridge allows the Kotlin Multiplatform Compose app to access HealthKit data through Swift interop.

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                     Compose UI (Kotlin)                          │
│                  HealthScreen (HealthClient.ios.kt)              │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│               IOSHealthKitBridge (Kotlin Interface)              │
│      (composeApp/src/iosMain/.../IOSHealthKitBridge.kt)         │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│              HealthKitBridgeAdapter (Swift)                      │
│      Adapts Swift types to Kotlin types (KotlinBoolean, etc.)   │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│              HealthKitKotlinBridge (Swift)                       │
│      Wraps Apple HealthKit APIs                                  │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                   Apple HealthKit Framework                      │
└─────────────────────────────────────────────────────────────────┘
```

## Setup Requirements

### 1. Add HealthKit Capability in Xcode

1. Open the project in Xcode
2. Select the app target
3. Go to "Signing & Capabilities"
4. Click "+ Capability"
5. Add "HealthKit"
6. Enable "Background Delivery" if you want background updates

### 2. Add Usage Descriptions to Info.plist

Add these keys to `iosApp/iosApp/Info.plist`:

```xml
<key>NSHealthShareUsageDescription</key>
<string>LEMURS needs access to your health data to track your fitness progress and provide personalized insights.</string>
<key>NSHealthUpdateUsageDescription</key>
<string>LEMURS needs to write health data to track your activities.</string>
```

### 3. Register the Bridge

In `iOSApp.swift`, add the bridge registration during initialization:

```swift
init() {
    // Register MSAL bridge
    registerMSALBridgeWithKotlin()
    
    // Register HealthKit bridge
    registerHealthKitBridgeWithKotlin()
    
    // Initialize Koin
    MainViewControllerKt.doInitKoin()
}
```

### 4. Add HealthKit Framework

The HealthKit framework should be automatically linked when you add the capability, but if needed, manually add it:

1. Select the app target
2. Go to "Build Phases"
3. Expand "Link Binary With Libraries"
4. Add `HealthKit.framework`

## Data Types Supported

| Data Type        | HealthKit Identifier      | Description                        |
|------------------|---------------------------|------------------------------------|
| Steps            | `.stepCount`              | Daily step count                   |
| Active Calories  | `.activeEnergyBurned`     | Calories burned through activity   |   
| Basal Calories   | `.basalEnergyBurned`      | Calories burned without activity   | 
| Distance         | `.distanceWalkingRunning` | Walking/running distance in meters |
| Heart Rate       | `.heartRate`              | Heart rate measurements (BPM)      |
| Sleep            | `.sleepAnalysis`          | Sleep duration and stages          |

## Usage from Kotlin

```kotlin
// Check availability
val isAvailable = IOSHealthKitBridgeProvider.bridge?.isHealthKitAvailable() ?: false

// Request authorization
IOSHealthKitBridgeProvider.bridge?.requestAuthorization(
    onSuccess = { granted ->
        println("Authorization result: $granted")
    },
    onError = { error ->
        println("Authorization error: $error")
    }
)

// Get step count
val now = System.currentTimeMillis()
val yesterday = now - 24 * 60 * 60 * 1000
IOSHealthKitBridgeProvider.bridge?.getStepCount(
    startTimeMillis = yesterday,
    endTimeMillis = now,
    onSuccess = { steps ->
        println("Steps: $steps")
    },
    onError = { error ->
        println("Error: $error")
    }
)
```

## Privacy Considerations

- HealthKit is only available on real iOS devices (limited functionality on Simulator)
- Users must explicitly grant permission for each data type
- iOS does not reveal whether the user granted or denied a specific permission (for privacy)
- The `authorizationStatus` only tells you if authorization was requested, not if it was granted
- Always handle the case where permissions are denied gracefully

## Differences from Android Health Connect

| Feature             | iOS HealthKit                  | Android Health Connect   |
|---------------------|--------------------------------|--------------------------|
| API Style           | Query-based                    | Read records             |
| Permissions         | Per-type, but status is hidden | Per-type, visible status |
| Background Delivery | Observer queries               | Changes tokens           |
| Data Sync           | Automatic via iCloud           | Per-app storage          |
| Simulator Support   | Limited                        | Emulator support varies  |

## Troubleshooting

### "HealthKit is not available"
- HealthKit is not supported on iPad (before iPadOS 17)
- HealthKit has limited support on iOS Simulator
- Test on a real device when possible

### Authorization Dialog Not Showing
- Make sure Info.plist has the required usage descriptions
- Make sure the HealthKit capability is added in Xcode
- The dialog only shows once per data type combination

### No Data Returned
- Check if the user has granted permissions in Settings > Privacy > Health
- HealthKit may return empty results if there's no data for the time range
- Some data types may not have data on the simulator
