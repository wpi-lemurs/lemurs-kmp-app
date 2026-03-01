import UIKit
import SwiftUI
import ComposeApp

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    @Environment(\.scenePhase) private var scenePhase

    var body: some View {
        var didInitialBLEScan: Bool = false
        ComposeView()
            .ignoresSafeArea()
            .onChange(of: scenePhase) { newPhase in
                switch newPhase {
                case .active:
                    // App became active - try to set up observer queries (will check authorization internally)
                    print("📱 App became active")
                    HealthDataTaskScheduler.shared.setupObserverQueries()
                    if !didInitialBLEScan {
                        BluetoothBackgroundEntrypointKt.runBluetoothBackgroundScan(durationSeconds: 15)
                        didInitialBLEScan = true
                    }
                case .background:
                    // App going to background - observer queries should continue to work
                    print("📱 App going to background - observer queries will continue")
                case .inactive:
                    break
                @unknown default:
                    break
                }
            }
    }
}



