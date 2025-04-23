package com.example.bossfinance

import android.content.Context
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.bossfinance.databinding.ActivityBackupRestoreBinding
import com.example.bossfinance.models.Transaction
import com.example.bossfinance.repository.TransactionRepository
import com.example.bossfinance.utils.ErrorHandler
import com.example.bossfinance.utils.FeedbackUtils
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackupRestoreActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBackupRestoreBinding
    private lateinit var transactionRepository: TransactionRepository
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
    private val backupFileName = "boss_finance_backup.json"
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBackupRestoreBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Initialize repository
        transactionRepository = TransactionRepository.getInstance(applicationContext)
        
        // Setup toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.backup_restore)
        
        // Setup click listeners
        setupExportButton()
        setupImportButton()
    }
    
    private fun setupExportButton() {
        binding.btnExportData.setOnClickListener {
            // Show loading state
            binding.progressExport.visibility = View.VISIBLE
            binding.tvExportStatus.visibility = View.GONE
            
            // Run export operation on a background thread to avoid UI freezing
            Thread {
                try {
                    exportData()
                } catch (e: Exception) {
                    runOnUiThread {
                        val errorMessage = ErrorHandler.handleException(this, e)
                        showExportMessage(errorMessage, false)
                    }
                } finally {
                    runOnUiThread {
                        binding.progressExport.visibility = View.GONE
                    }
                }
            }.start()
        }
    }
    
    private fun setupImportButton() {
        binding.btnImportData.setOnClickListener {
            // Show confirmation dialog before importing
            FeedbackUtils.showConfirmationDialog(
                context = this,
                title = getString(R.string.import_data),
                message = getString(R.string.confirm_import),
                positiveButtonText = getString(R.string.yes),
                negativeButtonText = getString(R.string.no),
                onPositiveClick = {
                    // Show loading state
                    binding.progressImport.visibility = View.VISIBLE
                    binding.tvImportStatus.visibility = View.GONE
                    
                    // Run import on background thread
                    Thread {
                        try {
                            importData()
                        } catch (e: Exception) {
                            runOnUiThread {
                                val errorMessage = ErrorHandler.handleException(this, e)
                                showImportMessage(errorMessage, false)
                            }
                        } finally {
                            runOnUiThread {
                                binding.progressImport.visibility = View.GONE
                            }
                        }
                    }.start()
                }
            )
        }
    }
    
    private fun exportData() {
        try {
            // Get all transactions
            val transactions = transactionRepository.getAllTransactions()
            
            if (transactions.isEmpty()) {
                throw IOException("No transactions to export")
            }
            
            // Convert transactions to JSON
            val jsonArray = JSONArray()
            transactions.forEach { transaction ->
                val jsonObject = JSONObject()
                jsonObject.put("id", transaction.id)
                jsonObject.put("title", transaction.title)
                jsonObject.put("amount", transaction.amount)
                jsonObject.put("category", transaction.category)
                jsonObject.put("date", dateFormat.format(transaction.date))
                jsonObject.put("isIncome", transaction.isIncome)
                jsonArray.put(jsonObject)
            }
            
            // Write to file in internal storage
            val file = File(filesDir, backupFileName)
            FileOutputStream(file).use {
                it.write(jsonArray.toString().toByteArray())
            }
            
            // Show success message on UI thread
            runOnUiThread {
                showExportMessage(getString(R.string.export_success, file.absolutePath), true)
                
                // Also show a snackbar for better visibility
                FeedbackUtils.showSuccessSnackbar(
                    binding.root, 
                    getString(R.string.export_success, file.absolutePath)
                )
            }
        } catch (e: Exception) {
            // Log the exception
            ErrorHandler.logError("BackupRestoreActivity", "Export failed", e)
            
            // Re-throw to be caught by the outer try/catch
            throw e
        }
    }
    
    private fun importData() {
        try {
            // Check if backup file exists
            val file = File(filesDir, backupFileName)
            if (!file.exists()) {
                throw IOException(getString(R.string.no_backup_found))
            }
            
            // Read JSON from file
            val json = FileInputStream(file).bufferedReader().use { it.readText() }
            if (json.isBlank()) {
                throw IOException("Backup file is empty")
            }
            
            val jsonArray = JSONArray(json)
            
            // Validate JSON data first
            if (jsonArray.length() == 0) {
                throw IOException("No transactions found in backup file")
            }
            
            // Convert JSON to transactions
            val transactions = mutableListOf<Transaction>()
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                
                // Validate required fields
                val requiredFields = listOf("id", "title", "amount", "category", "date", "isIncome")
                for (field in requiredFields) {
                    if (!jsonObject.has(field)) {
                        throw IOException("Invalid backup data: missing $field field")
                    }
                }
                
                val transaction = Transaction(
                    id = jsonObject.getString("id"),
                    title = jsonObject.getString("title"),
                    amount = jsonObject.getDouble("amount"),
                    category = jsonObject.getString("category"),
                    date = dateFormat.parse(jsonObject.getString("date")) ?: Date(),
                    isIncome = jsonObject.getBoolean("isIncome")
                )
                transactions.add(transaction)
            }
            
            // Clear existing transactions and add imported ones
            transactionRepository.replaceAllTransactions(transactions)
            
            // Show success message on UI thread
            runOnUiThread {
                showImportMessage(getString(R.string.import_success), true)
                
                // Also show a snackbar with action to refresh the app
                FeedbackUtils.showActionSnackbar(
                    binding.root,
                    getString(R.string.import_success),
                    getString(android.R.string.ok)
                ) {
                    // Navigate back to refresh data
                    finish()
                }
            }
        } catch (e: Exception) {
            // Log the exception
            ErrorHandler.logError("BackupRestoreActivity", "Import failed", e)
            
            // Re-throw to be caught by the outer try/catch
            throw e
        }
    }
    
    private fun showExportMessage(message: String, isSuccess: Boolean) {
        binding.tvExportStatus.text = message
        binding.tvExportStatus.visibility = View.VISIBLE
        binding.tvExportStatus.setTextColor(getColor(
            if (isSuccess) R.color.success_green else R.color.error_red
        ))
    }
    
    private fun showImportMessage(message: String, isSuccess: Boolean) {
        binding.tvImportStatus.text = message
        binding.tvImportStatus.visibility = View.VISIBLE
        binding.tvImportStatus.setTextColor(getColor(
            if (isSuccess) R.color.success_green else R.color.error_red
        ))
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}