package com.example.bossfinance.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.bossfinance.models.Transaction
import com.example.bossfinance.models.TransactionCategories
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class TransactionRepository private constructor(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )
    private val transactions = mutableListOf<Transaction>()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
    
    init {
        loadTransactions()
        if (transactions.isEmpty()) {
            // Add sample transactions for testing only if no saved transactions exist
            addTransaction(Transaction(
                title = "Monthly Salary",
                amount = 3200.00,
                category = "Salary",
                date = Date(),
                isIncome = true
            ))
            
            addTransaction(Transaction(
                title = "Groceries",
                amount = 125.50,
                category = "Food & Dining",
                date = Date(),
                isIncome = false
            ))
            
            addTransaction(Transaction(
                title = "Phone Bill",
                amount = 65.00,
                category = "Utilities",
                date = Date(),
                isIncome = false
            ))
            
            // Save the sample transactions
            saveTransactions()
        }
    }
    
    private fun loadTransactions() {
        val transactionsJson = sharedPreferences.getString(KEY_TRANSACTIONS, null) ?: return
        transactions.clear()
        
        try {
            val jsonArray = JSONArray(transactionsJson)
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
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun saveTransactions() {
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
        
        sharedPreferences.edit().putString(KEY_TRANSACTIONS, jsonArray.toString()).apply()
    }
    
    fun getAllTransactions(): List<Transaction> {
        return transactions.sortedByDescending { it.date }
    }
    
    fun getTransaction(id: String): Transaction? {
        return transactions.find { it.id == id }
    }
    
    fun addTransaction(transaction: Transaction) {
        transactions.add(transaction)
        saveTransactions()
        saveLastUsedCategory(transaction.category, transaction.isIncome)
    }
    
    fun updateTransaction(transaction: Transaction) {
        val index = transactions.indexOfFirst { it.id == transaction.id }
        if (index != -1) {
            transactions[index] = transaction
            saveTransactions()
            saveLastUsedCategory(transaction.category, transaction.isIncome)
        }
    }
    
    fun deleteTransaction(id: String) {
        transactions.removeIf { it.id == id }
        saveTransactions()
    }
    
    fun getTotalIncome(): Double {
        return transactions.filter { it.isIncome }.sumOf { it.amount }
    }
    
    fun getTotalExpenses(): Double {
        return transactions.filter { !it.isIncome }.sumOf { it.amount }
    }
    
    fun getCurrentBalance(): Double {
        return getTotalIncome() - getTotalExpenses()
    }
    
    // Replace all transactions with a new list (for import function)
    fun replaceAllTransactions(newTransactions: List<Transaction>): Boolean {
        return try {
            // First, completely clear SharedPreferences by removing the key entirely
            val editor = sharedPreferences.edit()
            editor.remove(KEY_TRANSACTIONS)
            // Force synchronous commit to ensure the key is removed
            val clearSuccess = editor.commit()
            
            if (!clearSuccess) {
                println("Warning: Failed to clear existing transaction data")
            }
            
            // Now clear in-memory transactions
            transactions.clear()
            
            // Add new transactions from the backup
            transactions.addAll(newTransactions)
            
            // Create JSON representation of all transactions
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
            
            // Save the new transaction data with synchronous commit
            val saveEditor = sharedPreferences.edit()
            saveEditor.putString(KEY_TRANSACTIONS, jsonArray.toString())
            val saveSuccess = saveEditor.commit()
            
            if (saveSuccess) {
                println("Successfully saved ${transactions.size} transactions from backup")
            } else {
                println("Failed to commit transaction data to SharedPreferences")
                return false
            }
            
            return true
        } catch (e: Exception) {
            println("Error replacing transactions: ${e.message}")
            e.printStackTrace()
            false
        }
    }
    
    // Force clear all cached data and reload from SharedPreferences
    fun refreshFromStorage() {
        transactions.clear()
        loadTransactions()
    }
    
    // Save the last used category for quick access
    private fun saveLastUsedCategory(category: String, isIncome: Boolean) {
        val key = if (isIncome) KEY_LAST_INCOME_CATEGORY else KEY_LAST_EXPENSE_CATEGORY
        sharedPreferences.edit().putString(key, category).apply()
    }
    
    // Get the last used category by type
    fun getLastUsedCategory(isIncome: Boolean): String {
        val key = if (isIncome) KEY_LAST_INCOME_CATEGORY else KEY_LAST_EXPENSE_CATEGORY
        val defaultCategory = if (isIncome) 
            TransactionCategories.INCOME_CATEGORIES.first() 
        else 
            TransactionCategories.EXPENSE_CATEGORIES.first()
            
        return sharedPreferences.getString(key, defaultCategory) ?: defaultCategory
    }
    
    // Get transactions by month and year
    fun getTransactionsByMonth(year: Int, month: Int): List<Transaction> {
        val calendar = java.util.Calendar.getInstance()
        return transactions.filter { transaction ->
            calendar.time = transaction.date
            calendar.get(java.util.Calendar.YEAR) == year && 
            calendar.get(java.util.Calendar.MONTH) == month
        }.sortedByDescending { it.date }
    }
    
    // Get income transactions
    fun getIncomeTransactions(): List<Transaction> {
        return transactions.filter { it.isIncome }.sortedByDescending { it.date }
    }
    
    // Get expense transactions
    fun getExpenseTransactions(): List<Transaction> {
        return transactions.filter { !it.isIncome }.sortedByDescending { it.date }
    }
    
    /**
     * Get category-wise spending data for the given date range
     * Returns a list of pairs where each pair contains (category, amount)
     */
    fun getCategorySpending(startDate: Date, endDate: Date): List<Pair<String, Double>> {
        // Get all expense transactions in the date range
        val expenseTransactions = transactions.filter { 
            !it.isIncome && it.date >= startDate && it.date <= endDate 
        }
        
        // Group by category and sum the amounts
        val categoryMap = mutableMapOf<String, Double>()
        
        expenseTransactions.forEach { transaction ->
            val currentAmount = categoryMap.getOrDefault(transaction.category, 0.0)
            categoryMap[transaction.category] = currentAmount + transaction.amount
        }
        
        // Convert to list of pairs sorted by amount (descending)
        return categoryMap.entries.map { 
            Pair(it.key, it.value) 
        }.sortedByDescending { it.second }
    }
    
    companion object {
        private const val PREFS_NAME = "boss_finance_transaction_prefs"
        private const val KEY_TRANSACTIONS = "transactions"
        private const val KEY_LAST_INCOME_CATEGORY = "last_income_category"
        private const val KEY_LAST_EXPENSE_CATEGORY = "last_expense_category"
        
        @Volatile
        private var INSTANCE: TransactionRepository? = null
        
        fun getInstance(context: Context): TransactionRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = TransactionRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
        
        // Add method to clear singleton instance when needed (for forced refresh)
        fun clearInstance() {
            synchronized(this) {
                INSTANCE = null
            }
        }
    }
}