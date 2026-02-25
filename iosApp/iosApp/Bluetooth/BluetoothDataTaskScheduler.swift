import Foundation
import BackgroundTasks
import ComposeApp

private let bluetoothScanTaskIdentifier = "com.lemurs.bluetooth.scan"

@objc public final class BluetoothDataTaskScheduler: NSObject {

    @objc public static let shared = BluetoothDataTaskScheduler()

    /// Avoid double-registering / double-scheduling
    private var didRegister = false

    private override init() {
        super.init()
    }

    // MARK: - Registration

    /// Register BG task with the system.
    /// Call once during app startup (your iOSApp.init is perfect).
    @objc public func registerBackgroundTasks() {
        guard !didRegister else { return }
        didRegister = true

        BGTaskScheduler.shared.register(
            forTaskWithIdentifier: bluetoothScanTaskIdentifier,
            using: nil
        ) { [weak self] task in
            self?.handleBluetoothScanTask(task as! BGAppRefreshTask)
        }

        print("✅ Registered Bluetooth BG task: \(bluetoothScanTaskIdentifier)")
    }

    // MARK: - Scheduling

    /// Schedule the next background scan.
    /// iOS decides the actual runtime; earliestBeginDate is a hint.
    @objc public func scheduleBackgroundBluetoothScan() {
        let request = BGAppRefreshTaskRequest(identifier: bluetoothScanTaskIdentifier)
        request.earliestBeginDate = Date(timeIntervalSinceNow: 15 * 60)

        do {
            try BGTaskScheduler.shared.submit(request)
            print("✅ Scheduled Bluetooth scan (earliest ~15 min)")
        } catch BGTaskScheduler.Error.notPermitted {
            print("❌ BGTaskScheduler not permitted. Check BGTaskSchedulerPermittedIdentifiers in Info.plist.")
        } catch BGTaskScheduler.Error.tooManyPendingTaskRequests {
            print("⚠️ Too many pending Bluetooth requests (already scheduled).")
        } catch BGTaskScheduler.Error.unavailable {
            print("⚠️ BGTaskScheduler unavailable (background refresh off / simulator / system).")
        } catch {
            print("❌ Failed scheduling Bluetooth scan: \(error.localizedDescription)")
        }
    }

    @objc public func cancelScheduledTasks() {
        BGTaskScheduler.shared.cancel(taskRequestWithIdentifier: bluetoothScanTaskIdentifier)
        print("🛑 Cancelled scheduled Bluetooth scan task")
    }

    // MARK: - Handler

    private func handleBluetoothScanTask(_ task: BGAppRefreshTask) {
        print("🔄 Bluetooth BG task started")

        // Schedule next run first
        scheduleBackgroundBluetoothScan()

        // Expiration handler (system says we're out of time)
        task.expirationHandler = {
            print("⚠️ Bluetooth BG task expired")
            task.setTaskCompleted(success: false)
        }

        // IMPORTANT: Ensure Koin + bridge are ready before calling Kotlin.
        // In your app, iOSApp.init registers bridges and calls doInitKoin().
        // If you keep that, we're good.

        // Call Kotlin entrypoint that performs scan + DAO insert
        // Prefer top-level function (most reliable Swift export):
        // ComposeAppKt.runBluetoothBackgroundScan(durationSeconds: 15)
        BluetoothBackgroundEntrypointKt.runBluetoothBackgroundScan(durationSeconds: 15)

        // If you want a more accurate completion, you can add a Kotlin callback later.
        task.setTaskCompleted(success: true)
        print("✅ Bluetooth BG task completed")
    }
}
