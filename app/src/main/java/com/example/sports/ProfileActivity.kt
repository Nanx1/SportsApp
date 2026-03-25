package com.example.sports

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val nameInput = findViewById<EditText>(R.id.profileNameInput)
        val heightInput = findViewById<EditText>(R.id.profileHeightInput)
        val weightInput = findViewById<EditText>(R.id.profileWeightInput)
        val saveButton = findViewById<Button>(R.id.saveProfileButton)

        SportsPrefs.getProfile(this)?.let { profile ->
            nameInput.setText(profile.name)
            heightInput.setText(profile.heightCm.toString())
            weightInput.setText(profile.weightKg.toString())
        }

        saveButton.setOnClickListener {
            val name = nameInput.text.toString().trim()
            val height = heightInput.text.toString().toDoubleOrNull()
            val weight = weightInput.text.toString().toDoubleOrNull()
            if (name.isEmpty() || height == null || weight == null || height <= 0 || weight <= 0) {
                Toast.makeText(this, R.string.profile_invalid_toast, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            SportsPrefs.saveProfile(this, SportsPrefs.UserProfile(name, height, weight))
            Toast.makeText(this, R.string.profile_saved_toast, Toast.LENGTH_SHORT).show()
            if (intent.getBooleanExtra(EXTRA_FIRST_SETUP, false)) {
                startActivity(Intent(this, CheckInActivity::class.java))
            }
            finish()
        }
    }

    companion object {
        const val EXTRA_FIRST_SETUP = "first_setup"
    }
}
