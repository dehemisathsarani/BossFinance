package com.example.bossfinance

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.example.bossfinance.databinding.ActivityTransactionEditBinding
import com.example.bossfinance.models.Transaction
import com.example.bossfinance.models.TransactionCategories
import com.example.bossfinance.repository.NotificationRepository
import com.example.bossfinance.repository.TransactionRepository
import com.example.bossfinance.utils.ErrorHandler
import com.example.bossfinance.utils.FeedbackUtils
import com.example.bossfinance.utils.InputValidator
import com.google.android.material.tabs.TabLayout
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TransactionEditActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTransactionEditBinding
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var notificationRepository: NotificationRepository
    private lateinit var inputValidator: InputValidator
    
    private var transactionDate = Calendar.getInstance()
    private var isIncome = true
    private var transactionId: String? = null
    private var isEditMode = false
    
    private val dateFormatter = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize binding and set content view
        binding = ActivityTransactionEditBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Initialize repositories and utilities
        transactionRepository = TransactionRepository.getInstance(applicationContext)
        notificationRepository = NotificationRepository.getInstance(applicationContext)
        inputValidator = InputValidator(this)
        
        // Check if we're editing an existing transaction
        transactionId = intent.getStringExtra(EXTRA_TRANSACTION_ID)
        isEditMode = transactionId != null
        
        // Setup UI components
        setupToolbar()
        setupTabLayout()
        setupDatePicker()
        setupInputValidation()
        setupSaveButton()
        setupDeleteButton()
        
        // Load transaction data if in edit mode
        if (isEditMode) {
            loadTransactionData()
        } else {
            // Default to income tab and setup category spinner
            updateCategorySpinner(isIncome)
            updateDateDisplay()
        }
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = if (isEditMode) 
            getString(R.string.edit_transaction) 
        else 
            getString(R.string.add_new_transaction)
    }
    
    private fun setupTabLayout() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                isIncome = tab.position == 0
                updateCategorySpinner(isIncome)
            }
            
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }
    
    private fun setupDatePicker() {
        binding.tvDatePicker.setOnClickListener {
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    transactionDate.set(Calendar.YEAR, year)
                    transactionDate.set(Calendar.MONTH, month)
                    transactionDate.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    updateDateDisplay()
                },
                transactionDate.get(Calendar.YEAR),
                transactionDate.get(Calendar.MONTH),
                transactionDate.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }
    
    private fun setupInputValidation() {
        // Add text change listeners to clear errors as user types
        binding.etTransactionTitle.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.titleInputLayout.error = null
        }
        
        binding.etAmount.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.amountInputLayout.error = null
        }
    }
    
    private fun updateDateDisplay() {
        binding.tvDatePicker.text = dateFormatter.format(transactionDate.time)
    }
    
    private fun updateCategorySpinner(isIncomeCategory: Boolean) {
        val categories = if (isIncomeCategory) 
            TransactionCategories.INCOME_CATEGORIES 
        else 
            TransactionCategories.EXPENSE_CATEGORIES
        
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            categories
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategory.adapter = adapter
        
        // If not editing an existing transaction, select the last used category for this type
        if (!isEditMode) {
            val lastUsedCategory = transactionRepository.getLastUsedCategory(isIncome)
            val position = categories.indexOf(lastUsedCategory)
            if (position != -1) {
                binding.spinnerCategory.setSelection(position)
            }
        }
    }
    
    private fun setupSaveButton() {
        binding.btnSaveTransaction.setOnClickListener {
            if (validateInputs()) {
                saveTransaction()
            }
        }
    }
    
    private fun setupDeleteButton() {
        // Only show delete button in edit mode
        if (isEditMode) {
            binding.btnDeleteTransaction.visibility = View.VISIBLE
            binding.btnDeleteTransaction.setOnClickListener {
                showDeleteConfirmationDialog()
            }
        } else {
            binding.btnDeleteTransaction.visibility = View.GONE
        }
    }
    
    private fun loadTransactionData() {
        try {
            val transaction = transactionId?.let { transactionRepository.getTransaction(it) }
            
            transaction?.let {
                // Set income/expense tab
                isIncome = it.isIncome
                binding.tabLayout.getTabAt(if (isIncome) 0 else 1)?.select()
                
                // Set fields
                binding.etTransactionTitle.setText(it.title)
                binding.etAmount.setText(it.amount.toString())
                
                // Set date
                transactionDate.time = it.date
                updateDateDisplay()
                
                // Set category spinner
                updateCategorySpinner(isIncome)
                val categoryPosition = if (isIncome) {
                    TransactionCategories.INCOME_CATEGORIES.indexOf(it.category)
                } else {
                    TransactionCategories.EXPENSE_CATEGORIES.indexOf(it.category)
                }
                if (categoryPosition != -1) {
                    binding.spinnerCategory.setSelection(categoryPosition)
                }
            } ?: run {
                // Transaction not found - show error and close activity
                FeedbackUtils.showErrorSnackbar(binding.root, getString(R.string.error_generic))
                finish()
            }
        } catch (e: Exception) {
            val errorMessage = ErrorHandler.handleException(this, e)
            FeedbackUtils.showErrorSnackbar(binding.root, errorMessage)
            finish()
        }
    }
    
    private fun validateInputs(): Boolean {
        var isValid = true
        
        // Validate title
        val titleError = inputValidator.validateRequired(binding.etTransactionTitle.text.toString())
        if (titleError != null) {
            binding.titleInputLayout.error = titleError
            isValid = false
        } else {
            binding.titleInputLayout.error = null
        }
        
        // Validate amount
        val amountError = inputValidator.validateAmount(binding.etAmount.text.toString())
        if (amountError != null) {
            binding.amountInputLayout.error = amountError
            isValid = false
        } else {
            binding.amountInputLayout.error = null
        }
        
        // Validate date (optional validation for demonstration)
        val dateError = inputValidator.validateDate(transactionDate.timeInMillis)
        if (dateError != null) {
            FeedbackUtils.showToast(this, dateError)
            isValid = false
        }
        
        // Validate category
        val category = binding.spinnerCategory.selectedItem?.toString()
        val categoryError = inputValidator.validateCategory(category)
        if (categoryError != null) {
            FeedbackUtils.showToast(this, categoryError)
            isValid = false
        }
        
        return isValid
    }
    
    private fun saveTransaction() {
        val title = binding.etTransactionTitle.text.toString().trim()
        
        // Parse the amount safely
        val amountStr = binding.etAmount.text.toString()
        val amount = try {
            amountStr.toDouble()
        } catch (e: NumberFormatException) {
            // This should be caught by validateInputs(), but added for safety
            binding.amountInputLayout.error = getString(R.string.error_amount_invalid)
            return
        }
        
        // Get category (already validated)
        val category = binding.spinnerCategory.selectedItem.toString()
        val date = transactionDate.time
        
        try {
            if (isEditMode && transactionId != null) {
                // Update existing transaction
                val transaction = Transaction(
                    id = transactionId!!,
                    title = title,
                    amount = amount,
                    category = category,
                    date = date,
                    isIncome = isIncome
                )
                transactionRepository.updateTransaction(transaction)
                FeedbackUtils.showSuccessSnackbar(binding.root, getString(R.string.transaction_saved))
            } else {
                // Create new transaction
                val transaction = Transaction(
                    title = title,
                    amount = amount,
                    category = category,
                    date = date,
                    isIncome = isIncome
                )
                transactionRepository.addTransaction(transaction)
                FeedbackUtils.showSuccessSnackbar(binding.root, getString(R.string.transaction_saved))
            }
            
            // Check budget threshold after adding/updating a transaction
            if (!isIncome) {
                notificationRepository.checkBudgetThresholdNow()
            }
            
            // Close activity after a short delay to allow snackbar to be seen
            binding.root.postDelayed({ finish() }, 1000)
        } catch (e: Exception) {
            // Handle any unexpected errors during save
            val errorMessage = ErrorHandler.handleException(this, e)
            FeedbackUtils.showErrorSnackbar(binding.root, errorMessage)
        }
    }
    
    private fun showDeleteConfirmationDialog() {
        FeedbackUtils.showConfirmationDialog(
            context = this,
            title = getString(R.string.delete_transaction),
            message = getString(R.string.delete_confirmation),
            positiveButtonText = getString(R.string.yes),
            negativeButtonText = getString(R.string.no),
            onPositiveClick = { deleteTransaction() }
        )
    }
    
    private fun deleteTransaction() {
        try {
            transactionId?.let {
                transactionRepository.deleteTransaction(it)
                FeedbackUtils.showSuccessSnackbar(binding.root, getString(R.string.transaction_deleted))
                // Close activity after a short delay to allow snackbar to be seen
                binding.root.postDelayed({ finish() }, 1000)
            }
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
    
    companion object {
        const val EXTRA_TRANSACTION_ID = "extra_transaction_id"
    }
}