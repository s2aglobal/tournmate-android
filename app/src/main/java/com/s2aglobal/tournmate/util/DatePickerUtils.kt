package com.s2aglobal.tournmate.util

import java.util.Calendar
import java.util.Date
import java.util.TimeZone

/**
 * Material3 [androidx.compose.material3.DatePicker] uses UTC midnight for
 * [androidx.compose.material3.DatePickerState.selectedDateMillis].
 * Always convert through UTC when reading/writing picker millis to avoid
 * off-by-one day shifts in US and other negative-offset timezones.
 */
object DatePickerUtils {
    private val utc: TimeZone get() = TimeZone.getTimeZone("UTC")

    /** Local date/time → UTC midnight millis for [rememberDatePickerState]. */
    fun toUtcPickerMillis(date: Date): Long {
        val local = Calendar.getInstance().apply { time = date }
        return Calendar.getInstance(utc).apply {
            clear()
            set(
                local.get(Calendar.YEAR),
                local.get(Calendar.MONTH),
                local.get(Calendar.DAY_OF_MONTH),
            )
        }.timeInMillis
    }

    /** UTC midnight millis from picker → local [Date], preserving time-of-day from [existing]. */
    fun applyPickerDate(existing: Date, pickerUtcMillis: Long): Date {
        val picked = Calendar.getInstance(utc).apply { timeInMillis = pickerUtcMillis }
        return Calendar.getInstance().apply {
            time = existing
            set(Calendar.YEAR, picked.get(Calendar.YEAR))
            set(Calendar.MONTH, picked.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, picked.get(Calendar.DAY_OF_MONTH))
        }.time
    }
}
