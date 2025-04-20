package com.example.bossfinance

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bossfinance.adapters.TransactionAdapter
import com.example.bossfinance.databinding.ActivityTransactionListBinding
import com.example.bossfinance.models.Transaction
import com.example.bossfinance.repository.TransactionRepository

class TransactionListActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTransactionListBinding
    private lateinit var transactionAdapter: TransactionAdapter
    private val transactionRepository = TransactionRepository.getInstance()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransactionListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupRecyclerView()
        setupFabButton()
    }
    
    override fun onResume() {
        super.onResume()
        loadTransactions()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.transactions)
    }
    
    private fun setupRecyclerView() {
        transactionAdapter = TransactionAdapter(emptyList()) { transaction ->
            // Handle transaction click - open edit screen
            val intent = Intent(this, TransactionEditActivity::class.java)
            intent.putExtra(TransactionEditActivity.EXTRA_TRANSACTION_ID, transaction.id)
            startActivity(intent)
        }
        
        binding.transactionsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@TransactionListActivity)
            adapter = transactionAdapter
        }
    }
    
    private fun setupFabButton() {
        binding.fabAddTransaction.setOnClickListener {
            val intent = Intent(this, TransactionEditActivity::class.java)
            startActivity(intent)
        }
    }
    
    private fun loadTransactions() {
        val transactions = transactionRepository.getAllTransactions()
        updateUIWithTransactions(transactions)
    }
    
    private fun updateUIWithTransactions(transactions: List<Transaction>) {
        if (transactions.isEmpty()) {
            binding.emptyView.visibility = View.VISIBLE
            binding.transactionsRecyclerView.visibility = View.GONE
        } else {
            binding.emptyView.visibility = View.GONE
            binding.transactionsRecyclerView.visibility = View.VISIBLE
            transactionAdapter.updateTransactions(transactions)
        }
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}