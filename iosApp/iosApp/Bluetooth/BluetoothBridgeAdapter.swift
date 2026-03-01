import Foundation
import ComposeApp

public final class BluetoothBridgeAdapter: NSObject, IOSBluetoothBridge {
    public static let shared = BluetoothBridgeAdapter()

    private override init() {}

    public func scan(
        durationSeconds: Int32,
        onSuccess: @escaping ([String]) -> Void,
        onError: @escaping (String) -> Void
    ) {
        BluetoothKotlinBridge.shared.scan(
            durationSeconds: Int(durationSeconds),
            onSuccess: { devices in
                onSuccess(devices)
            },
            onError: { err in
                onError(err)
            }
        )
    }
}