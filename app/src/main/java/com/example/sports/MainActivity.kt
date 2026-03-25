package com.example.sports

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.pow

class MainActivity : AppCompatActivity() {

    private val activities = listOf(
        SportActivity("\u6B65\u884C", 3.5),
        SportActivity("\u8DD1\u6B65", 9.8),
        SportActivity("\u9A91\u884C", 7.5),
        SportActivity("\u8DF3\u7EF3", 11.8),
        SportActivity("\u529B\u91CF\u8BAD\u7EC3", 6.0),
        SportActivity("\u5065\u8EAB\u64CD", 8.0)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupActivityDropdown()
        bindBmiCalculator()
        bindCalorieCalculator()
        bindCheckIn()
        bindVideoButtons()
        renderDemoStats()
    }

    private fun setupActivityDropdown() {
        val dropdown = findViewById<AutoCompleteTextView>(R.id.sportTypeInput)
        val labels = activities.map { it.name }
        dropdown.setAdapter(
            ArrayAdapter(
                this,
                android.R.layout.simple_dropdown_item_1line,
                labels
            )
        )
        dropdown.setText(labels.first(), false)
    }

    private fun bindBmiCalculator() {
        val heightInput = findViewById<EditText>(R.id.heightInput)
        val weightInput = findViewById<EditText>(R.id.weightInput)
        val resultView = findViewById<TextView>(R.id.bmiResult)
        val action = findViewById<Button>(R.id.calculateBmiButton)

        action.setOnClickListener {
            val heightCm = heightInput.text.toString().toDoubleOrNull()
            val weightKg = weightInput.text.toString().toDoubleOrNull()

            if (heightCm == null || weightKg == null || heightCm <= 0 || weightKg <= 0) {
                resultView.text = getString(R.string.bmi_invalid_hint)
                return@setOnClickListener
            }

            val bmi = weightKg / (heightCm / 100).pow(2)
            resultView.text = getString(
                R.string.bmi_result_template,
                formatOneDecimal(bmi),
                classifyBmi(bmi)
            )
        }
    }

    private fun bindCalorieCalculator() {
        val weightInput = findViewById<EditText>(R.id.sportWeightInput)
        val durationInput = findViewById<EditText>(R.id.durationInput)
        val sportTypeInput = findViewById<AutoCompleteTextView>(R.id.sportTypeInput)
        val resultView = findViewById<TextView>(R.id.calorieResult)
        val action = findViewById<Button>(R.id.calculateCalorieButton)

        action.setOnClickListener {
            val weight = weightInput.text.toString().toDoubleOrNull()
            val duration = durationInput.text.toString().toDoubleOrNull()
            val selectedName = sportTypeInput.text.toString()
            val sport = activities.find { it.name == selectedName } ?: activities.first()

            if (weight == null || duration == null || weight <= 0 || duration <= 0) {
                resultView.text = getString(R.string.calorie_invalid_hint)
                return@setOnClickListener
            }

            val calories = 0.0175 * sport.met * weight * duration
            resultView.text = getString(
                R.string.calorie_result_template,
                sport.name,
                formatOneDecimal(calories)
            )
        }
    }

    private fun bindCheckIn() {
        val prefs = getSharedPreferences("sports_demo", MODE_PRIVATE)
        val checkInResult = findViewById<TextView>(R.id.checkInResult)
        val streakView = findViewById<TextView>(R.id.streakValue)
        val totalView = findViewById<TextView>(R.id.totalCheckInValue)
        val action = findViewById<Button>(R.id.checkInButton)

        fun refresh() {
            val today = LocalDate.now()
            val lastDate = prefs.getString(KEY_LAST_CHECK_IN_DATE, null)?.let(LocalDate::parse)
            val currentStreak = prefs.getInt(KEY_STREAK, 0)
            val totalCount = prefs.getInt(KEY_TOTAL_CHECK_IN, 0)
            val alreadyCheckedIn = lastDate == today

            streakView.text = currentStreak.toString()
            totalView.text = totalCount.toString()
            checkInResult.text = if (alreadyCheckedIn) {
                getString(R.string.checkin_done_message)
            } else {
                getString(R.string.checkin_ready_message)
            }
            action.text = if (alreadyCheckedIn) {
                getString(R.string.checkin_button_done)
            } else {
                getString(R.string.checkin_button_ready)
            }
        }

        action.setOnClickListener {
            val today = LocalDate.now()
            val lastDate = prefs.getString(KEY_LAST_CHECK_IN_DATE, null)?.let(LocalDate::parse)
            if (lastDate == today) {
                Toast.makeText(this, R.string.checkin_repeat_toast, Toast.LENGTH_SHORT).show()
                refresh()
                return@setOnClickListener
            }

            val previousStreak = prefs.getInt(KEY_STREAK, 0)
            val newStreak = when {
                lastDate == null -> 1
                ChronoUnit.DAYS.between(lastDate, today) == 1L -> previousStreak + 1
                else -> 1
            }
            val totalCount = prefs.getInt(KEY_TOTAL_CHECK_IN, 0) + 1

            prefs.edit()
                .putString(KEY_LAST_CHECK_IN_DATE, today.toString())
                .putInt(KEY_STREAK, newStreak)
                .putInt(KEY_TOTAL_CHECK_IN, totalCount)
                .apply()

            Toast.makeText(this, R.string.checkin_success_toast, Toast.LENGTH_SHORT).show()
            refresh()
        }

        refresh()
    }

    private fun bindVideoButtons() {
        findViewById<Button>(R.id.openBilibiliButton).setOnClickListener {
            openUrl("bilibili://search?keyword=%E5%B8%95%E6%A2%85%E6%8B%89%20%E7%87%83%E8%84%82")
        }
        findViewById<Button>(R.id.openBrowserButton).setOnClickListener {
            openUrl("https://www.bilibili.com/video/BV1K4411d7ku")
        }
    }

    private fun renderDemoStats() {
        findViewById<TextView>(R.id.weeklyMinutesValue).text = "168"
        findViewById<TextView>(R.id.weeklyCaloriesValue).text = "1240 kcal"
        findViewById<TextView>(R.id.goalCompletionValue).text = "70%"

        findViewById<ProgressBar>(R.id.exerciseProgress).progress = 70
        findViewById<ProgressBar>(R.id.calorieProgress).progress = 62
        findViewById<ProgressBar>(R.id.checkInProgress).progress = 85
    }

    private fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.video_open_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun classifyBmi(bmi: Double): String = when {
        bmi < 18.5 -> "\u504F\u7626"
        bmi < 24.0 -> "\u6B63\u5E38"
        bmi < 28.0 -> "\u8D85\u91CD"
        else -> "\u80A5\u80D6"
    }

    private fun formatOneDecimal(value: Double): String =
        String.format(Locale.getDefault(), "%.1f", value)

    data class SportActivity(val name: String, val met: Double)

    companion object {
        private const val KEY_LAST_CHECK_IN_DATE = "last_check_in_date"
        private const val KEY_STREAK = "streak"
        private const val KEY_TOTAL_CHECK_IN = "total_check_in"
    }
}
