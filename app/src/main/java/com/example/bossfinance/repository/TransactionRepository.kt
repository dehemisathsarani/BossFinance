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
    fun replaceAllTransactions(newTransactions: List<Transaction>) {
        transactions.clear()
        transactions.addAll(newTransactions)
        saveTransactions()
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
    }
}