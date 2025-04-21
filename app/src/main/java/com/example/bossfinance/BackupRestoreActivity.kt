package com.example.bossfinance

import android.content.Context
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.bossfinance.databinding.ActivityBackupRestoreBinding
import com.example.bossfinance.models.Transaction
import com.example.bossfinance.repository.TransactionRepository
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
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
            exportData()
        }
    }
    
    private fun setupImportButton() {
        binding.btnImportData.setOnClickListener {
            // Show confirmation dialog before importing
            AlertDialog.Builder(this)
                .setTitle(R.string.import_data)
                .setMessage(R.string.confirm_import)
                .setPositiveButton(R.string.yes) { _, _ ->
                    importData()
                }
                .setNegativeButton(R.string.no, null)
                .show()
        }
    }
    
    private fun exportData() {
        try {
            // Get all transactions
            val transactions = transactionRepository.getAllTransactions()
            
            if (transactions.isEmpty()) {
                showExportMessage(getString(R.string.export_error, "No transactions to export"), false)
                return
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
            
            showExportMessage(getString(R.string.export_success, file.absolutePath), true)
        } catch (e: Exception) {
            showExportMessage(getString(R.string.export_error, e.message), false)
            e.printStackTrace()
        }
    }
    
    private fun importData() {
        try {
            // Check if backup file exists
            val file = File(filesDir, backupFileName)
            if (!file.exists()) {
                showImportMessage(getString(R.string.no_backup_found), false)
                return
            }
            
            // Read JSON from file
            val json = FileInputStream(file).bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(json)
            
            // Convert JSON to transactions
            val transactions = mutableListOf<Transaction>()
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
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
            // Note: This requires adding a method to the TransactionRepository
            transactionRepository.replaceAllTransactions(transactions)
            
            showImportMessage(getString(R.string.import_success), true)
        } catch (e: Exception) {
            showImportMessage(getString(R.string.import_error, e.message), false)
            e.printStackTrace()
        }
    }
    
    private fun showExportMessage(message: String, isSuccess: Boolean) {
        binding.tvExportStatus.text = message
        binding.tvExportStatus.visibility = View.VISIBLE
        binding.tvExportStatus.setTextColor(getColor(
            if (isSuccess) android.R.color.holo_green_dark else android.R.color.holo_red_dark
        ))
    }
    
    private fun showImportMessage(message: String, isSuccess: Boolean) {
        binding.tvImportStatus.text = message
        binding.tvImportStatus.visibility = View.VISIBLE
        binding.tvImportStatus.setTextColor(getColor(
            if (isSuccess) android.R.color.holo_green_dark else android.R.color.holo_red_dark
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