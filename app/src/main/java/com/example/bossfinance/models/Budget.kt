package com.example.bossfinance.models

import java.util.Currency
import java.util.Locale

/**
 * Budget model to store user's monthly budget settings
 */
data class Budget(
    val amount: Double,
    val currency: Currency = Currency.getInstance(Locale.getDefault()),
    val notificationEnabled: Boolean = true,
    val notificationThreshold: Int = 90 // Percentage threshold for notifications
)
