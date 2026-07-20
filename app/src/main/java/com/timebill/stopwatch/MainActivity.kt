package com.timebill.stopwatch

import android.graphics.Rect
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.textfield.TextInputLayout
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
        
        // 3. Handle WindowInsets for Header and Smooth Keyboard behavior
        setupWindowInsets()

        // 4. Setup Click Listeners
        setupClickListeners()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            val isImeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            
            // Handle Status Bar (Top)
            binding.homeHeader.headerRoot.updatePadding(top = systemBars.top)
            
            // Handle Keyboard/IME (Bottom) padding
            val bottomPadding = if (isImeVisible) ime.bottom else systemBars.bottom
            binding.homeScrollView.updatePadding(bottom = bottomPadding)
            
            // Toggle scrollability based on IME visibility
            binding.homeScrollView.isNestedScrollingEnabled = isImeVisible
            
            if (isImeVisible) {
                // Smoothly scroll to the focused field when keyboard opens
                binding.homeScrollView.post {
                    val focusedView = currentFocus
                    if (focusedView != null) {
                        // Find the TextInputLayout parent
                        val til = findParentTextInputLayout(focusedView)
                        val targetView = til ?: focusedView
                        
                        val rect = Rect()
                        targetView.getDrawingRect(rect)
                        binding.homeScrollView.offsetDescendantRectToMyCoords(targetView, rect)
                        
                        // Scroll so the focused field is centered or fully visible
                        binding.homeScrollView.smoothScrollTo(0, rect.bottom)
                    }
                }
            } else {
                // Smoothly restore original position when keyboard closes
                binding.homeScrollView.post {
                    binding.homeScrollView.smoothScrollTo(0, 0)
                }
            }
            
            // Handle horizontal system bars
            binding.main.updatePadding(left = systemBars.left, right = systemBars.right)
            
            insets
        }
    }

    private fun findParentTextInputLayout(view: View): TextInputLayout? {
        var parent = view.parent
        while (parent is ViewGroup) {
            if (parent is TextInputLayout) return parent
            parent = parent.parent
        }
        return null
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