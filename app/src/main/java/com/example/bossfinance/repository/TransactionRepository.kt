package com.example.bossfinance.repository

import com.example.bossfinance.models.Transaction
import java.util.Date

class TransactionRepository {
    // In-memory list of transactions (would use Room DB in a real app)
    private val transactions = mutableListOf<Transaction>()
    
    init {
        // Add some sample transactions for testing
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
    }
    
    fun getAllTransactions(): List<Transaction> {
        return transactions.sortedByDescending { it.date }
    }
    
    fun getTransaction(id: String): Transaction? {
        return transactions.find { it.id == id }
    }
    
    fun addTransaction(transaction: Transaction) {
        transactions.add(transaction)
    }
    
    fun updateTransaction(transaction: Transaction) {
        val index = transactions.indexOfFirst { it.id == transaction.id }
        if (index != -1) {
            transactions[index] = transaction
        }
    }
    
    fun deleteTransaction(id: String) {
        transactions.removeIf { it.id == id }
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
    
    companion object {
        // Singleton pattern
        @Volatile
        private var INSTANCE: TransactionRepository? = null
        
        fun getInstance(): TransactionRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = TransactionRepository()
                INSTANCE = instance
                instance
            }
        }
    }
}