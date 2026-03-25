package com.example.sports

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class BmiActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bmi)

        findViewById<Button>(R.id.calculateBmiButton).setOnClickListener {
            val height = findViewById<EditText>(R.id.heightInput).text.toString().toDoubleOrNull()
            val weight = findViewById<EditText>(R.id.weightInput).text.toString().toDoubleOrNull()
            val result = findViewById<TextView>(R.id.bmiResult)
            if (height == null || weight == null || height <= 0 || weight <= 0) {
                result.text = getString(R.string.bmi_invalid_hint)
                return@setOnClickListener
            }
            val bmi = DataTools.calculateBmi(height, weight)
            result.text = getString(
                R.string.bmi_result_template,
                String.format(Locale.getDefault(), "%.1f", bmi),
                DataTools.classifyBmi(bmi)
            )
        }
    }
}
