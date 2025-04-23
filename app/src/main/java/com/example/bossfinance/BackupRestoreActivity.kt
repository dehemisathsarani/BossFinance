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
        // Run in background to avoid UI freezing
        Thread {
            try {
                val backupFile = getLatestBackupFile()
                if (backupFile != null) {
                    val date = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                        .format(Date(backupFile.lastModified()))
                    runOnUiThread {
                        binding.tvBackupStatus.text = getString(R.string.backup_status_found, date)
                        binding.tvBackupStatus.visibility = View.VISIBLE
                        binding.tvBackupStatus.setTextColor(getColor(R.color.success_green))
                    }
                } else {
                    runOnUiThread {
                        binding.tvBackupStatus.text = getString(R.string.backup_status_not_found)
                        binding.tvBackupStatus.visibility = View.VISIBLE
                        binding.tvBackupStatus.setTextColor(getColor(R.color.neutral_gray))
                    }
                }
            } catch (e: Exception) {
                // Silent failure, no need to show error for this check
            }
        }.start()
    }
    
    private fun setupExportButton() {
        binding.btnExportData.setOnClickListener {
            // Show loading state
            binding.progressExport.visibility = View.VISIBLE
            binding.tvExportStatus.visibility = View.GONE
            
            // Run export operation on a background thread to avoid UI freezing
            Thread {
                try {
                    val filePath = exportData()
                    runOnUiThread {
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
            // First check if backup exists
            if (getLatestBackupFile() == null) {
                FeedbackUtils.showErrorSnackbar(
                    binding.root,
                    getString(R.string.no_backup_found)
                )
                return@setOnClickListener
            }
            
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
                val jsonObject = JSONObject().apply {
                    put("id", transaction.id)
                    put("title", transaction.title)
                    put("amount", transaction.amount)
                    put("category", transaction.category)
                    put("date", dateFormat.format(transaction.date))
                    put("isIncome", transaction.isIncome)
                }
                jsonArray.put(jsonObject)
            }
            
            // Always use the same file name for simplicity
            val file = File(filesDir, backupFileName)
            
            try {
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
    }
    
    private fun importData() {
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
                    
                    val transaction = Transaction(
                        id = id,
                        title = title,
                        amount = amount,
                        category = category,
                        date = date,
                        isIncome = isIncome
                    )
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
            
            // Clear existing transactions and add imported ones
            transactionRepository.replaceAllTransactions(transactions)
            
            // Show success message on UI thread
            runOnUiThread {
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