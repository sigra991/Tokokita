package com.example.utils

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object CurrencyUtils {
    fun formatRupiah(amount: Double): String {
        return try {
            val localeId = Locale("id", "ID")
            val numberFormat = NumberFormat.getCurrencyInstance(localeId)
            numberFormat.minimumFractionDigits = 0
            numberFormat.maximumFractionDigits = 0
            numberFormat.format(amount)
        } catch (e: Exception) {
            "Rp " + String.format(Locale.US, "%,.0f", amount)
        }
    }

    fun parseRupiah(text: String): Double {
        val cleanString = text.replace(Regex("[^0-9]"), "")
        return cleanString.toDoubleOrNull() ?: 0.0
    }

    fun calculateMargin(cost: Double, selling: Double): Double {
        if (cost <= 0) return 100.0
        return ((selling - cost) / cost) * 100.0
    }
}

object DateUtils {
    fun formatDate(timeInMillis: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale("id", "ID"))
        return sdf.format(Date(timeInMillis))
    }

    fun formatDateTime(timeInMillis: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("id", "ID"))
        return sdf.format(Date(timeInMillis))
    }

    fun getStartOfDayEpoch(daysAgo: Int = 0): Long {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -daysAgo)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    fun getEndOfDayEpoch(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }

    fun getDateRange(rangeType: String): Pair<Long, Long> {
        val end = getEndOfDayEpoch()
        val calendar = Calendar.getInstance()
        val start = when (rangeType) {
            "today" -> getStartOfDayEpoch(0)
            "week" -> {
                calendar.add(Calendar.DAY_OF_YEAR, -7)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar.timeInMillis
            }
            "month" -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar.timeInMillis
            }
            "year" -> {
                calendar.set(Calendar.DAY_OF_YEAR, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar.timeInMillis
            }
            else -> getStartOfDayEpoch(0)
        }
        return Pair(start, end)
    }
}
