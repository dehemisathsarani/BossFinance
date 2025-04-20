package com.example.bossfinance

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.bossfinance.databinding.ActivityMainBinding
import com.example.bossfinance.repository.TransactionRepository
import java.text.NumberFormat
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val transactionRepository = TransactionRepository.getInstance()
    private val budgetLimit = 1500.00 // This would typically come from user settings
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setSupportActionBar(binding.toolbar)
        
        setupClickListeners()
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh dashboard data when returning to this screen
        updateDashboardData()
    }
    
    private fun updateDashboardData() {
        // Get real data from repository
        val currentBalance = transactionRepository.getCurrentBalance()
        val totalIncome = transactionRepository.getTotalIncome()
        val totalExpenses = transactionRepository.getTotalExpenses()
        
        // Format currency values
        val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US)
        
        // Update current balance
        binding.tvCurrentBalance.text = currencyFormatter.format(currentBalance)
        
        // Update income & expenses
        binding.tvTotalIncome.text = currencyFormatter.format(totalIncome)
        binding.tvTotalExpenses.text = currencyFormatter.format(totalExpenses)
        
        // Update budget progress
        val budgetPercentage = ((totalExpenses / budgetLimit) * 100).toInt().coerceAtMost(100)
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
            .replace("$0 monthly budget", currencyFormatter.format(budgetLimit) + " monthly budget")
    }
    
    private fun setupClickListeners() {
        // Set up click listeners for the buttons
        binding.btnAddTransaction.setOnClickListener {
            val intent = Intent(this, TransactionListActivity::class.java)
            startActivity(intent)
        }
        
        binding.btnViewReports.setOnClickListener {
            // Navigate to Spending Analysis screen
            val intent = Intent(this, SpendingAnalysisActivity::class.java)
            startActivity(intent)
        }
        
        binding.btnBackupData.setOnClickListener {
            // TODO: Implement Backup Data functionality
            Toast.makeText(this, "Backup Data clicked", Toast.LENGTH_SHORT).show()
        }
        
        binding.fabQuickAdd.setOnClickListener {
            val intent = Intent(this, TransactionEditActivity::class.java)
            startActivity(intent)
        }
    }
}