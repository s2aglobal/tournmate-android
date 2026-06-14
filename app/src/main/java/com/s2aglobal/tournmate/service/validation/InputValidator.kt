package com.s2aglobal.tournmate.service.validation

import android.util.Patterns

sealed class ValidationResult {
    data object Valid : ValidationResult()
    data class Invalid(val message: String) : ValidationResult()

    val isValid: Boolean get() = this is Valid

    val errorMessage: String?
        get() = (this as? Invalid)?.message
}

object InputValidator {

    fun validatePhone(value: String, allowEmpty: Boolean = false): ValidationResult {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) {
            return if (allowEmpty) ValidationResult.Valid
            else ValidationResult.Invalid("Phone number is required.")
        }

        val allowedChars = "+0123456789 ()-."
        if (!trimmed.all { it in allowedChars }) {
            return ValidationResult.Invalid("Phone number contains invalid characters.")
        }

        val digits = trimmed.filter { it.isDigit() }
        if (digits.length < 10) return ValidationResult.Invalid("Phone number must have at least 10 digits.")
        if (digits.length > 15) return ValidationResult.Invalid("Phone number can't exceed 15 digits.")
        if (digits.toSet().size == 1) return ValidationResult.Invalid("Please enter a real phone number.")
        if (isSequentialDigits(digits)) return ValidationResult.Invalid("Please enter a real phone number.")

        return ValidationResult.Valid
    }

    fun validateEmail(value: String, allowEmpty: Boolean = false): ValidationResult {
        val trimmed = value.trim().lowercase()
        if (trimmed.isEmpty()) {
            return if (allowEmpty) ValidationResult.Valid
            else ValidationResult.Invalid("Email address is required.")
        }

        val emailRegex = Regex("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$")
        if (!emailRegex.matches(trimmed)) {
            return ValidationResult.Invalid("Enter a valid email (e.g. name@example.com).")
        }
        if (".." in trimmed) return ValidationResult.Invalid("Email can't contain consecutive dots.")

        val localPart = trimmed.substringBefore("@")
        if (localPart.startsWith(".") || localPart.endsWith(".")) {
            return ValidationResult.Invalid("Email local part can't start or end with a dot.")
        }

        val domain = trimmed.substringAfter("@")
        if (domain in disposableDomains) {
            return ValidationResult.Invalid("Please use a real email, not a disposable address.")
        }

        return ValidationResult.Valid
    }

    fun validateEventTitle(value: String): ValidationResult {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return ValidationResult.Invalid("Title is required.")
        if (trimmed.length < 3) return ValidationResult.Invalid("Title must be at least 3 characters.")
        if (trimmed.length > 100) return ValidationResult.Invalid("Title can't exceed 100 characters.")
        if (containsUrl(trimmed)) return ValidationResult.Invalid("Title can't contain links or URLs.")
        return ValidationResult.Valid
    }

    fun validateEventVenue(value: String): ValidationResult {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return ValidationResult.Invalid("Venue is required.")
        if (trimmed.length > 200) return ValidationResult.Invalid("Venue name can't exceed 200 characters.")
        return ValidationResult.Valid
    }

    fun validateEventNotes(value: String?): ValidationResult {
        val text = value?.trim() ?: return ValidationResult.Valid
        if (text.isEmpty()) return ValidationResult.Valid
        if (text.length > 500) return ValidationResult.Invalid("Notes can't exceed 500 characters.")
        if (containsUrl(text)) return ValidationResult.Invalid("Notes can't contain links or URLs.")
        return ValidationResult.Valid
    }

    fun validatePaymentInfo(value: String?): ValidationResult {
        val text = value?.trim() ?: return ValidationResult.Valid
        if (text.isEmpty()) return ValidationResult.Valid
        if (text.length > 500) return ValidationResult.Invalid("Payment info can't exceed 500 characters.")
        return ValidationResult.Valid
    }

    fun validatePrizeInfo(value: String?): ValidationResult {
        val text = value?.trim() ?: return ValidationResult.Valid
        if (text.isEmpty()) return ValidationResult.Valid
        if (text.length > 500) return ValidationResult.Invalid("Prize info can't exceed 500 characters.")
        if (containsUrl(text)) return ValidationResult.Invalid("Prize info can't contain links or URLs.")
        return ValidationResult.Valid
    }

    fun validateEntryFee(value: Double?): ValidationResult {
        val fee = value ?: return ValidationResult.Valid
        if (fee < 0) return ValidationResult.Invalid("Entry fee can't be negative.")
        if (fee > 10_000) return ValidationResult.Invalid("Entry fee can't exceed 10,000.")
        return ValidationResult.Valid
    }

    fun containsUrl(text: String): Boolean =
        Patterns.WEB_URL.matcher(text).find()

    private fun isSequentialDigits(digits: String): Boolean {
        if (digits.length < 10) return false
        val nums = digits.map { it.digitToInt() }
        for (i in 1 until nums.size) {
            if (nums[i] != (nums[i - 1] + 1) % 10) return false
        }
        return true
    }

    private val disposableDomains: Set<String> = setOf(
        "mailinator.com", "guerrillamail.com", "guerrillamail.net", "tempmail.com",
        "throwaway.email", "yopmail.com", "sharklasers.com", "guerrillamailblock.com",
        "grr.la", "dispostable.com", "trashmail.com", "trashmail.net",
        "10minutemail.com", "temp-mail.org", "fakeinbox.com", "maildrop.cc",
        "getairmail.com", "mailnesia.com", "tempr.email", "discard.email", "getnada.com",
    )
}
