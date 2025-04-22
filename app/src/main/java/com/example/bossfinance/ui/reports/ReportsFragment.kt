package com.example.bossfinance.ui.reports

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.bossfinance.R
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
    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_reports, container, false)
        
        // Initialize views
        tabLayout = root.findViewById(R.id.tabLayout)
        tvSelectedMonth = root.findViewById(R.id.tvSelectedMonth)
        chartContainer = root.findViewById(R.id.chartContainer)
        
        // Create chart instances
        createCharts()
        
        // Setup date display
        updateDateDisplay()
        
        // Setup previous/next month buttons
        root.findViewById<View>(R.id.btnPreviousMonth).setOnClickListener {
            calendar.add(Calendar.MONTH, -1)
            updateDateDisplay()
            updateChartData()
        }
        
        root.findViewById<View>(R.id.btnNextMonth).setOnClickListener {
            calendar.add(Calendar.MONTH, 1)
            updateDateDisplay()
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
        
        // Show pie chart by default
        updateChartVisibility(0)
        
        return root
    }
    
    private fun createCharts() {
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
    }
    
    private fun updateDateDisplay() {
        tvSelectedMonth.text = dateFormat.format(calendar.time)
    }
    
    private fun updateChartVisibility(tabPosition: Int) {
        // Remove all views from container
        chartContainer.removeAllViews()
        
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
    }
    
    private fun updateChartData() {
        when (tabLayout.selectedTabPosition) {
            0 -> loadPieChartData()
            1 -> loadBarChartData()
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
        
        val legend = chart.legend
        legend.isEnabled = true
        legend.verticalAlignment = Legend.LegendVerticalAlignment.TOP
        legend.horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
        legend.orientation = Legend.LegendOrientation.VERTICAL
        legend.setDrawInside(false)
    }
    
    private fun setupBarChart(chart: BarChart) {
        chart.setFitBars(true)
        chart.description.isEnabled = false
        chart.setDrawValueAboveBar(true)
        
        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.setDrawGridLines(false)
        
        val leftAxis = chart.axisLeft
        leftAxis.setDrawGridLines(true)
        leftAxis.axisMinimum = 0f
        
        chart.axisRight.isEnabled = false
        chart.animateY(1500)
    }
    
    private fun loadPieChartData() {
        // Sample data - replace with your actual data
        val entries = ArrayList<PieEntry>()
        entries.add(PieEntry(25f, "Food"))
        entries.add(PieEntry(35f, "Rent"))
        entries.add(PieEntry(15f, "Transport"))
        entries.add(PieEntry(10f, "Utilities"))
        entries.add(PieEntry(15f, "Others"))
        
        val colors = ArrayList<Int>()
        for (color in ColorTemplate.MATERIAL_COLORS) {
            colors.add(color)
        }
        
        val dataSet = PieDataSet(entries, "Expense Categories")
        dataSet.setColors(colors)
        dataSet.valueTextColor = Color.BLACK
        dataSet.valueTextSize = 12f
        
        val data = PieData(dataSet)
        data.setDrawValues(true)
        
        pieChart.data = data
        pieChart.invalidate()
    }
    
    private fun loadBarChartData() {
        // Sample data - replace with your actual data
        val entries = ArrayList<BarEntry>()
        entries.add(BarEntry(0f, 1000f))
        entries.add(BarEntry(1f, 1200f))
        entries.add(BarEntry(2f, 800f))
        entries.add(BarEntry(3f, 1500f))
        entries.add(BarEntry(4f, 900f))
        entries.add(BarEntry(5f, 1100f))
        
        val dataSet = BarDataSet(entries, "Monthly Expenses")
        // Fix the argument type mismatch by directly using the IntArray
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS, 255)
        dataSet.valueTextColor = Color.BLACK
        dataSet.valueTextSize = 12f
        
        val data = BarData(dataSet)
        data.barWidth = 0.9f
        
        // Set X-axis labels
        val xLabel = ArrayList<String>()
        xLabel.add("Jan")
        xLabel.add("Feb")
        xLabel.add("Mar")
        xLabel.add("Apr")
        xLabel.add("May")
        xLabel.add("Jun")
        
        barChart.xAxis.valueFormatter = IndexAxisValueFormatter(xLabel)
        barChart.data = data
        barChart.invalidate()
    }
}