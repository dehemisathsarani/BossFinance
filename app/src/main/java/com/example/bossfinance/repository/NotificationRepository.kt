package com.example.bossfinance.repository

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import com.example.bossfinance.models.NotificationSettings
import com.example.bossfinance.receivers.DailyReminderReceiver
import java.util.Calendar

/**
 * Repository for managing notification settings
 */
class NotificationRepository private constructor(private val context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )
    
    /**
     * Save notification settings
     */
    fun saveNotificationSettings(settings: NotificationSettings) {
        with(sharedPreferences.edit()) {
            putBoolean(KEY_BUDGET_ALERTS_ENABLED, settings.budgetAlertsEnabled)
            putInt(KEY_BUDGET_ALERT_THRESHOLD, settings.budgetAlertThreshold)
            putBoolean(KEY_DAILY_REMINDERS_ENABLED, settings.dailyRemindersEnabled)
            putInt(KEY_REMINDER_HOUR, settings.reminderHour)
            putInt(KEY_REMINDER_MINUTE, settings.reminderMinute)
            apply()
        }
        
        if (settings.dailyRemindersEnabled) {
            scheduleDailyReminder(settings.reminderHour, settings.reminderMinute)
        } else {
            cancelDailyReminder()
        }
    }
    
    /**
     * Get current notification settings
     */
    fun getNotificationSettings(): NotificationSettings {
        val budgetAlertsEnabled = sharedPreferences.getBoolean(KEY_BUDGET_ALERTS_ENABLED, true)
        val budgetAlertThreshold = sharedPreferences.getInt(KEY_BUDGET_ALERT_THRESHOLD, 90)
        val dailyRemindersEnabled = sharedPreferences.getBoolean(KEY_DAILY_REMINDERS_ENABLED, false)
        val reminderHour = sharedPreferences.getInt(KEY_REMINDER_HOUR, 20)
        val reminderMinute = sharedPreferences.getInt(KEY_REMINDER_MINUTE, 0)
        
        return NotificationSettings(
            budgetAlertsEnabled = budgetAlertsEnabled,
            budgetAlertThreshold = budgetAlertThreshold,
            dailyRemindersEnabled = dailyRemindersEnabled,
            reminderHour = reminderHour,
            reminderMinute = reminderMinute
        )
    }
    
    /**
     * Schedule daily reminder using AlarmManager
     */
    private fun scheduleDailyReminder(hour: Int, minute: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, DailyReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REMINDER_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Set time for the reminder
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            
            // If the time is in the past, set it for the next day
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        
        // Schedule the alarm to repeat daily
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }
    
    /**
     * Cancel daily reminder
     */
    private fun cancelDailyReminder() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, DailyReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REMINDER_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        alarmManager.cancel(pendingIntent)
    }
    
    companion object {
        private const val PREFS_NAME = "boss_finance_notification_prefs"
        private const val KEY_BUDGET_ALERTS_ENABLED = "budget_alerts_enabled"
        private const val KEY_BUDGET_ALERT_THRESHOLD = "budget_alert_threshold"
        private const val KEY_DAILY_REMINDERS_ENABLED = "daily_reminders_enabled"
        private const val KEY_REMINDER_HOUR = "reminder_hour"
        private const val KEY_REMINDER_MINUTE = "reminder_minute"
        private const val REMINDER_REQUEST_CODE = 2001
        
        @Volatile
        private var INSTANCE: NotificationRepository? = null
        
        fun getInstance(context: Context): NotificationRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = NotificationRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}