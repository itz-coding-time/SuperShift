package com.example.supershift.utils

import java.util.Calendar

object TimeUtils {

    fun calculateAssociateShiftProgress(startTimeStr: String, endTimeStr: String): Float {
        try {
            val startParts = startTimeStr.split(":")
            val endParts = endTimeStr.split(":")
            if (startParts.size != 2 || endParts.size != 2) return 0f

            val startMins = startParts[0].toInt() * 60 + startParts[1].toInt()
            val originalEndMins = endParts[0].toInt() * 60 + endParts[1].toInt()
            var endMins = originalEndMins

            val calendar = Calendar.getInstance()
            val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
            val currentMinute = calendar.get(Calendar.MINUTE)
            var currentMins = currentHour * 60 + currentMinute

            val crossesMidnight = originalEndMins <= startMins
            if (crossesMidnight) {
                endMins += 24 * 60
            }

            if (crossesMidnight) {
                if (currentMins <= originalEndMins) {
                    currentMins += 24 * 60
                } else if (currentMins >= startMins) {
                    // Do nothing
                } else {
                    return if (currentMins < startMins && currentMins > originalEndMins + 60) 0f else 1f
                }
            }

            if (currentMins < startMins) return 0f
            if (currentMins > endMins) return 1f

            val totalDuration = endMins - startMins
            val elapsed = currentMins - startMins

            return (elapsed.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)
        } catch (e: Exception) {
            return 0f
        }
    }

    fun isAssociateOnClock(startTimeStr: String, endTimeStr: String, currentTimeMs: Long = System.currentTimeMillis()): Boolean {
        try {
            val startParts = startTimeStr.split(":")
            val endParts = endTimeStr.split(":")
            if (startParts.size != 2 || endParts.size != 2) return true

            val startMins = startParts[0].toInt() * 60 + startParts[1].toInt()
            val originalEndMins = endParts[0].toInt() * 60 + endParts[1].toInt()
            var endMins = originalEndMins

            val calendar = Calendar.getInstance().apply { timeInMillis = currentTimeMs }
            val currentMins = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)

            val crossesMidnight = originalEndMins <= startMins
            if (crossesMidnight) {
                endMins += 24 * 60
            }

            var adjustedCurrentMins = currentMins
            if (crossesMidnight && currentMins <= originalEndMins) {
                adjustedCurrentMins += 24 * 60
            }

            return adjustedCurrentMins in startMins..endMins
        } catch (e: Exception) {
            return true
        }
    }
}