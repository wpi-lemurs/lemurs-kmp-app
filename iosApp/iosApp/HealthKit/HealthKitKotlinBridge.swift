import Foundation
import HealthKit
import ComposeApp

/// Swift HealthKit bridge that provides health data to Kotlin/Compose
/// This wraps the HealthKit API and exposes it via the IOSHealthKitBridge protocol
@objc public class HealthKitKotlinBridge: NSObject {

    @objc public static let shared = HealthKitKotlinBridge()

    private let healthStore: HKHealthStore?

    /// The types of health data we want to read
    private let typesToRead: Set<HKObjectType> = {
        var types = Set<HKObjectType>()

        if let stepCount = HKQuantityType.quantityType(forIdentifier: .stepCount) {
            types.insert(stepCount)
        }
        if let activeEnergy = HKQuantityType.quantityType(forIdentifier: .activeEnergyBurned) {
            types.insert(activeEnergy)
        }
        if let distance = HKQuantityType.quantityType(forIdentifier: .distanceWalkingRunning) {
            types.insert(distance)
        }
        if let heartRate = HKQuantityType.quantityType(forIdentifier: .heartRate) {
            types.insert(heartRate)
        }
        if let sleep = HKCategoryType.categoryType(forIdentifier: .sleepAnalysis) {
            types.insert(sleep)
        }

        return types
    }()

    private override init() {
        if HKHealthStore.isHealthDataAvailable() {
            self.healthStore = HKHealthStore()
        } else {
            self.healthStore = nil
        }
        super.init()
    }

    // MARK: - Availability Check

    @objc public func isHealthKitAvailable() -> Bool {
        return HKHealthStore.isHealthDataAvailable()
    }

    // MARK: - Authorization

    @objc public func checkAuthorizationStatus(
        onResult: @escaping (Bool) -> Void,
        onError: @escaping (String) -> Void
    ) {
        guard let healthStore = healthStore else {
            onError("HealthKit is not available on this device")
            return
        }

        // Check authorization status for step count as a representative type
        guard let stepType = HKQuantityType.quantityType(forIdentifier: .stepCount) else {
            onError("Could not create step count type")
            return
        }

        let status = healthStore.authorizationStatus(for: stepType)

        // Note: On iOS, we can only check if authorization has been requested,
        // not whether the user granted or denied it (for privacy reasons)
        switch status {
        case .sharingAuthorized:
            onResult(true)
        case .sharingDenied:
            // User explicitly denied
            onResult(false)
        case .notDetermined:
            // Haven't asked yet
            onResult(false)
        @unknown default:
            onResult(false)
        }
    }

    @objc public func requestAuthorization(
        onSuccess: @escaping (Bool) -> Void,
        onError: @escaping (String) -> Void
    ) {
        guard let healthStore = healthStore else {
            onError("HealthKit is not available on this device")
            return
        }

        healthStore.requestAuthorization(toShare: nil, read: typesToRead) { success, error in
            DispatchQueue.main.async {
                if let error = error {
                    onError("Authorization error: \(error.localizedDescription)")
                } else {
                    // Note: 'success' only indicates the authorization dialog was shown,
                    // not that the user actually granted permission
                    onSuccess(success)
                }
            }
        }
    }

    // MARK: - Step Count

    @objc public func getStepCount(
        startTimeMillis: Int64,
        endTimeMillis: Int64,
        onSuccess: @escaping (Int64) -> Void,
        onError: @escaping (String) -> Void
    ) {
        guard let healthStore = healthStore else {
            onError("HealthKit is not available")
            return
        }

        guard let stepType = HKQuantityType.quantityType(forIdentifier: .stepCount) else {
            onError("Could not create step count type")
            return
        }

        let startDate = Date(timeIntervalSince1970: Double(startTimeMillis) / 1000.0)
        let endDate = Date(timeIntervalSince1970: Double(endTimeMillis) / 1000.0)
        let predicate = HKQuery.predicateForSamples(withStart: startDate, end: endDate, options: .strictStartDate)

        let query = HKStatisticsQuery(
            quantityType: stepType,
            quantitySamplePredicate: predicate,
            options: .cumulativeSum
        ) { _, result, error in
            DispatchQueue.main.async {
                if let error = error {
                    onError("Error querying steps: \(error.localizedDescription)")
                    return
                }

                guard let sum = result?.sumQuantity() else {
                    onSuccess(0)
                    return
                }

                let steps = sum.doubleValue(for: HKUnit.count())
                onSuccess(Int64(steps))
            }
        }

        healthStore.execute(query)
    }

    // MARK: - Active Calories

    @objc public func getActiveCaloriesBurned(
        startTimeMillis: Int64,
        endTimeMillis: Int64,
        onSuccess: @escaping (Double) -> Void,
        onError: @escaping (String) -> Void
    ) {
        guard let healthStore = healthStore else {
            onError("HealthKit is not available")
            return
        }

        guard let energyType = HKQuantityType.quantityType(forIdentifier: .activeEnergyBurned) else {
            onError("Could not create energy type")
            return
        }

        let startDate = Date(timeIntervalSince1970: Double(startTimeMillis) / 1000.0)
        let endDate = Date(timeIntervalSince1970: Double(endTimeMillis) / 1000.0)
        let predicate = HKQuery.predicateForSamples(withStart: startDate, end: endDate, options: .strictStartDate)

        let query = HKStatisticsQuery(
            quantityType: energyType,
            quantitySamplePredicate: predicate,
            options: .cumulativeSum
        ) { _, result, error in
            DispatchQueue.main.async {
                if let error = error {
                    onError("Error querying calories: \(error.localizedDescription)")
                    return
                }

                guard let sum = result?.sumQuantity() else {
                    onSuccess(0.0)
                    return
                }

                let calories = sum.doubleValue(for: HKUnit.kilocalorie())
                onSuccess(calories)
            }
        }

        healthStore.execute(query)
    }

    // MARK: - Distance

    @objc public func getDistance(
        startTimeMillis: Int64,
        endTimeMillis: Int64,
        onSuccess: @escaping (Double) -> Void,
        onError: @escaping (String) -> Void
    ) {
        guard let healthStore = healthStore else {
            onError("HealthKit is not available")
            return
        }

        guard let distanceType = HKQuantityType.quantityType(forIdentifier: .distanceWalkingRunning) else {
            onError("Could not create distance type")
            return
        }

        let startDate = Date(timeIntervalSince1970: Double(startTimeMillis) / 1000.0)
        let endDate = Date(timeIntervalSince1970: Double(endTimeMillis) / 1000.0)
        let predicate = HKQuery.predicateForSamples(withStart: startDate, end: endDate, options: .strictStartDate)

        let query = HKStatisticsQuery(
            quantityType: distanceType,
            quantitySamplePredicate: predicate,
            options: .cumulativeSum
        ) { _, result, error in
            DispatchQueue.main.async {
                if let error = error {
                    onError("Error querying distance: \(error.localizedDescription)")
                    return
                }

                guard let sum = result?.sumQuantity() else {
                    onSuccess(0.0)
                    return
                }

                let meters = sum.doubleValue(for: HKUnit.meter())
                onSuccess(meters)
            }
        }

        healthStore.execute(query)
    }

    // MARK: - Heart Rate

    @objc public func getHeartRateSamples(
        startTimeMillis: Int64,
        endTimeMillis: Int64,
        onSuccess: @escaping ([Double]) -> Void,
        onError: @escaping (String) -> Void
    ) {
        guard let healthStore = healthStore else {
            onError("HealthKit is not available")
            return
        }

        guard let heartRateType = HKQuantityType.quantityType(forIdentifier: .heartRate) else {
            onError("Could not create heart rate type")
            return
        }

        let startDate = Date(timeIntervalSince1970: Double(startTimeMillis) / 1000.0)
        let endDate = Date(timeIntervalSince1970: Double(endTimeMillis) / 1000.0)
        let predicate = HKQuery.predicateForSamples(withStart: startDate, end: endDate, options: .strictStartDate)

        let sortDescriptor = NSSortDescriptor(key: HKSampleSortIdentifierStartDate, ascending: true)

        let query = HKSampleQuery(
            sampleType: heartRateType,
            predicate: predicate,
            limit: HKObjectQueryNoLimit,
            sortDescriptors: [sortDescriptor]
        ) { _, samples, error in
            DispatchQueue.main.async {
                if let error = error {
                    onError("Error querying heart rate: \(error.localizedDescription)")
                    return
                }

                guard let quantitySamples = samples as? [HKQuantitySample] else {
                    onSuccess([])
                    return
                }

                let heartRates = quantitySamples.map { sample in
                    sample.quantity.doubleValue(for: HKUnit.count().unitDivided(by: HKUnit.minute()))
                }

                onSuccess(heartRates)
            }
        }

        healthStore.execute(query)
    }

    // MARK: - Sleep Analysis

    @objc public func getSleepAnalysis(
        startTimeMillis: Int64,
        endTimeMillis: Int64,
        onSuccess: @escaping (Int64) -> Void,
        onError: @escaping (String) -> Void
    ) {
        guard let healthStore = healthStore else {
            onError("HealthKit is not available")
            return
        }

        guard let sleepType = HKCategoryType.categoryType(forIdentifier: .sleepAnalysis) else {
            onError("Could not create sleep type")
            return
        }

        let startDate = Date(timeIntervalSince1970: Double(startTimeMillis) / 1000.0)
        let endDate = Date(timeIntervalSince1970: Double(endTimeMillis) / 1000.0)
        let predicate = HKQuery.predicateForSamples(withStart: startDate, end: endDate, options: .strictStartDate)

        let sortDescriptor = NSSortDescriptor(key: HKSampleSortIdentifierStartDate, ascending: true)

        let query = HKSampleQuery(
            sampleType: sleepType,
            predicate: predicate,
            limit: HKObjectQueryNoLimit,
            sortDescriptors: [sortDescriptor]
        ) { _, samples, error in
            DispatchQueue.main.async {
                if let error = error {
                    onError("Error querying sleep: \(error.localizedDescription)")
                    return
                }

                guard let categorySamples = samples as? [HKCategorySample] else {
                    onSuccess(0)
                    return
                }

                // Filter for actual sleep (not just in bed)
                let sleepSamples = categorySamples.filter { sample in
                    // Include asleep states (iOS 16+) or inBed for older iOS
                    if #available(iOS 16.0, *) {
                        let value = HKCategoryValueSleepAnalysis(rawValue: sample.value)
                        return value == .asleepUnspecified ||
                               value == .asleepCore ||
                               value == .asleepDeep ||
                               value == .asleepREM
                    } else {
                        return sample.value == HKCategoryValueSleepAnalysis.asleep.rawValue
                    }
                }

                // Calculate total sleep duration in minutes
                var totalMinutes: Double = 0
                for sample in sleepSamples {
                    let duration = sample.endDate.timeIntervalSince(sample.startDate) / 60.0
                    totalMinutes += duration
                }

                onSuccess(Int64(totalMinutes))
            }
        }

        healthStore.execute(query)
    }

    // MARK: - Background Delivery

    @objc public func enableBackgroundDelivery(
        onSuccess: @escaping () -> Void,
        onError: @escaping (String) -> Void
    ) {
        guard let healthStore = healthStore else {
            onError("HealthKit is not available")
            return
        }

        let group = DispatchGroup()
        var anyError: Error?

        for type in typesToRead {
            guard let sampleType = type as? HKSampleType else { continue }

            group.enter()
            healthStore.enableBackgroundDelivery(for: sampleType, frequency: .hourly) { success, error in
                if let error = error {
                    anyError = error
                }
                group.leave()
            }
        }

        group.notify(queue: .main) {
            if let error = anyError {
                onError("Error enabling background delivery: \(error.localizedDescription)")
            } else {
                onSuccess()
            }
        }
    }
}

// MARK: - Kotlin Bridge Adapter

/// Adapter that conforms to Kotlin's IOSHealthKitBridge protocol
/// This class bridges Swift's HealthKitKotlinBridge to Kotlin's expected interface
public class HealthKitBridgeAdapter: IOSHealthKitBridge {

    public static let shared = HealthKitBridgeAdapter()

    private init() {}

    public func isHealthKitAvailable() -> Bool {
        return HealthKitKotlinBridge.shared.isHealthKitAvailable()
    }

    public func checkAuthorizationStatus(
        onResult: @escaping (KotlinBoolean) -> Void,
        onError: @escaping (String) -> Void
    ) {
        HealthKitKotlinBridge.shared.checkAuthorizationStatus(
            onResult: { isAuthorized in
                onResult(KotlinBoolean(bool: isAuthorized))
            },
            onError: onError
        )
    }

    public func requestAuthorization(
        onSuccess: @escaping (KotlinBoolean) -> Void,
        onError: @escaping (String) -> Void
    ) {
        HealthKitKotlinBridge.shared.requestAuthorization(
            onSuccess: { success in
                onSuccess(KotlinBoolean(bool: success))
            },
            onError: onError
        )
    }

    public func getStepCount(
        startTimeMillis: Int64,
        endTimeMillis: Int64,
        onSuccess: @escaping (KotlinLong) -> Void,
        onError: @escaping (String) -> Void
    ) {
        HealthKitKotlinBridge.shared.getStepCount(
            startTimeMillis: startTimeMillis,
            endTimeMillis: endTimeMillis,
            onSuccess: { steps in
                onSuccess(KotlinLong(value: steps))
            },
            onError: onError
        )
    }

    public func getActiveCaloriesBurned(
        startTimeMillis: Int64,
        endTimeMillis: Int64,
        onSuccess: @escaping (KotlinDouble) -> Void,
        onError: @escaping (String) -> Void
    ) {
        HealthKitKotlinBridge.shared.getActiveCaloriesBurned(
            startTimeMillis: startTimeMillis,
            endTimeMillis: endTimeMillis,
            onSuccess: { calories in
                onSuccess(KotlinDouble(value: calories))
            },
            onError: onError
        )
    }

    public func getDistance(
        startTimeMillis: Int64,
        endTimeMillis: Int64,
        onSuccess: @escaping (KotlinDouble) -> Void,
        onError: @escaping (String) -> Void
    ) {
        HealthKitKotlinBridge.shared.getDistance(
            startTimeMillis: startTimeMillis,
            endTimeMillis: endTimeMillis,
            onSuccess: { distance in
                onSuccess(KotlinDouble(value: distance))
            },
            onError: onError
        )
    }

    public func getHeartRateSamples(
        startTimeMillis: Int64,
        endTimeMillis: Int64,
        onSuccess: @escaping ([KotlinDouble]) -> Void,
        onError: @escaping (String) -> Void
    ) {
        HealthKitKotlinBridge.shared.getHeartRateSamples(
            startTimeMillis: startTimeMillis,
            endTimeMillis: endTimeMillis,
            onSuccess: { rates in
                let kotlinRates = rates.map { KotlinDouble(value: $0) }
                onSuccess(kotlinRates)
            },
            onError: onError
        )
    }

    public func getSleepAnalysis(
        startTimeMillis: Int64,
        endTimeMillis: Int64,
        onSuccess: @escaping (KotlinLong) -> Void,
        onError: @escaping (String) -> Void
    ) {
        HealthKitKotlinBridge.shared.getSleepAnalysis(
            startTimeMillis: startTimeMillis,
            endTimeMillis: endTimeMillis,
            onSuccess: { minutes in
                onSuccess(KotlinLong(value: minutes))
            },
            onError: onError
        )
    }

    public func enableBackgroundDelivery(
        onSuccess: @escaping () -> Void,
        onError: @escaping (String) -> Void
    ) {
        HealthKitKotlinBridge.shared.enableBackgroundDelivery(
            onSuccess: onSuccess,
            onError: onError
        )
    }
}

// MARK: - Registration Function

/// Extension to register the HealthKit bridge with Kotlin
/// Call this during app initialization
public func registerHealthKitBridgeWithKotlin() {
    IOSHealthKitBridgeProvider.shared.bridge = HealthKitBridgeAdapter.shared
}
