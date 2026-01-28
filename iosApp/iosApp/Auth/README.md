# iOS MSAL Authentication Setup

This document describes how to set up Microsoft Authentication Library (MSAL) for iOS authentication in the Lemurs app.

## Architecture

The iOS MSAL integration uses Swift/Kotlin interop:

```
┌─────────────────────────────────────────────────────────────────────┐
│                         Kotlin (Shared Code)                        │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │        MicrosoftApiAuthorizationService.ios.kt               │   │
│  │            (actual implementation)                           │   │
│  └──────────────────────────────────────────────────────────────┘   │
│                                │                                    │
│                                ▼                                    │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │                 IOSMSALBridge (interface)                    │   │
│  │             IOSMSALBridgeProvider (singleton)                │   │
│  └──────────────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────────────
                                │
                                ▼ (Kotlin/Native Interop)
┌─────────────────────────────────────────────────────────────────────┐
│                           Swift (iOS)                               │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │                  MSALBridgeAdapter                           │   │
│  │      (implements IOSMSALBridge, calls MSALKotlinBridge)      │   │
│  └──────────────────────────────────────────────────────────────┘   │
│                                │                                    │
│                                ▼                                    │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │                  MSALKotlinBridge                            │   │
│  │                  MSALAuthHelper                              │   │
│  └──────────────────────────────────────────────────────────────┘   │
│                                │                                    │
│                                ▼                                    │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │                  MSAL iOS SDK (CocoaPods)                    │   │
│  └──────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
```

## Setup Instructions

### 1. Install CocoaPods Dependencies

Navigate to the iosApp directory and install pods:

```bash
cd iosApp
pod install
```

After installation, always open the `.xcworkspace` file instead of `.xcodeproj`:

```bash
open iosApp.xcworkspace
```

### 2. Configure Azure AD App Registration

1. Go to [Azure Portal](https://portal.azure.com) → Azure Active Directory → App registrations
2. Register a new application or use an existing one
3. Add an iOS platform configuration:
   - Bundle ID: `com.lemurs` (or your actual bundle ID)
   - Redirect URI: `msauth.com.lemurs://auth`

### 3. Update Configuration

Edit `iosApp/iosApp/auth_config_claim_auth_ios.json`:

```json
{
    "client_id": "b00e7cc0-f93d-4caf-9f9c-c97d8d6f6a0d",
    "redirect_uri": "msauth.com.lemurs://auth",
    "authority": "https://login.microsoftonline.com/organizations",
    "broker_redirect_uri_registered": false
}
```

Or update the hardcoded values in `MSALAuthHelper.swift`:

```swift
private let clientId = "b00e7cc0-f93d-4caf-9f9c-c97d8d6f6a0d"
private let authority = "https://login.microsoftonline.com/organizations"
private let redirectUri = "msauth.com.lemurs://auth"
```

### 4. Update Info.plist

The `Info.plist` should already contain the necessary URL scheme configuration:

```xml
<key>CFBundleURLTypes</key>
<array>
    <dict>
        <key>CFBundleURLSchemes</key>
        <array>
            <string>msauth.com.lemurs</string>
        </array>
    </dict>
</array>

<key>LSApplicationQueriesSchemes</key>
<array>
    <string>msauthv2</string>
    <string>msauthv3</string>
</array>
```

### 5. Build and Run

1. Open `iosApp.xcworkspace` in Xcode
2. Build the project (Cmd+B)
3. Run on simulator or device

## Files Overview

### Swift Files (iosApp/iosApp/Auth/)

| File | Purpose |
|------|---------|
| `MSALAuthHelper.swift` | Core MSAL wrapper - handles initialization, token acquisition, sign-out |
| `MSALKotlinBridge.swift` | Bridge between Swift and Kotlin - exposes MSAL functionality to Kotlin |

### Kotlin Files (composeApp/src/iosMain/)

| File | Purpose |
|------|---------|
| `IOSMSALBridge.kt` | Interface definition and provider singleton |
| `MicrosoftApiAuthorizationService.ios.kt` | Actual implementation that uses the bridge |

### Configuration Files

| File | Purpose |
|------|---------|
| `auth_config_claim_auth_ios.json` | MSAL configuration (client ID, redirect URI, authority) |
| `Info.plist` | iOS app configuration with URL schemes |
| `Podfile` | CocoaPods dependency declaration |

## Troubleshooting

### "Swift MSAL bridge not available"

Make sure `registerMSALBridgeWithKotlin()` is called in `iOSApp.swift` before `initKoin()`.

### "Could not get root view controller"

The app needs a visible window before authentication can be triggered. Make sure you're not calling `acquireToken()` during app startup before the UI is ready.

### "Config file not found"

Ensure `auth_config_claim_auth_ios.json` is included in the Xcode project and added to the app target.

### "Unresolved reference 'MSAL'"

Run `pod install` and open the `.xcworkspace` file instead of `.xcodeproj`.

## Scopes

The app requests the following scope:
- `api://b00e7cc0-f93d-4caf-9f9c-c97d8d6f6a0d/lemurs`

Update the scopes in `MSALAuthHelper.swift` if needed.
