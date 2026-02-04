import Foundation
import BackgroundTasks
import HealthKit
import ComposeApp

/// Background task identifier for health data sync
private let healthSyncTaskIdentifier = "com.lemurs.lemurs_app.healthSync"

/// Swift implementation of background health data scheduling using BGTaskScheduler.
/// This handles periodic background sync of health data from HealthKit.
@objc public class HealthDataTaskScheduler: NSObject {

    @objc public static let shared = HealthDataTaskScheduler()

    private let healthStore: HKHealthStore?

    /// Check if running on simulator
    private var isSimulator: Bool {
        #if targetEnvironment(simulator)
        return true
        #else
        return false
        #endif
    }

    private override init() {
        if HKHealthStore.isHealthDataAvailable() {
            self.healthStore = HKHealthStore()
        } else {
            self.healthStore = nil
        }
        super.init()
    }

    // MARK: - Background Task Registration

    /// Register background tasks with the system.
    /// Call this from application(_:didFinishLaunchingWithOptions:) or app init
    @objc public func registerBackgroundTasks() {
        if isSimulator {
            print("⚠️ BGTaskScheduler not fully supported on Simulator - background tasks will be simulated")
            return
        }

        BGTaskScheduler.shared.register(
            forTaskWithIdentifier: healthSyncTaskIdentifier,
            using: nil
        ) { [weak self] task in
            self?.handleHealthSyncTask(task as! BGAppRefreshTask)
        }

        print("✅ Registered background health sync task: \(healthSyncTaskIdentifier)")
    }

    // MARK: - Task Scheduling

    /// Schedule the next background health sync
    @objc public func scheduleBackgroundHealthSync() {
        if isSimulator {
            print("⚠️ Skipping BGTaskScheduler on Simulator - use performImmediateSync() for testing")
            // On simulator, just log that we would schedule
            return
        }

        let request = BGAppRefreshTaskRequest(identifier: healthSyncTaskIdentifier)

        // Request to run after at least 15 minutes
        // Note: iOS determines the actual execution time based on usage patterns
        request.earliestBeginDate = Date(timeIntervalSinceNow: 15 * 60)

        do {
            try BGTaskScheduler.shared.submit(request)
            print("✅ Scheduled background health sync for ~15 minutes from now")
        } catch BGTaskScheduler.Error.notPermitted {
            print("❌ BGTaskScheduler: Task not permitted - check Info.plist BGTaskSchedulerPermittedIdentifiers")
        } catch BGTaskScheduler.Error.tooManyPendingTaskRequests {
            print("⚠️ BGTaskScheduler: Too many pending requests - task already scheduled")
        } catch BGTaskScheduler.Error.unavailable {
            print("⚠️ BGTaskScheduler: Unavailable (possibly on simulator or background refresh disabled)")
        } catch {
            print("❌ Failed to schedule background health sync: \(error.localizedDescription)")
        }
    }

    /// Cancel all scheduled health sync tasks
    @objc public func cancelScheduledTasks() {
        if isSimulator {
            print("⚠️ Skipping BGTaskScheduler cancel on Simulator")
            return
        }
        BGTaskScheduler.shared.cancel(taskRequestWithIdentifier: healthSyncTaskIdentifier)
        print("🛑 Cancelled scheduled health sync tasks")
    }

    // MARK: - Task Handling

    /// Handle the background health sync task when it's executed by the system
    private func handleHealthSyncTask(_ task: BGAppRefreshTask) {
        print("🔄 Background health sync task started")

        // Schedule the next occurrence
        scheduleBackgroundHealthSync()

        // Set up expiration handler
        task.expirationHandler = {
            print("⚠️ Background health sync task expired")
            task.setTaskCompleted(success: false)
        }

        // Perform the health data sync
        performHealthDataSync { success in
            print(success ? "✅ Background health sync completed successfully" : "❌ Background health sync failed")
            task.setTaskCompleted(success: success)
        }
    }

    // MARK: - Immediate Sync (for testing)

    /// Perform an immediate health data sync (not background)
    @objc public func performImmediateSync() {
        print("🔄 Performing immediate health data sync")
        performHealthDataSync { success in
            print(success ? "✅ Immediate health sync completed" : "❌ Immediate health sync failed")
        }
    }

    // MARK: - Health Data Sync Implementation

    /// Actually perform the health data sync
    private func performHealthDataSync(completion: @escaping (Bool) -> Void) {
        guard let healthStore = healthStore else {
            print("❌ HealthKit not available")
            completion(false)
            return
        }

        // Define the time range (last 24 hours)
        let now = Date()
        let yesterday = Calendar.current.date(byAdding: .day, value: -1, to: now)!

        // Convert to milliseconds for Kotlin
        let startTimeMillis = Int64(yesterday.timeIntervalSince1970 * 1000)
        let endTimeMillis = Int64(now.timeIntervalSince1970 * 1000)

        // Create predicate for the time range
        let predicate = HKQuery.predicateForSamples(
            withStart: yesterday,
            end: now,
            options: .strictStartDate
        )

        // Collect health data
        let dispatchGroup = DispatchGroup()
        var syncSuccess = true

        // Get the Kotlin callback
        let callback = IOSHealthDataCallbackProvider.shared.callback

        // Fetch step count
        if let stepType = HKQuantityType.quantityType(forIdentifier: .stepCount) {
            dispatchGroup.enter()
            fetchStatistics(for: stepType, predicate: predicate, unit: HKUnit.count()) { value in
                if let steps = value {
                    print("📊 Synced steps: \(Int(steps))")
                    // Send to Kotlin/API
                    callback?.onStepsCollected(steps: Int64(steps), startTimeMillis: startTimeMillis, endTimeMillis: endTimeMillis)
                }
                dispatchGroup.leave()
            }
        }

        // Fetch calories burned
        if let calorieType = HKQuantityType.quantityType(forIdentifier: .activeEnergyBurned) {
            dispatchGroup.enter()
            fetchStatistics(for: calorieType, predicate: predicate, unit: HKUnit.kilocalorie()) { value in
                if let calories = value {
                    print("📊 Synced calories: \(Int(calories)) kcal")
                    // Send to Kotlin/API
                    callback?.onCaloriesCollected(calories: calories, startTimeMillis: startTimeMillis, endTimeMillis: endTimeMillis)
                }
                dispatchGroup.leave()
            }
        }

        // Fetch distance
        if let distanceType = HKQuantityType.quantityType(forIdentifier: .distanceWalkingRunning) {
            dispatchGroup.enter()
            fetchStatistics(for: distanceType, predicate: predicate, unit: HKUnit.meter()) { value in
                if let distance = value {
                    print("📊 Synced distance: \(Int(distance)) meters")
                    // Send to Kotlin/API
                    callback?.onDistanceCollected(distanceMeters: distance, startTimeMillis: startTimeMillis, endTimeMillis: endTimeMillis)
                }
                dispatchGroup.leave()
            }
        }

        // Wait for all queries to complete
        dispatchGroup.notify(queue: .main) {
            // Notify Kotlin that sync is complete
            callback?.onSyncComplete(success: syncSuccess)
            completion(syncSuccess)
        }
    }

    /// Fetch cumulative statistics for a quantity type
    private func fetchStatistics(
        for quantityType: HKQuantityType,
        predicate: NSPredicate,
        unit: HKUnit,
        completion: @escaping (Double?) -> Void
    ) {
        guard let healthStore = healthStore else {
            completion(nil)
            return
        }

        let query = HKStatisticsQuery(
            quantityType: quantityType,
            quantitySamplePredicate: predicate,
            options: .cumulativeSum
        ) { _, statistics, error in
            guard error == nil, let sum = statistics?.sumQuantity() else {
                completion(nil)
                return
            }

            let value = sum.doubleValue(for: unit)
            completion(value)
        }

        healthStore.execute(query)
    }
}

// MARK: - Kotlin Bridge Adapter

/// Adapter that implements the Kotlin IOSHealthDataSchedulerBridge interface
public class HealthDataSchedulerBridgeAdapter: IOSHealthDataSchedulerBridge {

    public static let shared = HealthDataSchedulerBridgeAdapter()

    private init() {}

    public func scheduleBackgroundHealthSync() {
        HealthDataTaskScheduler.shared.scheduleBackgroundHealthSync()
    }

    public func performImmediateSync() {
        HealthDataTaskScheduler.shared.performImmediateSync()
    }

    public func cancelScheduledTasks() {
        HealthDataTaskScheduler.shared.cancelScheduledTasks()
    }
}

// MARK: - Registration Function

/// Register the health data scheduler bridge with Kotlin.
/// Call this during app initialization.
public func registerHealthDataSchedulerWithKotlin() {
    // Register background tasks with the system
    HealthDataTaskScheduler.shared.registerBackgroundTasks()

    // Set the Kotlin bridge
    IOSHealthDataSchedulerProvider.shared.bridge = HealthDataSchedulerBridgeAdapter.shared

    // Register the health data callback (Kotlin side)
    // This enables Swift to send collected health data back to Kotlin for API submission
    IOSHealthDataCallbackProvider.shared.register()

    print("✅ Health data scheduler bridge registered with Kotlin")
}
