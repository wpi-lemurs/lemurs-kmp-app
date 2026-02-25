import Foundation
import ComposeApp

public func registerBluetoothBridgeWithKotlin() {
    IOSBluetoothBridgeProvider.shared.bridge = BluetoothBridgeAdapter.shared
    
}
