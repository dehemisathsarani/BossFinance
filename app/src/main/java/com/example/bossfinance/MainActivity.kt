package com.example.bossfinance

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.bossfinance.databinding.ActivityMainBinding
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    
    // Demo values for UI presentation - would be replaced with actual data in the full app
    private val currentBalance = 2580.75
    private val totalIncome = 3200.00
    private val totalExpenses = 619.25
    private val budgetLimit = 1500.00
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setSupportActionBar(binding.toolbar)
        
        // Set up the dashboard
        setupDashboardData()
        setupClickListeners()
    }
    
    private fun setupDashboardData() {
        // Format currency values
        val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US)
        
        // Update current balance
        binding.tvCurrentBalance.text = currencyFormatter.format(currentBalance)
        
        // Update income & expenses
        binding.tvTotalIncome.text = currencyFormatter.format(totalIncome)
        binding.tvTotalExpenses.text = currencyFormatter.format(totalExpenses)
        
        // Update budget progress
        val budgetPercentage = ((totalExpenses / budgetLimit) * 100).toInt()
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
            // TODO: Implement Add Transaction screen
            Toast.makeText(this, "Add Transaction clicked", Toast.LENGTH_SHORT).show()
        }
        
        binding.btnViewReports.setOnClickListener {
            // TODO: Implement View Reports screen
            Toast.makeText(this, "View Reports clicked", Toast.LENGTH_SHORT).show()
        }
        
        binding.btnBackupData.setOnClickListener {
            // TODO: Implement Backup Data functionality
            Toast.makeText(this, "Backup Data clicked", Toast.LENGTH_SHORT).show()
        }
        
        binding.fabQuickAdd.setOnClickListener {
            // TODO: Implement Quick Add Transaction dialog
            Toast.makeText(this, "Quick Add Transaction clicked", Toast.LENGTH_SHORT).show()
        }
    }
}