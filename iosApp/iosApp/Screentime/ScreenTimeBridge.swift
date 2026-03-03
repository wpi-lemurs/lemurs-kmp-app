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

        #if canImport(DeviceActivity) && canImport(FamilyControls)
        let center = DeviceActivityCenter()

        print("📊 Setting up DeviceActivity monitoring for TestFlight")
        print("ℹ️  This build uses aggressive data collection suitable for TestFlight only")

        // Check if monitoring is already active from previous launch
        if isMonitoringAlreadyActive() {
            let appCount = UserDefaults.standard.integer(forKey: "screentime_app_selection_count")
            print("✅ DeviceActivity monitoring already active from previous session")
            print("ℹ️  Monitoring persists: \(appCount) app(s) selected previously")
            print("ℹ️  Threshold and interval monitoring continue across app launches")
            print("ℹ️  No need to restart monitoring - sessions are persistent")

            // Create shared container file if needed
            createSharedContainerFileIfNeeded()
            return
        }

        print("ℹ️  Starting initial DeviceActivity monitoring setup")
        print("ℹ️  User should select apps via app picker to enable threshold tracking")

        // Note: FamilyActivitySelection cannot be persisted across app launches
        // It's session-based only. But once monitoring is started, it persists!
        let selection = FamilyActivitySelection()

        // Create monitoring schedule (midnight to midnight)
        let schedule = DeviceActivitySchedule(
            intervalStart: DateComponents(hour: 0, minute: 0),
            intervalEnd: DateComponents(hour: 23, minute: 59),
            repeats: true
        )

        // Create threshold events that will fire when user reaches usage thresholds
        // These track TOTAL usage across all apps (if we can get all app tokens)
        let events: [DeviceActivityEvent.Name: DeviceActivityEvent] = [
            .init("usage_5min"): DeviceActivityEvent(
                applications: selection.applicationTokens,
                threshold: DateComponents(minute: 5)
            ),
            .init("usage_15min"): DeviceActivityEvent(
                applications: selection.applicationTokens,
                threshold: DateComponents(minute: 15)
            ),
            .init("usage_30min"): DeviceActivityEvent(
                applications: selection.applicationTokens,
                threshold: DateComponents(minute: 30)
            ),
            .init("usage_60min"): DeviceActivityEvent(
                applications: selection.applicationTokens,
                threshold: DateComponents(minute: 60)
            ),
            .init("usage_120min"): DeviceActivityEvent(
                applications: selection.applicationTokens,
                threshold: DateComponents(minute: 120)
            ),
            .init("usage_240min"): DeviceActivityEvent(
                applications: selection.applicationTokens,
                threshold: DateComponents(minute: 240)
            )
        ]

        let activityName = DeviceActivityName("daily_all_apps")

        do {
            try center.startMonitoring(activityName, during: schedule, events: events)
            print("✅ Started comprehensive DeviceActivity monitoring")

            if selection.applicationTokens.isEmpty {
                print("⚠️  No app tokens available - threshold events won't fire")
                print("ℹ️  User needs to select apps in Settings or via app picker")
            } else {
                print("✅ Monitoring \(selection.applicationTokens.count) app(s)")
                print("ℹ️  Threshold events will fire at: 5min, 15min, 30min, 1hr, 2hr, 4hr")
            }
        } catch {
            print("❌ Failed to start comprehensive monitoring: \(error.localizedDescription)")
        }

        // ALSO set up 4 six-hour intervals as fallback
        // iOS limits: Maximum ~5-6 concurrent DeviceActivity sessions
        print("📊 Setting up 4 six-hour interval monitoring as fallback...")

        let intervals: [(String, DateComponents, DateComponents)] = [
            ("interval_night", DateComponents(hour: 0, minute: 0), DateComponents(hour: 6, minute: 0)),
            ("interval_morning", DateComponents(hour: 6, minute: 0), DateComponents(hour: 12, minute: 0)),
            ("interval_afternoon", DateComponents(hour: 12, minute: 0), DateComponents(hour: 18, minute: 0)),
            ("interval_evening", DateComponents(hour: 18, minute: 0), DateComponents(hour: 23, minute: 59))
        ]

        for (name, start, end) in intervals {
            let schedule = DeviceActivitySchedule(
                intervalStart: start,
                intervalEnd: end,
                repeats: true
            )

            let activityName = DeviceActivityName(name)

            do {
                try center.startMonitoring(activityName, during: schedule)
                print("✅ Started interval: \(name) (\(start.hour!):00-\(end.hour!):\(end.minute ?? 0))")
            } catch {
                print("❌ Failed to start monitoring \(name): \(error.localizedDescription)")
            }
        }

        print("✅ DeviceActivity monitoring configured:")
        print("   • Threshold-based monitoring (requires app selection)")
        print("   • 4 six-hour intervals (always works)")
        print("   • Total activities: 5 (within iOS limit)")
        print("   • Interval callbacks: 4x per day at 6 AM, Noon, 6 PM, Midnight")

        // Mark monitoring as started so we don't restart on next launch
        markMonitoringAsStarted()

        // Create shared container file to verify it works
        createSharedContainerFileIfNeeded()

        #endif
    }

    /// Check if monitoring is already active from a previous session
    /// Once started, DeviceActivity monitoring persists across app launches until explicitly stopped
    private func isMonitoringAlreadyActive() -> Bool {
        // Check if we've ever started monitoring
        let hasStartedMonitoring = UserDefaults.standard.bool(forKey: "deviceactivity_monitoring_started")

        // Check if user has completed app selection
        let hasCompletedSelection = hasCompletedAppSelection()

        return hasStartedMonitoring && hasCompletedSelection
    }

    /// Mark that monitoring has been started (persists across launches)
    private func markMonitoringAsStarted() {
        UserDefaults.standard.set(true, forKey: "deviceactivity_monitoring_started")
        print("✅ Marked monitoring as started - will persist across app launches")
    }

    /// Stop DeviceActivity monitoring
    @available(iOS 16.0, *)
    @objc public func stopMonitoring() {
        #if canImport(DeviceActivity)
        let center = DeviceActivityCenter()

        // Stop threshold monitoring
        center.stopMonitoring([DeviceActivityName("daily_all_apps")])

        // Stop 4 six-hour interval monitoring sessions
        let intervalNames = ["interval_night", "interval_morning", "interval_afternoon", "interval_evening"]
        let activities = intervalNames.map { DeviceActivityName($0) }

        center.stopMonitoring(activities)

        // Clear the monitoring started flag
        UserDefaults.standard.set(false, forKey: "deviceactivity_monitoring_started")

        print("🛑 Stopped all DeviceActivity monitoring (5 activities)")
        #else
        print("⚠️ DeviceActivity not available")
        #endif
    }

    /// Apply saved app selection and restart monitoring with selected apps
    @available(iOS 16.0, *)
    public func applyAppSelection(_ selection: FamilyActivitySelection) {
        guard isFamilyControlsAvailable else {
            print("❌ Cannot apply app selection - Family Controls not available")
            return
        }

        #if canImport(DeviceActivity) && canImport(FamilyControls)
        print("📱 Applying app selection with \(selection.applicationTokens.count) app(s)")

        // DON'T stop monitoring - just update it
        // DeviceActivity sessions persist across launches, so we update not restart
        let center = DeviceActivityCenter()

        // 1. Set up threshold monitoring with selected apps
        let schedule = DeviceActivitySchedule(
            intervalStart: DateComponents(hour: 0, minute: 0),
            intervalEnd: DateComponents(hour: 23, minute: 59),
            repeats: true
        )

        let events: [DeviceActivityEvent.Name: DeviceActivityEvent] = [
            .init("usage_5min"): DeviceActivityEvent(
                applications: selection.applicationTokens,
                threshold: DateComponents(minute: 5)
            ),
            .init("usage_15min"): DeviceActivityEvent(
                applications: selection.applicationTokens,
                threshold: DateComponents(minute: 15)
            ),
            .init("usage_30min"): DeviceActivityEvent(
                applications: selection.applicationTokens,
                threshold: DateComponents(minute: 30)
            ),
            .init("usage_60min"): DeviceActivityEvent(
                applications: selection.applicationTokens,
                threshold: DateComponents(minute: 60)
            ),
            .init("usage_120min"): DeviceActivityEvent(
                applications: selection.applicationTokens,
                threshold: DateComponents(minute: 120)
            ),
            .init("usage_240min"): DeviceActivityEvent(
                applications: selection.applicationTokens,
                threshold: DateComponents(minute: 240)
            )
        ]

        let activityName = DeviceActivityName("daily_all_apps")

        do {
            try center.startMonitoring(activityName, during: schedule, events: events)
            print("✅ Threshold monitoring started with \(selection.applicationTokens.count) app(s)")
            print("✅ Thresholds: 5min, 15min, 30min, 1hr, 2hr, 4hr")
        } catch {
            print("❌ Failed to start threshold monitoring: \(error.localizedDescription)")
        }

        // 2. ALSO set up 4 six-hour intervals as fallback
        // iOS limits: Maximum ~5-6 concurrent DeviceActivity sessions
        // We use 1 for thresholds + 4 for intervals = 5 total (safe)
        print("📊 Setting up 4 six-hour interval monitoring (iOS activity limit workaround)...")

        let intervals: [(String, DateComponents, DateComponents)] = [
            ("interval_night", DateComponents(hour: 0, minute: 0), DateComponents(hour: 6, minute: 0)),
            ("interval_morning", DateComponents(hour: 6, minute: 0), DateComponents(hour: 12, minute: 0)),
            ("interval_afternoon", DateComponents(hour: 12, minute: 0), DateComponents(hour: 18, minute: 0)),
            ("interval_evening", DateComponents(hour: 18, minute: 0), DateComponents(hour: 23, minute: 59))
        ]

        for (name, start, end) in intervals {
            let intervalSchedule = DeviceActivitySchedule(
                intervalStart: start,
                intervalEnd: end,
                repeats: true
            )

            let intervalActivity = DeviceActivityName(name)

            do {
                try center.startMonitoring(intervalActivity, during: intervalSchedule)
                print("✅ Started interval: \(name) (\(start.hour!):00-\(end.hour!):\(end.minute ?? 0))")
            } catch {
                print("❌ Failed to start interval \(name): \(error.localizedDescription)")
            }
        }

        print("✅ Complete monitoring configured:")
        print("   • Threshold monitoring: \(selection.applicationTokens.count) apps")
        print("   • Six-hour intervals: 4 intervals per day")
        print("   • Total activities: 5 (within iOS limit)")
        print("   • Extension will fire 4x per day at: 6 AM, Noon, 6 PM, Midnight")

        // Mark monitoring as started so it persists across app launches
        markMonitoringAsStarted()

        // Create shared container file from main app to verify it works
        createSharedContainerFileIfNeeded()

        #endif
    }

    /// Create the shared container file if it doesn't exist
    /// This verifies shared container access works before extension fires
    private func createSharedContainerFileIfNeeded() {
        guard let containerURL = FileManager.default.containerURL(
            forSecurityApplicationGroupIdentifier: "group.com.lemurs.lemurs-app"
        ) else {
            print("❌ [Main App] Failed to access shared container")
            return
        }

        let fileURL = containerURL.appendingPathComponent("screentime_events.json")
        print("📂 [Main App] Shared container path: \(fileURL.path)")

        if FileManager.default.fileExists(atPath: fileURL.path) {
            print("✅ [Main App] Shared container file already exists")
        } else {
            // Create empty array file
            let emptyArray: [[String: Any]] = []
            do {
                let data = try JSONSerialization.data(withJSONObject: emptyArray, options: .prettyPrinted)
                try data.write(to: fileURL, options: .atomic)
                print("✅ [Main App] Created shared container file for extension")
                print("ℹ️  Extension will add events to this file when callbacks fire")
            } catch {
                print("❌ [Main App] Failed to create shared container file: \(error)")
            }
        }
    }

    /// Check if user has completed app selection
    @objc public func hasCompletedAppSelection() -> Bool {
        return UserDefaults.standard.bool(forKey: "screentime_app_selection_completed")
    }

    /// Load saved app selection from UserDefaults
    @available(iOS 16.0, *)
    public func loadSavedAppSelection() -> FamilyActivitySelection? {
        #if canImport(FamilyControls)
        guard let data = UserDefaults.standard.data(forKey: "screentime_app_selection") else {
            return nil
        }

        do {
            let selection = try JSONDecoder().decode(FamilyActivitySelection.self, from: data)
            print("📱 Loaded saved selection: \(selection.applicationTokens.count) app(s)")
            return selection
        } catch {
            print("❌ Failed to load saved selection: \(error)")
            return nil
        }
        #else
        return nil
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

        // FOR TESTING: Run after 15 minutes
        request.earliestBeginDate = Date(timeIntervalSinceNow: 15 * 60)

        do {
            try BGTaskScheduler.shared.submit(request)
            print("✅ Scheduled background screen time collection for ~15 minutes from now")
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

            // Schedule the next collection to ensure continuous data gathering
            self.scheduleBackgroundScreentimeCollection()
            print("🔄 Scheduled next collection for ~2 minutes from now")
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
        print("📂 Checking for extension data at: \(fileURL.path)")

        guard FileManager.default.fileExists(atPath: fileURL.path) else {
            print("ℹ️  No screen time events file found yet")
            print("ℹ️  DeviceActivityMonitor extension hasn't logged any events")
            print("ℹ️  Possible reasons:")
            print("     • Not enough time has passed (thresholds: 5/15/30/60/120/240 min)")
            print("     • User hasn't used the selected apps enough")
            print("     • Extension hasn't been triggered by iOS yet")
            print("     • Hourly intervals won't fire until top of next hour")
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

            // Log what types of events we have
            let eventTypes = events.compactMap { $0["type"] as? String }
            let typeCounts = Dictionary(grouping: eventTypes, by: { $0 }).mapValues { $0.count }
            print("📊 Event types: \(typeCounts)")

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
            let screentimeData = convertEventsToScreentime(filteredEvents, startTime: startTime, endTime: endTime)
            print("✅ Returning \(screentimeData.count) Screentime entries from extension data")
            return screentimeData

        } catch {
            print("❌ Failed to read events: \(error)")
            return getFallbackUsageData(startTimeMillis: startTimeMillis, endTimeMillis: endTimeMillis)
        }
    }

    /// Fallback to own app usage tracking when extension data is not available
    private func getFallbackUsageData(startTimeMillis: Int64, endTimeMillis: Int64) -> [Screentime] {
        // ScreenTimeTracker disabled - only using extension data
        print("ℹ️ No extension data available")
        print("ℹ️ ScreenTimeTracker disabled - returning empty data")
        print("ℹ️ Waiting for extension to log threshold or interval events")
        return []

        /* DISABLED - ScreenTimeTracker that only tracks own app
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
        */
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

            // Get actual duration from threshold events
            if type == "threshold_reached" {
                // Use actual duration if provided by extension
                if let durationSeconds = event["durationSeconds"] as? Double {
                    activityDurations[activity] = (activityDurations[activity] ?? 0) + durationSeconds
                    print("📊 Using actual duration: \(durationSeconds / 60) minutes")
                } else if let eventName = event["event"] as? String {
                    // Fallback: estimate duration from event name
                    if eventName.contains("5min") {
                        activityDurations[activity] = (activityDurations[activity] ?? 0) + (5 * 60)
                    } else if eventName.contains("15min") {
                        activityDurations[activity] = (activityDurations[activity] ?? 0) + (15 * 60)
                    } else if eventName.contains("30min") {
                        activityDurations[activity] = (activityDurations[activity] ?? 0) + (30 * 60)
                    } else if eventName.contains("60min") || eventName.contains("1hr") {
                        activityDurations[activity] = (activityDurations[activity] ?? 0) + (60 * 60)
                    } else if eventName.contains("120min") || eventName.contains("2hr") {
                        activityDurations[activity] = (activityDurations[activity] ?? 0) + (120 * 60)
                    } else if eventName.contains("240min") || eventName.contains("4hr") {
                        activityDurations[activity] = (activityDurations[activity] ?? 0) + (240 * 60)
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
