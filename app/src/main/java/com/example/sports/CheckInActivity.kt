package com.example.sports

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class CheckInActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!SportsPrefs.hasProfile(this)) {
            startActivity(
                Intent(this, ProfileActivity::class.java)
                    .putExtra(ProfileActivity.EXTRA_FIRST_SETUP, true)
            )
            finish()
            return
        }
        setContentView(R.layout.activity_check_in)

        val title = findViewById<TextView>(R.id.checkInTitle)
        val summary = findViewById<TextView>(R.id.checkInSummary)
        val streak = findViewById<TextView>(R.id.streakValue)
        val total = findViewById<TextView>(R.id.totalValue)
        val checkInButton = findViewById<Button>(R.id.checkInButton)
        val enterButton = findViewById<Button>(R.id.enterHomeButton)

        fun render() {
            val state = SportsPrefs.getCheckInState(this)
            streak.text = state.streak.toString()
            total.text = state.totalCount.toString()
            if (state.alreadyCheckedInToday) {
                title.text = getString(R.string.checkin_open_done_title)
                summary.text = getString(R.string.checkin_center_done_message)
                checkInButton.text = getString(R.string.checkin_center_done_button)
            } else {
                title.text = getString(R.string.checkin_open_title)
                summary.text = getString(R.string.checkin_center_ready_message)
                checkInButton.text = getString(R.string.checkin_center_button)
            }
        }

        checkInButton.setOnClickListener {
            val before = SportsPrefs.getCheckInState(this)
            val after = SportsPrefs.checkInToday(this)
            if (before.alreadyCheckedInToday) {
                Toast.makeText(this, R.string.checkin_repeat_toast, Toast.LENGTH_SHORT).show()
            } else if (after.alreadyCheckedInToday) {
                Toast.makeText(this, R.string.checkin_success_toast, Toast.LENGTH_SHORT).show()
            }
            render()
        }

        enterButton.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        render()
    }

    override fun onResume() {
        super.onResume()
        if (!SportsPrefs.hasProfile(this)) {
            startActivity(
                Intent(this, ProfileActivity::class.java)
                    .putExtra(ProfileActivity.EXTRA_FIRST_SETUP, true)
            )
            finish()
        }
    }
}
