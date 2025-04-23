package com.example.bossfinance.utils

import android.content.Context
import android.text.TextUtils
import android.util.Patterns
import com.example.bossfinance.R
import java.util.regex.Pattern

/**
 * Utility class for handling input validation throughout the app
 */
class InputValidator(private val context: Context) {

    /**
     * Validate text input is not empty
     * @return Null if valid, error message if invalid
     */
    fun validateRequired(text: String?): String? {
        return if (text.isNullOrBlank()) {
            context.getString(R.string.error_field_required)
        } else null
    }

    /**
     * Validate amount is valid (positive number)
     * @return Null if valid, error message if invalid
     */
    fun validateAmount(amount: String?): String? {
        if (amount.isNullOrBlank()) {
            return context.getString(R.string.error_field_required)
        }
        
        return try {
            val value = amount.toDouble()
            if (value <= 0) {
                context.getString(R.string.error_amount_positive)
            } else null
        } catch (e: NumberFormatException) {
            context.getString(R.string.error_amount_invalid)
        }
    }

    /**
     * Validate email format
     * @return Null if valid, error message if invalid
     */
    fun validateEmail(email: String?): String? {
        if (email.isNullOrBlank()) {
            return context.getString(R.string.error_field_required)
        }
        
        return if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            context.getString(R.string.error_email_invalid)
        } else null
    }

    /**
     * Validate a date is within a reasonable range
     * @return Null if valid, error message if invalid
     */
    fun validateDate(dateInMillis: Long): String? {
        val now = System.currentTimeMillis()
        val oneYearAgo = now - (365L * 24 * 60 * 60 * 1000)
        val fiveYearsFromNow = now + (5L * 365 * 24 * 60 * 60 * 1000)
        
        return when {
            dateInMillis < oneYearAgo -> context.getString(R.string.error_date_too_old)
            dateInMillis > fiveYearsFromNow -> context.getString(R.string.error_date_future)
            else -> null
        }
    }

    /**
     * Validate category is selected
     * @return Null if valid, error message if invalid
     */
    fun validateCategory(category: String?): String? {
        return if (category.isNullOrBlank()) {
            context.getString(R.string.error_category_required)
        } else null
    }
}