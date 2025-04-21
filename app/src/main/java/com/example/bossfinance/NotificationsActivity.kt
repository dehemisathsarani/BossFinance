package com.example.bossfinance

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.MenuItem
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.bossfinance.databinding.ActivityNotificationsBinding
import com.example.bossfinance.models.NotificationSettings
import com.example.bossfinance.repository.NotificationRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class NotificationsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNotificationsBinding
    private lateinit var notificationRepository: NotificationRepository
    private var reminderHour = 20
    private var reminderMinute = 0
    private var budgetThreshold = 90
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        notificationRepository = NotificationRepository.getInstance(this)
        
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.notifications_settings)
        
        loadCurrentSettings()
        setupBudgetAlertControls()
        setupDailyReminderControls()
        setupSaveButton()
    }
    
    private fun loadCurrentSettings() {
        val settings = notificationRepository.getNotificationSettings()
        
        // Budget alerts
        binding.switchBudgetAlerts.isChecked = settings.budgetAlertsEnabled
        budgetThreshold = settings.budgetAlertThreshold
        binding.seekBarThreshold.progress = budgetThreshold
        updateThresholdLabel()
        
        // Daily reminders
        binding.switchDailyReminders.isChecked = settings.dailyRemindersEnabled
        reminderHour = settings.reminderHour
        reminderMinute = settings.reminderMinute
        updateTimeButton()
    }
    
    private fun setupBudgetAlertControls() {
        binding.seekBarThreshold.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    budgetThreshold = progress
                    updateThresholdLabel()
                }
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }
    
    private fun updateThresholdLabel() {
        binding.tvThresholdValue.text = "$budgetThreshold%"
    }
    
    private fun setupDailyReminderControls() {
        binding.btnSetReminderTime.setOnClickListener {
            showTimePickerDialog()
        }
    }
    
    private fun showTimePickerDialog() {
        TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                reminderHour = hourOfDay
                reminderMinute = minute
                updateTimeButton()
            },
            reminderHour,
            reminderMinute,
            false
        ).show()
    }
    
    private fun updateTimeButton() {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, reminderHour)
            set(Calendar.MINUTE, reminderMinute)
        }
        
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        binding.btnSetReminderTime.text = timeFormat.format(calendar.time)
    }
    
    private fun setupSaveButton() {
        binding.btnSaveNotificationSettings.setOnClickListener {
            saveSettings()
        }
    }
    
    private fun saveSettings() {
        val settings = NotificationSettings(
            budgetAlertsEnabled = binding.switchBudgetAlerts.isChecked,
            budgetAlertThreshold = budgetThreshold,
            dailyRemindersEnabled = binding.switchDailyReminders.isChecked,
            reminderHour = reminderHour,
            reminderMinute = reminderMinute
        )
        
        notificationRepository.saveNotificationSettings(settings)
        
        Toast.makeText(this, getString(R.string.settings_saved), Toast.LENGTH_SHORT).show()
        finish()
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}