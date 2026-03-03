//
//  AppSelectionView.swift
//  iosApp
//
//  Screen Time App Selection for TestFlight
//

import SwiftUI
import FamilyControls

@available(iOS 16.0, *)
struct AppSelectionView: View {
    @State private var selection = FamilyActivitySelection()
    @State private var isPresented = false
    @State private var selectionSaved = false
    @State private var appCount = 0
    @Environment(\.dismiss) var dismiss

    var body: some View {
        let _ = print("🔍 View refresh - appCount: \(appCount), tokens: \(selection.applicationTokens.count)")

        VStack(spacing: 24) {
            // Header
            VStack(spacing: 12) {
                Image(systemName: "clock.fill")
                    .font(.system(size: 60))
                    .foregroundColor(.blue)

                Text("Track Your Phone Usage")
                    .font(.title)
                    .fontWeight(.bold)

                VStack(spacing: 8) {
                    Text("Select your most-used apps to track usage time")
                        .font(.body)
                        .fontWeight(.medium)
                        .multilineTextAlignment(.center)
                        .foregroundColor(.primary)

                    Text("The more apps you select, the more accurate the tracking (NOTE: Do not select 'All Apps' - only the ones you use most)")
                        .font(.caption)
                        .multilineTextAlignment(.center)
                        .foregroundColor(.secondary)

                    HStack(spacing: 16) {
                        Label("Social", systemImage: "person.2")
                        Label("Messages", systemImage: "message")
                        Label("Games", systemImage: "gamecontroller")
                    }
                    .font(.caption)
                    .foregroundColor(.blue)
                }
                .padding(.horizontal)
            }
            .padding(.top, 40)

            Spacer()

            // Selection Status
            VStack(spacing: 12) {
                if appCount == 0 {
                    HStack {
                        Image(systemName: "exclamationmark.triangle.fill")
                            .foregroundColor(.orange)
                        Text("No apps selected yet")
                            .foregroundColor(.secondary)
                    }
                } else {
                    HStack {
                        Image(systemName: "checkmark.circle.fill")
                            .foregroundColor(.green)
                        Text("\(appCount) app(s) selected")
                            .foregroundColor(.primary)
                    }
                }

                if selectionSaved {
                    HStack {
                        Image(systemName: "checkmark.circle.fill")
                            .foregroundColor(.green)
                        Text("Selection saved successfully!")
                            .foregroundColor(.green)
                    }
                    .padding()
                    .background(Color.green.opacity(0.1))
                    .cornerRadius(8)
                }
            }

            Spacer()

            // Buttons
            VStack(spacing: 16) {
                Button {
                    print("🔵 Select Apps tapped")
                    isPresented = true
                } label: {
                    HStack {
                        Image(systemName: "square.grid.2x2")
                        Text("Select Apps")
                    }
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(Color.blue)
                    .foregroundColor(.white)
                    .cornerRadius(12)
                }
                .familyActivityPicker(isPresented: $isPresented, selection: $selection)
                .onChange(of: isPresented) { oldValue, newValue in
                    if !newValue {
                        // Picker was dismissed
                        let count = selection.applicationTokens.count
                        print("🔍 Picker dismissed - selection count: \(count)")
                        appCount = count
                    }
                }

                if appCount > 0 {
                    // Save button - appears when apps selected
                    Button {
                        print("🟢 Save button tapped - appCount: \(appCount)")
                        saveSelection()
                    } label: {
                        HStack {
                            Image(systemName: "checkmark")
                            Text("Save \(appCount) Apps & Start Tracking")
                        }
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color.green)
                        .foregroundColor(.white)
                        .cornerRadius(12)
                    }
                } else {
                    // Encourage selection with info
                    VStack(spacing: 8) {
                        Text("⏱️ Select 10-20 apps to track usage time")
                            .font(.callout)
                            .fontWeight(.medium)
                        Text("Without app selection, we can only log timestamps")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                    .padding()
                    .background(Color.orange.opacity(0.1))
                    .cornerRadius(8)
                }

                // Skip button - but explain what they lose
                Button {
                    print("ℹ️ User skipped app selection")
                    skipAppSelection()
                } label: {
                    VStack(spacing: 4) {
                        Text("Skip (Not Recommended)")
                            .font(.body)
                        Text("Will NOT track usage time - only timestamps")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(Color.gray.opacity(0.2))
                    .foregroundColor(.primary)
                    .cornerRadius(12)
                }

                if selectionSaved {
                    Button {
                        dismiss()
                    } label: {
                        Text("Continue")
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(Color.gray.opacity(0.2))
                            .foregroundColor(.primary)
                            .cornerRadius(12)
                    }
                }
            }
            .padding(.horizontal, 24)
            .padding(.bottom, 40)
        }
    }

    private func saveSelection() {
        print("✅ Saving app selection: \(selection.applicationTokens.count) apps")

        // Save selection count to UserDefaults
        UserDefaults.standard.set(selection.applicationTokens.count, forKey: "screentime_app_selection_count")
        UserDefaults.standard.set(true, forKey: "screentime_app_selection_completed")

        // Apply selection to monitoring immediately
        ScreenTimeTaskScheduler.shared.applyAppSelection(selection)

        selectionSaved = true

        // Auto-dismiss after 1.5 seconds
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) {
            dismiss()
        }
    }

    private func skipAppSelection() {
        print("ℹ️ Skipping app selection - will use interval-only tracking")
        print("ℹ️ 4 interval callbacks per day will still provide usage data")

        // Mark as completed (even though skipped) so we don't keep prompting
        UserDefaults.standard.set(0, forKey: "screentime_app_selection_count")
        UserDefaults.standard.set(true, forKey: "screentime_app_selection_completed")

        // No need to apply selection - interval monitoring already running from initial setup

        selectionSaved = true

        // Auto-dismiss after 1 second
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
            dismiss()
        }
    }
}

@available(iOS 16.0, *)
struct AppSelectionView_Previews: PreviewProvider {
    static var previews: some View {
        AppSelectionView()
    }
}

