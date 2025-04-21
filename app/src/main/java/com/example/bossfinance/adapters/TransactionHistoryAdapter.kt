package com.example.bossfinance.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.bossfinance.R
import com.example.bossfinance.models.Transaction
import com.example.bossfinance.models.TransactionCategories
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class TransactionHistoryAdapter(
    private val context: Context,
    private var transactions: List<Transaction>,
    private val onTransactionClick: (Transaction) -> Unit
) : RecyclerView.Adapter<TransactionHistoryAdapter.TransactionViewHolder>() {
    
    private val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.getDefault())
    
    // Category icons mapping using constants
    private val categoryIcons = mapOf(
        TransactionCategories.FOOD_AND_DINING to R.drawable.ic_food,
        TransactionCategories.UTILITIES to R.drawable.ic_utilities,
        TransactionCategories.TRANSPORT to R.drawable.ic_transport,
        TransactionCategories.ENTERTAINMENT to R.drawable.ic_entertainment,
        TransactionCategories.SHOPPING to R.drawable.ic_shopping,
        TransactionCategories.SALARY to R.drawable.ic_money,
        TransactionCategories.INVESTMENT to R.drawable.ic_investment
    )
    
    // Default icons if category not found
    private val defaultIncomeIcon = R.drawable.ic_income
    private val defaultExpenseIcon = R.drawable.ic_expense
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_transaction_history, parent, false)
        return TransactionViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int): Unit {
        val transaction = transactions[position]
        holder.bind(transaction)
    }
    
    override fun getItemCount(): Int = transactions.size
    
    fun updateTransactions(newTransactions: List<Transaction>): Unit {
        transactions = newTransactions
        notifyDataSetChanged()
    }
    
    inner class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTransactionTitle)
        private val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        private val tvDate: TextView = itemView.findViewById(R.id.tvTransactionDate)
        private val tvAmount: TextView = itemView.findViewById(R.id.tvTransactionAmount)
        private val ivCategoryIcon: ImageView = itemView.findViewById(R.id.ivCategoryIcon)
        
        fun bind(transaction: Transaction): Unit {
            tvTitle.text = transaction.title
            tvCategory.text = transaction.category
            tvDate.text = dateFormat.format(transaction.date)
            
            // Format amount with + or - sign and color
            val prefix = if (transaction.isIncome) "+" else "-"
            tvAmount.text = prefix + " " + currencyFormatter.format(transaction.amount)
            
            // Set color based on income/expense
            val amountColor = if (transaction.isIncome) 
                ContextCompat.getColor(context, android.R.color.holo_green_dark)
            else
                ContextCompat.getColor(context, android.R.color.holo_red_dark)
            tvAmount.setTextColor(amountColor)
            
            // Set category icon
            val iconResId = categoryIcons[transaction.category] 
                ?: if (transaction.isIncome) defaultIncomeIcon else defaultExpenseIcon
            ivCategoryIcon.setImageResource(iconResId)
            
            // Set click listener for the item
            itemView.setOnClickListener {
                onTransactionClick(transaction)
            }
        }
    }
}