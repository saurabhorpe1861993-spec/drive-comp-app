package com.driver.rideevaluator.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import com.driver.rideevaluator.R
import com.driver.rideevaluator.parser.RideMetrics

class OverlayService : Service() {

    companion object {
        const val ACTION_SHOW = "com.driver.rideevaluator.ACTION_SHOW_OVERLAY"
        const val ACTION_HIDE = "com.driver.rideevaluator.ACTION_HIDE_OVERLAY"
        const val EXTRA_METRICS = "EXTRA_METRICS"

        fun showOverlay(context: Context, metrics: RideMetrics) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                return
            }
            val intent = Intent(context, OverlayService::class.java).apply {
                action = ACTION_SHOW
                putExtra(EXTRA_METRICS, metrics)
            }
            context.startService(intent)
        }

        fun hideOverlay(context: Context) {
            val intent = Intent(context, OverlayService::class.java).apply {
                action = ACTION_HIDE
            }
            context.startService(intent)
        }
    }

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private val autoDismissHandler = Handler(Looper.getMainLooper())
    private val autoDismissRunnable = Runnable { removeOverlay() }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW -> {
                val metrics = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_METRICS, RideMetrics::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_METRICS)
                }
                if (metrics != null) displayOverlay(metrics)
            }
            ACTION_HIDE -> removeOverlay()
        }
        return START_NOT_STICKY
    }

    private fun displayOverlay(metrics: RideMetrics) {
        autoDismissHandler.removeCallbacks(autoDismissRunnable)
        autoDismissHandler.postDelayed(autoDismissRunnable, 10000L)

        if (overlayView == null) {
            initOverlayView()
        }
        updateOverlayUI(metrics)
    }

    private fun initOverlayView() {
        val inflater = LayoutInflater.from(this)
        overlayView = inflater.inflate(R.layout.layout_ride_overlay, null)

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        overlayView?.findViewById<Button>(R.id.btn_accept_ride_overlay)?.setOnClickListener {
            RideMonitorService.acceptCurrentRideOnScreen(this)
            removeOverlay()
        }

        overlayView?.findViewById<ImageButton>(R.id.btn_close_overlay)?.setOnClickListener {
            removeOverlay()
        }

        try {
            windowManager?.addView(overlayView, layoutParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateOverlayUI(metrics: RideMetrics) {
        val view = overlayView ?: return
        val tvStatus = view.findViewById<TextView>(R.id.tv_evaluation_status)
        val tvTotalFare = view.findViewById<TextView>(R.id.tv_total_fare)
        val tvPerKmRate = view.findViewById<TextView>(R.id.tv_per_km_rate)
        val tvPickupKm = view.findViewById<TextView>(R.id.tv_pickup_km)
        val tvDropKm = view.findViewById<TextView>(R.id.tv_drop_km)
        val btnAccept = view.findViewById<Button>(R.id.btn_accept_ride_overlay)

        if (metrics.isGoodRide) {
            tvStatus.text = "PROFITABLE RIDE"
            tvStatus.setTextColor(Color.parseColor("#10B981"))
            tvPerKmRate.setTextColor(Color.parseColor("#34D399"))
            btnAccept.setBackgroundColor(Color.parseColor("#10B981"))
        } else {
            tvStatus.text = "SUB-PAR RIDE"
            tvStatus.setTextColor(Color.parseColor("#EF4444"))
            tvPerKmRate.setTextColor(Color.parseColor("#F87171"))
            btnAccept.setBackgroundColor(Color.parseColor("#059669"))
        }

        tvTotalFare.text = "₹${metrics.totalFare.toInt()}"
        tvPerKmRate.text = String.format("₹%.1f / KM", metrics.actualPerKmRate)
        tvPickupKm.text = "${metrics.pickupDistanceKm} KM"
        tvDropKm.text = "${metrics.dropoffDistanceKm} KM"
    }

    private fun removeOverlay() {
        autoDismissHandler.removeCallbacks(autoDismissRunnable)
        if (overlayView != null) {
            try {
                windowManager?.removeView(overlayView)
            } catch (ignored: Exception) {}
            overlayView = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        removeOverlay()
    }
}
