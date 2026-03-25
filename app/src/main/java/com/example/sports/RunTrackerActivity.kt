package com.example.sports

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.util.Locale

class RunTrackerActivity : AppCompatActivity(), LocationListener {

    private lateinit var locationManager: LocationManager
    private lateinit var routePreview: RoutePreviewView
    private lateinit var indoorPreviewText: TextView
    private lateinit var runModeValue: TextView
    private lateinit var statusView: TextView
    private lateinit var distanceView: TextView
    private lateinit var durationView: TextView
    private lateinit var positionView: TextView
    private lateinit var indoorConfigSection: View
    private lateinit var indoorSpeedInput: EditText
    private lateinit var indoorInclineInput: EditText
    private val routePoints = mutableListOf<RoutePreviewView.Point>()
    private val uiHandler = Handler(Looper.getMainLooper())
    private val ticker = object : Runnable {
        override fun run() {
            if (!tracking) return
            if (runMode == MODE_INDOOR) {
                updateIndoorMetrics()
            } else {
                render()
            }
            uiHandler.postDelayed(this, 1000L)
        }
    }
    private var tracking = false
    private var totalDistanceMeters = 0f
    private var lastLocation: Location? = null
    private var startElapsed = 0L
    private var holdAnimator: android.animation.ValueAnimator? = null
    private var runMode: String = MODE_OUTDOOR
    private var indoorSpeedKmH = 0.0
    private var indoorInclinePercent = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_run_tracker)

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        runMode = intent.getStringExtra(EXTRA_RUN_MODE) ?: MODE_OUTDOOR
        routePreview = findViewById(R.id.routePreview)
        indoorPreviewText = findViewById(R.id.indoorPreviewText)
        runModeValue = findViewById(R.id.runModeValue)
        statusView = findViewById(R.id.runStatusValue)
        distanceView = findViewById(R.id.runDistanceValue)
        durationView = findViewById(R.id.runDurationValue)
        positionView = findViewById(R.id.runPositionValue)
        indoorConfigSection = findViewById(R.id.indoorConfigSection)
        indoorSpeedInput = findViewById(R.id.indoorSpeedInput)
        indoorInclineInput = findViewById(R.id.indoorInclineInput)

        configureModeUi()

        findViewById<Button>(R.id.startRunButton).setOnClickListener { startTracking() }
        setupStopButton(findViewById(R.id.stopRunButton))

        render()
    }

    private fun setupStopButton(button: FrameLayout) {
        val progressView = button.findViewById<View>(R.id.stopRunProgress)
        progressView.post {
            progressView.pivotX = 0f
            progressView.pivotY = (progressView.height / 2f)
            progressView.scaleX = 0f
        }
        resetStopProgress(progressView)
        button.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (!tracking) {
                        Toast.makeText(this, R.string.run_long_press_hint, Toast.LENGTH_SHORT).show()
                        return@setOnTouchListener true
                    }
                    startHoldAnimation(progressView)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    cancelHoldAnimation(progressView)
                    true
                }
                else -> false
            }
        }
    }

    private fun startHoldAnimation(progressView: View) {
        holdAnimator?.cancel()
        holdAnimator = android.animation.ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 3000L
            addUpdateListener {
                progressView.scaleX = it.animatedValue as Float
            }
            addListener(object : AnimatorListenerAdapter() {
                private var cancelled = false

                override fun onAnimationCancel(animation: Animator) {
                    cancelled = true
                }

                override fun onAnimationEnd(animation: Animator) {
                    if (!cancelled) {
                        stopTracking(saveRecord = true)
                    }
                    resetStopProgress(progressView)
                }
            })
            start()
        }
    }

    private fun cancelHoldAnimation(progressView: View) {
        val animator = holdAnimator
        if (animator != null && animator.isRunning) {
            animator.cancel()
            resetStopProgress(progressView)
        }
    }

    private fun resetStopProgress(progressView: View) {
        progressView.scaleX = 0f
    }

    private fun startTracking() {
        totalDistanceMeters = 0f
        lastLocation = null
        routePoints.clear()
        routePreview.submitPoints(routePoints)
        startElapsed = SystemClock.elapsedRealtime()
        tracking = true
        statusView.text = getString(R.string.run_status_tracking)

        if (runMode == MODE_INDOOR) {
            val speed = indoorSpeedInput.text.toString().toDoubleOrNull()
            val incline = indoorInclineInput.text.toString().toDoubleOrNull()
            if (speed == null || incline == null || speed <= 0 || incline < 0) {
                tracking = false
                statusView.text = getString(R.string.run_status_idle)
                Toast.makeText(this, R.string.run_indoor_invalid, Toast.LENGTH_SHORT).show()
                return
            }
            indoorSpeedKmH = speed
            indoorInclinePercent = incline
            updateIndoorMetrics()
        } else {
            if (!hasLocationPermission()) {
                tracking = false
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                    REQUEST_LOCATION
                )
                return
            }

            var requestedAnyProvider = false
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000L, 2f, this)
                requestedAnyProvider = true
            }
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 3000L, 3f, this)
                requestedAnyProvider = true
            }
            if (!requestedAnyProvider) {
                tracking = false
                statusView.text = getString(R.string.run_status_provider_missing)
                Toast.makeText(this, R.string.run_provider_missing, Toast.LENGTH_SHORT).show()
                return
            }
        }
        Toast.makeText(this, R.string.run_tracking_started, Toast.LENGTH_SHORT).show()
        uiHandler.removeCallbacks(ticker)
        uiHandler.post(ticker)
        render()
    }

    private fun stopTracking(saveRecord: Boolean) {
        if (!tracking) return
        if (runMode == MODE_OUTDOOR) {
            locationManager.removeUpdates(this)
        }
        tracking = false
        uiHandler.removeCallbacks(ticker)
        statusView.text = getString(R.string.run_status_stopped)

        val durationMinutes = ((SystemClock.elapsedRealtime() - startElapsed) / 60000.0).coerceAtLeast(0.5)
        val profile = SportsPrefs.getProfile(this)
        val calories = when {
            profile == null -> 0.0
            runMode == MODE_INDOOR -> {
                DataTools.estimateIndoorRun(
                    weightKg = profile.weightKg,
                    durationMinutes = durationMinutes,
                    speedKmH = indoorSpeedKmH,
                    inclinePercent = indoorInclinePercent
                ).calories
            }
            else -> DataTools.estimateCalories(8.8, profile.weightKg, durationMinutes)
        }

        if (saveRecord) {
            SportsPrefs.addRecord(
                this,
                title = if (runMode == MODE_INDOOR) {
                    getString(R.string.run_mode_indoor)
                } else {
                    getString(R.string.run_record_title)
                },
                detail = if (runMode == MODE_INDOOR) {
                    getString(
                        R.string.run_record_detail_indoor_template,
                        String.format(Locale.getDefault(), "%.2f", totalDistanceMeters / 1000f),
                        String.format(Locale.getDefault(), "%.1f", indoorSpeedKmH),
                        String.format(Locale.getDefault(), "%.1f", indoorInclinePercent)
                    )
                } else {
                    getString(
                        R.string.run_record_detail_template,
                        String.format(Locale.getDefault(), "%.2f", totalDistanceMeters / 1000f),
                        routePoints.size
                    )
                },
                calories = calories,
                durationMinutes = durationMinutes
            )
        }

        Toast.makeText(this, R.string.run_tracking_stopped, Toast.LENGTH_SHORT).show()
        render()
    }

    override fun onLocationChanged(location: Location) {
        if (runMode != MODE_OUTDOOR) return
        val previous = lastLocation
        if (previous != null) {
            totalDistanceMeters += previous.distanceTo(location)
        }
        lastLocation = location
        routePoints += RoutePreviewView.Point(location.latitude, location.longitude)
        routePreview.submitPoints(routePoints.toList())
        render()
    }

    private fun render() {
        distanceView.text = getString(
            R.string.run_distance_template,
            String.format(Locale.getDefault(), "%.2f", totalDistanceMeters / 1000f)
        )
        val durationMs = if (tracking && startElapsed > 0L) {
            SystemClock.elapsedRealtime() - startElapsed
        } else {
            0L
        }
        val totalSeconds = durationMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        durationView.text = getString(R.string.run_duration_template, minutes, seconds)

        val location = lastLocation
        positionView.text = if (runMode == MODE_INDOOR) {
            getString(R.string.run_position_indoor)
        } else if (location == null) {
            getString(R.string.run_position_empty)
        } else {
            getString(
                R.string.run_position_template,
                String.format(Locale.getDefault(), "%.5f", location.latitude),
                String.format(Locale.getDefault(), "%.5f", location.longitude)
            )
        }
    }

    private fun configureModeUi() {
        val indoorMode = runMode == MODE_INDOOR
        runModeValue.text = if (indoorMode) {
            getString(R.string.run_mode_indoor)
        } else {
            getString(R.string.run_mode_outdoor)
        }
        indoorConfigSection.visibility = if (indoorMode) View.VISIBLE else View.GONE
        indoorPreviewText.visibility = if (indoorMode) View.VISIBLE else View.GONE
        routePreview.visibility = if (indoorMode) View.GONE else View.VISIBLE
        positionView.text = if (indoorMode) {
            getString(R.string.run_position_indoor)
        } else {
            getString(R.string.run_position_empty)
        }
    }

    private fun updateIndoorMetrics() {
        val profile = SportsPrefs.getProfile(this)
        val durationMinutes = ((SystemClock.elapsedRealtime() - startElapsed) / 60000.0).coerceAtLeast(0.0)
        totalDistanceMeters = (indoorSpeedKmH * durationMinutes / 60.0 * 1000.0).toFloat()
        if (profile != null) {
            val estimate = DataTools.estimateIndoorRun(
                weightKg = profile.weightKg,
                durationMinutes = durationMinutes,
                speedKmH = indoorSpeedKmH,
                inclinePercent = indoorInclinePercent
            )
            indoorPreviewText.text = getString(
                R.string.run_record_detail_indoor_template,
                String.format(Locale.getDefault(), "%.2f", estimate.distanceKm),
                String.format(Locale.getDefault(), "%.1f", indoorSpeedKmH),
                String.format(Locale.getDefault(), "%.1f", indoorInclinePercent)
            )
        }
        render()
    }

    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION && grantResults.any { it == PackageManager.PERMISSION_GRANTED }) {
            startTracking()
        } else if (requestCode == REQUEST_LOCATION) {
            Toast.makeText(this, R.string.run_permission_denied, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (tracking) {
            if (runMode == MODE_OUTDOOR) {
                locationManager.removeUpdates(this)
            }
        }
        uiHandler.removeCallbacks(ticker)
        holdAnimator?.cancel()
    }

    companion object {
        private const val REQUEST_LOCATION = 1001
        const val EXTRA_RUN_MODE = "run_mode"
        const val MODE_OUTDOOR = "outdoor"
        const val MODE_INDOOR = "indoor"
    }
}
