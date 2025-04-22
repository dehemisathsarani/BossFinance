package com.example.bossfinance.receivers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.bossfinance.BudgetSetupActivity
import com.example.bossfinance.R
import com.example.bossfinance.models.Budget
import com.example.bossfinance.repository.BudgetRepository
import com.example.bossfinance.repository.NotificationRepository
import com.example.bossfinance.repository.TransactionRepository
import java.text.NumberFormat
import java.util.Locale

/**
 * BroadcastReceiver to handle budget threshold exceeded alerts
 */
class BudgetMonitorReceiver : BroadcastReceiver() {

    companion object {
        const val BUDGET_CHANNEL_ID = "budget_alert_channel"
        const val BUDGET_NOTIFICATION_ID = 1001
    }

    override fun onReceive(context: Context, intent: Intent) {
        // Initialize repositories
        val budgetRepository = BudgetRepository.getInstance(context)
        val transactionRepository = TransactionRepository.getInstance(context)
        val notificationRepository = NotificationRepository.getInstance(context)
        
        // Get budget settings
        val budget = budgetRepository.getBudget()
        val totalExpenses = transactionRepository.getTotalExpenses()
        val notificationSettings = notificationRepository.getNotificationSettings()
        
        // Check if budget alerts are enabled and threshold is exceeded
        if (notificationSettings.budgetAlertsEnabled) {
            val percentage = budgetRepository.getBudgetUsagePercentage(totalExpenses)
            
            // Check if the percentage exceeds the threshold
            if (percentage >= notificationSettings.budgetAlertThreshold) {
                showBudgetExceededAlert(context, budget, totalExpenses, percentage)
            }
        }
    }
    
    private fun showBudgetExceededAlert(context: Context, budget: Budget, expenses: Double, percentage: Int) {
        // Create notification channel for budget alerts (for Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val budgetAlertName = context.getString(R.string.budget_alert_title)
            val budgetAlertDesc = context.getString(R.string.budget_alert_message)
            val importance = NotificationManager.IMPORTANCE_HIGH
            
            val channel = NotificationChannel(BUDGET_CHANNEL_ID, budgetAlertName, importance).apply {
                description = budgetAlertDesc
            }
            
            // Register the channel with the system
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
        
        // Format the currency values
        val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.getDefault())
        currencyFormatter.currency = budget.currency
        val expensesFormatted = currencyFormatter.format(expenses)
        val budgetFormatted = currencyFormatter.format(budget.amount)
        
        // Create an intent to open the Budget Setup screen when notification is tapped
        val intent = Intent(context, BudgetSetupActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 
            0, 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Build the notification
        val notification = NotificationCompat.Builder(context, BUDGET_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.budget_alert_title))
            .setContentText(context.getString(R.string.budget_alert_message, percentage, expensesFormatted, budgetFormatted))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        // Show the notification
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(BUDGET_NOTIFICATION_ID, notification)
    }
}