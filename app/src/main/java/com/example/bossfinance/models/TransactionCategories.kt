package com.example.bossfinance.models

/**
 * Constants for transaction categories
 */
object TransactionCategories {
    // Expense categories
    const val FOOD_AND_DINING = "Food and Dining"
    const val UTILITIES = "Utilities"
    const val TRANSPORT = "Transport"
    const val ENTERTAINMENT = "Entertainment"
    const val SHOPPING = "Shopping"
    
    // Income categories
    const val SALARY = "Salary"
    const val INVESTMENT = "Investment"
    
    // Category lists
    val EXPENSE_CATEGORIES = listOf(
        FOOD_AND_DINING,
        UTILITIES,
        TRANSPORT,
        ENTERTAINMENT,
        SHOPPING
    )
    
    val INCOME_CATEGORIES = listOf(
        SALARY,
        INVESTMENT
    )
}