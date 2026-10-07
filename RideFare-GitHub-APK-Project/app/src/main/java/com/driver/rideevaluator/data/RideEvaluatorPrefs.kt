package com.driver.rideevaluator.data

import android.content.Context
import android.content.SharedPreferences

class RideEvaluatorPrefs(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "ride_evaluator_settings",
        Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_SERVICE_ENABLED = "key_service_enabled"
        private const val KEY_MIN_PER_KM_RATE = "key_min_per_km_rate"
        private const val KEY_MAX_PICKUP_KM = "key_max_pickup_km"
        private const val KEY_MIN_TOTAL_FARE = "key_min_total_fare"

        const val DEFAULT_MIN_PER_KM_RATE = 15.0f
        const val DEFAULT_MAX_PICKUP_KM = 3.0f
        const val DEFAULT_MIN_TOTAL_FARE = 50.0f
    }

    var isServiceEnabled: Boolean
        get() = prefs.getBoolean(KEY_SERVICE_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_SERVICE_ENABLED, value).apply()

    var minPerKmRate: Float
        get() = prefs.getFloat(KEY_MIN_PER_KM_RATE, DEFAULT_MIN_PER_KM_RATE)
        set(value) = prefs.edit().putFloat(KEY_MIN_PER_KM_RATE, value).apply()

    var maxPickupDistanceKm: Float
        get() = prefs.getFloat(KEY_MAX_PICKUP_KM, DEFAULT_MAX_PICKUP_KM)
        set(value) = prefs.edit().putFloat(KEY_MAX_PICKUP_KM, value).apply()

    var minTotalFare: Float
        get() = prefs.getFloat(KEY_MIN_TOTAL_FARE, DEFAULT_MIN_TOTAL_FARE)
        set(value) = prefs.edit().putFloat(KEY_MIN_TOTAL_FARE, value).apply()
}
