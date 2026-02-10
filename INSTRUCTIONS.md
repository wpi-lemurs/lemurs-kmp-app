# LEMURS Mobile App - Kotlin Multiplatform (KMP) Documentation

# Table of Contents

- [Overview](#overview)
- [Running the Project](#running-the-project)
  - [Running Android App](#running-android-app)
- [Project Architecture](#project-architecture)
- [Source Set Structure](#source-set-structure)
- [Code Flow Diagram](#code-flow-diagram)
- [expect/actual Pattern](#expectactual-pattern)
- [iOS Native Integration](#ios-native-integration)
  - [Swift/Kotlin Interop Architecture](#swiftkotlin-interop-architecture)
  - [Microsoft Authentication (MSAL) Setup](#microsoft-authentication-msal-setup)
  - [HealthKit Integration](#healthkit-integration)
- [Adding New Functionality](#adding-new-functionality)
- [Build & Run](#build--run)
- [Project Structure Summary](#project-structure-summary)
- [Creating release APKs for Android](#creating-release-apks-for-android)
- [Creating release builds for iOS](#creating-release-builds-for-ios)
- [Additional Resources](#additional-resources)

## Overview

This project is a **Kotlin Multiplatform (KMP)** mobile application targeting both **Android** and **iOS** platforms. It uses **Compose Multiplatform** for building shared UI components and business logic while allowing platform-specific implementations where needed.

> **Migration Note**: This project is a migration from the original lemurs-app repository.  
> **Original Repository**: [https://github.com/wpi-lemurs/lemurs-app]

---
## Running the Project

### Running Android App
1. Install Android Studio
2. Generate a project-local debug keystore
3. Install and configure Java 17 (The Android Gradle Plugin 8.11.1 requires at least JDK 17)
4. Update your Android project files to wire in the new redirect URI
5. Clean, rebuild, and install the debug APK:
   ```bash
   Build → Clean Project
   Build → Rebuild Project
   Run ▶︎ (ensure your Run Configuration is for the composeApp module)
   ```
* Contact the advisory team for the keystore passwords and any other credentials.

### Running iOS App
1. Open the iosApp/ directory in Xcode
2. Install dependencies
3. Download simulator or connect an iphone to device
4. Select your target and press the play button
   
## Project Architecture

### High-Level Structure

```
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                              KOTLIN MULTIPLATFORM PROJECT                           │
├─────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                     │
│  ┌──────────────────────────────────────────────────────────────────────────────┐   │
│  │                            composeApp/                                       │   │
│  │  ┌────────────────────────────────────────────────────────────────────────┐  │   │
│  │  │                         commonMain/                                    │  │   │
│  │  │     • Shared Kotlin code (business logic, ViewModels, data layer)      │  │   │
│  │  │     • Compose Multiplatform UI components                              │  │   │
│  │  │     • Platform-agnostic interfaces (expect declarations)               │  │   │
│  │  └────────────────────────────────────────────────────────────────────────┘  │   │
│  │                           │                                                  │   │
│  │           ┌───────────────┴───────────────┐                                  │   │
│  │           │                               │                                  │   │
│  │           ▼                               ▼                                  │   │
│  │  ┌─────────────────────┐      ┌─────────────────────┐                        │   │
│  │  │     androidMain/    │      │      iosMain/       │                        │   │
│  │  │   • Android-specific│      │   • iOS-specific    │                        │   │
│  │  │     implementations │      │     implementations │                        │   │
│  │  │   • actual funcs    │      │   • actual funcs    │                        │   │
│  │  │   • Native APIs     │      │   • Native APIs     │                        │   │
│  │  └─────────────────────┘      └─────────────────────┘                        │   │
│  └──────────────────────────────────────────────────────────────────────────────┘   │
│                │                               │                                    │
│                │                               │                                    │
│                ▼                               ▼                                    │
│  ┌─────────────────────────┐      ┌─────────────────────────────────────────────┐   │
│  │    Android App Output   │      │                  iosApp/                    │   │
│  │         (APK/AAB)       │      │  ┌───────────────────────────────────────┐  │   │
│  │                         │      │  │           Swift/SwiftUI Layer         │  │   │
│  │  • Uses androidMain     │      │  │   • Native iOS app entry point        │  │   │
│  │    + commonMain         │      │  │   • Swift bridges for native APIs     │  │   │
│  │                         │      │  │   • Uses iosMain + commonMain         │  │   │
│  └─────────────────────────┘      │  └───────────────────────────────────────┘  │   │
│                                   └─────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────────────┘
```

---

## Source Set Structure

### `composeApp/src/` Directory

| Folder          | Description                      | Example Files                                              |
|-----------------|----------------------------------|------------------------------------------------------------|
| `commonMain/`   | Shared code for all platforms    | `App.kt`, ViewModels, Repositories, Data classes           |
| `commonTest/`   | Shared test code                 | Unit tests for shared logic                                |
| `androidMain/`  | Android-specific implementations | `MainActivity.kt`, Android services, `Platform.android.kt` |
| `iosMain/`      | iOS-specific implementations     | `MainViewController.kt`, iOS bridges, `Platform.ios.kt`    |

### `iosApp/` Directory

| Folder              | Description                                          |
|---------------------|------------------------------------------------------|
| `iosApp/`           | Native Swift/SwiftUI code                            |
| `iosApp/Auth/`      | Microsoft Authentication (MSAL) Swift implementation |
| `iosApp/HealthKit/` | Apple HealthKit Swift bridge                         |

---

## Code Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                                   USER                                          │
└─────────────────────────────────────────────────────────────────────────────────┘
                    │                                       │
                    │ Android Device                        │ iOS Device
                    ▼                                       ▼
┌───────────────────────────────────┐     ┌────────────────────────────────────────┐ 
│         Android Runtime           │     │             iOS Runtime                │
│    ┌──────────────────────────┐   │     │   ┌───────────────────────────────┐    │
│    │     MainActivity.kt      │   │     │   │    iOSApp.swift (SwiftUI)     │    │
│    │      (androidMain)       │   │     │   │    ContentView.swift          │    │
│    └────────────┬─────────────┘   │     │   └───────────────┬───────────────┘    │
│                 │                 │     │                   │                    │
│                 ▼                 │     │                   ▼                    │
│    ┌──────────────────────────┐   │     │   ┌───────────────────────────────┐    │
│    │   Platform.android.kt    │   │     │   │    MainViewController.kt      │    │
│    │   (actual functions)     │   │     │   │   Platform.ios.kt (actual)    │    │
│    └────────────┬─────────────┘   │     │   └───────────────┬───────────────┘    │
│                 │                 │     │                   │                    │
└─────────────────┼─────────────────┘     └───────────────────┼────────────────────┘
                  │                                           │
                  │                                           │
                  └──────────────────┬────────────────────────┘
                                     │
                                     ▼
                    ┌────────────────────────────────────┐
                    │          commonMain/               │
                    │  ┌──────────────────────────────┐  │
                    │  │         App.kt               │  │
                    │  │    (Compose Entry Point)     │  │
                    │  └──────────────┬───────────────┘  │
                    │                 │                  │
                    │                 ▼                  │
                    │  ┌──────────────────────────────┐  │
                    │  │    UI Layer (Compose UI)     │  │
                    │  │    Screens, Components       │  │
                    │  └──────────────┬───────────────┘  │
                    │                 │                  │
                    │                 ▼                  │
                    │  ┌──────────────────────────────┐  │
                    │  │   ViewModels / Use Cases     │  │
                    │  └──────────────┬───────────────┘  │
                    │                 │                  │
                    │                 ▼                  │
                    │  ┌──────────────────────────────┐  │
                    │  │    Data Layer                │  │
                    │  │   Repositories, DAOs, APIs   │  │
                    │  └──────────────────────────────┘  │
                    └────────────────────────────────────┘
```

---

## expect/actual Pattern

KMP uses the `expect`/`actual` pattern for platform-specific implementations. We define "interfaces" in a sense in commonMain which are then implemented in each platform.

### Example: Platform Declaration

**commonMain** (`Platform.kt`):
```kotlin
// Expect declaration - interface only
expect fun getPlatformName(): String
```

**androidMain** (`Platform.android.kt`):
```kotlin
// Actual implementation for Android
actual fun getPlatformName(): String = "Android ${Build.VERSION.SDK_INT}"
```

**iosMain** (`Platform.ios.kt`):
```kotlin
// Actual implementation for iOS
actual fun getPlatformName(): String = UIDevice.currentDevice.systemName()
```

---

## iOS Native Integration

### Swift/Kotlin Interop Architecture

For iOS-specific features that require Swift APIs (like HealthKit, MSAL), we use a **bridge pattern**:

```
┌────────────────────────────────────────────────────────────────────────────────┐
│                         SWIFT ↔ KOTLIN BRIDGE PATTERN                          │
├────────────────────────────────────────────────────────────────────────────────┤
│                                                                                │
│  ┌───────────────────────────────────────────────────────────────────────────┐ │
│  │                        Kotlin Side (iosMain/)                             │ │
│  │  ┌─────────────────────────────────────────────────────────────────────┐  │ │
│  │  │   1. Define Bridge Interface (e.g., IOSHealthKitBridge)             │  │ │
│  │  │   2. Create Provider Singleton (e.g., IOSHealthKitBridgeProvider)   │  │ │
│  │  │   3. Use in Kotlin code via provider.bridge?.methodName()           │  │ │
│  │  └─────────────────────────────────────────────────────────────────────┘  │ │
│  └───────────────────────────────────────────────────────────────────────────┘ │
│                                     ▲                                          │
│                                     │  Kotlin/Native Interop                   │
│                                     ▼                                          │
│  ┌───────────────────────────────────────────────────────────────────────────┐ │
│  │                         Swift Side (iosApp/)                              │ │
│  │  ┌─────────────────────────────────────────────────────────────────────┐  │ │
│  │  │   1. BridgeAdapter - implements Kotlin interface                    │  │ │
│  │  │   2. KotlinBridge - wraps native Swift/iOS APIs                     │  │ │
│  │  │   3. Register bridge in iOSApp.swift at startup                     │  │ │
│  │  └─────────────────────────────────────────────────────────────────────┘  │ │
│  └───────────────────────────────────────────────────────────────────────────┘ │
│                                     │                                          │
│                                     ▼                                          │
│  ┌───────────────────────────────────────────────────────────────────────────┐ │
│  │                     Native iOS SDK (HealthKit, MSAL, etc.)                │ │
│  └───────────────────────────────────────────────────────────────────────────┘ │
└────────────────────────────────────────────────────────────────────────────────┘
```

### Microsoft Authentication (MSAL) Setup

For detailed MSAL setup, see: [`iosApp/iosApp/Auth/README.md`](./iosApp/iosApp/Auth/README.md)

**Architecture Overview:**

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          MSAL AUTHENTICATION FLOW                           │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  Kotlin (Shared)                                                            │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │  MicrosoftApiAuthorizationService.ios.kt → IOSMSALBridgeProvider     │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
│                                     │                                       │
│                                     ▼                                       │
│  Swift (iOS)                                                                │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │  MSALBridgeAdapter → MSALKotlinBridge → MSALAuthHelper               │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
│                                     │                                       │
│                                     ▼                                       │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │                    MSAL iOS SDK (via CocoaPods)                      │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
│                                     │                                       │
│                                     ▼                                       │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │                 Azure Active Directory / Microsoft Graph             │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────┘
```

**Key Files:**
- `iosApp/iosApp/Auth/MSALAuthHelper.swift` - Core MSAL wrapper
- `iosApp/iosApp/Auth/MSALKotlinBridge.swift` - Swift to Kotlin bridge
- `composeApp/src/iosMain/.../IOSMSALBridge.kt` - Kotlin interface

### HealthKit Integration

For detailed HealthKit setup, see: [`iosApp/iosApp/HealthKit/README.md`](./iosApp/iosApp/HealthKit/README.md)

**Architecture Overview:**

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           HEALTHKIT DATA FLOW                               │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  Kotlin (commonMain)                                                        │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │                    HealthScreen / HealthClient                       │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
│                                     │                                       │
│                                     ▼                                       │
│  Kotlin (iosMain)                                                           │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │            IOSHealthKitBridge → IOSHealthKitBridgeProvider           │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
│                                     │                                       │
│                                     ▼                                       │
│  Swift (iosApp)                                                             │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │  HealthKitBridgeAdapter → HealthKitKotlinBridge                      │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
│                                     │                                       │
│                                     ▼                                       │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │                     Apple HealthKit Framework                        │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Adding New Functionality

### Step 1: Shared Logic (commonMain)

Add shared code that works on both platforms:

```
composeApp/src/commonMain/kotlin/com/lemurs/lemurs_app/
├── data/              # Data layer (repositories, DAOs, models)
├── di/                # Dependency injection (Koin modules)
├── health/            # Health-related features
├── survey/            # Survey features
├── ui/                # UI components
└── util/              # Utility functions
```

### Step 2: Platform-Specific (if needed)

**For Android** (`androidMain/`):
- Add Android-specific implementations
- Use Android SDK APIs directly
- Define `actual` implementations for `expect` declarations

**For iOS** (`iosMain/`):
- Add Kotlin implementations that bridge to Swift
- Define `actual` implementations for `expect` declarations
- Create bridge interfaces for Swift interop

### Step 3: Native iOS Code (if needed)

For iOS-only APIs (HealthKit, MSAL, etc.):

1. **Create Kotlin Bridge Interface** (`iosMain/`):
   ```kotlin
   interface IOSMyFeatureBridge {
       fun doSomething(onSuccess: (String) -> Unit, onError: (String) -> Unit)
   }
   
   object IOSMyFeatureBridgeProvider {
       var bridge: IOSMyFeatureBridge? = null
   }
   ```

2. **Create Swift Implementation** (`iosApp/iosApp/MyFeature/`):
   ```swift
   class MyFeatureBridgeAdapter: IOSMyFeatureBridge {
       func doSomething(onSuccess: @escaping (String) -> Void, onError: @escaping (String) -> Void) {
           // Call native iOS APIs
       }
   }
   ```

3. **Register in iOSApp.swift**:
   ```swift
   init() {
       IOSMyFeatureBridgeProvider.shared.bridge = MyFeatureBridgeAdapter()
       MainViewControllerKt.doInitKoin()
   }
   ```

---

## Build & Run

### Android

```bash
# Debug build
./gradlew :composeApp:assembleDebug

# Release build
./gradlew :composeApp:assembleRelease
```

### iOS

1. Install CocoaPods dependencies:
   ```bash
   cd iosApp
   pod install
   ```

2. Open in Xcode:
   ```bash
   open iosApp.xcworkspace
   ```

3. Build and run from Xcode (Cmd+R)

---

## Project Structure Summary

```
Test-multiplatform-mobile/
├── composeApp/                      # Main Kotlin Multiplatform module
│   ├── src/
│   │   ├── commonMain/              # ✅ Shared code (all platforms)
│   │   │   └── kotlin/com/lemurs/lemurs_app/
│   │   │       ├── App.kt           # Main Compose entry point
│   │   │       ├── data/            # Data layer
│   │   │       ├── di/              # Dependency injection
│   │   │       ├── health/          # Health features
│   │   │       ├── survey/          # Survey features
│   │   │       ├── ui/              # UI components
│   │   │       └── util/            # Utilities
│   │   │
│   │   ├── androidMain/             # 🤖 Android-specific code
│   │   │   └── kotlin/com/lemurs/lemurs_app/
│   │   │       ├── MainActivity.kt
│   │   │       ├── Platform.android.kt
│   │   │       └── ...
│   │   │
│   │   ├── iosMain/                 # 🍎 iOS-specific Kotlin code
│   │   │   └── kotlin/com/lemurs/lemurs_app/
│   │   │       ├── MainViewController.kt
│   │   │       ├── Platform.ios.kt
│   │   │       └── ...
│   │   │
│   │   └── commonTest/              # Shared tests
│   │
│   └── build.gradle.kts             # Module build config
│
├── iosApp/                          # 🍎 Native iOS app container
│   ├── iosApp/
│   │   ├── iOSApp.swift             # iOS app entry point
│   │   ├── ContentView.swift        # SwiftUI content view
│   │   ├── Auth/                    # MSAL authentication
│   │   │   ├── MSALAuthHelper.swift
│   │   │   ├── MSALKotlinBridge.swift
│   │   │   └── README.md
│   │   ├── HealthKit/               # HealthKit integration
│   │   │   ├── HealthKitKotlinBridge.swift
│   │   │   └── README.md
│   │   └── Info.plist
│   │
│   ├── Podfile                      # CocoaPods dependencies
│   └── iosApp.xcworkspace           # Xcode workspace (use this!)
│
├── gradle/                          # Gradle configuration
│   └── libs.versions.toml           # Version catalog
│
├── build.gradle.kts                 # Root build config
├── settings.gradle.kts              # Project settings
└── README.md                        # Project overview
```

---
# Creating release APKs for Android
1. Switch to release branch in git:
   ```bash
   git checkout release
   ```
2. Ensure the `upload-keystore.jks` file is present in the `composeApp/` directory. If not, obtain it from the project maintainers.
3. Check that the Android project files use the correct redirect URI for release builds.
4. In Android Studio, go to **Build > Generate Signed Bundle / APK...**
5. Select **APK** and click **Next**.
6. Choose the `upload-keystore.jks` file, enter the keystore password, key alias, and key password. Click **Next**.
7. Select the `release` build type and any desired flavors. Click **Finish**.
8. The signed APK will be generated in the `composeApp/release` directory.
9. Then follow the instructions at the following link to update the deployed version: [Deploy new apk](https://github.com/wpi-lemurs/lemurs-api/blob/main/INSTRUCTION.md#how-to-deploy-new-release-apk-to-server-and-update-download-link)

# Creating release builds for iOS
1. Open the `iosApp/` directory in Xcode.
2. Ensure you are signed into an apple id account with Developer access
3. In the topbar, select Product/Archive
4. Once the build completes, upload to your preferred platform. We use Testflight

## Additional Resources

- [Kotlin Multiplatform Documentation](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)
- [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/)
- [iOS MSAL Authentication](./iosApp/iosApp/Auth/README.md)
- [iOS HealthKit Integration](./iosApp/iosApp/HealthKit/README.md)
