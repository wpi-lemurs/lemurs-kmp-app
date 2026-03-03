//
//  DeviceActivityMonitorExtension.swift
//  ScreentimeExtension
//
//  Created by Murphy, Jacob on 3/1/26.
//

import DeviceActivity
import Foundation

// This extension receives system callbacks when screen time events occur
// and saves them to a shared container for the main app to read
class DeviceActivityMonitorExtension: DeviceActivityMonitor {

    override init() {
        super.init()
        print("🔵 [Extension] DeviceActivityMonitorExtension initialized")

        // Create the shared container file immediately to verify extension is working
        createSharedContainerFile()
    }

    /// Create the shared container file if it doesn't exist, to verify extension is working
    private func createSharedContainerFile() {
        guard let containerURL = FileManager.default.containerURL(
            forSecurityApplicationGroupIdentifier: "group.com.lemurs.lemurs-app"
        ) else {
            print("❌ [Extension] Failed to access shared container in init")
            return
        }

        let fileURL = containerURL.appendingPathComponent("screentime_events.json")

        if !FileManager.default.fileExists(atPath: fileURL.path) {
            // Create empty array file
            let emptyArray: [[String: Any]] = []
            do {
                let data = try JSONSerialization.data(withJSONObject: emptyArray, options: .prettyPrinted)
                try data.write(to: fileURL, options: .atomic)
                print("✅ [Extension] Created shared container file at init")
            } catch {
                print("❌ [Extension] Failed to create file: \(error)")
            }
        } else {
            print("ℹ️ [Extension] Shared container file already exists")
        }
    }

    // MARK: - Interval Events

    override func intervalDidStart(for activity: DeviceActivityName) {
        super.intervalDidStart(for: activity)
        
        print("📊 [Extension] Screen Time interval started: \(activity.rawValue)")
        saveEvent(type: "interval_start", activity: activity.rawValue, event: nil)
    }
    
    override func intervalDidEnd(for activity: DeviceActivityName) {
        super.intervalDidEnd(for: activity)
        
        print("📊 [Extension] Screen Time interval ended: \(activity.rawValue)")
        saveEvent(type: "interval_end", activity: activity.rawValue, event: nil)
    }
    
    override func intervalWillStartWarning(for activity: DeviceActivityName) {
        super.intervalWillStartWarning(for: activity)
        
        print("⚠️ [Extension] Interval will start soon: \(activity.rawValue)")
        saveEvent(type: "interval_will_start", activity: activity.rawValue, event: nil)
    }
    
    override func intervalWillEndWarning(for activity: DeviceActivityName) {
        super.intervalWillEndWarning(for: activity)
        
        print("⚠️ [Extension] Interval will end soon: \(activity.rawValue)")
        saveEvent(type: "interval_will_end", activity: activity.rawValue, event: nil)
    }

    // MARK: - Threshold Events

    override func eventDidReachThreshold(_ event: DeviceActivityEvent.Name, activity: DeviceActivityName) {
        super.eventDidReachThreshold(event, activity: activity)

        print("⏱️ [Extension] Threshold reached: \(event.rawValue) for activity: \(activity.rawValue)")

        // Parse the threshold duration from the event name
        let durationMinutes = parseDurationFromEventName(event.rawValue)

        saveEvent(
            type: "threshold_reached",
            activity: activity.rawValue,
            event: event.rawValue,
            durationMinutes: durationMinutes
        )
    }
    
    override func eventWillReachThresholdWarning(_ event: DeviceActivityEvent.Name, activity: DeviceActivityName) {
        super.eventWillReachThresholdWarning(event, activity: activity)
        
        print("⚠️ [Extension] Approaching threshold: \(event.rawValue) for activity: \(activity.rawValue)")
        saveEvent(type: "threshold_warning", activity: activity.rawValue, event: event.rawValue)
    }

    // MARK: - Helper Methods

    /// Parse duration in minutes from event name (e.g., "usage_15min" -> 15)
    private func parseDurationFromEventName(_ eventName: String) -> Int? {
        // Extract number from patterns like "usage_5min", "usage_15min", etc.
        let components = eventName.components(separatedBy: "_")
        if components.count >= 2 {
            let minuteString = components[1].replacingOccurrences(of: "min", with: "")
            return Int(minuteString)
        }
        return nil
    }

    // MARK: - Data Storage

    /// Save event data to shared container for main app to read
    private func saveEvent(type: String, activity: String, event: String?, durationMinutes: Int? = nil) {
        // Access shared container
        guard let containerURL = FileManager.default.containerURL(
            forSecurityApplicationGroupIdentifier: "group.com.lemurs.lemurs-app"
        ) else {
            print("❌ [Extension] Failed to access shared container")
            return
        }

        let fileURL = containerURL.appendingPathComponent("screentime_events.json")
        let timestamp = Date().timeIntervalSince1970

        // Create event data
        var eventData: [String: Any] = [
            "type": type,
            "activity": activity,
            "timestamp": timestamp,
            "date": ISO8601DateFormatter().string(from: Date())
        ]

        if let event = event {
            eventData["event"] = event
        }

        // Add duration information for threshold events
        if let duration = durationMinutes {
            eventData["durationMinutes"] = duration
            eventData["durationSeconds"] = duration * 60
            print("📊 [Extension] Recording usage duration: \(duration) minutes")
        }

        // ... existing code to save to file ...

        do {
            var events: [[String: Any]] = []

            // Load existing events if file exists
            if FileManager.default.fileExists(atPath: fileURL.path) {
                let existingData = try Data(contentsOf: fileURL)
                if let existingEvents = try JSONSerialization.jsonObject(with: existingData) as? [[String: Any]] {
                    events = existingEvents
                }
            }

            // Add new event
            events.append(eventData)

            // Keep only last 1000 events to prevent file from growing too large
            if events.count > 1000 {
                events = Array(events.suffix(1000))
            }

            // Save back to file
            let jsonData = try JSONSerialization.data(withJSONObject: events, options: .prettyPrinted)
            try jsonData.write(to: fileURL, options: .atomic)

            print("✅ [Extension] Saved event: \(type) to shared container")
        } catch {
            print("❌ [Extension] Failed to save event: \(error.localizedDescription)")
        }
    }
}
