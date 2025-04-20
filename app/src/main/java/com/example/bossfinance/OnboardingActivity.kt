package com.example.bossfinance

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.example.bossfinance.databinding.ActivityOnboardingBinding

class OnboardingActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var onboardingAdapter: OnboardingAdapter
    private lateinit var indicatorsContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setOnboardingItems()
        setupIndicators()
        setCurrentIndicator(0)
        setupListeners()
    }

    private fun setOnboardingItems() {
        onboardingAdapter = OnboardingAdapter(
            listOf(
                OnboardingItem(
                    R.drawable.onboardscreen1,
                    getString(R.string.onboarding_title_1),
                    getString(R.string.onboarding_desc_1)
                ),
                OnboardingItem(
                    R.drawable.onboardscreen2,
                    getString(R.string.onboarding_title_2),
                    getString(R.string.onboarding_desc_2)
                ),
                OnboardingItem(
                    R.drawable.onboardscreen3,
                    getString(R.string.onboarding_title_3),
                    getString(R.string.onboarding_desc_3)
                )
            )
        )
        binding.viewPager.adapter = onboardingAdapter

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                setCurrentIndicator(position)
                updateButtonText(position)
            }
        })
    }

    private fun setupIndicators() {
        indicatorsContainer = binding.indicatorsContainer
        val indicators = arrayOfNulls<ImageView>(onboardingAdapter.itemCount)
        val layoutParams: LinearLayout.LayoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        layoutParams.setMargins(8, 0, 8, 0)
        
        for (i in indicators.indices) {
            indicators[i] = ImageView(applicationContext)
            indicators[i]?.apply {
                setImageDrawable(ContextCompat.getDrawable(applicationContext, R.drawable.indicator_inactive))
                this.layoutParams = layoutParams
            }
            indicatorsContainer.addView(indicators[i])
        }
    }

    private fun setCurrentIndicator(position: Int) {
        val childCount = indicatorsContainer.childCount
        for (i in 0 until childCount) {
            val imageView = indicatorsContainer.getChildAt(i) as ImageView
            if (i == position) {
                imageView.setImageDrawable(ContextCompat.getDrawable(applicationContext, R.drawable.indicator_active))
            } else {
                imageView.setImageDrawable(ContextCompat.getDrawable(applicationContext, R.drawable.indicator_inactive))
            }
        }
    }

    private fun updateButtonText(position: Int) {
        if (position == onboardingAdapter.itemCount - 1) {
            binding.nextButton.text = getString(R.string.get_started)
        } else {
            binding.nextButton.text = getString(R.string.next)
        }
    }

    private fun setupListeners() {
        binding.skipButton.setOnClickListener {
            navigateToMainActivity()
        }
        
        binding.nextButton.setOnClickListener {
            if (binding.viewPager.currentItem + 1 < onboardingAdapter.itemCount) {
                binding.viewPager.currentItem = binding.viewPager.currentItem + 1
            } else {
                navigateToMainActivity()
            }
        }
    }

    private fun navigateToMainActivity() {
        startActivity(Intent(applicationContext, MainActivity::class.java))
        finish()
    }
}