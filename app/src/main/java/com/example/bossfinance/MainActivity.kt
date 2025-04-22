package com.example.bossfinance

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.fragment.app.Fragment
import com.example.bossfinance.databinding.ActivityMainBinding
import com.example.bossfinance.models.Budget
import com.example.bossfinance.repository.BudgetRepository
import com.example.bossfinance.repository.NotificationRepository
import com.example.bossfinance.repository.TransactionRepository
import com.example.bossfinance.ui.dashboard.DashboardFragment
import com.example.bossfinance.ui.history.HistoryFragment
import com.example.bossfinance.ui.reports.ReportsFragment
import com.example.bossfinance.ui.settings.SettingsFragment
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
        
        // Setup navigation
        setupBottomNavigation()
        
        // Setup FAB for quick add transaction
        binding.fabQuickAdd.setOnClickListener {
            val intent = Intent(this, TransactionEditActivity::class.java)
            startActivity(intent)
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Check if budget threshold is exceeded and show notification if needed
        checkBudgetThreshold()
    }
    
    private fun setupBottomNavigation() {
        // Set default fragment
        if (supportFragmentManager.findFragmentById(R.id.nav_host_fragment) == null) {
            loadFragment(DashboardFragment())
        }
        
        // Set up bottom navigation click listener
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_dashboard -> {
                    loadFragment(DashboardFragment())
                    return@setOnItemSelectedListener true
                }
                R.id.navigation_history -> {
                    loadFragment(HistoryFragment())
                    return@setOnItemSelectedListener true
                }
                R.id.navigation_add -> {
                    // Open transaction add screen directly
                    val intent = Intent(this, TransactionEditActivity::class.java)
                    startActivity(intent)
                    return@setOnItemSelectedListener false
                }
                R.id.navigation_reports -> {
                    loadFragment(ReportsFragment())
                    return@setOnItemSelectedListener true
                }
                R.id.navigation_settings -> {
                    loadFragment(SettingsFragment())
                    return@setOnItemSelectedListener true
                }
                else -> false
            }
        }
    }
    
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, fragment)
            .commit()
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