package com.alhasanah.alhasanahmedia.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// Utility Functions for formatting
fun formatRupiah(amount: Long?): String {
    val localeID = Locale("in", "ID")
    val numberFormat = NumberFormat.getCurrencyInstance(localeID)
    numberFormat.maximumFractionDigits = 0
    return numberFormat.format(amount ?: 0L)
}

fun formatDate(date: LocalDate?): String {
    return date?.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault())) ?: "-"
}

fun formatDateOnly(dateString: String?): String {
    return try {
        dateString ?: return "-"
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("id", "ID"))
        val date = inputFormat.parse(dateString.take(19))
        outputFormat.format(date!!)
    } catch (e: Exception) {
        dateString ?: "-"
    }
}

fun formatStringDate(dateString: String?): String {
    return try {
        dateString ?: return "-"
        // Handle various ISO formats from Supabase
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("EEEE, d MMMM yyyy • HH:mm 'WIB'", Locale("id", "ID"))
        val date = inputFormat.parse(dateString.take(19)) // Take only the main part of ISO
        outputFormat.format(date!!)
    } catch (e: Exception) {
        dateString ?: "-"
    }
}
