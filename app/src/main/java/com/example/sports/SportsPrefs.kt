package com.example.sports

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object SportsPrefs {
    private const val PREFS_NAME = "sports_demo"
    private const val KEY_LAST_CHECK_IN_DATE = "last_check_in_date"
    private const val KEY_STREAK = "streak"
    private const val KEY_TOTAL_CHECK_IN = "total_check_in"
    private const val KEY_PROFILE = "profile"
    private const val KEY_RECORDS = "records"
    private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MM-dd HH:mm")

    data class CheckInState(
        val totalCount: Int,
        val streak: Int,
        val alreadyCheckedInToday: Boolean
    )

    data class UserProfile(
        val name: String,
        val heightCm: Double,
        val weightKg: Double
    )

    data class ExerciseRecord(
        val title: String,
        val detail: String,
        val calories: Double,
        val durationMinutes: Double,
        val createdAt: String
    )

    fun getCheckInState(context: Context): CheckInState {
        val prefs = prefs(context)
        val today = LocalDate.now()
        val lastDate = prefs.getString(KEY_LAST_CHECK_IN_DATE, null)?.let(LocalDate::parse)
        return CheckInState(
            totalCount = prefs.getInt(KEY_TOTAL_CHECK_IN, 0),
            streak = prefs.getInt(KEY_STREAK, 0),
            alreadyCheckedInToday = lastDate == today
        )
    }

    fun checkInToday(context: Context): CheckInState {
        val prefs = prefs(context)
        val today = LocalDate.now()
        val lastDate = prefs.getString(KEY_LAST_CHECK_IN_DATE, null)?.let(LocalDate::parse)
        if (lastDate == today) {
            return getCheckInState(context)
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

        return CheckInState(totalCount = totalCount, streak = newStreak, alreadyCheckedInToday = true)
    }

    fun saveProfile(context: Context, profile: UserProfile) {
        val json = JSONObject()
            .put("name", profile.name)
            .put("heightCm", profile.heightCm)
            .put("weightKg", profile.weightKg)
        prefs(context).edit().putString(KEY_PROFILE, json.toString()).apply()
    }

    fun getProfile(context: Context): UserProfile? {
        val raw = prefs(context).getString(KEY_PROFILE, null) ?: return null
        val json = JSONObject(raw)
        return UserProfile(
            name = json.optString("name"),
            heightCm = json.optDouble("heightCm"),
            weightKg = json.optDouble("weightKg")
        )
    }

    fun hasProfile(context: Context): Boolean = getProfile(context) != null

    fun addRecord(context: Context, title: String, detail: String, calories: Double, durationMinutes: Double) {
        val prefs = prefs(context)
        val records = JSONArray(prefs.getString(KEY_RECORDS, "[]"))
        val json = JSONObject()
            .put("title", title)
            .put("detail", detail)
            .put("calories", calories)
            .put("durationMinutes", durationMinutes)
            .put("createdAt", formatter.format(LocalDateTime.now()))

        val newRecords = JSONArray()
        newRecords.put(json)
        for (index in 0 until minOf(records.length(), 19)) {
            newRecords.put(records.getJSONObject(index))
        }
        prefs.edit().putString(KEY_RECORDS, newRecords.toString()).apply()
    }

    fun getRecords(context: Context): List<ExerciseRecord> {
        val records = JSONArray(prefs(context).getString(KEY_RECORDS, "[]"))
        return buildList {
            for (index in 0 until records.length()) {
                val json = records.getJSONObject(index)
                add(
                    ExerciseRecord(
                        title = json.optString("title"),
                        detail = json.optString("detail"),
                        calories = json.optDouble("calories"),
                        durationMinutes = json.optDouble("durationMinutes"),
                        createdAt = json.optString("createdAt")
                    )
                )
            }
        }
    }

    fun getAverageExerciseCaloriesPerDay(context: Context): Double {
        val records = getRecords(context)
        if (records.isEmpty()) return 0.0
        return records.sumOf { it.calories } / 7.0
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
