package com.example.bossfinance

import android.os.Bundle
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.bossfinance.databinding.ActivityBudgetSetupBinding
import com.example.bossfinance.models.Budget
import com.example.bossfinance.repository.BudgetRepository
import java.util.Currency
import java.util.Locale

class BudgetSetupActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBudgetSetupBinding
    private lateinit var budgetRepository: BudgetRepository
    private var notificationThreshold = 90
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBudgetSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        budgetRepository = BudgetRepository.getInstance(this)
        
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.budget_setup)
        
        setupCurrencySpinner()
        setupCurrentValues()
        setupNotificationThresholdControl()
        setupSaveButton()
    }
    
    private fun setupCurrencySpinner() {
        val availableCurrencies = Currency.getAvailableCurrencies().sortedBy { it.displayName }
        val currencyNames = availableCurrencies.map { "${it.displayName} (${it.symbol})" }.toTypedArray()
        
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, currencyNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCurrency.adapter = adapter
        
        // Set default selection to current system currency or user's saved preference
        val currentBudget = budgetRepository.getBudget()
        val currentCurrencyIndex = availableCurrencies.indexOfFirst { 
            it.currencyCode == currentBudget.currency.currencyCode 
        }
        if (currentCurrencyIndex != -1) {
            binding.spinnerCurrency.setSelection(currentCurrencyIndex)
        }
    }
    
    private fun setupCurrentValues() {
        val budget = budgetRepository.getBudget()
        
        // Set current budget amount
        binding.etBudgetAmount.setText(budget.amount.toString())
        
        // Set notification state
        binding.switchNotifications.isChecked = budget.notificationEnabled
        
        // Set notification threshold
        notificationThreshold = budget.notificationThreshold
        binding.seekBarThreshold.progress = notificationThreshold
        updateThresholdLabel()
    }
    
    private fun setupNotificationThresholdControl() {
        binding.seekBarThreshold.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    notificationThreshold = progress
                    updateThresholdLabel()
                }
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }
    
    private fun updateThresholdLabel() {
        binding.tvThresholdValue.text = "$notificationThreshold%"
    }
    
    private fun setupSaveButton() {
        binding.btnSaveBudget.setOnClickListener {
            if (validateInputs()) {
                saveBudgetSettings()
            }
        }
    }
    
    private fun validateInputs(): Boolean {
        val amountText = binding.etBudgetAmount.text.toString()
        if (amountText.isBlank() || amountText.toDoubleOrNull() == null || amountText.toDouble() <= 0) {
            binding.budgetAmountInputLayout.error = getString(R.string.please_enter_valid_amount)
            return false
        }
        binding.budgetAmountInputLayout.error = null
        return true
    }
    
    private fun saveBudgetSettings() {
        val amount = binding.etBudgetAmount.text.toString().toDouble()
        
        // Get selected currency
        val availableCurrencies = Currency.getAvailableCurrencies().sortedBy { it.displayName }
        val selectedCurrency = availableCurrencies[binding.spinnerCurrency.selectedItemPosition]
        
        val notificationsEnabled = binding.switchNotifications.isChecked
        
        val budget = Budget(
            amount = amount,
            currency = selectedCurrency,
            notificationEnabled = notificationsEnabled,
            notificationThreshold = notificationThreshold
        )
        
        budgetRepository.saveBudget(budget)
        
        Toast.makeText(this, getString(R.string.budget_saved), Toast.LENGTH_SHORT).show()
        finish()
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
