package com.example.sports

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class WeightForecastActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weight_forecast)

        val profile = SportsPrefs.getProfile(this)
        if (profile != null) {
            findViewById<EditText>(R.id.currentWeightInput).setText(profile.weightKg.toString())
            findViewById<EditText>(R.id.heightInput).setText(profile.heightCm.toString())
        }

        findViewById<Button>(R.id.calculateForecastButton).setOnClickListener {
            val weight = findViewById<EditText>(R.id.currentWeightInput).text.toString().toDoubleOrNull()
            val height = findViewById<EditText>(R.id.heightInput).text.toString().toDoubleOrNull()
            val intake = findViewById<EditText>(R.id.intakeInput).text.toString().toDoubleOrNull()
            val target = findViewById<EditText>(R.id.targetWeightInput).text.toString().toDoubleOrNull()
            val result = findViewById<TextView>(R.id.forecastResult)
            if (
                weight == null || height == null || intake == null || target == null ||
                weight <= 0 || height <= 0 || intake <= 0 || target <= 0
            ) {
                result.text = getString(R.string.forecast_invalid_hint)
                return@setOnClickListener
            }

            val avgExercise = SportsPrefs.getAverageExerciseCaloriesPerDay(this)
            val forecast = DataTools.estimateWeightGoalDate(
                currentWeightKg = weight,
                heightCm = height,
                intakeCalories = intake,
                averageExerciseCalories = avgExercise,
                targetWeightKg = target
            )
            result.text = if (forecast.targetDate == null) {
                getString(
                    R.string.forecast_no_deficit_result,
                    String.format(Locale.getDefault(), "%.0f", forecast.dailyDeficit)
                )
            } else {
                getString(
                    R.string.forecast_result_template,
                    String.format(Locale.getDefault(), "%.1f", forecast.targetWeightKg),
                    forecast.targetDate.toString(),
                    String.format(Locale.getDefault(), "%.0f", forecast.dailyDeficit),
                    String.format(Locale.getDefault(), "%.0f", avgExercise)
                )
            }
        }
    }
}
