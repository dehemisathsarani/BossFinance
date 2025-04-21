package com.example.bossfinance.models

/**
 * Notification settings model
 */
data class NotificationSettings(
    val budgetAlertsEnabled: Boolean = true,
    val budgetAlertThreshold: Int = 90,
    val dailyRemindersEnabled: Boolean = false,
    val reminderHour: Int = 20,  // Default 8:00 PM
    val reminderMinute: Int = 0
)