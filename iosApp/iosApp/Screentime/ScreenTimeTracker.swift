//
// ScreenTimeTracker.swift
// Alternative screen time tracking without DeviceActivityMonitor extension
//
// This approach uses app lifecycle notifications to track our own app usage

import Foundation
import SwiftUI
import ComposeApp

/// Alternative screen time tracking that works without the DeviceActivityMonitor extension
@objc public class ScreenTimeTracker: NSObject {

    @objc public static let shared = ScreenTimeTracker()

    private var ownAppStartTime: Date?
    private var ownAppTotalTime: TimeInterval = 0

    private let userDefaults = UserDefaults.standard
    private let ownAppUsageKey = "com.lemurs.ownAppUsage"

    private override init() {
        super.init()
        setupAppLifecycleTracking()
    }

    // MARK: - Track Our Own App Usage

    private func setupAppLifecycleTracking() {
        // Track when our app becomes active
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(appBecameActive),
            name: UIApplication.didBecomeActiveNotification,
            object: nil
        )

        // Track when our app goes to background
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(appWentToBackground),
            name: UIApplication.didEnterBackgroundNotification,
            object: nil
        )

        // Track when app will terminate
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(appWillTerminate),
            name: UIApplication.willTerminateNotification,
            object: nil
        )
    }

    @objc private func appBecameActive() {
        ownAppStartTime = Date()
        print("📱 App became active - starting usage timer")
    }

    @objc private func appWentToBackground() {
        saveOwnAppUsageSession()
        print("📱 App went to background - saved usage session")
    }

    @objc private func appWillTerminate() {
        saveOwnAppUsageSession()
        print("📱 App will terminate - saved usage session")
    }

    private func saveOwnAppUsageSession() {
        guard let startTime = ownAppStartTime else { return }

        let duration = Date().timeIntervalSince(startTime)
        ownAppTotalTime += duration

        // Save to UserDefaults with timestamp
        var sessions = loadOwnAppSessions()
        let session: [String: Any] = [
            "startTime": startTime.timeIntervalSince1970,
            "endTime": Date().timeIntervalSince1970,
            "duration": duration
        ]
        sessions.append(session)

        // Keep only last 24 hours of sessions
        let oneDayAgo = Date().addingTimeInterval(-24 * 60 * 60).timeIntervalSince1970
        sessions = sessions.filter { session in
            if let endTime = session["endTime"] as? TimeInterval {
                return endTime >= oneDayAgo
            }
            return false
        }

        userDefaults.set(sessions, forKey: ownAppUsageKey)
        ownAppStartTime = nil
    }

    private func loadOwnAppSessions() -> [[String: Any]] {
        return userDefaults.array(forKey: ownAppUsageKey) as? [[String: Any]] ?? []
    }

    // MARK: - Get Usage Data

    /// Get our app's usage for the specified time range
    @objc public func getOwnAppUsage(startTimeMillis: Int64, endTimeMillis: Int64) -> [Screentime] {
        let sessions = loadOwnAppSessions()
        let startTime = TimeInterval(startTimeMillis) / 1000.0
        let endTime = TimeInterval(endTimeMillis) / 1000.0

        var result: [Screentime] = []
        var totalDuration: TimeInterval = 0
        var lastUsed: TimeInterval = 0

        for session in sessions {
            guard let sessionStart = session["startTime"] as? TimeInterval,
                  let sessionEnd = session["endTime"] as? TimeInterval,
                  let duration = session["duration"] as? TimeInterval else {
                continue
            }

            // Check if session overlaps with requested time range
            if sessionEnd >= startTime && sessionStart <= endTime {
                totalDuration += duration
                lastUsed = max(lastUsed, sessionEnd)
            }
        }

        if totalDuration > 0 {
            let screentime = Screentime(
                date: String(Int64(Date().timeIntervalSince1970 * 1000)),
                startTime: formatDate(startTime),
                endTime: formatDate(endTime),
                appName: Bundle.main.bundleIdentifier ?? "com.lemurs.lemurs-app",
                totalTime: Int64(totalDuration * 1000), // Convert to milliseconds
                lastTimeUsed: formatDate(lastUsed)
            )
            result.append(screentime)
        }

        return result
    }

    // MARK: - Helper Methods

    private func formatDate(_ timestamp: TimeInterval) -> String {
        let date = Date(timeIntervalSince1970: timestamp)
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        return formatter.string(from: date)
    }

    /// Clear old usage data
    @objc public func clearOldUsageData() {
        let oneDayAgo = Date().addingTimeInterval(-24 * 60 * 60).timeIntervalSince1970
        var sessions = loadOwnAppSessions()
        sessions = sessions.filter { session in
            if let endTime = session["endTime"] as? TimeInterval {
                return endTime >= oneDayAgo
            }
            return false
        }
        userDefaults.set(sessions, forKey: ownAppUsageKey)
        print("🗑️  Cleared usage data older than 24 hours")
    }
}

