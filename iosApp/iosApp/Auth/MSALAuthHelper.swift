import Foundation
import MSAL

/// Callback protocol for MSAL authentication results - can be called from Kotlin
@objc public protocol MSALAuthCallback {
    func onSuccess(accessToken: String)
    func onError(error: String)
    func onAccountLoaded(hasAccount: Bool)
    func onSignOutComplete()
}

/// MSAL Authentication Helper - Swift wrapper for MSAL SDK
/// This class is exposed to Kotlin/Native via Objective-C interop
@objc public class MSALAuthHelper: NSObject {

    @objc public static let shared = MSALAuthHelper()

    private var application: MSALPublicClientApplication?
    private var currentAccount: MSALAccount?
    private let scopes = ["api://b00e7cc0-f93d-4caf-9f9c-c97d8d6f6a0d/lemurs"]

    // Client ID and authority from your Azure AD app registration
    private let clientId = "b00e7cc0-f93d-4caf-9f9c-c97d8d6f6a0d"
    private let authority = "https://login.microsoftonline.com/organizations"
    // Set to nil to let MSAL generate the default redirect URI based on bundle ID
    // The generated URI will be: msauth.<bundle-id>://auth
    // Make sure to register this URI in Azure AD portal
    private var redirectUri: String? = "msauth.com.lemurs.iOS.v1://auth"
    private override init() {
        super.init()
    }

    /// Initialize the MSAL client
    @objc public func initialize(callback: MSALAuthCallback) {
        do {
            // Log bundle ID for debugging
            let bundleId = Bundle.main.bundleIdentifier ?? "unknown"
            print("MSAL Debug: Bundle ID = \(bundleId)")
            print("MSAL Debug: Expected redirect URI = msauth.\(bundleId)://auth")

            let authorityURL = try MSALAADAuthority(url: URL(string: authority)!)

            let config = MSALPublicClientApplicationConfig(
                clientId: clientId,
                redirectUri: redirectUri,
                authority: authorityURL
            )

            // Configure keychain access group for token persistence across app launches
            // This uses the standard Microsoft ADAL cache keychain group
            config.cacheConfig.keychainSharingGroup = "com.microsoft.adalcache"

            // Log the actual redirect URI that will be used
            print("MSAL Debug: Configured redirect URI = \(config.redirectUri ?? "nil (auto-generated)")")
            print("MSAL Debug: Keychain sharing group = \(config.cacheConfig.keychainSharingGroup ?? "nil")")

            application = try MSALPublicClientApplication(configuration: config)

            print("MSAL Debug: Initialization successful!")

            // Load any existing account
            loadCurrentAccount(callback: callback)

        } catch {
            print("Failed to initialize MSAL: \(error)")
            callback.onError(error: "Failed to initialize MSAL: \(error.localizedDescription)")
        }
    }

    /// Initialize MSAL from a configuration file in the bundle
    @objc public func initializeFromConfig(configFileName: String, callback: MSALAuthCallback) {
        guard let configPath = Bundle.main.path(forResource: configFileName, ofType: "json") else {
            callback.onError(error: "Config file not found: \(configFileName).json")
            return
        }

        do {
            let configData = try Data(contentsOf: URL(fileURLWithPath: configPath))
            guard let config = try JSONSerialization.jsonObject(with: configData) as? [String: Any],
                  let clientId = config["client_id"] as? String else {
                callback.onError(error: "Invalid config file format")
                return
            }

            let redirectUri = config["redirect_uri"] as? String ?? self.redirectUri
            let authorityString = config["authority"] as? String ?? self.authority

            let authorityURL = try MSALAADAuthority(url: URL(string: authorityString)!)

            let msalConfig = MSALPublicClientApplicationConfig(
                clientId: clientId,
                redirectUri: redirectUri,
                authority: authorityURL
            )

            application = try MSALPublicClientApplication(configuration: msalConfig)

            // Load any existing account
            loadCurrentAccount(callback: callback)

        } catch {
            print("Failed to initialize MSAL from config: \(error)")
            callback.onError(error: "Failed to initialize MSAL: \(error.localizedDescription)")
        }
    }

    /// Load the current signed-in account
    private func loadCurrentAccount(callback: MSALAuthCallback) {
        guard let application = application else {
            callback.onAccountLoaded(hasAccount: false)
            return
        }

        do {
            let accounts = try application.allAccounts()
            if let account = accounts.first {
                currentAccount = account
                callback.onAccountLoaded(hasAccount: true)
            } else {
                callback.onAccountLoaded(hasAccount: false)
            }
        } catch {
            print("Failed to load accounts: \(error)")
            callback.onAccountLoaded(hasAccount: false)
        }
    }

    /// Acquire token interactively (shows login UI)
    @objc public func acquireTokenInteractively(viewController: UIViewController, callback: MSALAuthCallback) {
        guard let application = application else {
            callback.onError(error: "MSAL not initialized")
            return
        }

        let webViewParameters = MSALWebviewParameters(authPresentationViewController: viewController)

        let parameters = MSALInteractiveTokenParameters(scopes: scopes, webviewParameters: webViewParameters)
        parameters.promptType = .selectAccount

        application.acquireToken(with: parameters) { [weak self] result, error in
            DispatchQueue.main.async {
                if let error = error {
                    let nsError = error as NSError
                    if nsError.domain == MSALErrorDomain {
                        if nsError.code == MSALError.userCanceled.rawValue {
                            callback.onError(error: "User cancelled login")
                            return
                        }
                    }
                    callback.onError(error: "Authentication failed: \(error.localizedDescription)")
                    return
                }

                guard let result = result else {
                    callback.onError(error: "No result returned")
                    return
                }

                self?.currentAccount = result.account
                callback.onSuccess(accessToken: result.accessToken)
            }
        }
    }

    /// Acquire token silently (for already signed-in users)
    @objc public func acquireTokenSilently(callback: MSALAuthCallback) {
        guard let application = application else {
            callback.onError(error: "MSAL not initialized")
            return
        }

        guard let account = currentAccount else {
            callback.onError(error: "No account available for silent token acquisition")
            return
        }

        let parameters = MSALSilentTokenParameters(scopes: scopes, account: account)

        application.acquireTokenSilent(with: parameters) { result, error in
            DispatchQueue.main.async {
                if let error = error {
                    let nsError = error as NSError
                    if nsError.domain == MSALErrorDomain,
                       nsError.code == MSALError.interactionRequired.rawValue {
                        // Silent acquisition failed, interaction required
                        callback.onError(error: "INTERACTION_REQUIRED")
                        return
                    }
                    callback.onError(error: "Silent token acquisition failed: \(error.localizedDescription)")
                    return
                }

                guard let result = result else {
                    callback.onError(error: "No result returned")
                    return
                }

                callback.onSuccess(accessToken: result.accessToken)
            }
        }
    }

    /// Sign out the current account
    @objc public func signOut(callback: MSALAuthCallback) {
        guard let application = application else {
            callback.onError(error: "MSAL not initialized")
            return
        }

        guard let account = currentAccount else {
            callback.onSignOutComplete()
            return
        }

        do {
            try application.remove(account)
            currentAccount = nil
            callback.onSignOutComplete()
        } catch {
            callback.onError(error: "Sign out failed: \(error.localizedDescription)")
        }
    }

    /// Check if user is currently signed in
    @objc public func isSignedIn() -> Bool {
        return currentAccount != nil
    }

    /// Get the current account username
    @objc public func getCurrentUsername() -> String? {
        return currentAccount?.username
    }
}
