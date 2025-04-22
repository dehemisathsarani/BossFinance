package com.example.bossfinance.ui.reports

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.bossfinance.R
import com.example.bossfinance.models.Transaction
import com.example.bossfinance.repository.TransactionRepository
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.tabs.TabLayout
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ReportsFragment : Fragment() {

    private lateinit var pieChart: PieChart
    private lateinit var barChart: BarChart
    private lateinit var chartContainer: ViewGroup
    private lateinit var tabLayout: TabLayout
    private lateinit var tvSelectedMonth: TextView
    private lateinit var tvNoData: TextView
    private lateinit var categoryListView: androidx.recyclerview.widget.RecyclerView
    private lateinit var tvCategorySpending: TextView
    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    private lateinit var transactionRepository: TransactionRepository
    
    // Maps to store aggregated spending data
    private val categorySpending = mutableMapOf<String, Double>()
    private val monthlySpending = mutableMapOf<Int, Double>()
    
    // Chart colors
    private val chartColors = listOf(
        Color.rgb(64, 89, 128), Color.rgb(149, 165, 124), 
        Color.rgb(217, 184, 162), Color.rgb(191, 134, 134), 
        Color.rgb(179, 48, 80), Color.rgb(192, 255, 140),
        Color.rgb(255, 247, 140), Color.rgb(255, 208, 140),
        Color.rgb(140, 234, 255), Color.rgb(255, 140, 157)
    )
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_reports, container, false)
        
        // Initialize repository
        transactionRepository = TransactionRepository.getInstance(requireContext())
        
        // Initialize views
        tabLayout = root.findViewById(R.id.tabLayout)
        tvSelectedMonth = root.findViewById(R.id.tvSelectedMonth)
        chartContainer = root.findViewById(R.id.chartContainer)
        tvNoData = root.findViewById(R.id.tvNoData)
        categoryListView = root.findViewById(R.id.categoryList)
        tvCategorySpending = root.findViewById(R.id.tvCategorySpending)
        
        // Create chart instances
        createCharts()
        
        // Setup date display
        updateDateDisplay()
        
        // Setup previous/next month buttons
        root.findViewById<View>(R.id.btnPreviousMonth).setOnClickListener {
            calendar.add(Calendar.MONTH, -1)
            updateDateDisplay()
            loadDataForCurrentMonth()
            updateChartData()
        }
        
        root.findViewById<View>(R.id.btnNextMonth).setOnClickListener {
            calendar.add(Calendar.MONTH, 1)
            updateDateDisplay()
            loadDataForCurrentMonth()
            updateChartData()
        }
        
        // Setup tab selection listener
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.position?.let { updateChartVisibility(it) }
            }
            
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
        
        // Load data for current month
        loadDataForCurrentMonth()
        
        // Show pie chart by default
        updateChartVisibility(0)
        
        return root
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh data when coming back to this fragment
        loadDataForCurrentMonth()
        updateChartData()
    }
    
    private fun createCharts() {
        try {
            // Create pie chart
            pieChart = PieChart(requireContext())
            pieChart.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setupPieChart(pieChart)
            
            // Create bar chart
            barChart = BarChart(requireContext())
            barChart.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setupBarChart(barChart)
        } catch (e: Exception) {
            Log.e("ReportsFragment", "Error creating charts: ${e.message}", e)
        }
    }
    
    private fun updateDateDisplay() {
        tvSelectedMonth.text = dateFormat.format(calendar.time)
    }
    
    private fun loadDataForCurrentMonth() {
        try {
            // Clear previous data
            categorySpending.clear()
            monthlySpending.clear()
            
            // Get all transactions
            val allTransactions = transactionRepository.getAllTransactions()
            
            // Filter for current month and expenses only
            val monthlyExpenses = filterTransactionsForCurrentMonth(allTransactions)
                .filter { !it.isIncome }
            
            Log.d("ReportsFragment", "Found ${monthlyExpenses.size} expenses for current month")
            
            // Aggregate spending by category
            for (transaction in monthlyExpenses) {
                val currentAmount = categorySpending[transaction.category] ?: 0.0
                categorySpending[transaction.category] = currentAmount + transaction.amount
            }
            
            // Calculate spending for last 6 months (for bar chart)
            calculateMonthlySpending(allTransactions)
            
            // Update UI based on data availability
            updateEmptyState()
        } catch (e: Exception) {
            Log.e("ReportsFragment", "Error loading data: ${e.message}", e)
        }
    }
    
    private fun updateEmptyState() {
        if (categorySpending.isEmpty()) {
            tvNoData.visibility = View.VISIBLE
            tvCategorySpending.visibility = View.GONE
            categoryListView.visibility = View.GONE
        } else {
            tvNoData.visibility = View.GONE
            tvCategorySpending.visibility = View.VISIBLE
            categoryListView.visibility = View.VISIBLE
        }
    }
    
    private fun calculateMonthlySpending(transactions: List<Transaction>) {
        // Get expenses for the last 6 months
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)
        
        // Initialize map with last 6 months
        for (i in 0 until 6) {
            val monthToCheck = (currentMonth - i + 12) % 12
            monthlySpending[monthToCheck] = 0.0
        }
        
        // Filter expenses (non-income transactions)
        val expenses = transactions.filter { !it.isIncome }
        
        // Aggregate by month
        for (transaction in expenses) {
            val cal = Calendar.getInstance()
            cal.time = transaction.date
            val month = cal.get(Calendar.MONTH)
            val year = cal.get(Calendar.YEAR)
            
            // Only include transactions from the current year or previous year if we're looking at past months
            if (year == currentYear || (year == currentYear - 1 && month > currentMonth)) {
                if (monthlySpending.containsKey(month)) {
                    monthlySpending[month] = monthlySpending[month]!! + transaction.amount
                }
            }
        }
    }
    
    private fun filterTransactionsForCurrentMonth(transactions: List<Transaction>): List<Transaction> {
        // Set calendar to first day of current month
        val startCalendar = Calendar.getInstance()
        startCalendar.time = calendar.time
        startCalendar.set(Calendar.DAY_OF_MONTH, 1)
        startCalendar.set(Calendar.HOUR_OF_DAY, 0)
        startCalendar.set(Calendar.MINUTE, 0)
        startCalendar.set(Calendar.SECOND, 0)
        startCalendar.set(Calendar.MILLISECOND, 0)
        
        // Set calendar to last day of current month
        val endCalendar = Calendar.getInstance()
        endCalendar.time = calendar.time
        endCalendar.set(Calendar.DAY_OF_MONTH, endCalendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        endCalendar.set(Calendar.HOUR_OF_DAY, 23)
        endCalendar.set(Calendar.MINUTE, 59)
        endCalendar.set(Calendar.SECOND, 59)
        endCalendar.set(Calendar.MILLISECOND, 999)
        
        return transactions.filter { 
            it.date in startCalendar.time..endCalendar.time 
        }
    }
    
    private fun updateChartVisibility(tabPosition: Int) {
        try {
            // Remove all views from container
            chartContainer.removeAllViews()
            
            // If no data, show message and return
            if (categorySpending.isEmpty()) {
                chartContainer.addView(tvNoData)
                return
            }
            
            // Add selected chart
            when (tabPosition) {
                0 -> {
                    chartContainer.addView(pieChart)
                    loadPieChartData()
                }
                1 -> {
                    chartContainer.addView(barChart)
                    loadBarChartData()
                }
            }
        } catch (e: Exception) {
            Log.e("ReportsFragment", "Error updating chart visibility: ${e.message}", e)
        }
    }
    
    private fun updateChartData() {
        try {
            updateEmptyState()
            
            if (categorySpending.isEmpty()) {
                chartContainer.removeAllViews()
                chartContainer.addView(tvNoData)
                return
            }
            
            when (tabLayout.selectedTabPosition) {
                0 -> loadPieChartData()
                1 -> loadBarChartData()
            }
        } catch (e: Exception) {
            Log.e("ReportsFragment", "Error updating chart data: ${e.message}", e)
        }
    }
    
    private fun setupPieChart(chart: PieChart) {
        chart.setDrawHoleEnabled(true)
        chart.setUsePercentValues(true)
        chart.setEntryLabelTextSize(12f)
        chart.setEntryLabelColor(Color.BLACK)
        chart.setCenterText("Expense Categories")
        chart.setCenterTextSize(16f)
        chart.description.isEnabled = false
        chart.setDrawEntryLabels(true)
        chart.isRotationEnabled = true
        chart.isHighlightPerTapEnabled = true
        chart.setExtraOffsets(5f, 10f, 5f, 5f)
        
        val legend = chart.legend
        legend.isEnabled = true
        legend.verticalAlignment = Legend.LegendVerticalAlignment.TOP
        legend.horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
        legend.orientation = Legend.LegendOrientation.VERTICAL
        legend.setDrawInside(false)
        legend.textSize = 12f
    }
    
    private fun setupBarChart(chart: BarChart) {
        chart.setFitBars(true)
        chart.description.isEnabled = false
        chart.setDrawValueAboveBar(true)
        chart.setDrawBarShadow(false)
        chart.setDrawGridBackground(false)
        chart.setPinchZoom(false)
        chart.setExtraOffsets(10f, 10f, 10f, 10f)
        
        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.setDrawGridLines(false)
        xAxis.textSize = 12f
        
        val leftAxis = chart.axisLeft
        leftAxis.setDrawGridLines(true)
        leftAxis.axisMinimum = 0f
        leftAxis.textSize = 12f
        
        chart.axisRight.isEnabled = false
        chart.animateY(1500)
    }
    
    private fun loadPieChartData() {
        try {
            if (categorySpending.isEmpty()) {
                return
            }
            
            val entries = ArrayList<PieEntry>()
            
            // Limit to top 5 categories and group others
            val sortedCategories = categorySpending.entries.sortedByDescending { it.value }
            val topCategories = sortedCategories.take(5)
            val otherAmount = sortedCategories.drop(5).sumOf { it.value }
            
            // Add top categories
            for ((category, amount) in topCategories) {
                entries.add(PieEntry(amount.toFloat(), category))
            }
            
            // Add "Other" category if needed
            if (otherAmount > 0) {
                entries.add(PieEntry(otherAmount.toFloat(), getString(R.string.other_category)))
            }
            
            if (entries.isEmpty()) {
                pieChart.setNoDataText("No expense data available")
                pieChart.invalidate()
                return
            }
            
            // Configure pie chart data set
            val dataSet = PieDataSet(entries, "")
            dataSet.colors = chartColors.take(entries.size).toList()
            dataSet.valueTextSize = 12f
            dataSet.valueTextColor = Color.BLACK
            dataSet.valueFormatter = PercentFormatter(pieChart)
            dataSet.sliceSpace = 3f
            
            // Configure pie chart data
            val data = PieData(dataSet)
            data.setDrawValues(true)
            
            pieChart.data = data
            pieChart.highlightValues(null)
            pieChart.invalidate()
        } catch (e: Exception) {
            Log.e("ReportsFragment", "Error loading pie chart data: ${e.message}", e)
            pieChart.setNoDataText("Error loading chart data")
            pieChart.invalidate()
        }
    }
    
    private fun loadBarChartData() {
        try {
            if (monthlySpending.isEmpty()) {
                barChart.setNoDataText("No expense data available")
                barChart.invalidate()
                return
            }
            
            val entries = ArrayList<BarEntry>()
            
            // Get month names for labels
            val monthNames = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", 
                                     "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
            val xLabels = ArrayList<String>()
            
            // Sort months in chronological order for display
            val currentMonth = calendar.get(Calendar.MONTH)
            val sortedMonths = monthlySpending.keys.sortedBy { 
                (currentMonth - it + 12) % 12 
            }
            
            // Create bar entries and labels
            sortedMonths.forEachIndexed { index, month ->
                entries.add(BarEntry(index.toFloat(), monthlySpending[month]?.toFloat() ?: 0f))
                xLabels.add(monthNames[month])
            }
            
            if (entries.isEmpty()) {
                barChart.setNoDataText("No expense data available")
                barChart.invalidate()
                return
            }
            
            // Configure bar data set
            val dataSet = BarDataSet(entries, "Monthly Expenses")
            dataSet.setColors(*ColorTemplate.MATERIAL_COLORS)
            dataSet.valueTextColor = Color.BLACK
            dataSet.valueTextSize = 12f
            
            // Configure bar data
            val data = BarData(dataSet)
            data.barWidth = 0.9f
            
            // Set X-axis labels
            barChart.xAxis.valueFormatter = IndexAxisValueFormatter(xLabels)
            barChart.data = data
            barChart.invalidate()
        } catch (e: Exception) {
            Log.e("ReportsFragment", "Error loading bar chart data: ${e.message}", e)
            barChart.setNoDataText("Error loading chart data")
            barChart.invalidate()
        }
    }
}