package com.driver.rideevaluator.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.driver.rideevaluator.data.RideEvaluatorPrefs
import com.driver.rideevaluator.parser.RideOfferParser
import com.driver.rideevaluator.parser.RideMetrics

class RideMonitorService : AccessibilityService() {

    companion object {
        private const val TAG = "RideMonitorService"
        
        var instance: RideMonitorService? = null
            private set

        val TARGET_PACKAGES = setOf(
            "com.ubercab.driver",
            "com.rapido.rider",
            "com.rapido.captain",
            "com.olacabs.partner",
            "com.olacabs.driver"
        )

        fun acceptCurrentRideOnScreen(context: Context): Boolean {
            val service = instance ?: return false
            val rootNode = service.rootInActiveWindow ?: return false
            try {
                return service.findAndClickAcceptNode(rootNode)
            } finally {
                rootNode.recycle()
            }
        }
    }

    private lateinit var prefs: RideEvaluatorPrefs
    private var lastOfferSignature: String? = null
    private var lastEventTimestamp: Long = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        prefs = RideEvaluatorPrefs(applicationContext)
        Log.i(TAG, "RideMonitorService connected.")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || !prefs.isServiceEnabled) return

        val eventType = event.eventType
        if (eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
            eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        ) return

        val now = System.currentTimeMillis()
        if (now - lastEventTimestamp < 250L) return
        lastEventTimestamp = now

        val pkg = event.packageName?.toString() ?: ""
        if (pkg.isNotEmpty() && !TARGET_PACKAGES.contains(pkg) && !pkg.contains("rideevaluator")) {
            return
        }

        val rootNode = rootInActiveWindow ?: return
        try {
            processActiveWindow(rootNode)
        } finally {
            rootNode.recycle()
        }
    }

    private fun processActiveWindow(rootNode: AccessibilityNodeInfo) {
        val collectedTexts = mutableListOf<String>()
        traverseNodeTree(rootNode, collectedTexts)

        if (collectedTexts.isEmpty()) return

        val parsedOffer = RideOfferParser.parseTexts(collectedTexts)

        if (parsedOffer != null) {
            val signature = "${parsedOffer.pickupDistanceKm}_${parsedOffer.dropoffDistanceKm}_${parsedOffer.totalFare}"
            if (signature != lastOfferSignature) {
                lastOfferSignature = signature
                val evaluation = evaluateOffer(parsedOffer)
                OverlayService.showOverlay(applicationContext, evaluation)
            }
        } else {
            lastOfferSignature = null
        }
    }

    private fun traverseNodeTree(node: AccessibilityNodeInfo?, output: MutableList<String>) {
        if (node == null || !node.isVisibleToUser) return

        node.text?.let { text ->
            val str = text.toString().trim()
            if (str.isNotEmpty()) output.add(str)
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                traverseNodeTree(child, output)
                child.recycle()
            }
        }
    }

    private fun findAndClickAcceptNode(rootNode: AccessibilityNodeInfo): Boolean {
        val acceptKeywords = listOf("Accept", "ACCEPT", "Accept Ride", "Tap to accept")
        for (keyword in acceptKeywords) {
            val nodes = rootNode.findAccessibilityNodeInfosByText(keyword)
            if (!nodes.isNullOrEmpty()) {
                for (node in nodes) {
                    if (node.isClickable) {
                        val success = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        node.recycle()
                        if (success) return true
                    }
                    var parent = node.parent
                    while (parent != null) {
                        if (parent.isClickable) {
                            val success = parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                            parent.recycle()
                            node.recycle()
                            if (success) return true
                        }
                        val nextParent = parent.parent
                        parent.recycle()
                        parent = nextParent
                    }
                    node.recycle()
                }
            }
        }
        return false
    }

    private fun evaluateOffer(offer: RideOfferParser.RawRideOffer): RideMetrics {
        val totalDistance = offer.pickupDistanceKm + offer.dropoffDistanceKm
        val perKmRate = if (totalDistance > 0.05) offer.totalFare / totalDistance else 0.0

        val rateMatches = perKmRate >= prefs.minPerKmRate
        val pickupMatches = offer.pickupDistanceKm <= prefs.maxPickupDistanceKm
        val fareMatches = offer.totalFare >= prefs.minTotalFare
        val isGoodRide = rateMatches && pickupMatches && fareMatches

        return RideMetrics(
            pickupDistanceKm = offer.pickupDistanceKm,
            dropoffDistanceKm = offer.dropoffDistanceKm,
            totalDistanceKm = totalDistance,
            totalFare = offer.totalFare,
            actualPerKmRate = perKmRate,
            isGoodRide = isGoodRide,
            rateMatches = rateMatches,
            pickupMatches = pickupMatches,
            fareMatches = fareMatches,
            minTargetRate = prefs.minPerKmRate,
            maxAllowedPickup = prefs.maxPickupDistanceKm,
            minTargetFare = prefs.minTotalFare
        )
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }
}
