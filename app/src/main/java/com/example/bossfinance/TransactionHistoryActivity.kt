package com.example.bossfinance

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bossfinance.adapters.TransactionHistoryAdapter
import com.example.bossfinance.databinding.ActivityTransactionHistoryBinding
import com.example.bossfinance.models.Transaction
import com.example.bossfinance.repository.TransactionRepository
import com.google.android.material.chip.Chip

class TransactionHistoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTransactionHistoryBinding
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var adapter: TransactionHistoryAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransactionHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Initialize repository
        transactionRepository = TransactionRepository.getInstance(this)
        
        // Set up toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.transaction_history)
        
        // Set up RecyclerView
        setupRecyclerView()
        
        // Set up filter chips
        setupFilterChips()
        
        // Load transactions (default: all)
        loadTransactions()
    }
    
    private fun setupRecyclerView() {
        adapter = TransactionHistoryAdapter(this, emptyList()) { transaction ->
            // Handle transaction click - navigate to edit screen
            val intent = Intent(this, TransactionEditActivity::class.java)
            intent.putExtra("transaction_id", transaction.id)
            startActivity(intent)
        }
        
        binding.transactionsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.transactionsRecyclerView.adapter = adapter
    }
    
    private fun setupFilterChips() {
        binding.chipAll.setOnClickListener {
            loadTransactions()
        }
        
        binding.chipIncome.setOnClickListener {
            loadTransactions(isIncome = true)
        }
        
        binding.chipExpense.setOnClickListener {
            loadTransactions(isIncome = false)
        }
    }
    
    private fun loadTransactions(isIncome: Boolean? = null) {
        val transactions = when (isIncome) {
            true -> transactionRepository.getIncomeTransactions()
            false -> transactionRepository.getExpenseTransactions()
            null -> transactionRepository.getAllTransactions()
        }
        
        // Update RecyclerView and empty state
        adapter.updateTransactions(transactions)
        
        if (transactions.isEmpty()) {
            binding.tvEmptyState.visibility = View.VISIBLE
            binding.transactionsRecyclerView.visibility = View.GONE
        } else {
            binding.tvEmptyState.visibility = View.GONE
            binding.transactionsRecyclerView.visibility = View.VISIBLE
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh data when returning to this screen
        loadTransactions(
            when {
                binding.chipIncome.isChecked -> true
                binding.chipExpense.isChecked -> false
                else -> null
            }
        )
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}