import SwiftUI
import ComposeApp
import MSAL
import HealthKit

@main
struct iOSApp: App {

    init() {
        // Register the MSAL Swift bridge with Kotlin before initializing Koin
        registerMSALBridgeWithKotlin()

        // Register the HealthKit Swift bridge with Kotlin
        registerHealthKitBridgeWithKotlin()

        // Register the Health Data Scheduler bridge with Kotlin
        // This also registers background tasks with BGTaskScheduler
        registerHealthDataSchedulerWithKotlin()

        // Register the Bluetooth Swift bridge with Kotlin
        // This also registers background tasks with BGTaskScheduler
        registerBluetoothBridgeWithKotlin()
        registerBluetoothSchedulerWithKotlin()

        // Initialize Koin for dependency injection
        MainViewControllerKt.doInitKoin()

        // Register the Screen Time Scheduler bridge with Kotlin
        // Must be AFTER Koin initialization since ScreentimeWorker uses Koin
        registerScreenTimeSchedulerWithKotlin()

        // Request HealthKit permissions on app start
        requestHealthKitPermissionsOnStart()

        // Request Screen Time permissions on app start
        requestScreenTimePermissionsOnStart()

        // Schedule background health data sync
        HealthDataTaskScheduler.shared.scheduleBackgroundHealthSync()

        // Request notification permissions and schedule daily survey notifications
        ComposeApp.NotificationUtil().requestNotificationPermission()
        ComposeApp.NotificationUtil().scheduleDailySurveyNotifications()

        // Schedule background screen time collection
        ScreenTimeTaskScheduler.shared.scheduleBackgroundScreentimeCollection()
    }

    /// Request HealthKit permissions when the app starts
    private func requestHealthKitPermissionsOnStart() {
        // Check if HealthKit is available
        guard HKHealthStore.isHealthDataAvailable() else {
            print("⚠️ HealthKit is not available on this device")
            return
        }

        let healthStore = HKHealthStore()

        // Define the types we want to read
        var typesToRead = Set<HKObjectType>()

        if let stepCount = HKQuantityType.quantityType(forIdentifier: .stepCount) {
            typesToRead.insert(stepCount)
        }
        if let activeEnergy = HKQuantityType.quantityType(forIdentifier: .activeEnergyBurned) {
            typesToRead.insert(activeEnergy)
        }
        if let basalEnergy = HKQuantityType.quantityType(forIdentifier: .basalEnergyBurned) {
            typesToRead.insert(basalEnergy)
        }
        if let distance = HKQuantityType.quantityType(forIdentifier: .distanceWalkingRunning) {
            typesToRead.insert(distance)
        }
        if let heartRate = HKQuantityType.quantityType(forIdentifier: .heartRate) {
            typesToRead.insert(heartRate)
        }
        if let sleep = HKCategoryType.categoryType(forIdentifier: .sleepAnalysis) {
            typesToRead.insert(sleep)
        }
        // Add walking speed for passive background collection
        if let walkingSpeed = HKQuantityType.quantityType(forIdentifier: .walkingSpeed) {
            typesToRead.insert(walkingSpeed)
        }

        // Request authorization - this will show the permissions dialog
        healthStore.requestAuthorization(toShare: nil, read: typesToRead) { success, error in
            DispatchQueue.main.async {
                if let error = error {
                    print("❌ HealthKit authorization error: \(error.localizedDescription)")
                } else if success {
                    print("✅ HealthKit authorization dialog was presented")
                    // Mark that authorization was requested (for read-only, we can't check if granted)
                    HealthDataTaskScheduler.shared.markAuthorizationRequested()
                    // After authorization, enable background delivery and set up observer queries
                    HealthDataTaskScheduler.shared.enableBackgroundDelivery()
                    HealthDataTaskScheduler.shared.setupObserverQueries()
                    print("✅ Background delivery and observer queries enabled after authorization")
                } else {
                    print("⚠️ HealthKit authorization was not successful")
                }
            }
        }
    }

    /// Request Screen Time permissions when the app starts (iOS 15+)
    private func requestScreenTimePermissionsOnStart() {
        // Check if running on simulator
        #if targetEnvironment(simulator)
        print("⚠️ Screen Time API not supported on Simulator - skipping authorization")
        print("ℹ️  Screen Time requires a physical device for testing")
        return
        #endif

        guard #available(iOS 15.0, *) else {
            print("⚠️ Screen Time API requires iOS 15.0+")
            return
        }

        // Check if already authorized and start monitoring
        if ScreenTimeTaskScheduler.shared.isAuthorizationGranted() {
            print("✅ Screen Time already authorized")
            if #available(iOS 16.0, *) {
                // Start monitoring with the extension
                ScreenTimeTaskScheduler.shared.requestAuthorization { _ in }
            }
        } else {
            // Request authorization (will start monitoring on grant)
            ScreenTimeTaskScheduler.shared.requestAuthorization { granted in
                if granted {
                    print("✅ Screen Time authorization granted")
                } else {
                    print("⚠️ Screen Time authorization denied or cancelled")
                }
            }
        }
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in
                    // Handle MSAL authentication redirects
                    MSALPublicClientApplication.handleMSALResponse(
                        url,
                        sourceApplication: nil
                    )
                }
        }
    }
}