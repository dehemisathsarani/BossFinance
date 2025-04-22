package com.example.bossfinance.ui.settings

import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreferenceCompat
import com.example.bossfinance.R
import com.example.bossfinance.preferences.AppPreferences
import java.text.NumberFormat
import java.util.Currency

class SettingsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var appPreferences: AppPreferences

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_preferences, rootKey)
        appPreferences = AppPreferences.getInstance(requireContext())
        
        setupPreferences()
    }

    private fun setupPreferences() {
        // Budget Settings
        val budgetPreference = findPreference<EditTextPreference>("monthly_budget")
        budgetPreference?.setOnBindEditTextListener { editText ->
            editText.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        
        val currencyPreference = findPreference<ListPreference>("currency_code")
        currencyPreference?.summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
        
        // Dark mode toggle
        val darkModePreference = findPreference<SwitchPreferenceCompat>("dark_mode")
        darkModePreference?.setOnPreferenceChangeListener { _, newValue ->
            val darkModeEnabled = newValue as Boolean
            updateTheme(darkModeEnabled)
            true
        }
        
        // About section
        val versionPreference = findPreference<Preference>("app_version")
        versionPreference?.summary = getAppVersion()
    }
    
    private fun updateTheme(darkModeEnabled: Boolean) {
        if (darkModeEnabled) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }
    
    private fun getAppVersion(): String {
        val packageInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
        return "${packageInfo.versionName} (${packageInfo.versionCode})"
    }
    
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        when (key) {
            "monthly_budget" -> {
                val budgetString = sharedPreferences.getString(key, "0") ?: "0"
                try {
                    val budgetValue = budgetString.toDouble()
                    appPreferences.setMonthlyBudget(budgetValue)
                } catch (e: NumberFormatException) {
                    appPreferences.setMonthlyBudget(0.0)
                }
            }
            "currency_code" -> {
                val currencyCode = sharedPreferences.getString(key, "USD") ?: "USD"
                appPreferences.setCurrencyCode(currencyCode)
            }
            "budget_alerts_enabled" -> {
                val enabled = sharedPreferences.getBoolean(key, true)
                appPreferences.setBudgetAlertsEnabled(enabled)
            }
            "transaction_reminders_enabled" -> {
                val enabled = sharedPreferences.getBoolean(key, false)
                appPreferences.setTransactionRemindersEnabled(enabled)
            }
            "dark_mode" -> {
                val enabled = sharedPreferences.getBoolean(key, false)
                appPreferences.setDarkMode(enabled)
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        preferenceManager.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
    }
    
    override fun onPause() {
        super.onPause()
        preferenceManager.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
    }
}