package com.example.sports

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.Locale
import kotlin.math.pow

class MainActivity : AppCompatActivity() {

    private lateinit var dataSection: View
    private lateinit var followSection: View
    private lateinit var runSection: View
    private lateinit var profileSection: View

    private var currentWorkout: Workout? = null
    private var startedAt: Long = 0L
    private var awaitingReturn = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (!SportsPrefs.hasProfile(this)) {
            startActivity(
                Intent(this, ProfileActivity::class.java).putExtra(
                    ProfileActivity.EXTRA_FIRST_SETUP,
                    true
                )
            )
            finish()
            return
        }

        dataSection = findViewById(R.id.dataSection)
        followSection = findViewById(R.id.followSection)
        runSection = findViewById(R.id.runSection)
        profileSection = findViewById(R.id.profileSection)

        setupBottomNav()
        setupDataSection()
        setupFollowSection()
        setupRunSection()
        setupProfileSection()
        showSection(R.id.nav_data)
    }

    override fun onResume() {
        super.onResume()
        refreshDataSection()
        refreshProfileSection()

        if (awaitingReturn && currentWorkout != null && startedAt > 0L) {
            showFollowDurationDialog()
            awaitingReturn = false
            startedAt = 0L
        }
    }

    private fun setupBottomNav() {
        findViewById<BottomNavigationView>(R.id.bottomNavigation).setOnItemSelectedListener { item ->
            showSection(item.itemId)
            true
        }
    }

    private fun showSection(itemId: Int) {
        dataSection.visibility = if (itemId == R.id.nav_data) View.VISIBLE else View.GONE
        runSection.visibility = if (itemId == R.id.nav_run) View.VISIBLE else View.GONE
        followSection.visibility = if (itemId == R.id.nav_follow) View.VISIBLE else View.GONE
        profileSection.visibility = if (itemId == R.id.nav_profile) View.VISIBLE else View.GONE
    }

    private fun setupDataSection() {
        findViewById<View>(R.id.openBmiToolButton).setOnClickListener {
            startActivity(Intent(this, BmiActivity::class.java))
        }
        findViewById<View>(R.id.openCalorieToolButton).setOnClickListener {
            startActivity(Intent(this, CalorieActivity::class.java))
        }
        findViewById<View>(R.id.openForecastToolButton).setOnClickListener {
            startActivity(Intent(this, WeightForecastActivity::class.java))
        }
        refreshDataSection()
    }

    private fun refreshDataSection() {
        val profile = SportsPrefs.getProfile(this)
        val state = SportsPrefs.getCheckInState(this)
        findViewById<TextView>(R.id.homeNameValue).text =
            profile?.name ?: getString(R.string.profile_missing_short)
        findViewById<TextView>(R.id.homeTotalCheckInValue).text = state.totalCount.toString()
        findViewById<TextView>(R.id.homeStreakValue).text = state.streak.toString()
        renderRecords()
    }

    private fun renderRecords() {
        val container = findViewById<LinearLayout>(R.id.recordsContainer)
        container.removeAllViews()
        val records = SportsPrefs.getRecords(this)
        if (records.isEmpty()) {
            container.addView(TextView(this).apply {
                text = getString(R.string.record_empty)
                textSize = 14f
                setTextColor(getColor(R.color.text_secondary))
            })
            return
        }

        records.forEach { record ->
            val card = layoutInflater.inflate(R.layout.item_record, container, false)
            card.findViewById<TextView>(R.id.recordTitle).text = record.title
            card.findViewById<TextView>(R.id.recordDetail).text = record.detail
            card.findViewById<TextView>(R.id.recordMeta).text = getString(
                R.string.record_meta_template,
                record.createdAt,
                String.format(Locale.getDefault(), "%.0f", record.calories)
            )
            card.setOnClickListener {
                AlertDialog.Builder(this)
                    .setTitle(record.title)
                    .setMessage(
                        getString(
                            R.string.record_detail_dialog,
                            record.detail,
                            String.format(Locale.getDefault(), "%.1f", record.durationMinutes),
                            String.format(Locale.getDefault(), "%.0f", record.calories),
                            record.createdAt
                        )
                    )
                    .setPositiveButton(R.string.dialog_ok, null)
                    .show()
            }
            container.addView(card)
        }
    }

    private fun setupFollowSection() {
        findViewById<View>(R.id.t25Button).setOnClickListener {
            launchWorkout(
                Workout(
                    title = "T25",
                    detail = getString(R.string.video_t25_detail),
                    appUri = "bilibili://video/BV1w4411a7Bm",
                    met = 8.5
                )
            )
        }
        findViewById<View>(R.id.pamelaButton).setOnClickListener {
            launchWorkout(
                Workout(
                    title = "\u5E15\u6885\u62C9",
                    detail = getString(R.string.video_pamela_detail),
                    appUri = "bilibili://video/BV1R3411A7g4",
                    met = 7.8
                )
            )
        }
        findViewById<View>(R.id.slimLegButton).setOnClickListener {
            launchWorkout(
                Workout(
                    title = "\u7626\u817F\u8BAD\u7EC3",
                    detail = getString(R.string.video_slim_leg_detail),
                    appUri = "bilibili://video/BV1eK4y1t7zi",
                    met = 5.8
                )
            )
        }
    }

    private fun launchWorkout(workout: Workout) {
        currentWorkout = workout
        startedAt = SystemClock.elapsedRealtime()
        awaitingReturn = true
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(workout.appUri)))
        } catch (_: Exception) {
            awaitingReturn = false
            Toast.makeText(this, R.string.video_open_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showFollowDurationDialog() {
        val workout = currentWorkout ?: return
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_follow_result, null, false)
        val durationInput = view.findViewById<EditText>(R.id.followDurationInput)
        val caloriesText = view.findViewById<TextView>(R.id.followCaloriesPreview)
        val profile = SportsPrefs.getProfile(this)
        val suggestedMinutes = ((SystemClock.elapsedRealtime() - startedAt) / 60000.0).coerceAtLeast(1.0)
        durationInput.setText(String.format(Locale.getDefault(), "%.1f", suggestedMinutes))

        fun updatePreview() {
            val minutes = durationInput.text.toString().toDoubleOrNull() ?: 0.0
            val calories = if (profile == null) 0.0 else DataTools.estimateCalories(workout.met, profile.weightKg, minutes)
            caloriesText.text = getString(
                R.string.video_follow_calories_template,
                String.format(Locale.getDefault(), "%.0f", calories)
            )
        }

        durationInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) = updatePreview()
        })
        updatePreview()

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.video_result_title, workout.title))
            .setView(view)
            .setPositiveButton(R.string.save_record) { _, _ ->
                val minutes = durationInput.text.toString().toDoubleOrNull() ?: suggestedMinutes
                val calories = if (profile == null) 0.0 else DataTools.estimateCalories(workout.met, profile.weightKg, minutes)
                SportsPrefs.addRecord(
                    this,
                    title = workout.title,
                    detail = getString(
                        R.string.video_record_detail_template,
                        workout.detail,
                        String.format(Locale.getDefault(), "%.1f", minutes)
                    ),
                    calories = calories,
                    durationMinutes = minutes
                )
                refreshDataSection()
                Toast.makeText(this, R.string.record_saved_toast, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    private fun setupRunSection() {
        findViewById<View>(R.id.openOutdoorRunButton).setOnClickListener {
            startActivity(
                Intent(this, RunTrackerActivity::class.java)
                    .putExtra(RunTrackerActivity.EXTRA_RUN_MODE, RunTrackerActivity.MODE_OUTDOOR)
            )
        }
        findViewById<View>(R.id.openIndoorRunButton).setOnClickListener {
            startActivity(
                Intent(this, RunTrackerActivity::class.java)
                    .putExtra(RunTrackerActivity.EXTRA_RUN_MODE, RunTrackerActivity.MODE_INDOOR)
            )
        }
    }

    private fun setupProfileSection() {
        val nameInput = findViewById<EditText>(R.id.profileNameInput)
        val heightInput = findViewById<EditText>(R.id.profileHeightInput)
        val weightInput = findViewById<EditText>(R.id.profileWeightInput)
        val bmiText = findViewById<TextView>(R.id.profileBmiValue)

        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                val height = heightInput.text.toString().toDoubleOrNull()
                val weight = weightInput.text.toString().toDoubleOrNull()
                bmiText.text = if (height == null || weight == null || height <= 0 || weight <= 0) {
                    getString(R.string.profile_bmi_placeholder)
                } else {
                    val bmi = weight / (height / 100).pow(2)
                    getString(
                        R.string.profile_bmi_template,
                        String.format(Locale.getDefault(), "%.1f", bmi),
                        DataTools.classifyBmi(bmi)
                    )
                }
            }
        }
        heightInput.addTextChangedListener(watcher)
        weightInput.addTextChangedListener(watcher)

        findViewById<View>(R.id.saveProfileButton).setOnClickListener {
            val name = nameInput.text.toString().trim()
            val height = heightInput.text.toString().toDoubleOrNull()
            val weight = weightInput.text.toString().toDoubleOrNull()
            if (name.isEmpty() || height == null || weight == null || height <= 0 || weight <= 0) {
                Toast.makeText(this, R.string.profile_invalid_toast, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            SportsPrefs.saveProfile(this, SportsPrefs.UserProfile(name, height, weight))
            refreshDataSection()
            Toast.makeText(this, R.string.profile_saved_toast, Toast.LENGTH_SHORT).show()
        }
    }

    private fun refreshProfileSection() {
        val profile = SportsPrefs.getProfile(this) ?: return
        findViewById<EditText>(R.id.profileNameInput).setText(profile.name)
        findViewById<EditText>(R.id.profileHeightInput).setText(profile.heightCm.toString())
        findViewById<EditText>(R.id.profileWeightInput).setText(profile.weightKg.toString())
        val bmi = profile.weightKg / (profile.heightCm / 100).pow(2)
        findViewById<TextView>(R.id.profileBmiValue).text = getString(
            R.string.profile_bmi_template,
            String.format(Locale.getDefault(), "%.1f", bmi),
            DataTools.classifyBmi(bmi)
        )
    }

    data class Workout(
        val title: String,
        val detail: String,
        val appUri: String,
        val met: Double
    )
}
