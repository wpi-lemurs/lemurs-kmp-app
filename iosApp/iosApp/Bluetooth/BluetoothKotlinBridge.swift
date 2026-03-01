import Foundation
import CoreBluetooth

@objc public final class BluetoothKotlinBridge: NSObject {
    @objc public static let shared = BluetoothKotlinBridge()

    private var central: CBCentralManager!
    private var discovered = Set<String>()

    private var pendingDuration: Int = 15
    private var scanning = false

    private var onSuccess: (([String]) -> Void)?
    private var onError: ((String) -> Void)?
    private var stopWorkItem: DispatchWorkItem?

    private override init() {
        super.init()

        central = CBCentralManager(
            delegate: self,
            queue: .main,
            options: nil
        )
        print("🟦 (CoreBluetooth) BluetoothKotlinBridge central created")
    }

    public func scan(durationSeconds: Int,
                     onSuccess: @escaping ([String]) -> Void,
                     onError: @escaping (String) -> Void) {

        print("🟦 (CoreBluetooth) BluetoothKotlinBridge.scan called for \(durationSeconds)s")
        self.onSuccess = onSuccess
        self.onError = onError
        self.pendingDuration = durationSeconds
        self.discovered.removeAll()

        // if state already powered on, start immediately
        if central.state == .poweredOn {
            startScanNow()
        } else {
            // otherwise wait for centralManagerDidUpdateState
            print("🟦 (CoreBluetooth) waiting for central state, current=\(central.state.rawValue)")
        }
    }

    private func startScanNow() {
        guard !scanning else { return }
        scanning = true

        print("🟩 (CoreBluetooth) startScanNow")
        central.scanForPeripherals(withServices: nil, options: nil)

        stopWorkItem?.cancel()
        let item = DispatchWorkItem { [weak self] in
            self?.finishScan(success: true)
        }
        stopWorkItem = item
        DispatchQueue.main.asyncAfter(deadline: .now() + .seconds(pendingDuration), execute: item)
    }

    private func finishScan(success: Bool) {
        print("🟨 (CoreBluetooth) finishScan success=\(success) count=\(discovered.count)")
        central.stopScan()
        scanning = false

        if success { onSuccess?(Array(discovered)) }
        onSuccess = nil
        onError = nil
        stopWorkItem = nil
    }
}

extension BluetoothKotlinBridge: CBCentralManagerDelegate {
    public func centralManagerDidUpdateState(_ central: CBCentralManager) {
        print("🟦 (CoreBluetooth) central state update = \(central.state.rawValue)")

        switch central.state {
        case .poweredOn:
            startScanNow()

        case .unsupported:
            onError?("Bluetooth unsupported")
            finishScan(success: false)

        case .unauthorized:
            onError?("Bluetooth unauthorized")
            finishScan(success: false)

        case .poweredOff:
            onError?("Bluetooth powered off")
            finishScan(success: false)

        default:
            break
        }
    }

    public func centralManager(_ central: CBCentralManager,
                               didDiscover peripheral: CBPeripheral,
                               advertisementData: [String : Any],
                               rssi RSSI: NSNumber) {
        let name = peripheral.name ?? "Unknown"
        let uuid = peripheral.identifier.uuidString
        discovered.insert("\(name) (\(uuid))")
    }
}
