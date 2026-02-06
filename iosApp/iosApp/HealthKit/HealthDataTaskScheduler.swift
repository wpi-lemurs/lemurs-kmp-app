import Foundation
import BackgroundTasks
import HealthKit
import ComposeApp

/// Background task identifier for health data sync
private let healthSyncTaskIdentifier = "com.lemurs.lemurs_app.healthSync"

/// Swift implementation of background health data scheduling using BGTaskScheduler.
/// This handles periodic background sync of health data from HealthKit.
/// Supports passive background collection even when app is in background or phone is locked.
@objc public class HealthDataTaskScheduler: NSObject {

    @objc public static let shared = HealthDataTaskScheduler()

    private let healthStore: HKHealthStore?

    /// Active observer queries for background delivery
    private var observerQueries: [HKObserverQuery] = []

    /// User defaults key for storing last sync time
    private let lastSyncTimeKey = "com.lemurs.lastHealthSyncTime"

    /// Check if running on simulator
    private var isSimulator: Bool {
        #if targetEnvironment(simulator)
        return true
        #else
        return false
        #endif
    }

    /// The health data types we want to collect passively
    private var healthDataTypes: [HKQuantityType] {
        var types: [HKQuantityType] = []

        // Steps (count)
        if let stepType = HKQuantityType.quantityType(forIdentifier: .stepCount) {
            types.append(stepType)
        }
        // Total Calories Burned (active + basal = total)
        if let calorieType = HKQuantityType.quantityType(forIdentifier: .activeEnergyBurned) {
            types.append(calorieType)
        }
        // Distance (meters)
        if let distanceType = HKQuantityType.quantityType(forIdentifier: .distanceWalkingRunning) {
            types.append(distanceType)
        }
        // Speed (meters/second) - walking speed
        if let speedType = HKQuantityType.quantityType(forIdentifier: .walkingSpeed) {
            types.append(speedType)
        }

        return types
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

    // MARK: - Background Delivery Setup

    /// Check if HealthKit authorization has been requested (not denied).
    /// Note: For read-only access, iOS doesn't tell us if the user actually granted permission
    /// (for privacy reasons). We can only check if the authorization dialog was shown.
    /// The authorizationStatus only works reliably for WRITE (sharing) permissions.
    @objc public func isAuthorizationRequested() -> Bool {
        guard let healthStore = healthStore else {
            return false
        }

        // Check authorization for step count as a representative type
        guard let stepType = HKQuantityType.quantityType(forIdentifier: .stepCount) else {
            return false
        }

        let status = healthStore.authorizationStatus(for: stepType)
        // For read-only access, we just need to ensure authorization was requested
        // .notDetermined means we haven't asked yet
        // .sharingDenied or .sharingAuthorized means the dialog was shown
        // Note: Since we only request READ permissions, status will typically be .notDetermined
        // even after user grants permission. We'll track this ourselves.
        return status != .notDetermined || hasRequestedAuthorization
    }

    /// Flag to track if we've requested authorization
    private static var _hasRequestedAuthorization = false
    private var hasRequestedAuthorization: Bool {
        get { HealthDataTaskScheduler._hasRequestedAuthorization }
        set { HealthDataTaskScheduler._hasRequestedAuthorization = newValue }
    }

    /// Mark that authorization has been requested
    @objc public func markAuthorizationRequested() {
        hasRequestedAuthorization = true
        print("✅ HealthKit authorization marked as requested")
    }

    /// Enable background delivery for all health data types.
    /// This allows HealthKit to wake up the app when new data is available.
    @objc public func enableBackgroundDelivery() {
        guard let healthStore = healthStore else {
            print("❌ HealthKit not available for background delivery")
            return
        }

        for quantityType in healthDataTypes {
            healthStore.enableBackgroundDelivery(for: quantityType, frequency: .immediate) { success, error in
                if let error = error {
                    print("❌ Failed to enable background delivery for \(quantityType.identifier): \(error.localizedDescription)")
                } else if success {
                    print("✅ Background delivery enabled for \(quantityType.identifier)")
                }
            }
        }
    }

    /// Set up observer queries for passive data collection.
    /// These queries will trigger when new health data is written to HealthKit,
    /// even when the app is in the background or the phone is locked.
    /// Note: Call this after authorization has been requested.
    @objc public func setupObserverQueries() {
        guard let healthStore = healthStore else {
            print("❌ HealthKit not available for observer queries")
            return
        }

        // Stop any existing observers
        stopObserverQueries()

        print("🔄 Setting up observer queries for passive data collection...")

        for quantityType in healthDataTypes {
            let query = HKObserverQuery(sampleType: quantityType, predicate: nil) { [weak self] query, completionHandler, error in

                if let error = error {
                    print("❌ Observer query error for \(quantityType.identifier): \(error.localizedDescription)")
                    completionHandler()
                    return
                }

                print("📊 Observer triggered for \(quantityType.identifier) - new data available")

                // Fetch and send the new data
                self?.fetchAndSendData(for: quantityType) {
                    // Must call completion handler to let HealthKit know we're done
                    completionHandler()
                }
            }

            observerQueries.append(query)
            healthStore.execute(query)
            print("✅ Observer query set up for \(quantityType.identifier)")
        }
    }

    /// Stop all observer queries
    @objc public func stopObserverQueries() {
        guard let healthStore = healthStore else { return }

        for query in observerQueries {
            healthStore.stop(query)
        }
        observerQueries.removeAll()
        print("🛑 Stopped all observer queries")
    }

    /// Fetch and send data for a specific quantity type
    private func fetchAndSendData(for quantityType: HKQuantityType, completion: @escaping () -> Void) {
        let now = Date()
        let lastSync = getLastSyncTime(for: quantityType) ?? Calendar.current.date(byAdding: .hour, value: -1, to: now)!

        let predicate = HKQuery.predicateForSamples(
            withStart: lastSync,
            end: now,
            options: .strictStartDate
        )

        let startTimeMillis = Int64(lastSync.timeIntervalSince1970 * 1000)
        let endTimeMillis = Int64(now.timeIntervalSince1970 * 1000)

        let callback = IOSHealthDataCallbackProvider.shared.callback

        switch quantityType.identifier {
        case HKQuantityTypeIdentifier.stepCount.rawValue:
            fetchStatistics(for: quantityType, predicate: predicate, unit: HKUnit.count(), useDiscreteAverage: false) { [weak self] value in
                if let steps = value, steps > 0 {
                    print("📊 Passive sync: \(Int(steps)) steps")
                    callback?.onStepsCollected(steps: Int64(steps), startTimeMillis: startTimeMillis, endTimeMillis: endTimeMillis)
                    self?.saveLastSyncTime(now, for: quantityType)
                } else {
                    print("ℹ️ No step data in time range (this is normal if no walking occurred)")
                }
                completion()
            }

        case HKQuantityTypeIdentifier.activeEnergyBurned.rawValue:
            fetchStatistics(for: quantityType, predicate: predicate, unit: HKUnit.kilocalorie(), useDiscreteAverage: false) { [weak self] value in
                if let calories = value, calories > 0 {
                    print("📊 Passive sync: \(Int(calories)) kcal")
                    callback?.onCaloriesCollected(calories: calories, startTimeMillis: startTimeMillis, endTimeMillis: endTimeMillis)
                    self?.saveLastSyncTime(now, for: quantityType)
                } else {
                    print("ℹ️ No calorie data in time range (this is normal if no activity occurred)")
                }
                completion()
            }

        case HKQuantityTypeIdentifier.distanceWalkingRunning.rawValue:
            fetchStatistics(for: quantityType, predicate: predicate, unit: HKUnit.meter(), useDiscreteAverage: false) { [weak self] value in
                if let distance = value, distance > 0 {
                    print("📊 Passive sync: \(Int(distance)) meters")
                    callback?.onDistanceCollected(distanceMeters: distance, startTimeMillis: startTimeMillis, endTimeMillis: endTimeMillis)
                    self?.saveLastSyncTime(now, for: quantityType)
                } else {
                    print("ℹ️ No distance data in time range (this is normal if no walking occurred)")
                }
                completion()
            }

        case HKQuantityTypeIdentifier.walkingSpeed.rawValue:
            // Speed uses discrete average, not cumulative sum
            let speedUnit = HKUnit.meter().unitDivided(by: HKUnit.second())
            fetchStatistics(for: quantityType, predicate: predicate, unit: speedUnit, useDiscreteAverage: true) { [weak self] value in
                if let speed = value, speed > 0 {
                    print("📊 Passive sync: \(speed) m/s")
                    callback?.onSpeedCollected(speedMetersSecond: speed, startTimeMillis: startTimeMillis, endTimeMillis: endTimeMillis)
                    self?.saveLastSyncTime(now, for: quantityType)
                } else {
                    print("ℹ️ No speed data in time range (this is normal if no walking occurred)")
                }
                completion()
            }

        default:
            completion()
        }
    }

    // MARK: - Last Sync Time Management

    /// Get the last sync time for a specific data type
    private func getLastSyncTime(for quantityType: HKQuantityType) -> Date? {
        let key = "\(lastSyncTimeKey).\(quantityType.identifier)"
        let timeInterval = UserDefaults.standard.double(forKey: key)
        return timeInterval > 0 ? Date(timeIntervalSince1970: timeInterval) : nil
    }

    /// Save the last sync time for a specific data type
    private func saveLastSyncTime(_ date: Date, for quantityType: HKQuantityType) {
        let key = "\(lastSyncTimeKey).\(quantityType.identifier)"
        UserDefaults.standard.set(date.timeIntervalSince1970, forKey: key)
    }

    /// Legacy: Get global last sync time (kept for backward compatibility)
    private func getLastSyncTime() -> Date? {
        let timeInterval = UserDefaults.standard.double(forKey: lastSyncTimeKey)
        return timeInterval > 0 ? Date(timeIntervalSince1970: timeInterval) : nil
    }

    private func saveLastSyncTime(_ date: Date) {
        UserDefaults.standard.set(date.timeIntervalSince1970, forKey: lastSyncTimeKey)
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

        if let speedType = HKQuantityType.quantityType(forIdentifier: .walkingSpeed) {
            dispatchGroup.enter()
            // Walking speed uses discrete average and meters/second unit
            let speedUnit = HKUnit.meter().unitDivided(by: HKUnit.second())
            fetchStatistics(for: speedType, predicate: predicate, unit: speedUnit, useDiscreteAverage: true) { value in
                if let speed = value {
                    print("📊 Synced speed: \(speed) meters/s")
                    // Send to Kotlin/API
                    callback?.onSpeedCollected(speedMetersSecond: speed, startTimeMillis: startTimeMillis, endTimeMillis: endTimeMillis)
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

    /// Fetch statistics for a quantity type
    /// - Parameters:
    ///   - quantityType: The health data type to query
    ///   - predicate: Time range predicate
    ///   - unit: The unit to convert the result to
    ///   - useDiscreteAverage: If true, uses discrete average (for speed); if false, uses cumulative sum (for steps, calories, distance)
    ///   - completion: Callback with the result value or nil
    private func fetchStatistics(
        for quantityType: HKQuantityType,
        predicate: NSPredicate,
        unit: HKUnit,
        useDiscreteAverage: Bool = false,
        completion: @escaping (Double?) -> Void
    ) {
        guard let healthStore = healthStore else {
            completion(nil)
            return
        }

        let options: HKStatisticsOptions = useDiscreteAverage ? .discreteAverage : .cumulativeSum

        let query = HKStatisticsQuery(
            quantityType: quantityType,
            quantitySamplePredicate: predicate,
            options: options
        ) { _, statistics, error in
            guard error == nil else {
                print("❌ Statistics query error: \(error!.localizedDescription)")
                completion(nil)
                return
            }

            let quantity: HKQuantity?
            if useDiscreteAverage {
                quantity = statistics?.averageQuantity()
            } else {
                quantity = statistics?.sumQuantity()
            }

            guard let qty = quantity else {
                completion(nil)
                return
            }

            let value = qty.doubleValue(for: unit)
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

    public func enableBackgroundDelivery() {
        HealthDataTaskScheduler.shared.enableBackgroundDelivery()
    }

    public func setupObserverQueries() {
        HealthDataTaskScheduler.shared.setupObserverQueries()
    }

    public func stopObserverQueries() {
        HealthDataTaskScheduler.shared.stopObserverQueries()
    }
}

// MARK: - Registration Function

/// Register the health data scheduler bridge with Kotlin.
/// Call this during app initialization.
/// Note: Observer queries and background delivery will be set up AFTER authorization is granted
public func registerHealthDataSchedulerWithKotlin() {
    // Register background tasks with the system
    HealthDataTaskScheduler.shared.registerBackgroundTasks()

    // Set the Kotlin bridge
    IOSHealthDataSchedulerProvider.shared.bridge = HealthDataSchedulerBridgeAdapter.shared

    // Register the health data callback (Kotlin side)
    // This enables Swift to send collected health data back to Kotlin for API submission
    IOSHealthDataCallbackProvider.shared.register()

    // NOTE: Background delivery and observer queries will be set up AFTER
    // HealthKit authorization is granted in requestHealthKitPermissionsOnStart()

    print("✅ Health data scheduler bridge registered with Kotlin")
}
