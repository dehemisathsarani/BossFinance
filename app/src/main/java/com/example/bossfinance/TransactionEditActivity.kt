package com.example.bossfinance

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.bossfinance.databinding.ActivityTransactionEditBinding
import com.example.bossfinance.models.Transaction
import com.example.bossfinance.models.TransactionCategories
import com.example.bossfinance.repository.TransactionRepository
import com.google.android.material.tabs.TabLayout
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TransactionEditActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTransactionEditBinding
    private lateinit var transactionRepository: TransactionRepository
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
        
        // Initialize repository
        transactionRepository = TransactionRepository.getInstance(applicationContext)
        
        // Check if we're editing an existing transaction
        transactionId = intent.getStringExtra(EXTRA_TRANSACTION_ID)
        isEditMode = transactionId != null
        
        // Setup UI components
        setupToolbar()
        setupTabLayout()
        setupDatePicker()
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
        }
    }
    
    private fun validateInputs(): Boolean {
        var isValid = true
        
        // Check title
        if (binding.etTransactionTitle.text.isNullOrBlank()) {
            binding.titleInputLayout.error = getString(R.string.please_enter_title)
            isValid = false
        } else {
            binding.titleInputLayout.error = null
        }
        
        // Check amount
        if (binding.etAmount.text.isNullOrBlank() || 
            binding.etAmount.text.toString().toDoubleOrNull() == null ||
            binding.etAmount.text.toString().toDouble() <= 0) {
            binding.amountInputLayout.error = getString(R.string.please_enter_amount)
            isValid = false
        } else {
            binding.amountInputLayout.error = null
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
            // Show error and return early if amount can't be parsed
            binding.amountInputLayout.error = getString(R.string.please_enter_valid_amount)
            return
        }
        
        // Make sure we have a valid category selected
        val category = binding.spinnerCategory.selectedItem?.toString() ?: run {
            Toast.makeText(this, getString(R.string.please_select_category), Toast.LENGTH_SHORT).show()
            return
        }
        
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
                Toast.makeText(this, getString(R.string.transaction_saved), Toast.LENGTH_SHORT).show()
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
                Toast.makeText(this, getString(R.string.transaction_saved), Toast.LENGTH_SHORT).show()
            }
            finish()
        } catch (e: Exception) {
            // Handle any unexpected errors during save
            Toast.makeText(this, "Error saving transaction", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }
    
    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_transaction)
            .setMessage(R.string.delete_confirmation)
            .setPositiveButton(R.string.yes) { _, _ ->
                deleteTransaction()
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }
    
    private fun deleteTransaction() {
        transactionId?.let {
            transactionRepository.deleteTransaction(it)
            Toast.makeText(this, getString(R.string.transaction_deleted), Toast.LENGTH_SHORT).show()
            finish()
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