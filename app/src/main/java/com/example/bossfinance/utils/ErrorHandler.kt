package com.example.bossfinance.utils

import android.content.Context
import android.util.Log
import com.example.bossfinance.R
import java.io.IOException

/**
 * Utility class to handle exceptions and errors throughout the app
 */
class ErrorHandler {
    
    companion object {
        private const val TAG = "BossFinance-ErrorHandler"
        
        /**
         * Handle generic exceptions with proper user feedback
         * 
         * @param context The context to use for string resources
         * @param exception The exception to handle
         * @param defaultMessage The default message to show if the exception type is not recognized
         * @return A user-friendly error message
         */
        fun handleException(
            context: Context,
            exception: Exception,
            defaultMessage: String = context.getString(R.string.error_generic)
        ): String {
            // Log the exception for debugging
            Log.e(TAG, "Error occurred: ${exception.message}", exception)
            
            return when (exception) {
                is IOException -> context.getString(R.string.error_storage)
                is NumberFormatException -> context.getString(R.string.error_amount_invalid)
                is IllegalArgumentException -> exception.message ?: defaultMessage
                is IllegalStateException -> exception.message ?: defaultMessage
                is SecurityException -> context.getString(R.string.error_storage)
                else -> defaultMessage
            }
        }
        
        /**
         * Log an error without user feedback
         * 
         * @param tag The tag for the log message
         * @param message The message to log
         * @param exception The exception to log (optional)
         */
        fun logError(tag: String, message: String, exception: Exception? = null) {
            if (exception != null) {
                Log.e(tag, message, exception)
            } else {
                Log.e(tag, message)
            }
        }
        
        /**
         * Validate a condition and throw a customized exception if it fails
         * 
         * @param condition The condition to validate
         * @param errorMessage The error message to include in the exception
         * @throws IllegalArgumentException if the condition is false
         */
        fun validateOrThrow(condition: Boolean, errorMessage: String) {
            if (!condition) {
                throw IllegalArgumentException(errorMessage)
            }
        }
    }
}