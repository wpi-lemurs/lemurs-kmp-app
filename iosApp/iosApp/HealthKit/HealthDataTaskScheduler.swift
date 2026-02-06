import Foundation
import BackgroundTasks
import HealthKit
import ComposeApp

/// Background task identifier for health data sync
private let healthSyncTaskIdentifier = "com.lemurs.lemurs_app.healthSync"

/// UserDefaults keys for storing anchors
private let anchorKeyPrefix = "com.lemurs.healthAnchor."

/// Swift implementation of background health data scheduling using BGTaskScheduler.
/// This handles periodic background sync of health data from HealthKit.
/// Supports passive background collection even when app is in background or phone is locked.
///
/// Uses HKAnchoredObjectQuery for incremental, granular data collection:
/// - No duplicates
/// - No missing samples
/// - Exact sample-level granularity
/// - Automatic incremental sync via anchors
@objc public class HealthDataTaskScheduler: NSObject {

    @objc public static let shared = HealthDataTaskScheduler()

    private let healthStore: HKHealthStore?

    /// Active observer queries for background delivery
    private var observerQueries: [HKObserverQuery] = []

    /// Stored anchors for each data type (for incremental sync)
    private var anchors: [String: HKQueryAnchor] = [:]

    /// User defaults key for storing last sync time (legacy, kept for compatibility)
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

    // MARK: - Anchored Object Query (Granular, Incremental Sync)

    /// Maximum samples to process per query to prevent memory issues
    private let maxSamplesPerQuery = 100

    /// Maximum samples to log individually (to prevent console spam)
    private let maxSamplesToLog = 5

    /// Fetch and send data for a specific quantity type using HKAnchoredObjectQuery.
    /// This provides:
    /// - Exact sample-level granularity (each walking burst, each burn segment, etc.)
    /// - No duplicates (anchor tracks what's been processed)
    /// - No missing samples (anchor ensures nothing is skipped)
    /// - Automatic incremental sync
    private func fetchAndSendData(for quantityType: HKQuantityType, completion: @escaping () -> Void) {
        guard let healthStore = healthStore else {
            print("❌ HealthKit not available")
            completion()
            return
        }

        // Get the stored anchor for this data type
        let anchor = getAnchor(for: quantityType)

        // If no anchor exists (first sync), limit to last 24 hours to prevent memory overload
        var predicate: NSPredicate? = nil
        if anchor == nil {
            let now = Date()
            let oneDayAgo = Calendar.current.date(byAdding: .day, value: -1, to: now)!
            predicate = HKQuery.predicateForSamples(withStart: oneDayAgo, end: now, options: [])
            print("ℹ️ First sync for \(quantityType.identifier) - limiting to last 24 hours")
        }

        let callback = IOSHealthDataCallbackProvider.shared.callback

        // Create anchored query with limit to prevent memory issues
        let query = HKAnchoredObjectQuery(
            type: quantityType,
            predicate: predicate,
            anchor: anchor,
            limit: maxSamplesPerQuery
        ) { [weak self] query, samples, deletedObjects, newAnchor, error in

            if let error = error {
                print("❌ Anchored query error for \(quantityType.identifier): \(error.localizedDescription)")
                completion()
                return
            }

            // Save the new anchor for next incremental sync
            if let newAnchor = newAnchor {
                self?.saveAnchor(newAnchor, for: quantityType)
            }

            // Process the samples
            guard let quantitySamples = samples as? [HKQuantitySample], !quantitySamples.isEmpty else {
                print("ℹ️ No new \(quantityType.identifier) samples since last sync")
                completion()
                return
            }

            print("📊 Processing \(quantitySamples.count) \(quantityType.identifier) samples")

            // Process samples in batches to avoid memory pressure
            self?.processSamplesBatched(quantitySamples, for: quantityType, callback: callback)

            completion()
        }

        healthStore.execute(query)
    }

    /// Process samples in batches and aggregate to reduce API calls
    private func processSamplesBatched(_ samples: [HKQuantitySample], for quantityType: HKQuantityType, callback: IOSHealthDataCallback?) {
        guard !samples.isEmpty else { return }

        // Aggregate samples by summing values and using min/max timestamps
        var totalValue: Double = 0
        var minStartDate = samples[0].startDate
        var maxEndDate = samples[0].endDate

        for sample in samples {
            if sample.startDate < minStartDate { minStartDate = sample.startDate }
            if sample.endDate > maxEndDate { maxEndDate = sample.endDate }

            switch quantityType.identifier {
            case HKQuantityTypeIdentifier.stepCount.rawValue:
                totalValue += sample.quantity.doubleValue(for: HKUnit.count())
            case HKQuantityTypeIdentifier.activeEnergyBurned.rawValue:
                totalValue += sample.quantity.doubleValue(for: HKUnit.kilocalorie())
            case HKQuantityTypeIdentifier.distanceWalkingRunning.rawValue:
                totalValue += sample.quantity.doubleValue(for: HKUnit.meter())
            case HKQuantityTypeIdentifier.walkingSpeed.rawValue:
                // For speed, use average instead of sum
                let speedUnit = HKUnit.meter().unitDivided(by: HKUnit.second())
                totalValue += sample.quantity.doubleValue(for: speedUnit)
            default:
                break
            }
        }

        // For speed, calculate average
        if quantityType.identifier == HKQuantityTypeIdentifier.walkingSpeed.rawValue {
            totalValue = totalValue / Double(samples.count)
        }

        let startTimeMillis = Int64(minStartDate.timeIntervalSince1970 * 1000)
        let endTimeMillis = Int64(maxEndDate.timeIntervalSince1970 * 1000)

        // Send aggregated data
        switch quantityType.identifier {
        case HKQuantityTypeIdentifier.stepCount.rawValue:
            print("  📊 Total Steps: \(Int(totalValue)) | \(formatDate(minStartDate)) → \(formatDate(maxEndDate)) (\(samples.count) samples)")
            callback?.onStepsCollected(steps: Int64(totalValue), startTimeMillis: startTimeMillis, endTimeMillis: endTimeMillis)

        case HKQuantityTypeIdentifier.activeEnergyBurned.rawValue:
            print("  📊 Total Calories: \(Int(totalValue)) kcal | \(formatDate(minStartDate)) → \(formatDate(maxEndDate)) (\(samples.count) samples)")
            callback?.onCaloriesCollected(calories: totalValue, startTimeMillis: startTimeMillis, endTimeMillis: endTimeMillis)

        case HKQuantityTypeIdentifier.distanceWalkingRunning.rawValue:
            print("  📊 Total Distance: \(Int(totalValue)) m | \(formatDate(minStartDate)) → \(formatDate(maxEndDate)) (\(samples.count) samples)")
            callback?.onDistanceCollected(distanceMeters: totalValue, startTimeMillis: startTimeMillis, endTimeMillis: endTimeMillis)

        case HKQuantityTypeIdentifier.walkingSpeed.rawValue:
            print("  📊 Avg Speed: \(String(format: "%.2f", totalValue)) m/s | \(formatDate(minStartDate)) → \(formatDate(maxEndDate)) (\(samples.count) samples)")
            callback?.onSpeedCollected(speedMetersSecond: totalValue, startTimeMillis: startTimeMillis, endTimeMillis: endTimeMillis)

        default:
            break
        }
    }

    /// Process individual samples and send to Kotlin callback (kept for granular mode if needed)
    private func processSamples(_ samples: [HKQuantitySample], for quantityType: HKQuantityType, callback: IOSHealthDataCallback?) {
        let samplesToLog = min(samples.count, maxSamplesToLog)

        for (index, sample) in samples.enumerated() {
            let startTimeMillis = Int64(sample.startDate.timeIntervalSince1970 * 1000)
            let endTimeMillis = Int64(sample.endDate.timeIntervalSince1970 * 1000)

            let shouldLog = index < samplesToLog

            switch quantityType.identifier {
            case HKQuantityTypeIdentifier.stepCount.rawValue:
                let steps = sample.quantity.doubleValue(for: HKUnit.count())
                if shouldLog { print("  📍 Steps: \(Int(steps)) | \(formatDate(sample.startDate)) → \(formatDate(sample.endDate))") }
                callback?.onStepsCollected(steps: Int64(steps), startTimeMillis: startTimeMillis, endTimeMillis: endTimeMillis)

            case HKQuantityTypeIdentifier.activeEnergyBurned.rawValue:
                let calories = sample.quantity.doubleValue(for: HKUnit.kilocalorie())
                if shouldLog { print("  📍 Calories: \(Int(calories)) kcal | \(formatDate(sample.startDate)) → \(formatDate(sample.endDate))") }
                callback?.onCaloriesCollected(calories: calories, startTimeMillis: startTimeMillis, endTimeMillis: endTimeMillis)

            case HKQuantityTypeIdentifier.distanceWalkingRunning.rawValue:
                let distance = sample.quantity.doubleValue(for: HKUnit.meter())
                if shouldLog { print("  📍 Distance: \(Int(distance)) m | \(formatDate(sample.startDate)) → \(formatDate(sample.endDate))") }
                callback?.onDistanceCollected(distanceMeters: distance, startTimeMillis: startTimeMillis, endTimeMillis: endTimeMillis)

            case HKQuantityTypeIdentifier.walkingSpeed.rawValue:
                let speedUnit = HKUnit.meter().unitDivided(by: HKUnit.second())
                let speed = sample.quantity.doubleValue(for: speedUnit)
                if shouldLog { print("  📍 Speed: \(String(format: "%.2f", speed)) m/s | \(formatDate(sample.startDate)) → \(formatDate(sample.endDate))") }
                callback?.onSpeedCollected(speedMetersSecond: speed, startTimeMillis: startTimeMillis, endTimeMillis: endTimeMillis)

            default:
                break
            }
        }

        if samples.count > samplesToLog {
            print("  ... and \(samples.count - samplesToLog) more samples")
        }
    }

    /// Format date for logging
    private func formatDate(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "HH:mm:ss"
        return formatter.string(from: date)
    }

    // MARK: - Anchor Management

    /// Get the stored anchor for a specific data type
    private func getAnchor(for quantityType: HKQuantityType) -> HKQueryAnchor? {
        // Check in-memory cache first
        if let anchor = anchors[quantityType.identifier] {
            return anchor
        }

        // Load from UserDefaults
        let key = "\(anchorKeyPrefix)\(quantityType.identifier)"
        if let data = UserDefaults.standard.data(forKey: key) {
            do {
                let anchor = try NSKeyedUnarchiver.unarchivedObject(ofClass: HKQueryAnchor.self, from: data)
                if let anchor = anchor {
                    anchors[quantityType.identifier] = anchor
                }
                return anchor
            } catch {
                print("⚠️ Failed to load anchor for \(quantityType.identifier): \(error)")
                return nil
            }
        }

        return nil
    }

    /// Save the anchor for a specific data type
    private func saveAnchor(_ anchor: HKQueryAnchor, for quantityType: HKQuantityType) {
        // Save to in-memory cache
        anchors[quantityType.identifier] = anchor

        // Persist to UserDefaults
        let key = "\(anchorKeyPrefix)\(quantityType.identifier)"
        do {
            let data = try NSKeyedArchiver.archivedData(withRootObject: anchor, requiringSecureCoding: true)
            UserDefaults.standard.set(data, forKey: key)
        } catch {
            print("⚠️ Failed to save anchor for \(quantityType.identifier): \(error)")
        }
    }

    /// Clear all stored anchors (useful for resetting sync state)
    @objc public func clearAllAnchors() {
        anchors.removeAll()
        for type in healthDataTypes {
            let key = "\(anchorKeyPrefix)\(type.identifier)"
            UserDefaults.standard.removeObject(forKey: key)
        }
        print("🗑️ Cleared all health data anchors")
    }

    // MARK: - Legacy Last Sync Time Management (kept for compatibility)

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

    /// Actually perform the health data sync using HKAnchoredObjectQuery.
    /// This provides granular, incremental sync with no duplicates.
    private func performHealthDataSync(completion: @escaping (Bool) -> Void) {
        guard let healthStore = healthStore else {
            print("❌ HealthKit not available")
            completion(false)
            return
        }

        print("🔄 Starting anchored health data sync for all types...")

        let dispatchGroup = DispatchGroup()
        var syncSuccess = true
        let callback = IOSHealthDataCallbackProvider.shared.callback

        // Sync each health data type using anchored queries
        for quantityType in healthDataTypes {
            dispatchGroup.enter()

            let anchor = getAnchor(for: quantityType)

            // If no anchor exists (first sync), limit to last 24 hours
            var predicate: NSPredicate? = nil
            if anchor == nil {
                let now = Date()
                let oneDayAgo = Calendar.current.date(byAdding: .day, value: -1, to: now)!
                predicate = HKQuery.predicateForSamples(withStart: oneDayAgo, end: now, options: [])
            }

            let query = HKAnchoredObjectQuery(
                type: quantityType,
                predicate: predicate,
                anchor: anchor,
                limit: maxSamplesPerQuery
            ) { [weak self] query, samples, deletedObjects, newAnchor, error in

                defer { dispatchGroup.leave() }

                if let error = error {
                    print("❌ Anchored sync error for \(quantityType.identifier): \(error.localizedDescription)")
                    return
                }

                // Save the new anchor
                if let newAnchor = newAnchor {
                    self?.saveAnchor(newAnchor, for: quantityType)
                }

                // Process samples
                guard let quantitySamples = samples as? [HKQuantitySample], !quantitySamples.isEmpty else {
                    print("ℹ️ No new \(quantityType.identifier) samples")
                    return
                }

                print("📊 Syncing \(quantitySamples.count) \(quantityType.identifier) samples")
                self?.processSamplesBatched(quantitySamples, for: quantityType, callback: callback)
            }

            healthStore.execute(query)
        }

        // Wait for all queries to complete
        dispatchGroup.notify(queue: .main) {
            print("✅ Anchored health data sync complete")
            callback?.onSyncComplete(success: syncSuccess)
            completion(syncSuccess)
        }
    }

    // MARK: - Legacy Statistics Query (kept for compatibility)

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

    public func clearAllAnchors() {
        HealthDataTaskScheduler.shared.clearAllAnchors()
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
