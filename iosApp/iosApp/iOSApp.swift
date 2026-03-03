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

        // Register the Screen Time Swift bridge with Kotlin
        // This also registers background tasks with BGTaskScheduler
        registerScreenTimeSchedulerWithKotlin()

        // Initialize Koin for dependency injection
        MainViewControllerKt.doInitKoin()


        // Initialize SendDataScheduler and register its background tasks
        let sendDataScheduler = ComposeApp.SendDataScheduler()
        sendDataScheduler.registerBackgroundTasks()

        // Request HealthKit permissions on app start
        requestHealthKitPermissionsOnStart()

        // Request Screen Time permissions on app start
        requestScreenTimePermissionsOnStart()

        // FOR TESTING: Trigger immediate screen time collection after a short delay
        // This ensures we collect data right away instead of waiting for background tasks
        #if DEBUG
        func triggerCollectionAndSync() {
            print("🔄 [TESTING] Triggering immediate screen time collection")
            ScreenTimeTaskScheduler.shared.performImmediateCollection()

            // After collection, trigger immediate sync to API
            DispatchQueue.main.asyncAfter(deadline: .now() + 5.0) {
                print("🔄 [TESTING] Triggering immediate screen time API sync")
                let syncScheduler = ComposeApp.SendDataScheduler()
                Task {
                    do {
                        let success = try await syncScheduler.performImmediateSync()
                        if success.boolValue {
                            print("✅ [TESTING] Screen time data synced to API successfully")
                        } else {
                            print("❌ [TESTING] Screen time sync to API failed - possibly no new data")
                        }

                        // Schedule next collection-sync cycle in 2 minutes
                        DispatchQueue.main.asyncAfter(deadline: .now() + 120.0) {
                            print("🔄 [TESTING] Starting next collection-sync cycle")
                            triggerCollectionAndSync()
                        }
                    } catch {
                        print("❌ [TESTING] Screen time sync error: \(error.localizedDescription)")
                    }
                }
            }
        }

        // Start the first collection-sync cycle
        DispatchQueue.main.asyncAfter(deadline: .now() + 5.0) {
            triggerCollectionAndSync()
        }
        #endif

        // Schedule background health data sync
        HealthDataTaskScheduler.shared.scheduleBackgroundHealthSync()

        // Request notification permissions and schedule daily survey notifications
        ComposeApp.NotificationUtil().requestNotificationPermission()
        ComposeApp.NotificationUtil().scheduleDailySurveyNotifications()

        // Schedule background screen time collection
        ScreenTimeTaskScheduler.shared.scheduleBackgroundScreentimeCollection()

        // Schedule background data sync to API (every 15 minutes)
        sendDataScheduler.scheduleScreentime()
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
                // Start monitoring with the extension (only if not already active)
                ScreenTimeTaskScheduler.shared.requestAuthorization { _ in }

                // Only show app selection if:
                // 1. User hasn't completed selection yet, AND
                // 2. Monitoring is NOT already active from previous session
                DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) {
                    let hasCompletedSelection = ScreenTimeTaskScheduler.shared.hasCompletedAppSelection()
                    let isMonitoringActive = UserDefaults.standard.bool(forKey: "deviceactivity_monitoring_started")

                    if !hasCompletedSelection && !isMonitoringActive {
                        print("📱 First time setup - prompting for app selection...")
                        AppSelectionCoordinator.shared.presentAppSelection()
                    } else if hasCompletedSelection {
                        print("ℹ️ App selection already completed - monitoring persists from previous session")
                    }
                }
            }
        } else {
            // Request authorization (will start monitoring on grant)
            ScreenTimeTaskScheduler.shared.requestAuthorization { granted in
                if granted {
                    print("✅ Screen Time authorization granted")

                    // After authorization, prompt for app selection (first time only)
                    if #available(iOS 16.0, *) {
                        DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
                            print("📱 Prompting for app selection after authorization...")
                            AppSelectionCoordinator.shared.presentAppSelection()
                        }
                    }
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