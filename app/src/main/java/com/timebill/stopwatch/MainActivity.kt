package com.timebill.stopwatch

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputLayout
import com.timebill.stopwatch.databinding.ActivityMainBinding
import com.timebill.stopwatch.databinding.LayoutSessionSuccessDialogBinding
import com.timebill.stopwatch.model.Session
import com.timebill.stopwatch.repository.FirebaseRepository
import com.timebill.stopwatch.utils.PreferenceManager
import com.timebill.stopwatch.viewmodel.StopwatchViewModel
import com.timebill.stopwatch.viewmodel.ViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: StopwatchViewModel
    private var currentBorderColor: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentBorderColor = ContextCompat.getColor(this, R.color.accent_gold)
        setupWindowInsets()
        setupViewModel()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupViewModel() {
        val guestId = PreferenceManager(this).getGuestId()
        val repository = FirebaseRepository(guestId)
        val factory = ViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[StopwatchViewModel::class.java]
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.timerText.collectLatest {
                binding.homeHero.tvTimer.text = it
            }
        }

        lifecycleScope.launch {
            viewModel.earningsText.collectLatest {
                binding.homeHero.tvLiveEarnings.text = it
            }
        }

        lifecycleScope.launch {
            viewModel.isRunning.collectLatest { isRunning ->
                updateUI(isRunning, viewModel.isPaused.value)
            }
        }

        lifecycleScope.launch {
            viewModel.isPaused.collectLatest { isPaused ->
                updateUI(viewModel.isRunning.value, isPaused)
            }
        }
    }

    private fun updateUI(isRunning: Boolean, isPaused: Boolean) {
        val goldColor = ContextCompat.getColor(this, R.color.accent_gold)
        val redColor = android.graphics.Color.parseColor("#E53935")

        // Large Home Button UI
        if (isRunning) {
            binding.homeHero.btnMainAction.setImageResource(R.drawable.ic_stop_large)
            binding.homeHero.tvActionLabel.setText(R.string.label_stop)
            animateBorderColor(redColor)
        } else {
            binding.homeHero.btnMainAction.setImageResource(R.drawable.ic_play_large)
            binding.homeHero.tvActionLabel.setText(R.string.label_start_tracking)
            animateBorderColor(goldColor)
        }

        // Header Play/Pause Icon UI
        if (isRunning) {
            if (isPaused) {
                binding.homeHeader.btnHeaderPlayPause.setImageResource(R.drawable.ic_play_rounded)
            } else {
                binding.homeHeader.btnHeaderPlayPause.setImageResource(R.drawable.ic_pause_rounded)
            }
        } else {
            binding.homeHeader.btnHeaderPlayPause.setImageResource(R.drawable.ic_play_rounded)
        }
    }

    private fun animateBorderColor(targetColor: Int) {
        if (currentBorderColor == targetColor) return

        val animator = ValueAnimator.ofObject(ArgbEvaluator(), currentBorderColor, targetColor)
        animator.duration = 200
        animator.addUpdateListener { animation ->
            val color = animation.animatedValue as Int
            val ripple = binding.homeHero.btnMainAction.background as? RippleDrawable
            val shape = ripple?.getDrawable(0) as? GradientDrawable
            shape?.setStroke(4.dpToPx(), color)
            currentBorderColor = color
        }
        animator.start()
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    private fun setupClickListeners() {
        binding.homeHeader.btnMenu.setOnClickListener {
            // Menu logic
        }

        binding.homeHeader.btnHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        binding.homeHeader.btnHeaderPlayPause.setOnClickListener {
            val rate = binding.homeHero.etHourlyRate.text.toString().toDoubleOrNull() ?: 0.0
            val name = binding.homeHero.etClientName.text.toString()
            viewModel.toggleStartPause(rate, name)
        }

        binding.homeHero.btnMainAction.setOnClickListener {
            if (viewModel.isRunning.value) {
                // Save current timer text before reset
                val currentTimer = binding.homeHero.tvTimer.text.toString()
                val session = viewModel.stopTimer()
                viewModel.saveSession(session)
                showSuccessDialog(session, currentTimer)
            } else {
                val rate = binding.homeHero.etHourlyRate.text.toString().toDoubleOrNull() ?: 0.0
                val name = binding.homeHero.etClientName.text.toString()
                viewModel.startTimer(rate, name)
            }
        }
    }

    private fun showSuccessDialog(session: Session, duration: String) {
        val dialogBinding = LayoutSessionSuccessDialogBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))

        dialogBinding.tvDialogClient.text = "Client: ${if (session.clientName.isNullOrEmpty()) "Unnamed" else session.clientName}"
        dialogBinding.tvDialogDuration.text = "Duration: $duration"
        dialogBinding.tvDialogEarnings.text = String.format(Locale.getDefault(), "Earnings: ₹%.2f", session.earnings)
        
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        dialogBinding.tvDialogDateTime.text = "Date: ${sdf.format(Date(session.timestamp ?: System.currentTimeMillis()))}"

        dialogBinding.btnCloseDialog.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun formatDuration(millis: Long): String {
        val seconds = (millis / 1000) % 60
        val minutes = (millis / (1000 * 60)) % 60
        val hours = (millis / (1000 * 60 * 60))
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            val isImeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            
            binding.homeHeader.headerRoot.updatePadding(top = systemBars.top)
            
            val bottomPadding = if (isImeVisible) ime.bottom else systemBars.bottom
            binding.homeScrollView.updatePadding(bottom = bottomPadding)
            
            binding.homeScrollView.isNestedScrollingEnabled = isImeVisible
            
            if (isImeVisible) {
                binding.homeScrollView.post {
                    val focusedView = currentFocus
                    if (focusedView != null) {
                        val til = findParentTextInputLayout(focusedView)
                        val targetView = til ?: focusedView
                        val rect = Rect()
                        targetView.getDrawingRect(rect)
                        binding.homeScrollView.offsetDescendantRectToMyCoords(targetView, rect)
                        binding.homeScrollView.smoothScrollTo(0, rect.bottom)
                    }
                }
            } else {
                binding.homeScrollView.post {
                    binding.homeScrollView.smoothScrollTo(0, 0)
                }
            }
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
}