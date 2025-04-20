package com.example.bossfinance

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import com.example.bossfinance.databinding.ActivitySplashBinding

class SplashActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Apply fade-in animation to logo and app name
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        binding.ivLogo.startAnimation(fadeIn)
        binding.tvAppName.startAnimation(fadeIn)
        
        // Set alpha to 1 after animation for smooth transition
        binding.ivLogo.alpha = 1f
        binding.tvAppName.alpha = 1f
        
        // Navigate to onboarding screen after 2 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, OnboardingActivity::class.java)
            startActivity(intent)
            finish()
        }, 2000) // 2000ms = 2 seconds
    }
}