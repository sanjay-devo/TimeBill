package com.timebill.stopwatch

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.timebill.stopwatch.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 1. Enable Edge-to-Edge
        enableEdgeToEdge()
        
        // 2. Setup ViewBinding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // 3. Handle WindowInsets for the Header
        // We apply top padding to the header to avoid overlapping with the status bar
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            
            // Apply top padding to the header container
            binding.homeHeader.headerRoot.updatePadding(top = systemBars.top)
            
            // Apply bottom padding to the root if needed, or handle it in the content area
            v.updatePadding(left = systemBars.left, right = systemBars.right, bottom = systemBars.bottom)
            
            insets
        }

        // 4. Setup Click Listeners
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.homeHeader.btnMenu.setOnClickListener {
            Toast.makeText(this, "Menu Clicked", Toast.LENGTH_SHORT).show()
        }

        binding.homeHeader.btnHistory.setOnClickListener {
            Toast.makeText(this, "History Clicked", Toast.LENGTH_SHORT).show()
        }
    }
}