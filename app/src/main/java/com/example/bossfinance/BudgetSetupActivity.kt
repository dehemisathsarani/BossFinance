package com.example.bossfinance

import android.os.Bundle
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import com.example.bossfinance.databinding.ActivityBudgetSetupBinding
import com.example.bossfinance.models.Budget
import com.example.bossfinance.repository.BudgetRepository
import com.example.bossfinance.utils.ErrorHandler
import com.example.bossfinance.utils.FeedbackUtils
import com.example.bossfinance.utils.InputValidator
import java.util.Currency
import java.util.Locale

class BudgetSetupActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBudgetSetupBinding
    private lateinit var budgetRepository: BudgetRepository
    private lateinit var inputValidator: InputValidator
    private var notificationThreshold = 90
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBudgetSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        budgetRepository = BudgetRepository.getInstance(this)
        inputValidator = InputValidator(this)
        
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.budget_setup)
        
        setupCurrencySpinner()
        setupCurrentValues()
        setupInputValidation()
        setupNotificationThresholdControl()
        setupSaveButton()
    }
    
    private fun setupCurrencySpinner() {
        try {
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
        } catch (e: Exception) {
            val errorMessage = ErrorHandler.handleException(this, e)
            FeedbackUtils.showErrorSnackbar(binding.root, errorMessage)
            // Fall back to default locale currency
            binding.spinnerCurrency.setSelection(0)
        }
    }
    
    private fun setupCurrentValues() {
        try {
            val budget = budgetRepository.getBudget()
            
            // Set current budget amount
            binding.etBudgetAmount.setText(budget.amount.toString())
            
            // Set notification state
            binding.switchNotifications.isChecked = budget.notificationEnabled
            
            // Set notification threshold
            notificationThreshold = budget.notificationThreshold
            binding.seekBarThreshold.progress = notificationThreshold
            updateThresholdLabel()
        } catch (e: Exception) {
            val errorMessage = ErrorHandler.handleException(this, e)
            FeedbackUtils.showErrorSnackbar(binding.root, errorMessage)
        }
    }
    
    private fun setupInputValidation() {
        // Clear error as the user types
        binding.etBudgetAmount.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.budgetAmountInputLayout.error = null
        }
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
        // Validate amount using our InputValidator
        val amountError = inputValidator.validateAmount(binding.etBudgetAmount.text.toString())
        if (amountError != null) {
            binding.budgetAmountInputLayout.error = amountError
            return false
        }
        
        binding.budgetAmountInputLayout.error = null
        return true
    }
    
    private fun saveBudgetSettings() {
        try {
            val amountText = binding.etBudgetAmount.text.toString()
            val amount = amountText.toDouble()
            
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
            
            FeedbackUtils.showSuccessSnackbar(binding.root, getString(R.string.budget_saved))
            
            // Close activity after a short delay to show the snackbar
            binding.root.postDelayed({ finish() }, 1000)
        } catch (e: Exception) {
            val errorMessage = ErrorHandler.handleException(this, e)
            FeedbackUtils.showErrorSnackbar(binding.root, errorMessage)
        }
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
