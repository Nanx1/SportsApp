package com.example.sports

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.LocalDate
import kotlin.math.pow

object DataTools {
    data class SportActivity(val name: String, val met: Double)

    val sportActivities = listOf(
        SportActivity("\u81EA\u7531\u953B\u70BC", 3.8),
        SportActivity("\u6B65\u884C", 3.2),
        SportActivity("\u8DD1\u6B65", 8.8),
        SportActivity("\u9A91\u884C", 6.8),
        SportActivity("\u8DF3\u7EF3", 10.2),
        SportActivity("\u529B\u91CF\u8BAD\u7EC3", 5.2),
        SportActivity("\u5065\u8EAB\u64CD", 6.8),
        SportActivity("\u745C\u4F3D", 2.6),
        SportActivity("\u6E38\u6CF3", 7.0),
        SportActivity("\u7BF7\u7403", 5.8),
        SportActivity("\u821E\u8E48", 4.8),
        SportActivity("\u666E\u62C9\u63D0", 2.8),
        SportActivity("\u692D\u5706\u673A", 4.5)
    )

    fun classifyBmi(bmi: Double): String = when {
        bmi < 18.5 -> "\u504F\u7626"
        bmi < 24.0 -> "\u6B63\u5E38"
        bmi < 28.0 -> "\u8D85\u91CD"
        else -> "\u80A5\u80D6"
    }

    fun calculateBmi(heightCm: Double, weightKg: Double): Double =
        weightKg / (heightCm / 100).pow(2)

    fun estimateCalories(met: Double, weightKg: Double, durationMinutes: Double): Double =
        0.0175 * met * weightKg * durationMinutes * 0.9

    data class IndoorRunEstimate(
        val distanceKm: Double,
        val calories: Double,
        val effectiveMet: Double
    )

    fun estimateIndoorRun(
        weightKg: Double,
        durationMinutes: Double,
        speedKmH: Double,
        inclinePercent: Double
    ): IndoorRunEstimate {
        val speedMetersPerMinute = speedKmH * 1000.0 / 60.0
        val grade = (inclinePercent / 100.0).coerceAtLeast(0.0)
        val vo2 = if (speedKmH < 8.0) {
            0.1 * speedMetersPerMinute + 1.8 * speedMetersPerMinute * grade + 3.5
        } else {
            0.2 * speedMetersPerMinute + 0.9 * speedMetersPerMinute * grade + 3.5
        }
        val met = (vo2 / 3.5 * 0.92).coerceAtLeast(3.0)
        return IndoorRunEstimate(
            distanceKm = speedKmH * durationMinutes / 60.0,
            calories = estimateCalories(met, weightKg, durationMinutes),
            effectiveMet = met
        )
    }

    data class WeightForecast(
        val targetWeightKg: Double,
        val dailyDeficit: Double,
        val targetDate: LocalDate?
    )

    @RequiresApi(Build.VERSION_CODES.O)
    fun estimateWeightGoalDate(
        currentWeightKg: Double,
        heightCm: Double,
        intakeCalories: Double,
        averageExerciseCalories: Double,
        targetWeightKg: Double? = null
    ): WeightForecast {
        val maintenance = currentWeightKg * 30.0
        val bmiTargetWeight = 23.9 * (heightCm / 100).pow(2)
        val safeTarget = targetWeightKg ?: bmiTargetWeight
        val deficit = maintenance + averageExerciseCalories - intakeCalories
        val weightToLose = (currentWeightKg - safeTarget).coerceAtLeast(0.0)
        val days = if (deficit > 0 && weightToLose > 0) {
            (weightToLose * 7700.0 / deficit).toLong().coerceAtLeast(1L)
        } else {
            0L
        }
        return WeightForecast(
            targetWeightKg = safeTarget,
            dailyDeficit = deficit,
            targetDate = if (days > 0) LocalDate.now().plusDays(days) else null
        )
    }
}
