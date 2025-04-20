package com.example.bossfinance

import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.bossfinance.databinding.ActivitySpendingAnalysisBinding
import com.example.bossfinance.models.Transaction
import com.example.bossfinance.repository.TransactionRepository
import com.github.mikephil.charting.animation.Easing
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
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.tabs.TabLayout
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class SpendingAnalysisActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySpendingAnalysisBinding
    private val transactionRepository = TransactionRepository.getInstance()
    private val calendar = Calendar.getInstance()
    private val monthFormatter = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    
    // Maps to store aggregated spending data
    private val categorySpending = mutableMapOf<String, Double>()
    
    // Chart colors
    private val chartColors = listOf(
        Color.rgb(64, 89, 128), Color.rgb(149, 165, 124), 
        Color.rgb(217, 184, 162), Color.rgb(191, 134, 134), 
        Color.rgb(179, 48, 80), Color.rgb(192, 255, 140),
        Color.rgb(255, 247, 140), Color.rgb(255, 208, 140),
        Color.rgb(140, 234, 255), Color.rgb(255, 140, 157)
    )
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySpendingAnalysisBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Setup toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.spending_analysis)
        
        // Setup month navigation
        setupMonthNavigation()
        
        // Setup chart type tabs
        setupChartTypeTabs()
        
        // Load and display data for current month
        updateMonthDisplay()
        loadDataForCurrentMonth()
        updateCharts()
    }
    
    private fun setupMonthNavigation() {
        // Set initial month display
        binding.tvCurrentMonth.text = monthFormatter.format(calendar.time)
        
        // Setup previous month button
        binding.btnPreviousMonth.setOnClickListener {
            calendar.add(Calendar.MONTH, -1)
            updateMonthDisplay()
            loadDataForCurrentMonth()
            updateCharts()
        }
        
        // Setup next month button
        binding.btnNextMonth.setOnClickListener {
            calendar.add(Calendar.MONTH, 1)
            updateMonthDisplay()
            loadDataForCurrentMonth()
            updateCharts()
        }
    }
    
    private fun updateMonthDisplay() {
        binding.tvCurrentMonth.text = monthFormatter.format(calendar.time)
    }
    
    private fun setupChartTypeTabs() {
        binding.chartTypeTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> { // Pie Chart
                        binding.pieChart.visibility = View.VISIBLE
                        binding.barChart.visibility = View.GONE
                    }
                    1 -> { // Bar Chart
                        binding.pieChart.visibility = View.GONE
                        binding.barChart.visibility = View.VISIBLE
                    }
                }
            }
            
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }
    
    private fun loadDataForCurrentMonth() {
        // Clear previous data
        categorySpending.clear()
        
        // Get all transactions
        val allTransactions = transactionRepository.getAllTransactions()
        
        // Filter for current month and expenses only
        val monthlyExpenses = filterTransactionsForCurrentMonth(allTransactions)
            .filter { !it.isIncome }
        
        // If no data, show message and hide charts
        if (monthlyExpenses.isEmpty()) {
            binding.tvNoDataMessage.visibility = View.VISIBLE
            binding.pieChart.visibility = View.GONE
            binding.barChart.visibility = View.GONE
            return
        } else {
            binding.tvNoDataMessage.visibility = View.GONE
            if (binding.chartTypeTabs.selectedTabPosition == 0) {
                binding.pieChart.visibility = View.VISIBLE
            } else {
                binding.barChart.visibility = View.VISIBLE
            }
        }
        
        // Aggregate spending by category
        for (transaction in monthlyExpenses) {
            val currentAmount = categorySpending[transaction.category] ?: 0.0
            categorySpending[transaction.category] = currentAmount + transaction.amount
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
    
    private fun updateCharts() {
        updatePieChart()
        updateBarChart()
    }
    
    private fun updatePieChart() {
        // Create entries for pie chart
        val entries = mutableListOf<PieEntry>()
        
        // Limit to top categories and group others
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
        
        // Configure pie chart data set
        val dataSet = PieDataSet(entries, "")
        dataSet.colors = chartColors
        dataSet.valueTextSize = 12f
        dataSet.valueTextColor = Color.WHITE
        dataSet.valueFormatter = PercentFormatter(binding.pieChart)
        
        // Configure pie chart data
        val data = PieData(dataSet)
        
        // Configure pie chart
        with(binding.pieChart) {
            this.data = data
            description.isEnabled = false
            isDrawHoleEnabled = true
            setHoleColor(Color.TRANSPARENT)
            holeRadius = 40f
            setDrawEntryLabels(false)
            setUsePercentValues(true)
            legend.isEnabled = true
            legend.orientation = Legend.LegendOrientation.VERTICAL
            legend.verticalAlignment = Legend.LegendVerticalAlignment.CENTER
            legend.horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
            legend.setDrawInside(false)
            animateY(1000, Easing.EaseInOutQuad)
            invalidate()
        }
    }
    
    private fun updateBarChart() {
        // Check if there's data before attempting to create the chart
        if (categorySpending.isEmpty()) {
            binding.barChart.visibility = View.GONE
            binding.tvNoDataMessage.visibility = View.VISIBLE
            return
        }

        // Create entries for bar chart
        val entries = mutableListOf<BarEntry>()
        val categoryLabels = mutableListOf<String>()
        
        // Limit to top categories and group others like in the pie chart
        val sortedCategories = categorySpending.entries.sortedByDescending { it.value }
        val topCategories = sortedCategories.take(5)
        val otherAmount = sortedCategories.drop(5).sumOf { it.value }
        
        // Add top categories
        topCategories.forEachIndexed { index, (category, amount) ->
            entries.add(BarEntry(index.toFloat(), amount.toFloat()))
            categoryLabels.add(category)
        }
        
        // Add "Other" category if needed
        if (otherAmount > 0) {
            entries.add(BarEntry(entries.size.toFloat(), otherAmount.toFloat()))
            categoryLabels.add(getString(R.string.other_category))
        }
        
        // Configure bar data set
        val dataSet = BarDataSet(entries, getString(R.string.category_spending))
        dataSet.colors = chartColors.take(entries.size).toList()
        dataSet.valueTextSize = 10f  // Smaller text size to prevent overlap
        dataSet.valueTextColor = Color.BLACK
        
        // Format the values to currency
        val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US)
        dataSet.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                // Simplify currency display for better readability
                if (value >= 1000) {
                    return currencyFormatter.format(value).replace(".00", "")
                }
                return currencyFormatter.format(value)
            }
        }
        
        // Configure bar data
        val data = BarData(dataSet)
        data.barWidth = 0.5f  // Make bars slightly thinner
        
        // Configure bar chart
        with(binding.barChart) {
            this.data = data
            description.isEnabled = false
            
            // Enable legend for consistency with pie chart
            legend.isEnabled = true
            legend.orientation = Legend.LegendOrientation.VERTICAL
            legend.verticalAlignment = Legend.LegendVerticalAlignment.CENTER
            legend.horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
            legend.setDrawInside(false)
            legend.textSize = 12f
            
            // X-axis setup
            xAxis.valueFormatter = IndexAxisValueFormatter(categoryLabels)
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.granularity = 1f
            xAxis.isGranularityEnabled = true
            xAxis.setDrawGridLines(false)
            xAxis.labelRotationAngle = 45f
            xAxis.labelCount = categoryLabels.size
            xAxis.textSize = 11f  // Slightly smaller text
            
            // Y-axis setup
            axisLeft.setDrawGridLines(true)
            axisLeft.axisMinimum = 0f
            axisLeft.textSize = 11f
            axisRight.isEnabled = false
            
            // Ensure proper margins for the chart content
            setExtraOffsets(15f, 10f, 30f, 20f)
            
            // Make sure we have visible height
            minimumHeight = 500
            
            // Other settings
            setFitBars(true)
            animateY(1500, Easing.EaseInOutQuad)
            
            // Improve touch interactions
            isDoubleTapToZoomEnabled = false
            setPinchZoom(false)
            
            // Force redraw
            notifyDataSetChanged()
            invalidate()
        }
        
        // Ensure chart is visible if in the bar chart tab
        if (binding.chartTypeTabs.selectedTabPosition == 1) {
            binding.barChart.visibility = View.VISIBLE
            binding.pieChart.visibility = View.GONE
            binding.tvNoDataMessage.visibility = View.GONE
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
