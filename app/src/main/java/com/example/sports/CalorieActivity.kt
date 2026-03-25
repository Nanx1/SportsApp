package com.example.sports

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.widget.ArrayAdapter
import java.util.Locale

class CalorieActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calorie)

        val spinner = findViewById<Spinner>(R.id.sportTypeSpinner)
        spinner.adapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                DataTools.sportActivities.map { it.name }
            ).also { adapter ->
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }

        findViewById<Button>(R.id.calculateCalorieButton).setOnClickListener {
            val weight = findViewById<EditText>(R.id.sportWeightInput).text.toString().toDoubleOrNull()
            val duration = findViewById<EditText>(R.id.durationInput).text.toString().toDoubleOrNull()
            val selected = spinner.selectedItem?.toString().orEmpty()
            val result = findViewById<TextView>(R.id.calorieResult)
            val sport = DataTools.sportActivities.find { it.name == selected } ?: DataTools.sportActivities.first()
            if (weight == null || duration == null || weight <= 0 || duration <= 0) {
                result.text = getString(R.string.calorie_invalid_hint)
                return@setOnClickListener
            }
            val calories = DataTools.estimateCalories(sport.met, weight, duration)
            result.text = getString(
                R.string.calorie_result_template,
                sport.name,
                String.format(Locale.getDefault(), "%.1f", calories)
            )
        }
    }
}
