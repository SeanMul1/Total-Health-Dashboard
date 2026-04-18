//package com.totalhealthdashboard.ui.physical
//
//import android.content.Context
//import android.os.Handler
//import android.os.Looper
//import android.util.Log
//import androidx.health.connect.client.HealthConnectClient
//import androidx.health.connect.client.permission.HealthPermission
//import androidx.health.connect.client.records.*
//import androidx.health.connect.client.request.ReadRecordsRequest
//import androidx.health.connect.client.time.TimeRangeFilter
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//import java.time.Instant
//import java.time.LocalDate
//import java.time.ZoneId
//
//object HealthConnectHelper {
//
//    private const val TAG = "HEALTH_CONNECT"
//    private val mainHandler = Handler(Looper.getMainLooper())
//
//    @JvmField
//    val PERMISSIONS = setOf(
//        HealthPermission.getReadPermission(StepsRecord::class),
//        HealthPermission.getReadPermission(HeartRateRecord::class),
//        HealthPermission.getReadPermission(SleepSessionRecord::class),
//        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
//        HealthPermission.getReadPermission(DistanceRecord::class),
//        HealthPermission.getReadPermission(FloorsClimbedRecord::class)
//    )
//
//    interface Callback {
//        fun onData(steps: Int, distanceKm: Double, calories: Int,
//                   heartRate: Int, sleepHours: Double, floors: Int)
//        fun onError(message: String)
//        fun onPermissionRequired()
//    }
//
//    @JvmStatic
//    fun isAvailable(context: Context): Boolean =
//        HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
//
//    @JvmStatic
//    fun checkPermissionsAndRead(context: Context, callback: Callback) {
//        if (!isAvailable(context)) {
//            mainHandler.post { callback.onError("Health Connect not available on this device") }
//            return
//        }
//        CoroutineScope(Dispatchers.IO).launch {
//            try {
//                val client = HealthConnectClient.getOrCreate(context)
//                val granted = client.permissionController.getGrantedPermissions()
//                if (granted.containsAll(PERMISSIONS)) {
//                    readDataInternal(client, callback)
//                } else {
//                    mainHandler.post { callback.onPermissionRequired() }
//                }
//            } catch (e: Exception) {
//                Log.e(TAG, "Permission check error: ${e.message}")
//                mainHandler.post { callback.onError("Permission check failed: ${e.message}") }
//            }
//        }
//    }
//
//    @JvmStatic
//    fun readData(context: Context, callback: Callback) {
//        if (!isAvailable(context)) {
//            mainHandler.post { callback.onError("Health Connect not available") }
//            return
//        }
//        CoroutineScope(Dispatchers.IO).launch {
//            try {
//                val client = HealthConnectClient.getOrCreate(context)
//                readDataInternal(client, callback)
//            } catch (e: Exception) {
//                Log.e(TAG, "Read error: ${e.message}")
//                mainHandler.post { callback.onError("Read failed: ${e.message}") }
//            }
//        }
//    }
//
//    private suspend fun readDataInternal(client: HealthConnectClient, callback: Callback) {
//        val startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()
//        val now = Instant.now()
//        val yesterday = LocalDate.now().minusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
//        val todayRange = TimeRangeFilter.between(startOfDay, now)
//        val sleepRange = TimeRangeFilter.between(yesterday, now)
//
//        try {
//            val steps = client.readRecords(ReadRecordsRequest(StepsRecord::class, todayRange))
//                .records.sumOf { it.count }.toInt()
//            Log.d(TAG, "Steps: $steps")
//
//            val distanceKm = Math.round(
//                client.readRecords(ReadRecordsRequest(DistanceRecord::class, todayRange))
//                    .records.sumOf { it.distance.inMeters } / 1000.0 * 10.0) / 10.0
//            Log.d(TAG, "Distance: $distanceKm km")
//
//            val calories = client.readRecords(
//                ReadRecordsRequest(ActiveCaloriesBurnedRecord::class, todayRange))
//                .records.sumOf { it.energy.inKilocalories }.toInt()
//            Log.d(TAG, "Calories: $calories")
//
//            val floors = client.readRecords(
//                ReadRecordsRequest(FloorsClimbedRecord::class, todayRange))
//                .records.sumOf { it.floors }.toInt()
//            Log.d(TAG, "Floors: $floors")
//
//            val hrSamples = client.readRecords(
//                ReadRecordsRequest(HeartRateRecord::class, todayRange))
//                .records.flatMap { it.samples }
//            val heartRate = if (hrSamples.isNotEmpty())
//                hrSamples.map { it.beatsPerMinute }.average().toInt() else 0
//            Log.d(TAG, "Heart rate: $heartRate")
//
//            val sleepMins = client.readRecords(
//                ReadRecordsRequest(SleepSessionRecord::class, sleepRange))
//                .records.sumOf {
//                    (it.endTime.toEpochMilli() - it.startTime.toEpochMilli()) / 60000
//                }
//            val sleepHours = Math.round(sleepMins / 60.0 * 10.0) / 10.0
//            Log.d(TAG, "Sleep: $sleepHours hours")
//
//            mainHandler.post {
//                callback.onData(steps, distanceKm, calories, heartRate, sleepHours, floors)
//            }
//        } catch (e: Exception) {
//            Log.e(TAG, "Data read error: ${e.message}")
//            mainHandler.post { callback.onError("Failed to read: ${e.message}") }
//        }
//    }
//}