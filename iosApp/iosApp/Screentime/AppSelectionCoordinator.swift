//
//  AppSelectionCoordinator.swift
//  iosApp
//
//  Coordinator to present App Selection view from Compose/UIKit
//

import SwiftUI
import UIKit

@available(iOS 16.0, *)
@objc public class AppSelectionCoordinator: NSObject {

    @objc public static let shared = AppSelectionCoordinator()

    private override init() {
        super.init()
    }

    /// Present the app selection screen
    @objc public func presentAppSelection() {
        guard let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
              let rootViewController = windowScene.windows.first?.rootViewController else {
            print("❌ Failed to get root view controller")
            return
        }

        print("📱 Presenting app selection screen...")

        let appSelectionView = AppSelectionView()
        let hostingController = UIHostingController(rootView: appSelectionView)
        hostingController.modalPresentationStyle = .pageSheet

        if let sheet = hostingController.sheetPresentationController {
            sheet.detents = [.large()]
            sheet.prefersGrabberVisible = true
        }

        // Find the topmost view controller
        var topController = rootViewController
        while let presented = topController.presentedViewController {
            topController = presented
        }

        topController.present(hostingController, animated: true) {
            print("✅ App selection screen presented")
        }
    }

    /// Check if app selection should be shown (first time after Screen Time auth)
    @objc public func shouldShowAppSelection() -> Bool {
        let hasScreenTimeAuth = ScreenTimeTaskScheduler.shared.isAuthorizationGranted()
        let hasCompletedSelection = ScreenTimeTaskScheduler.shared.hasCompletedAppSelection()

        // Show if user has Screen Time auth but hasn't selected apps yet
        return hasScreenTimeAuth && !hasCompletedSelection
    }
}

