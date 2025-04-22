package com.example.bossfinance.ui.reports

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bossfinance.databinding.FragmentReportsBinding
import com.example.bossfinance.repository.TransactionRepository
import java.util.Calendar
import java.util.Date

class ReportsFragment : Fragment() {

    private var _binding: FragmentReportsBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var transactionRepository: TransactionRepository
    private var selectedDate = Date() // Default to current date

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReportsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        transactionRepository = TransactionRepository.getInstance(requireContext())
        
        setupDateSelector()
        setupTabLayout()
        setupCategoryList()
        updateReports()
    }
    
    private fun setupDateSelector() {
        updateMonthYearDisplay()
        
        binding.btnPreviousMonth.setOnClickListener {
            // Navigate to previous month
            val calendar = Calendar.getInstance()
            calendar.time = selectedDate
            calendar.add(Calendar.MONTH, -1)
            selectedDate = calendar.time
            updateMonthYearDisplay()
            updateReports()
        }
        
        binding.btnNextMonth.setOnClickListener {
            // Navigate to next month
            val calendar = Calendar.getInstance()
            calendar.time = selectedDate
            calendar.add(Calendar.MONTH, 1)
            selectedDate = calendar.time
            updateMonthYearDisplay()
            updateReports()
        }
    }
    
    private fun updateMonthYearDisplay() {
        binding.tvSelectedMonth.text = formatMonthYear(selectedDate)
    }
    
    private fun formatMonthYear(date: Date): String {
        val calendar = Calendar.getInstance()
        calendar.time = date
        val month = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, java.util.Locale.getDefault())
        val year = calendar.get(Calendar.YEAR)
        return "$month $year"
    }
    
    private fun getFirstDayOfMonth(date: Date): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.time
    }
    
    private fun getLastDayOfMonth(date: Date): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.time
    }
    
    private fun setupTabLayout() {
        binding.tabLayout.addOnTabSelectedListener(object : 
            com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                // Update chart type based on selected tab
                updateChartType(tab?.position ?: 0)
            }
            
            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })
    }
    
    private fun setupCategoryList() {
        binding.categoryList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = CategorySpendingAdapter(requireContext(), emptyList())
        }
    }
    
    private fun updateReports() {
        val startDate = getFirstDayOfMonth(selectedDate)
        val endDate = getLastDayOfMonth(selectedDate)
        
        // Get category-wise spending from repository
        val categorySpending = transactionRepository.getCategorySpending(startDate, endDate)
        
        if (categorySpending.isEmpty()) {
            binding.tvNoData.visibility = View.VISIBLE
            binding.chartContainer.visibility = View.GONE
        } else {
            binding.tvNoData.visibility = View.GONE
            binding.chartContainer.visibility = View.VISIBLE
            
            // Update chart based on selected type
            updateChartWithData(categorySpending)
            
            // Update category list
            (binding.categoryList.adapter as CategorySpendingAdapter).updateData(categorySpending)
        }
    }
    
    private fun updateChartType(tabPosition: Int) {
        // 0 = Pie Chart, 1 = Bar Chart
        val chartType = if (tabPosition == 0) ChartType.PIE else ChartType.BAR
        
        // Get data and update chart
        val startDate = getFirstDayOfMonth(selectedDate)
        val endDate = getLastDayOfMonth(selectedDate)
        val categorySpending = transactionRepository.getCategorySpending(startDate, endDate)
        
        if (categorySpending.isNotEmpty()) {
            updateChartWithData(categorySpending, chartType)
        }
    }
    
    private fun updateChartWithData(
        categorySpending: List<Pair<String, Double>>,
        chartType: ChartType = if (binding.tabLayout.selectedTabPosition == 0) ChartType.PIE else ChartType.BAR
    ) {
        // Here you would update the chart with the provided data
        // This depends on which charting library you're using
        // For example, MPAndroidChart, AnyChart, etc.
        
        // For this example, we'll just set a placeholder
        // In a real implementation, you would create and update the actual chart
        when (chartType) {
            ChartType.PIE -> {
                // Update pie chart with data
                // Example: pieChart.setData(convertToPieEntries(categorySpending))
            }
            ChartType.BAR -> {
                // Update bar chart with data
                // Example: barChart.setData(convertToBarEntries(categorySpending))
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    enum class ChartType {
        PIE, BAR
    }
    
    // Inner class for CategorySpendingAdapter since it was missing
    inner class CategorySpendingAdapter(
        private val context: android.content.Context,
        private var items: List<Pair<String, Double>>
    ) : androidx.recyclerview.widget.RecyclerView.Adapter<CategorySpendingAdapter.ViewHolder>() {
        
        fun updateData(newItems: List<Pair<String, Double>>) {
            this.items = newItems
            notifyDataSetChanged()
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val itemView = android.view.LayoutInflater.from(context)
                .inflate(android.R.layout.simple_list_item_2, parent, false)
            return ViewHolder(itemView)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            val categoryName = item.first
            val amount = item.second
            
            val textView1 = holder.itemView.findViewById<android.widget.TextView>(android.R.id.text1)
            val textView2 = holder.itemView.findViewById<android.widget.TextView>(android.R.id.text2)
            
            textView1.text = categoryName
            textView2.text = java.text.NumberFormat.getCurrencyInstance().format(amount)
        }
        
        override fun getItemCount(): Int = items.size
        
        inner class ViewHolder(itemView: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView)
    }
}