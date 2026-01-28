import SwiftUI
import ComposeApp
import MSAL

@main
struct iOSApp: App {

    init() {
        // Register the MSAL Swift bridge with Kotlin before initializing Koin
        registerMSALBridgeWithKotlin()

        // Initialize Koin for dependency injection
        MainViewControllerKt.doInitKoin()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in
                    // Handle MSAL authentication redirects
                    MSALPublicClientApplication.handleMSALResponse(
                        url,
                        sourceApplication: nil
                    )
                }
        }
    }
}