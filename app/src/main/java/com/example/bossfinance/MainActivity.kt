package com.example.bossfinance

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import com.example.bossfinance.databinding.ActivityMainBinding
import com.example.bossfinance.models.Budget
import com.example.bossfinance.repository.BudgetRepository
import com.example.bossfinance.repository.NotificationRepository
import com.example.bossfinance.repository.TransactionRepository
import java.text.NumberFormat
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var budgetRepository: BudgetRepository
    private lateinit var notificationRepository: NotificationRepository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        budgetRepository = BudgetRepository.getInstance(this)
        transactionRepository = TransactionRepository.getInstance(this)
        notificationRepository = NotificationRepository.getInstance(this)
        
        setSupportActionBar(binding.toolbar)
        
        // Create notification channels for budget alerts and daily reminders
        createNotificationChannels()
        
        setupClickListeners()
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh dashboard data when returning to this screen
        updateDashboardData()
        
        // Check if budget threshold is exceeded and show notification if needed
        checkBudgetThreshold()
    }
    
    private fun updateDashboardData() {
        // Get real data from repository
        val currentBalance = transactionRepository.getCurrentBalance()
        val totalIncome = transactionRepository.getTotalIncome()
        val totalExpenses = transactionRepository.getTotalExpenses()
        
        // Get budget from repository
        val budget = budgetRepository.getBudget()
        
        // Format currency values with the user-selected currency
        val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.getDefault())
        currencyFormatter.currency = budget.currency
        
        // Update current balance
        binding.tvCurrentBalance.text = currencyFormatter.format(currentBalance)
        
        // Update income & expenses
        binding.tvTotalIncome.text = currencyFormatter.format(totalIncome)
        binding.tvTotalExpenses.text = currencyFormatter.format(totalExpenses)
        
        // Update budget progress
        val budgetPercentage = budgetRepository.getBudgetUsagePercentage(totalExpenses)
        binding.tvBudgetPercentage.text = "$budgetPercentage%"
        binding.budgetProgressBar.progress = budgetPercentage
        
        // Change progress bar color based on budget usage
        val colorRes = when {
            budgetPercentage < 50 -> android.R.color.holo_green_dark
            budgetPercentage < 80 -> android.R.color.holo_orange_dark
            else -> android.R.color.holo_red_dark
        }
        binding.budgetProgressBar.setIndicatorColor(getColor(colorRes))
        
        // Update budget info text
        binding.tvBudgetInfo.text = getString(R.string.budget_info)
            .replace("$0 spent", currencyFormatter.format(totalExpenses) + " spent")
            .replace("$0 monthly budget", currencyFormatter.format(budget.amount) + " monthly budget")
    }
    
    private fun setupClickListeners() {
        // Set up click listeners for the buttons
        binding.btnAddTransaction.setOnClickListener {
            // Navigate directly to transaction edit screen for adding a new transaction
            val intent = Intent(this, TransactionEditActivity::class.java)
            startActivity(intent)
        }
        
        binding.btnViewReports.setOnClickListener {
            // Navigate to Spending Analysis screen
            val intent = Intent(this, SpendingAnalysisActivity::class.java)
            startActivity(intent)
        }
        
        binding.btnSetBudget.setOnClickListener {
            // Navigate to Budget Setup screen
            val intent = Intent(this, BudgetSetupActivity::class.java)
            startActivity(intent)
        }
        
        binding.btnNotifications.setOnClickListener {
            // Navigate to Notifications settings screen
            val intent = Intent(this, NotificationsActivity::class.java)
            startActivity(intent)
        }
        
        binding.btnBackupData.setOnClickListener {
            // Navigate to Backup & Restore screen
            val intent = Intent(this, BackupRestoreActivity::class.java)
            startActivity(intent)
        }
        
        binding.fabQuickAdd.setOnClickListener {
            val intent = Intent(this, TransactionEditActivity::class.java)
            startActivity(intent)
        }
        
        // Add click listener to budget progress section to navigate to budget setup
        binding.budgetProgressBar.setOnClickListener {
            val intent = Intent(this, BudgetSetupActivity::class.java)
            startActivity(intent)
        }
        
        binding.tvBudgetPercentage.setOnClickListener {
            val intent = Intent(this, BudgetSetupActivity::class.java)
            startActivity(intent)
        }
    }
    
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Budget alert channel
            val budgetAlertName = getString(R.string.budget_alert_title)
            val budgetAlertDesc = getString(R.string.budget_alert_message, 0, "$0", "$0")
            val budgetAlertChannel = NotificationChannel(
                BUDGET_CHANNEL_ID, 
                budgetAlertName, 
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = budgetAlertDesc
            }
            
            // Daily reminder channel
            val reminderName = getString(R.string.daily_reminder_title)
            val reminderDesc = getString(R.string.daily_reminder_message)
            val reminderChannel = NotificationChannel(
                REMINDER_CHANNEL_ID, 
                reminderName, 
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = reminderDesc
            }
            
            // Register channels with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(budgetAlertChannel)
            notificationManager.createNotificationChannel(reminderChannel)
        }
    }
    
    private fun checkBudgetThreshold() {
        val budget = budgetRepository.getBudget()
        val totalExpenses = transactionRepository.getTotalExpenses()
        val notificationSettings = notificationRepository.getNotificationSettings()
        
        // Check if budget alerts are enabled and threshold is exceeded
        if (notificationSettings.budgetAlertsEnabled) {
            // Use threshold from notification settings
            val percentage = budgetRepository.getBudgetUsagePercentage(totalExpenses)
            if (percentage >= notificationSettings.budgetAlertThreshold) {
                showBudgetAlert(budget, totalExpenses)
            }
        }
    }
    
    private fun showBudgetAlert(budget: Budget, expenses: Double) {
        val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.getDefault())
        currencyFormatter.currency = budget.currency
        
        val percentage = budgetRepository.getBudgetUsagePercentage(expenses)
        val expensesFormatted = currencyFormatter.format(expenses)
        val budgetFormatted = currencyFormatter.format(budget.amount)
        
        // Show notification
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(this, BUDGET_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.budget_alert_title))
            .setContentText(getString(R.string.budget_alert_message, percentage, expensesFormatted, budgetFormatted))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(BUDGET_NOTIFICATION_ID, notification)
    }
    
    companion object {
        private const val BUDGET_CHANNEL_ID = "budget_alert_channel"
        private const val REMINDER_CHANNEL_ID = "daily_reminder_channel"
        private const val BUDGET_NOTIFICATION_ID = 1001
    }
}