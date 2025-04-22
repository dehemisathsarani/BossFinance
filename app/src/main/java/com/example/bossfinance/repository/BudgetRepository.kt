package com.example.bossfinance.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.bossfinance.models.Budget
import java.util.Currency
import java.util.Locale

/**
 * Repository for managing budget settings
 * Uses SharedPreferences to persist budget data
 */
class BudgetRepository private constructor(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )
    
    /**
     * Save budget settings
     */
    fun saveBudget(budget: Budget) {
        with(sharedPreferences.edit()) {
            putFloat(KEY_BUDGET_AMOUNT, budget.amount.toFloat())
            putString(KEY_BUDGET_CURRENCY, budget.currency.currencyCode)
            putBoolean(KEY_NOTIFICATION_ENABLED, budget.notificationEnabled)
            putInt(KEY_NOTIFICATION_THRESHOLD, budget.notificationThreshold)
            apply()
        }
    }
    
    /**
     * Get current budget settings
     */
    fun getBudget(): Budget {
        val amount = sharedPreferences.getFloat(KEY_BUDGET_AMOUNT, 1500.00f).toDouble()
        val currencyCode = sharedPreferences.getString(
            KEY_BUDGET_CURRENCY, 
            Currency.getInstance(Locale.getDefault()).currencyCode
        )
        val notificationEnabled = sharedPreferences.getBoolean(KEY_NOTIFICATION_ENABLED, true)
        val notificationThreshold = sharedPreferences.getInt(KEY_NOTIFICATION_THRESHOLD, 90)
        
        val currency = try {
            Currency.getInstance(currencyCode)
        } catch (e: Exception) {
            Currency.getInstance(Locale.getDefault())
        }
        
        return Budget(
            amount = amount,
            currency = currency,
            notificationEnabled = notificationEnabled,
            notificationThreshold = notificationThreshold
        )
    }
    
    /**
     * Check if a budget has been set
     */
    fun hasBudgetSet(): Boolean {
        return sharedPreferences.contains(KEY_BUDGET_AMOUNT)
    }
    
    /**
     * Get budget usage percentage
     */
    fun getBudgetUsagePercentage(expenses: Double): Int {
        val budget = getBudget()
        return ((expenses / budget.amount) * 100).toInt().coerceIn(0, 100)
    }
    
    /**
     * Check if budget threshold has been exceeded
     */
    fun isThresholdExceeded(expenses: Double): Boolean {
        val budget = getBudget()
        val usagePercentage = getBudgetUsagePercentage(expenses)
        return usagePercentage >= budget.notificationThreshold
    }
    
    /**
     * Get monthly budget amount
     * This method is used by the dashboard to get just the amount
     */
    fun getMonthlyBudget(): Double {
        return getBudget().amount
    }
    
    companion object {
        private const val PREFS_NAME = "boss_finance_budget_prefs"
        private const val KEY_BUDGET_AMOUNT = "budget_amount"
        private const val KEY_BUDGET_CURRENCY = "budget_currency"
        private const val KEY_NOTIFICATION_ENABLED = "notification_enabled"
        private const val KEY_NOTIFICATION_THRESHOLD = "notification_threshold"
        
        @Volatile
        private var INSTANCE: BudgetRepository? = null
        
        fun getInstance(context: Context): BudgetRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = BudgetRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
