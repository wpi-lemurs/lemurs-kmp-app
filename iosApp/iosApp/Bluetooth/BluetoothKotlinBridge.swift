import Foundation
import CoreBluetooth

@objc public final class BluetoothKotlinBridge: NSObject {
    @objc public static let shared = BluetoothKotlinBridge()

    private var central: CBCentralManager?
    private var discovered = Set<String>()

    private var onSuccess: (([String]) -> Void)?
    private var onError: ((String) -> Void)?
    private var stopWorkItem: DispatchWorkItem?

    private override init() {
        super.init()
    }

    /// Call from adapter. Starts central manager if needed, then scans when poweredOn.
    public func scan(
        durationSeconds: Int,
        onSuccess: @escaping ([String]) -> Void,
        onError: @escaping (String) -> Void
    ) {
        self.onSuccess = onSuccess
        self.onError = onError
        self.discovered.removeAll()

        // Using restoration identifier is useful if iOS relaunches you for BLE events.
        let options: [String: Any] = [
            CBCentralManagerOptionRestoreIdentifierKey: "com.lemurs.bluetooth.restore"
        ]

        // Important: Use main queue for delegate callbacks to be predictable.
        self.central = CBCentralManager(delegate: self, queue: .main, options: options)

        // Stop timer (cancel any previous)
        stopWorkItem?.cancel()
        let item = DispatchWorkItem { [weak self] in
            self?.finishScan(success: true)
        }
        stopWorkItem = item
        DispatchQueue.main.asyncAfter(deadline: .now() + .seconds(durationSeconds), execute: item)
    }

    private func finishScan(success: Bool) {
        central?.stopScan()
        if success {
            onSuccess?(Array(discovered))
        }
        onSuccess = nil
        onError = nil
        stopWorkItem = nil
    }
}

extension BluetoothKotlinBridge: CBCentralManagerDelegate {
    public func centralManagerDidUpdateState(_ central: CBCentralManager) {
        switch central.state {
        case .poweredOn:
            central.scanForPeripherals(withServices: nil, options: nil)

        case .unsupported:
            onError?("Bluetooth unsupported on this device")
            finishScan(success: false)

        case .unauthorized:
            onError?("Bluetooth unauthorized (check permissions)")
            finishScan(success: false)

        case .poweredOff:
            onError?("Bluetooth is powered off")
            finishScan(success: false)

        default:
            // resetting / unknown: wait; if you want you can timeout earlier
            break
        }
    }

    public func centralManager(
        _ central: CBCentralManager,
        didDiscover peripheral: CBPeripheral,
        advertisementData: [String : Any],
        rssi RSSI: NSNumber
    ) {
        let name = peripheral.name ?? "Unknown"
        let uuid = peripheral.identifier.uuidString
        discovered.insert("\(name) (\(uuid))")
    }

    public func centralManager(_ central: CBCentralManager, willRestoreState dict: [String : Any]) {
        // If iOS relaunches your app due to BLE events, you can restart scanning.
        central.scanForPeripherals(withServices: nil, options: nil)
    }
}