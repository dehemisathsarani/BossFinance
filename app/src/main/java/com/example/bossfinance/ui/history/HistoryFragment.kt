package com.example.bossfinance.ui.history

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bossfinance.TransactionEditActivity
import com.example.bossfinance.databinding.FragmentHistoryBinding
import com.example.bossfinance.repository.TransactionRepository
import com.example.bossfinance.models.Transaction
import java.util.Date

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var transactionAdapter: TransactionHistoryAdapter
    private var currentFilter = TransactionFilter.ALL

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        transactionRepository = TransactionRepository.getInstance(requireContext())
        
        setupTransactionList()
        setupFilterChips()
        loadTransactions()
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh data when coming back to this fragment
        loadTransactions()
    }
    
    private fun setupTransactionList() {
        transactionAdapter = TransactionHistoryAdapter(requireContext(), emptyList()) { transaction ->
            // Handle transaction item click - navigate to edit screen
            val intent = Intent(requireContext(), TransactionEditActivity::class.java).apply {
                putExtra(TransactionEditActivity.EXTRA_TRANSACTION_ID, transaction.id)
            }
            startActivity(intent)
        }
        
        binding.transactionList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = transactionAdapter
        }
    }
    
    private fun setupFilterChips() {
        binding.chipAll.setOnClickListener {
            currentFilter = TransactionFilter.ALL
            loadTransactions()
        }
        
        binding.chipIncome.setOnClickListener {
            currentFilter = TransactionFilter.INCOME
            loadTransactions()
        }
        
        binding.chipExpense.setOnClickListener {
            currentFilter = TransactionFilter.EXPENSE
            loadTransactions()
        }
    }
    
    private fun loadTransactions() {
        val transactions = when (currentFilter) {
            TransactionFilter.ALL -> transactionRepository.getAllTransactions()
            TransactionFilter.INCOME -> transactionRepository.getIncomeTransactions()
            TransactionFilter.EXPENSE -> transactionRepository.getExpenseTransactions()
        }
        
        if (transactions.isEmpty()) {
            binding.tvNoTransactions.visibility = View.VISIBLE
            binding.transactionList.visibility = View.GONE
        } else {
            binding.tvNoTransactions.visibility = View.GONE
            binding.transactionList.visibility = View.VISIBLE
            transactionAdapter.updateData(transactions)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    enum class TransactionFilter {
        ALL, INCOME, EXPENSE
    }
    
    // Inner class for TransactionHistoryAdapter using the correct Transaction model
    inner class TransactionHistoryAdapter(
        private val context: android.content.Context,
        private var transactions: List<Transaction>,
        private val onClick: (Transaction) -> Unit
    ) : androidx.recyclerview.widget.RecyclerView.Adapter<TransactionHistoryAdapter.ViewHolder>() {
        
        fun updateData(newTransactions: List<Transaction>) {
            this.transactions = newTransactions
            notifyDataSetChanged()
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val itemView = android.view.LayoutInflater.from(context)
                .inflate(android.R.layout.simple_list_item_2, parent, false)
            return ViewHolder(itemView)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val transaction = transactions[position]
            
            val textView1 = holder.itemView.findViewById<android.widget.TextView>(android.R.id.text1)
            val textView2 = holder.itemView.findViewById<android.widget.TextView>(android.R.id.text2)
            
            textView1.text = transaction.title
            
            val amountText = if (!transaction.isIncome) {
                "-${java.text.NumberFormat.getCurrencyInstance().format(transaction.amount)}"
            } else {
                java.text.NumberFormat.getCurrencyInstance().format(transaction.amount)
            }
            textView2.text = "$amountText • ${transaction.category} • ${formatDate(transaction.date)}"
            
            holder.itemView.setOnClickListener { onClick(transaction) }
        }
        
        private fun formatDate(date: Date): String {
            return java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault()).format(date)
        }
        
        override fun getItemCount(): Int = transactions.size
        
        inner class ViewHolder(itemView: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView)
    }
}