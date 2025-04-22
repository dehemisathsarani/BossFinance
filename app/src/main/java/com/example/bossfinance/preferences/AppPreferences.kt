package com.example.bossfinance.preferences

import android.content.Context
import android.content.SharedPreferences

/**
 * Singleton class to manage application preferences
 */
class AppPreferences private constructor(context: Context) {
    
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        PREFERENCES_NAME, Context.MODE_PRIVATE
    )
    
    companion object {
        private const val PREFERENCES_NAME = "boss_finance_preferences"
        
        // Budget Settings
        private const val KEY_MONTHLY_BUDGET = "monthly_budget"
        private const val KEY_CURRENCY_CODE = "currency_code"
        
        // Notification Settings
        private const val KEY_BUDGET_ALERTS_ENABLED = "budget_alerts_enabled"
        private const val KEY_TRANSACTION_REMINDERS_ENABLED = "transaction_reminders_enabled"
        
        // Theme Settings
        private const val KEY_DARK_MODE = "dark_mode"
        
        @Volatile
        private var instance: AppPreferences? = null
        
        fun getInstance(context: Context): AppPreferences {
            return instance ?: synchronized(this) {
                instance ?: AppPreferences(context.applicationContext).also { instance = it }
            }
        }
    }
    
    // Budget Settings
    
    fun setMonthlyBudget(budget: Double) {
        sharedPreferences.edit().putFloat(KEY_MONTHLY_BUDGET, budget.toFloat()).apply()
    }
    
    fun getMonthlyBudget(): Double {
        return sharedPreferences.getFloat(KEY_MONTHLY_BUDGET, 0f).toDouble()
    }
    
    fun setCurrencyCode(code: String) {
        sharedPreferences.edit().putString(KEY_CURRENCY_CODE, code).apply()
    }
    
    fun getCurrencyCode(): String {
        return sharedPreferences.getString(KEY_CURRENCY_CODE, "USD") ?: "USD"
    }
    
    // Notification Settings
    
    fun setBudgetAlertsEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_BUDGET_ALERTS_ENABLED, enabled).apply()
    }
    
    fun getBudgetAlertsEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_BUDGET_ALERTS_ENABLED, true)
    }
    
    fun setTransactionRemindersEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_TRANSACTION_REMINDERS_ENABLED, enabled).apply()
    }
    
    fun getTransactionRemindersEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_TRANSACTION_REMINDERS_ENABLED, false)
    }
    
    // Theme Settings
    
    fun setDarkMode(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_DARK_MODE, enabled).apply()
    }
    
    fun getDarkMode(): Boolean {
        return sharedPreferences.getBoolean(KEY_DARK_MODE, false)
    }
    
    // App First Launch
    
    fun isFirstLaunch(): Boolean {
        val key = "is_first_launch"
        val isFirst = sharedPreferences.getBoolean(key, true)
        
        if (isFirst) {
            sharedPreferences.edit().putBoolean(key, false).apply()
        }
        
        return isFirst
    }
    
    // Clear all preferences
    
    fun clearAll() {
        sharedPreferences.edit().clear().apply()
    }
}