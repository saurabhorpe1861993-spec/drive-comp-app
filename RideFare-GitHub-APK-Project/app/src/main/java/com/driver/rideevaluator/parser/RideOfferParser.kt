package com.driver.rideevaluator.parser

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.regex.Pattern

@Parcelize
data class RideMetrics(
    val pickupDistanceKm: Double,
    val dropoffDistanceKm: Double,
    val totalDistanceKm: Double,
    val totalFare: Double,
    val actualPerKmRate: Double,
    val isGoodRide: Boolean,
    val rateMatches: Boolean,
    val pickupMatches: Boolean,
    val fareMatches: Boolean,
    val minTargetRate: Float,
    val maxAllowedPickup: Float,
    val minTargetFare: Float
) : Parcelable

object RideOfferParser {

    data class RawRideOffer(
        val pickupDistanceKm: Double,
        val dropoffDistanceKm: Double,
        val totalFare: Double
    )

    private val FARE_PATTERN_1 = Pattern.compile("(?:₹|rs\.?|inr)\s*(\d+(?:\.\d+)?)", Pattern.CASE_INSENSITIVE)
    private val FARE_PATTERN_2 = Pattern.compile("(\d+(?:\.\d+)?)\s*(?:₹|rs\.?)", Pattern.CASE_INSENSITIVE)
    private val PICKUP_PATTERN = Pattern.compile("(?:pickup|pick\s*up|away|to\s*pickup)\s*[:\-]?\s*(\d+(?:\.\d+)?)\s*(?:km|kms)?", Pattern.CASE_INSENSITIVE)
    private val DROPOFF_PATTERN = Pattern.compile("(?:drop|dropoff|drop\s*off|trip|destination)\s*[:\-]?\s*(\d+(?:\.\d+)?)\s*(?:km|kms)?", Pattern.CASE_INSENSITIVE)
    private val STANDALONE_KM_PATTERN = Pattern.compile("(\d+(?:\.\d+)?)\s*(?:km|kms)", Pattern.CASE_INSENSITIVE)

    fun parseTexts(texts: List<String>): RawRideOffer? {
        val combinedText = texts.joinToString(" 
 ")

        var fare: Double? = null
        var pickupKm: Double? = null
        var dropoffKm: Double? = null

        val mFare1 = FARE_PATTERN_1.matcher(combinedText)
        if (mFare1.find()) {
            fare = mFare1.group(1)?.toDoubleOrNull()
        } else {
            val mFare2 = FARE_PATTERN_2.matcher(combinedText)
            if (mFare2.find()) fare = mFare2.group(1)?.toDoubleOrNull()
        }

        val mPickup = PICKUP_PATTERN.matcher(combinedText)
        if (mPickup.find()) pickupKm = mPickup.group(1)?.toDoubleOrNull()

        val mDrop = DROPOFF_PATTERN.matcher(combinedText)
        if (mDrop.find()) dropoffKm = mDrop.group(1)?.toDoubleOrNull()

        if (pickupKm == null || dropoffKm == null) {
            val allKmMatches = mutableListOf<Double>()
            val mKm = STANDALONE_KM_PATTERN.matcher(combinedText)
            while (mKm.find()) {
                mKm.group(1)?.toDoubleOrNull()?.let { allKmMatches.add(it) }
            }
            if (allKmMatches.size >= 2) {
                if (pickupKm == null) pickupKm = allKmMatches[0]
                if (dropoffKm == null) dropoffKm = allKmMatches[1]
            } else if (allKmMatches.size == 1) {
                if (dropoffKm == null) dropoffKm = allKmMatches[0]
                if (pickupKm == null) pickupKm = 1.0
            }
        }

        if (fare != null && fare > 0 && dropoffKm != null && dropoffKm > 0) {
            return RawRideOffer(
                pickupDistanceKm = pickupKm ?: 1.0,
                dropoffDistanceKm = dropoffKm,
                totalFare = fare
            )
        }
        return null
    }
}
