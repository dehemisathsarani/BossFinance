package com.example.bossfinance.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.bossfinance.TransactionEditActivity
import com.example.bossfinance.BudgetSetupActivity
import com.example.bossfinance.SpendingAnalysisActivity
import com.example.bossfinance.BackupRestoreActivity
import com.example.bossfinance.databinding.FragmentDashboardBinding
import com.example.bossfinance.repository.BudgetRepository
import com.example.bossfinance.repository.TransactionRepository
import java.text.NumberFormat
import java.util.Locale

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var budgetRepository: BudgetRepository
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        transactionRepository = TransactionRepository.getInstance(requireContext())
        budgetRepository = BudgetRepository.getInstance(requireContext())
        
        updateFinancialData()
        setupClickListeners()
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh data when coming back to this fragment
        updateFinancialData()
    }

    private fun updateFinancialData() {
        // Update current balance
        val currentBalance = transactionRepository.getCurrentBalance()
        binding.tvCurrentBalance.text = currencyFormatter.format(currentBalance)
        
        // Update income
        val totalIncome = transactionRepository.getTotalIncome()
        binding.tvTotalIncome.text = currencyFormatter.format(totalIncome)
        
        // Update expenses
        val totalExpenses = transactionRepository.getTotalExpenses()
        binding.tvTotalExpenses.text = currencyFormatter.format(totalExpenses)
        
        // Update budget progress
        val budgetAmount = budgetRepository.getMonthlyBudget()
        val budgetInfo = if (budgetAmount > 0) {
            "${currencyFormatter.format(totalExpenses)} spent of ${currencyFormatter.format(budgetAmount)} monthly budget"
        } else {
            "No budget set. Tap to set a budget."
        }
        binding.tvBudgetInfo.text = budgetInfo
        
        if (budgetAmount > 0) {
            val percentage = ((totalExpenses / budgetAmount) * 100.0).toInt().coerceAtMost(100)
            binding.budgetProgressBar.progress = percentage
            binding.tvBudgetPercentage.text = "$percentage%"
        } else {
            binding.budgetProgressBar.progress = 0
            binding.tvBudgetPercentage.text = "0%"
        }
    }

    private fun setupClickListeners() {
        // Set up click listeners for the buttons
        binding.btnAddTransaction.setOnClickListener {
            // Navigate directly to transaction edit screen for adding a new transaction
            val intent = Intent(requireContext(), TransactionEditActivity::class.java)
            startActivity(intent)
        }
        
        binding.btnViewReports.setOnClickListener {
            // Navigate to Spending Analysis screen
            val intent = Intent(requireContext(), SpendingAnalysisActivity::class.java)
            startActivity(intent)
        }
        
        binding.btnSetBudget.setOnClickListener {
            // Navigate to Budget Setup screen
            val intent = Intent(requireContext(), BudgetSetupActivity::class.java)
            startActivity(intent)
        }
        
        binding.btnBackupData.setOnClickListener {
            // Navigate to Backup & Restore screen
            val intent = Intent(requireContext(), BackupRestoreActivity::class.java)
            startActivity(intent)
        }
        
        // Add click listener to budget progress section to navigate to budget setup
        binding.budgetProgressBar.setOnClickListener {
            val intent = Intent(requireContext(), BudgetSetupActivity::class.java)
            startActivity(intent)
        }
        
        binding.tvBudgetPercentage.setOnClickListener {
            val intent = Intent(requireContext(), BudgetSetupActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}