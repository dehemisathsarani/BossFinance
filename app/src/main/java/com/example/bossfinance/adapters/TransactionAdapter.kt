package com.example.bossfinance.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.bossfinance.R
import com.example.bossfinance.databinding.ItemTransactionBinding
import com.example.bossfinance.models.Transaction
import com.example.bossfinance.models.TransactionCategories
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class TransactionAdapter(
    private var transactions: List<Transaction>,
    private val onTransactionClicked: (Transaction) -> Unit
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
    
    // Category icons mapping
    private val categoryIcons = mapOf(
        TransactionCategories.FOOD_AND_DINING to R.drawable.ic_food,
        TransactionCategories.UTILITIES to R.drawable.ic_utilities,
        TransactionCategories.TRANSPORT to R.drawable.ic_transport,
        TransactionCategories.ENTERTAINMENT to R.drawable.ic_entertainment,
        TransactionCategories.SHOPPING to R.drawable.ic_shopping,
        TransactionCategories.SALARY to R.drawable.ic_money,
        TransactionCategories.INVESTMENT to R.drawable.ic_investment
    )
    
    // Default icons for categories without specific icons
    private val defaultIncomeIcon = R.drawable.ic_income
    private val defaultExpenseIcon = R.drawable.ic_expense

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding = ItemTransactionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TransactionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(transactions[position])
    }

    override fun getItemCount(): Int = transactions.size

    fun updateTransactions(newTransactions: List<Transaction>) {
        this.transactions = newTransactions
        notifyDataSetChanged()
    }

    inner class TransactionViewHolder(
        private val binding: ItemTransactionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onTransactionClicked(transactions[position])
                }
            }
        }

        fun bind(transaction: Transaction) {
            binding.apply {
                tvTransactionTitle.text = transaction.title
                tvTransactionCategory.text = transaction.category
                tvTransactionDate.text = dateFormat.format(transaction.date)

                // Format the amount with +/- sign based on income/expense
                val amountStr = if (transaction.isIncome) {
                    "+" + currencyFormat.format(transaction.amount)
                } else {
                    "-" + currencyFormat.format(transaction.amount)
                }
                
                tvTransactionAmount.text = amountStr
                
                // Set color based on transaction type
                tvTransactionAmount.setTextColor(
                    binding.root.context.getColor(
                        if (transaction.isIncome) android.R.color.holo_green_dark
                        else android.R.color.holo_red_dark
                    )
                )
                
                // Set category icon
                val iconResId = categoryIcons[transaction.category] 
                    ?: if (transaction.isIncome) defaultIncomeIcon else defaultExpenseIcon
                ivCategoryIcon.setImageResource(iconResId)
            }
        }
    }
}