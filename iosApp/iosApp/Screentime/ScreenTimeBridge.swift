//
// Created by Murphy, Jacob on 2/11/26.
//

import Foundation
import BackgroundTasks
import ComposeApp

// FamilyControls and DeviceActivity are only available with paid Apple Developer accounts
// with proper entitlements. We conditionally import them if available.
#if canImport(FamilyControls)
import FamilyControls
#endif

#if canImport(DeviceActivity)
import DeviceActivity
#endif

/// Background task identifier for screen time collection
private let screentimeTaskIdentifier = "com.lemurs.lemurs_app.screentimeCollection"

/// Swift implementation of background screen time scheduling using BGTaskScheduler.
/// This handles periodic background sync of screen time data.
/// Follows the same pattern as HealthDataTaskScheduler for consistency.
///
/// Note: iOS Screen Time API (Family Controls/DeviceActivity) has strict privacy controls.
/// Users must explicitly grant permission, and the API is limited compared to Android.
@objc public class ScreenTimeTaskScheduler: NSObject {

    @objc public static let shared = ScreenTimeTaskScheduler()

    /// Lazy initialization to avoid Koin initialization order issues.
    /// ScreentimeWorker uses Koin dependency injection, which must be initialized first.
    private lazy var screentimeWorker = ScreentimeWorker()

    /// Check if Family Controls is available (requires paid Apple Developer account)
    private var isFamilyControlsAvailable: Bool {
        #if canImport(FamilyControls)
        return true
        #else
        return false
        #endif
    }

    /// Check if running on simulator
    private var isSimulator: Bool {
        #if targetEnvironment(simulator)
        return true
        #else
        return false
        #endif
    }

    private override init() {
        super.init()
        if !isFamilyControlsAvailable {
            print("⚠️ Family Controls framework not available")
            print("ℹ️  Screen Time functionality requires:")
            print("   • Paid Apple Developer account")
            print("   • Family Controls capability enabled in Xcode")
            print("   • Proper provisioning profile")
            print("ℹ️  Screen Time features will be disabled")
        }
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
            forTaskWithIdentifier: screentimeTaskIdentifier,
            using: nil
        ) { [weak self] task in
            self?.handleScreentimeTask(task as! BGAppRefreshTask)
        }

        print("✅ Registered background screen time task: \(screentimeTaskIdentifier)")
    }

    // MARK: - Permissions

    /// Check if Screen Time authorization is granted (iOS 15+).
    @available(iOS 15.0, *)
    @objc public func isAuthorizationGranted() -> Bool {
        guard isFamilyControlsAvailable else {
            print("ℹ️ Screen Time authorization check skipped - Family Controls not available")
            return false
        }

        if isSimulator {
            print("ℹ️ Screen Time authorization check skipped on Simulator")
            return false
        }

        #if canImport(FamilyControls)
        let status = AuthorizationCenter.shared.authorizationStatus
        let isAuthorized = status == .approved
        print("ℹ️ Screen Time authorization status: \(status.rawValue), authorized: \(isAuthorized)")
        return isAuthorized
        #else
        return false
        #endif
    }

    /// Request Screen Time authorization from the user (iOS 15+).
    /// This presents a system dialog requiring user approval.
    @available(iOS 15.0, *)
    @objc public func requestAuthorization(completion: @escaping (Bool) -> Void) {
        guard isFamilyControlsAvailable else {
            print("⚠️ Screen Time authorization not available - Family Controls framework missing")
            print("ℹ️  Requires paid Apple Developer account with Family Controls capability")
            completion(false)
            return
        }

        if isSimulator {
            print("⚠️ Screen Time authorization not available on Simulator")
            print("ℹ️  Family Controls requires a physical iOS device")
            completion(false)
            return
        }

        #if canImport(FamilyControls)
        let center = AuthorizationCenter.shared

        Task {
            do {
                try await center.requestAuthorization(for: .individual)
                print("✅ Screen Time authorization granted")

                // Start monitoring after authorization
                if #available(iOS 16.0, *) {
                    await MainActor.run {
                        self.startDeviceActivityMonitoring()
                    }
                }

                completion(true)
            } catch let error as NSError {
                // Check for sandbox restriction error (code 4099 with error 159)
                if error.domain == NSCocoaErrorDomain && error.code == 4099 {
                    print("❌ Screen Time authorization failed: Sandbox restriction")
                    print("ℹ️  This requires:")
                    print("   1. Family Controls capability in Xcode project")
                    print("   2. Proper provisioning profile with Family Controls entitlement")
                    print("   3. App signed with development or distribution certificate")
                    print("ℹ️  To enable in Xcode:")
                    print("   • Open project in Xcode")
                    print("   • Select target > Signing & Capabilities")
                    print("   • Click + Capability > Family Controls")
                    print("   • Ensure proper provisioning profile is selected")
                } else {
                    print("❌ Screen Time authorization denied: \(error.localizedDescription)")
                }
                completion(false)
            }
        }
        #else
        completion(false)
        #endif
    }

    // MARK: - DeviceActivity Monitoring

    /// Start DeviceActivity monitoring with the extension
    @available(iOS 16.0, *)
    private func startDeviceActivityMonitoring() {
        guard isFamilyControlsAvailable else {
            print("❌ Cannot start monitoring - Family Controls not available")
            return
        }

        #if canImport(DeviceActivity)
        let center = DeviceActivityCenter()

        // Create daily monitoring schedule
        let schedule = DeviceActivitySchedule(
            intervalStart: DateComponents(hour: 0, minute: 0),
            intervalEnd: DateComponents(hour: 23, minute: 59),
            repeats: true
        )

        // Create threshold events
        let events: [DeviceActivityEvent.Name: DeviceActivityEvent] = [
            .init("usage_15min"): DeviceActivityEvent(
                threshold: DateComponents(minute: 15)
            ),
            .init("usage_30min"): DeviceActivityEvent(
                threshold: DateComponents(minute: 30)
            ),
            .init("usage_60min"): DeviceActivityEvent(
                threshold: DateComponents(minute: 60)
            )
        ]

        let activityName = DeviceActivityName("daily_monitoring")

        do {
            try center.startMonitoring(activityName, during: schedule, events: events)
            print("✅ Started DeviceActivity monitoring with extension")
            print("ℹ️  Extension will log threshold events to shared container")
        } catch {
            print("❌ Failed to start monitoring: \(error)")
        }
        #endif
    }

    /// Stop DeviceActivity monitoring
    @available(iOS 16.0, *)
    @objc public func stopMonitoring() {
        #if canImport(DeviceActivity)
        let center = DeviceActivityCenter()
        let activityName = DeviceActivityName("daily_monitoring")
        center.stopMonitoring([activityName])
        print("🛑 Stopped DeviceActivity monitoring")
        #endif
    }

    // MARK: - Task Scheduling

    /// Schedule the next background screen time collection
    @objc public func scheduleBackgroundScreentimeCollection() {
        if isSimulator {
            print("⚠️ Skipping BGTaskScheduler on Simulator - use performImmediateCollection() for testing")
            return
        }

        let request = BGAppRefreshTaskRequest(identifier: screentimeTaskIdentifier)

        // FOR TESTING: Run after 2 minutes (change back to 15 for production)
        request.earliestBeginDate = Date(timeIntervalSinceNow: 2 * 60)

        do {
            try BGTaskScheduler.shared.submit(request)
            print("✅ Scheduled background screen time collection for ~2 minutes from now (TESTING)")
        } catch BGTaskScheduler.Error.notPermitted {
            print("❌ BGTaskScheduler: Task not permitted - check Info.plist BGTaskSchedulerPermittedIdentifiers")
        } catch BGTaskScheduler.Error.tooManyPendingTaskRequests {
            print("⚠️ BGTaskScheduler: Too many pending requests - task already scheduled")
        } catch BGTaskScheduler.Error.unavailable {
            print("⚠️ BGTaskScheduler: Unavailable (possibly on simulator or background refresh disabled)")
        } catch {
            print("❌ Failed to schedule background screen time collection: \(error.localizedDescription)")
        }
    }

    /// Cancel all scheduled screen time collection tasks
    @objc public func cancelScheduledTasks() {
        if isSimulator {
            print("⚠️ Skipping BGTaskScheduler cancel on Simulator")
            return
        }
        BGTaskScheduler.shared.cancel(taskRequestWithIdentifier: screentimeTaskIdentifier)
        print("🛑 Cancelled scheduled screen time collection tasks")
    }

    // MARK: - Task Handling

    /// Handle the background screen time collection task when it's executed by the system
    private func handleScreentimeTask(_ task: BGAppRefreshTask) {
        print("🔄 Background screen time collection task started")

        // Schedule the next occurrence
        scheduleBackgroundScreentimeCollection()

        // Set up expiration handler
        task.expirationHandler = {
            print("⚠️ Background screen time collection task expired")
            task.setTaskCompleted(success: false)
        }

        // Perform the screen time data collection
        performScreentimeCollection { success in
            print(success ? "✅ Background screen time collection completed successfully" : "❌ Background screen time collection failed")
            task.setTaskCompleted(success: success)
        }
    }

    // MARK: - Immediate Collection (for testing)

    /// Perform an immediate screen time data collection (not background)
    @objc public func performImmediateCollection() {
        print("🔄 Performing immediate screen time collection")
        performScreentimeCollection { success in
            print(success ? "✅ Immediate screen time collection completed" : "❌ Immediate screen time collection failed")
        }
    }

    // MARK: - Screen Time Collection Implementation

    /// Actually perform the screen time data collection.
    /// This calls into the Kotlin ScreentimeWorker which handles the business logic.
    private func performScreentimeCollection(completion: @escaping (Bool) -> Void) {
        print("🔄 Starting screen time data collection...")

        // Check authorization (iOS 15+)
        if #available(iOS 15.0, *) {
            guard isAuthorizationGranted() else {
                print("❌ Screen Time authorization not granted - skipping collection")
                completion(false)
                return
            }
        } else {
            print("⚠️ Screen Time API requires iOS 15.0+ - skipping collection")
            completion(false)
            return
        }

        // Call the Kotlin worker to perform the actual work
        screentimeWorker.executeWork { success in
            let successBool = success as! Bool
            print(successBool ? "✅ Screen time collection work completed" : "❌ Screen time collection work failed")
            completion(successBool)
        }
    }
}

// MARK: - Bridge Adapter for Kotlin Integration

/// Adapter class to bridge between Kotlin's ScreentimeScheduler and Swift's ScreenTimeTaskScheduler.
/// This follows the same pattern as HealthDataSchedulerBridgeAdapter.
@objc public class ScreenTimeSchedulerBridgeAdapter: NSObject {

    @objc public static let shared = ScreenTimeSchedulerBridgeAdapter()

    private override init() {
        super.init()
    }

    /// Schedule periodic screen time collection (every 15 minutes)
    @objc public func schedule() {
        print("📅 Kotlin requested to schedule screen time collection")
        ScreenTimeTaskScheduler.shared.scheduleBackgroundScreentimeCollection()
    }

    /// Schedule quick screen time collection for testing
    @objc public func scheduleQuick() {
        print("📅 Kotlin requested quick screen time collection (immediate)")
        ScreenTimeTaskScheduler.shared.performImmediateCollection()
    }

    /// Cancel all scheduled tasks
    @objc public func cancelAll() {
        print("🛑 Kotlin requested to cancel all screen time tasks")
        ScreenTimeTaskScheduler.shared.cancelScheduledTasks()
    }

    /// Request Screen Time authorization
    @available(iOS 15.0, *)
    @objc public func requestAuthorization(completion: @escaping (Bool) -> Void) {
        print("🔐 Kotlin requested Screen Time authorization")
        ScreenTimeTaskScheduler.shared.requestAuthorization(completion: completion)
    }

    /// Check if authorization is granted
    @available(iOS 15.0, *)
    @objc public func isAuthorized() -> Bool {
        return ScreenTimeTaskScheduler.shared.isAuthorizationGranted()
    }
}

// MARK: - Kotlin Bridge Setup

/// Extension to set the Swift bridge in Kotlin's IOSScreenTimeSchedulerProvider
extension ScreenTimeSchedulerBridgeAdapter: IOSScreenTimeSchedulerBridge {
    // Methods already implemented above - this just conforms to the protocol
}

// MARK: - Screen Time Data Collection Bridge

/// Adapter class to provide screen time usage data to Kotlin.
/// This handles the actual data collection from iOS Screen Time APIs.
@objc public class ScreenTimeDataBridgeAdapter: NSObject {

    @objc public static let shared = ScreenTimeDataBridgeAdapter()

    private override init() {
        super.init()
    }

    /// Get usage statistics for the specified time range.
    /// Reads data collected by the DeviceActivityMonitor extension from shared container.
    @objc public func getUsageStats(startTimeMillis: Int64, endTimeMillis: Int64) -> [Screentime] {
        print("📊 Kotlin requested screen time data from \(startTimeMillis) to \(endTimeMillis)")

        guard #available(iOS 15.0, *) else {
            print("⚠️ Screen Time API requires iOS 15.0+")
            return []
        }

        // Check authorization
        guard isAuthorized() else {
            print("❌ Screen Time authorization not granted")
            return []
        }

        // Try to read events from shared container written by extension
        guard let containerURL = FileManager.default.containerURL(
            forSecurityApplicationGroupIdentifier: "group.com.lemurs.lemurs-app"
        ) else {
            print("❌ Failed to access shared container")
            return getFallbackUsageData(startTimeMillis: startTimeMillis, endTimeMillis: endTimeMillis)
        }

        let fileURL = containerURL.appendingPathComponent("screentime_events.json")

        guard FileManager.default.fileExists(atPath: fileURL.path) else {
            print("ℹ️  No screen time events file found yet")
            print("ℹ️  DeviceActivityMonitor extension hasn't logged any events")
            print("ℹ️  Extension triggers at: interval boundaries (midnight) or thresholds (15/30/60 min)")
            print("🔄 Using fallback: tracking own app usage via lifecycle")
            return getFallbackUsageData(startTimeMillis: startTimeMillis, endTimeMillis: endTimeMillis)
        }

        do {
            let data = try Data(contentsOf: fileURL)
            guard let events = try JSONSerialization.jsonObject(with: data) as? [[String: Any]] else {
                print("⚠️ Invalid events format")
                return getFallbackUsageData(startTimeMillis: startTimeMillis, endTimeMillis: endTimeMillis)
            }

            print("📊 Found \(events.count) total events in extension storage")

            // Filter events by time range
            let startTime = Double(startTimeMillis) / 1000.0
            let endTime = Double(endTimeMillis) / 1000.0

            let filteredEvents = events.filter { event in
                guard let timestamp = event["timestamp"] as? Double else { return false }
                return timestamp >= startTime && timestamp <= endTime
            }

            print("📊 Found \(filteredEvents.count) events in requested time range")

            if filteredEvents.isEmpty {
                print("ℹ️  No extension events in time range, using fallback")
                return getFallbackUsageData(startTimeMillis: startTimeMillis, endTimeMillis: endTimeMillis)
            }

            // Convert events to Screentime objects
            return convertEventsToScreentime(filteredEvents, startTime: startTime, endTime: endTime)

        } catch {
            print("❌ Failed to read events: \(error)")
            return getFallbackUsageData(startTimeMillis: startTimeMillis, endTimeMillis: endTimeMillis)
        }
    }

    /// Fallback to own app usage tracking when extension data is not available
    private func getFallbackUsageData(startTimeMillis: Int64, endTimeMillis: Int64) -> [Screentime] {
        let tracker = ScreenTimeTracker.shared
        let ownAppData = tracker.getOwnAppUsage(
            startTimeMillis: startTimeMillis,
            endTimeMillis: endTimeMillis
        )

        if !ownAppData.isEmpty {
            print("✅ Using own app usage data: \(ownAppData.count) entries")
        } else {
            print("ℹ️  No usage data available (user hasn't used app in this time range)")
        }

        return ownAppData
    }

    private func convertEventsToScreentime(_ events: [[String: Any]], startTime: Double, endTime: Double) -> [Screentime] {
        var result: [Screentime] = []

        // Group events by activity
        var activityDurations: [String: Double] = [:]
        var activityLastSeen: [String: Double] = [:]

        for event in events {
            guard let type = event["type"] as? String,
                  let activity = event["activity"] as? String,
                  let timestamp = event["timestamp"] as? Double else {
                continue
            }

            activityLastSeen[activity] = max(activityLastSeen[activity] ?? 0, timestamp)

            // Estimate duration based on threshold events
            if type == "threshold_reached" {
                if let eventName = event["event"] as? String {
                    // Parse duration from event name (e.g., "usage_15min" = 15 minutes)
                    if eventName.contains("15min") {
                        activityDurations[activity] = (activityDurations[activity] ?? 0) + (15 * 60)
                    } else if eventName.contains("30min") {
                        activityDurations[activity] = (activityDurations[activity] ?? 0) + (30 * 60)
                    } else if eventName.contains("60min") {
                        activityDurations[activity] = (activityDurations[activity] ?? 0) + (60 * 60)
                    }
                }
            }
        }

        // Create Screentime entries
        for (activity, duration) in activityDurations where duration > 0 {
            let lastUsed = activityLastSeen[activity] ?? endTime

            let screentime = Screentime(
                date: String(Int64(Date().timeIntervalSince1970 * 1000)),
                startTime: formatDate(startTime),
                endTime: formatDate(endTime),
                appName: activity,
                totalTime: Int64(duration * 1000), // Convert to milliseconds
                lastTimeUsed: formatDate(lastUsed)
            )

            result.append(screentime)
        }

        print("📊 Converted to \(result.count) Screentime entries")
        return result
    }

    private func formatDate(_ timestamp: Double) -> String {
        let date = Date(timeIntervalSince1970: timestamp)
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        return formatter.string(from: date)
    }

    /// Check if Screen Time authorization is granted.
    @available(iOS 15.0, *)
    @objc public func isAuthorized() -> Bool {
        #if canImport(FamilyControls)
        #if targetEnvironment(simulator)
        print("ℹ️ Screen Time authorization check skipped on Simulator")
        return false
        #else
        let status = AuthorizationCenter.shared.authorizationStatus
        let authorized = status == .approved
        print("ℹ️ Screen Time authorization status: \(status.rawValue), authorized: \(authorized)")
        return authorized
        #endif
        #else
        print("ℹ️ Screen Time authorization check skipped - Family Controls not available")
        return false
        #endif
    }
}

/// Extension to conform to Kotlin interface
extension ScreenTimeDataBridgeAdapter: IOSScreenTimeDataBridge {
    // Methods already implemented above - this just conforms to the protocol
}

// MARK: - Registration Function

/// Register the screen time scheduler with Kotlin.
/// Call this during app initialization (similar to health data scheduler).
public func registerScreenTimeSchedulerWithKotlin() {
    // Register background tasks with the system
    ScreenTimeTaskScheduler.shared.registerBackgroundTasks()

    // Set the scheduler bridge in Kotlin
    IOSScreenTimeSchedulerProvider.shared.bridge = ScreenTimeSchedulerBridgeAdapter.shared

    // Set the data collection bridge in Kotlin
    IOSScreenTimeDataProvider.shared.bridge = ScreenTimeDataBridgeAdapter.shared

    print("✅ Screen time scheduler bridge registered with Kotlin")
    print("✅ Screen time data bridge registered with Kotlin")
    print("ℹ️  Note: Screen Time authorization must be requested separately")
    print("ℹ️  Add 'com.lemurs.lemurs_app.screentimeCollection' to Info.plist BGTaskSchedulerPermittedIdentifiers")
    print("⚠️  iOS Screen Time API has severe limitations - data collection may be restricted")
}
