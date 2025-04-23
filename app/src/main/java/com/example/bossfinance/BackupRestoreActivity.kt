package com.example.bossfinance

import android.content.Context
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.bossfinance.databinding.ActivityBackupRestoreBinding
import com.example.bossfinance.models.Transaction
import com.example.bossfinance.repository.TransactionRepository
import com.example.bossfinance.utils.ErrorHandler
import com.example.bossfinance.utils.FeedbackUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

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
        
        // Check and display if backup exists
        checkAndDisplayBackupStatus()
    }
    
    private fun checkAndDisplayBackupStatus() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val backupFile = getLatestBackupFile()
                if (backupFile != null) {
                    val date = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                        .format(Date(backupFile.lastModified()))
                        
                    withContext(Dispatchers.Main) {
                        binding.tvBackupStatus.text = getString(R.string.backup_status_found, date)
                        binding.tvBackupStatus.visibility = View.VISIBLE
                        binding.tvBackupStatus.setTextColor(getColor(R.color.success_green))
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        binding.tvBackupStatus.text = getString(R.string.backup_status_not_found)
                        binding.tvBackupStatus.visibility = View.VISIBLE
                        binding.tvBackupStatus.setTextColor(getColor(R.color.neutral_gray))
                    }
                }
            } catch (e: Exception) {
                // Silent failure, no need to show error for this check
                ErrorHandler.logError("BackupRestoreActivity", "Error checking backup status", e)
            }
        }
    }
    
    private fun setupExportButton() {
        binding.btnExportData.setOnClickListener {
            // Show loading state
            binding.progressExport.visibility = View.VISIBLE
            binding.tvExportStatus.visibility = View.GONE
            
            // Use Kotlin coroutines for background processing
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    // Call exportData but don't store unused return value
                    exportData()
                    withContext(Dispatchers.Main) {
                        showExportMessage(getString(R.string.export_success_simple), true)
                        
                        // Also show a snackbar for better visibility
                        FeedbackUtils.showSuccessSnackbar(
                            binding.root, 
                            getString(R.string.export_success_simple)
                        )
                        
                        // Update backup status
                        checkAndDisplayBackupStatus()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        val errorMessage = ErrorHandler.handleException(this@BackupRestoreActivity, e)
                        showExportMessage(errorMessage, false)
                    }
                } finally {
                    withContext(Dispatchers.Main) {
                        binding.progressExport.visibility = View.GONE
                    }
                }
            }
        }
    }
    
    private fun setupImportButton() {
        binding.btnImportData.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                // First check if backup exists
                val backupFile = getLatestBackupFile()
                
                withContext(Dispatchers.Main) {
                    if (backupFile == null) {
                        FeedbackUtils.showErrorSnackbar(
                            binding.root,
                            getString(R.string.no_backup_found)
                        )
                        return@withContext
                    }
                    
                    // Show confirmation dialog before importing
                    FeedbackUtils.showConfirmationDialog(
                        context = this@BackupRestoreActivity,
                        title = getString(R.string.import_data),
                        message = getString(R.string.confirm_import),
                        positiveButtonText = getString(R.string.yes),
                        negativeButtonText = getString(R.string.no),
                        onPositiveClick = {
                            // Show loading state
                            binding.progressImport.visibility = View.VISIBLE
                            binding.tvImportStatus.visibility = View.GONE
                            
                            // Run import in coroutine
                            lifecycleScope.launch(Dispatchers.IO) {
                                try {
                                    importData()
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        val errorMessage = ErrorHandler.handleException(this@BackupRestoreActivity, e)
                                        showImportMessage(errorMessage, false)
                                    }
                                } finally {
                                    withContext(Dispatchers.Main) {
                                        binding.progressImport.visibility = View.GONE
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
    
    private fun exportData(): String {
        try {
            // Get all transactions
            val transactions = transactionRepository.getAllTransactions()
            
            if (transactions.isEmpty()) {
                throw IOException(getString(R.string.no_transactions_to_export))
            }
            
            // Convert transactions to JSON
            val jsonArray = JSONArray()
            transactions.forEach { transaction ->
                try {
                    val jsonObject = JSONObject().apply {
                        put("id", transaction.id)
                        put("title", transaction.title)
                        put("amount", transaction.amount)
                        put("category", transaction.category)
                        put("date", dateFormat.format(transaction.date))
                        put("isIncome", transaction.isIncome)
                    }
                    jsonArray.put(jsonObject)
                } catch (e: Exception) {
                    ErrorHandler.logError("BackupRestoreActivity", "Error converting transaction to JSON", e)
                    // Continue with other transactions even if one fails
                }
            }
            
            if (jsonArray.length() == 0) {
                throw IOException(getString(R.string.no_transactions_to_export))
            }
            
            // Always use the same file name for simplicity
            val file = File(filesDir, backupFileName)
            
            try {
                // Ensure the parent directory exists
                file.parentFile?.mkdirs()
                
                // Write to file in internal storage with proper error handling
                FileOutputStream(file).use { outputStream ->
                    outputStream.write(jsonArray.toString().toByteArray())
                    outputStream.flush()
                }
                
                return file.absolutePath
            } catch (e: IOException) {
                throw IOException("Failed to write backup file: ${e.message}")
            }
            
        } catch (e: Exception) {
            // Log the exception
            ErrorHandler.logError("BackupRestoreActivity", "Export failed", e)
            
            // Re-throw to be caught by the outer try/catch
            throw e
        }
    }
    
    private fun getLatestBackupFile(): File? {
        try {
            // First try the exact file name
            val exactFile = File(filesDir, backupFileName)
            if (exactFile.exists() && exactFile.isFile && exactFile.length() > 0) {
                return exactFile
            }
            
            // If not found, try to find any backup file
            val backupFiles = filesDir.listFiles { file -> 
                file.isFile && file.name.contains("boss_finance_backup") && file.name.endsWith(".json") 
            } ?: return null
            
            return if (backupFiles.isNotEmpty()) {
                backupFiles.maxByOrNull { it.lastModified() }
            } else {
                null
            }
        } catch (e: Exception) {
            ErrorHandler.logError("BackupRestoreActivity", "Error finding backup file", e)
            return null
        }
    }
    
    private suspend fun importData() {
        try {
            // Find the backup file
            val backupFile = getLatestBackupFile()
                ?: throw IOException(getString(R.string.no_backup_found))
            
            // Read JSON from file with proper error handling
            val json = try {
                FileInputStream(backupFile).use { inputStream ->
                    inputStream.bufferedReader().use { it.readText() }
                }
            } catch (e: IOException) {
                throw IOException("Failed to read backup file: ${e.message}")
            }
            
            if (json.isBlank()) {
                throw IOException(getString(R.string.backup_file_empty))
            }
            
            // Parse JSON data
            val jsonArray = try {
                JSONArray(json)
            } catch (e: JSONException) {
                throw IOException(getString(R.string.invalid_backup_format))
            }
            
            // Validate JSON data first
            if (jsonArray.length() == 0) {
                throw IOException(getString(R.string.no_transactions_in_backup))
            }
            
            // Log details for debugging
            println("Found ${jsonArray.length()} transactions in backup file")
            
            // Convert JSON to transactions
            val transactions = mutableListOf<Transaction>()
            var parseErrors = 0
            
            for (i in 0 until jsonArray.length()) {
                try {
                    val jsonObject = jsonArray.getJSONObject(i)
                    
                    // Validate required fields
                    val requiredFields = listOf("id", "title", "amount", "category", "date", "isIncome")
                    val missingFields = requiredFields.filter { !jsonObject.has(it) }
                    
                    if (missingFields.isNotEmpty()) {
                        parseErrors++
                        ErrorHandler.logError(
                            "BackupRestoreActivity", 
                            "Missing fields in transaction at index $i: ${missingFields.joinToString()}"
                        )
                        continue // Skip this transaction
                    }
                    
                    // Parse transaction data with proper error handling
                    val id = try {
                        jsonObject.getString("id")
                    } catch (e: Exception) {
                        // Generate a new ID if original can't be parsed
                        UUID.randomUUID().toString()
                    }
                    
                    val title = try {
                        jsonObject.getString("title")
                    } catch (e: Exception) {
                        "Unknown Transaction"
                    }
                    
                    val amount = try {
                        jsonObject.getDouble("amount")
                    } catch (e: Exception) {
                        parseErrors++
                        continue // Skip this transaction
                    }
                    
                    val category = try {
                        jsonObject.getString("category")
                    } catch (e: Exception) {
                        "Miscellaneous"
                    }
                    
                    // Parse date with proper error handling
                    val dateStr = jsonObject.getString("date")
                    val date = try {
                        dateFormat.parse(dateStr) ?: Date()
                    } catch (e: ParseException) {
                        // Use current date if parsing fails
                        Date()
                    }
                    
                    val isIncome = try {
                        jsonObject.getBoolean("isIncome")
                    } catch (e: Exception) {
                        // Default to expense if can't determine
                        false
                    }
                    
                    // Create the transaction with appropriate error checks for model compatibility
                    val transaction = try {
                        Transaction(
                            id = id,
                            title = title,
                            amount = amount,
                            category = category,
                            date = date,
                            isIncome = isIncome
                        )
                    } catch (e: Exception) {
                        ErrorHandler.logError("BackupRestoreActivity", "Error creating Transaction object", e)
                        parseErrors++
                        continue // Skip if model construction fails
                    }
                    
                    transactions.add(transaction)
                } catch (e: Exception) {
                    // Log the error but continue processing other transactions
                    parseErrors++
                    ErrorHandler.logError(
                        "BackupRestoreActivity", 
                        "Error parsing transaction at index $i", 
                        e
                    )
                }
            }
            
            // Make sure we have at least some transactions to import
            if (transactions.isEmpty()) {
                throw IOException(getString(R.string.no_valid_transactions_in_backup))
            }
            
            println("Successfully parsed ${transactions.size} transactions from backup")
            
            // Clear existing transactions and add imported ones
            val importSuccess = transactionRepository.replaceAllTransactions(transactions)
            
            if (!importSuccess) {
                throw IOException(getString(R.string.import_error, "Failed to update database"))
            }
            
            // Reset the repository singleton to ensure fresh data
            withContext(Dispatchers.Main) {
                // Force repository to refresh by recreating the instance
                TransactionRepository.clearInstance()
                transactionRepository = TransactionRepository.getInstance(applicationContext)
            }
            
            // Show success message on UI thread
            withContext(Dispatchers.Main) {
                val message = if (parseErrors > 0) {
                    getString(R.string.import_partial_success, transactions.size, parseErrors)
                } else {
                    getString(R.string.import_success)
                }
                
                showImportMessage(message, true)
                
                // Also show a snackbar with action to refresh the app
                FeedbackUtils.showActionSnackbar(
                    binding.root,
                    message,
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