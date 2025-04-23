package com.bossfinance.app;

import android.app.Application;
import androidx.appcompat.app.AppCompatDelegate;

public class BossFinanceApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Set night mode as default
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
    }
}