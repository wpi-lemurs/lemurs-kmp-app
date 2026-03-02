# DeviceActivityMonitor Extension Setup Instructions

## Files Created

✅ **DeviceActivityMonitorExtension.swift** - Extension implementation that receives system callbacks  
✅ **Info.plist** - Extension configuration  
✅ **ScreenTimeExtension.entitlements** - Required capabilities  
✅ **ScreenTimeBridge.swift** - Updated to read from extension's shared container

## Xcode Setup Required

The extension files have been created, but you **must manually add the extension target in Xcode**. Follow these steps:

### Step 1: Open Xcode

```bash
cd iosApp
open iosApp.xcworkspace  # Important: open .xcworkspace, not .xcodeproj
```

### Step 2: Add Extension Target

1. In Xcode, select the **iosApp project** in the navigator (top-level)
2. At the bottom of the targets list, click the **+** button
3. In the template chooser:
   - Filter: Search for "extension"
   - Select: **"App Extension"** template
   - Click **Next**

4. Choose template type:
   - Select **"Device Activity Monitor Extension"**
   - Click **Next**

5. Configure:
   - **Product Name:** `ScreenTimeExtension`
   - **Bundle Identifier:** Will auto-populate based on app ID (should be like `com.lemurs.v1.dev.ScreenTimeExtension` for Debug)
   - **Team:** Your Apple Developer Team
   - Click **Finish**

6. When prompted "Activate scheme?":
   - Click **Cancel** (we'll use the main app scheme)

### Step 3: Replace Template Files

Xcode created template files. Replace them with our implementation:

1. In Project Navigator, find the **ScreenTimeExtension** folder
2. **Delete** these auto-generated files:
   - `DeviceActivityMonitorExtension.swift` (Xcode's template)
   - `Info.plist` (if it's a different one)

3. **Add our files**:
   - Right-click **ScreenTimeExtension** folder
   - Select **Add Files to "iosApp"...**
   - Navigate to: `iosApp/ScreenTimeExtension/`
   - Select all files:
     - `DeviceActivityMonitorExtension.swift`
     - `Info.plist`
     - `ScreenTimeExtension.entitlements`
   - **Important:** Check "Copy items if needed"
   - **Important:** Under "Add to targets", select **ScreenTimeExtension**
   - Click **Add**

### Step 4: Configure Extension Target

Select the **ScreenTimeExtension** target:

#### A. General Tab
- **Deployment Info:**
  - iOS Deployment Target: **16.0** (DeviceActivity requires iOS 16+)
  
- **Bundle Identifier:**
  - Debug: `com.lemurs.v1.dev.ScreenTimeExtension`
  - Release: `com.lemurs.v1.ScreenTimeExtension`
  - Must match pattern: `[MainAppBundleID].ScreenTimeExtension`

#### B. Signing & Capabilities Tab

1. **Automatic Signing:** Enable (or use manual with proper profiles)
2. **Team:** Select your paid Apple Developer team
3. **Add Capabilities:**
   - Click **+ Capability**
   - Add **App Groups**:
     - Check: `group.com.lemurs.lemurs-app`
   - Add **Family Controls**:
     - Will be automatically added

4. **Verify Entitlements:**
   - Ensure `ScreenTimeExtension.entitlements` is selected
   - Should contain:
     - Family Controls: ✅
     - App Groups: `group.com.lemurs.lemurs-app`

#### C. Build Settings Tab
- **Info.plist File:** `ScreenTimeExtension/Info.plist`
- **Code Signing Entitlements:** `ScreenTimeExtension/ScreenTimeExtension.entitlements`

### Step 5: Configure Main App Target

Select the **iosApp** (or main) target:

#### A. Signing & Capabilities

1. **Add App Groups** (if not already added):
   - Click **+ Capability**
   - Add **App Groups**
   - Check: `group.com.lemurs.lemurs-app`

2. **Verify Entitlements:**
   - File: `iosApp/iosApp/iosApp.entitlements`
   - Should contain:
     - Family Controls: ✅
     - App Groups: `group.com.lemurs.lemurs-app`

#### B. Build Phases

1. Go to **Build Phases** tab
2. Expand **Embed Foundation Extensions** (or create if missing)
3. Click **+**
4. Add `ScreenTimeExtension.appex`
5. Ensure "Code Sign On Copy" is checked

### Step 6: Configure Apple Developer Portal

You **must** configure App Groups in the Developer Portal:

1. Go to [developer.apple.com/account](https://developer.apple.com/account/)
2. Navigate to **Certificates, Identifiers & Profiles**

#### For Main App Bundle ID

1. Find: `com.lemurs.v1.dev` (Debug) or `com.lemurs.v1` (Release)
2. Click to edit
3. Enable **App Groups**
4. Select/Create: `group.com.lemurs.lemurs-app`
5. Enable **Family Controls**
6. Save

#### For Extension Bundle ID

1. If not exists, create new App ID:
   - Bundle ID: `com.lemurs.v1.dev.ScreenTimeExtension`
   - Description: "Screen Time Extension Debug"
2. Enable **App Groups**: `group.com.lemurs.lemurs-app`
3. Enable **Family Controls**
4. Save

5. Repeat for Release if needed: `com.lemurs.v1.ScreenTimeExtension`

#### Create/Update Provisioning Profiles

1. Go to **Profiles** section
2. Create new or regenerate:
   - **Main App Profile**: With App Groups + Family Controls
   - **Extension Profile**: With App Groups + Family Controls
3. Download profiles
4. In Xcode: Preferences > Accounts > Download Manual Profiles

### Step 7: Build and Test

```bash
# Clean build
Product > Clean Build Folder (Cmd+Shift+K)

# Build
Product > Build (Cmd+B)

# Verify extension is embedded
# After build, check: Build/Products/Debug-iphoneos/LemursApp.app/PlugIns/
# Should contain: ScreenTimeExtension.appex
```

## How It Works

```
Main App → Requests Authorization → Starts DeviceActivity Monitoring
                                           ↓
                                    System Callbacks
                                           ↓
Extension → Receives Events → Saves to Shared Container (App Group)
                                           ↓
Main App → Reads Events → Converts to Screentime → Database → API
```

## What Data Gets Collected

The extension logs:
- **Interval starts/ends** - Daily monitoring periods
- **Threshold events** - When usage reaches 15/30/60 minutes
- **Activity names** - Monitoring activity identifiers
- **Timestamps** - When each event occurred

**Note:** You still cannot get per-app usage like Android. The API only provides:
- Aggregate threshold notifications
- Interval boundaries
- Estimated usage based on thresholds reached

## Testing

1. **Run on physical device** (iOS 16+)
2. **Grant authorization** when prompted
3. **Check console** for:
   ```
   ✅ Screen Time authorization granted
   ✅ Started DeviceActivity monitoring with extension
   ```
4. **Use device** normally for 15+ minutes
5. **Check logs** from extension:
   ```swift
   let containerURL = FileManager.default.containerURL(
       forSecurityApplicationGroupIdentifier: "group.com.lemurs.lemurs-app"
   )
   let fileURL = containerURL!.appendingPathComponent("screentime_events.json")
   let data = try! Data(contentsOf: fileURL)
   print(String(data: data, encoding: .utf8)!)
   ```

## Troubleshooting

**"Extension not embedded":**
- Check Build Phases > Embed Foundation Extensions
- Ensure ScreenTimeExtension.appex is listed

**"Cannot access shared container":**
- Verify App Group ID matches in both entitlements
- Regenerate provisioning profiles
- Clean build folder

**"Authorization fails":**
- Must use paid Apple Developer account
- Verify Family Controls in both targets
- Check provisioning profiles include capabilities

**"Extension never receives callbacks":**
- Monitoring only works on physical devices (iOS 16+)
- Wait for interval boundaries (midnight) or threshold events
- Check authorization status in app

## Summary

✅ Extension files created  
⚠️ **Manual Xcode setup required** (cannot be automated)  
⚠️ **Apple Developer Portal configuration required**  
⚠️ **Paid Apple Developer account required**  
⚠️ **Physical device required** for testing  
⚠️ **Still limited data** compared to Android  

The extension is the **best possible iOS solution**, but still has significant limitations due to Apple's API design.

