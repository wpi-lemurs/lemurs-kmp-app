import Foundation
import ComposeApp

public func registerBluetoothSchedulerWithKotlin() {
    // Register BG task with iOS
    BluetoothDataTaskScheduler.shared.registerBackgroundTasks()

    // (Optional) schedule one immediately on startup
    BluetoothDataTaskScheduler.shared.scheduleBackgroundBluetoothScan()

    print("✅ Bluetooth scheduler registered")
}
