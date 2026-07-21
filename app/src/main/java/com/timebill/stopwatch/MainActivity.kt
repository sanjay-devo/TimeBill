package com.timebill.stopwatch

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.core.widget.addTextChangedListener
import com.google.android.material.textfield.TextInputLayout
import androidx.core.view.GravityCompat
import java.text.DecimalFormat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.timebill.stopwatch.databinding.ActivityMainBinding
import com.timebill.stopwatch.databinding.LayoutSessionSuccessDialogBinding
import com.timebill.stopwatch.model.Session
import com.timebill.stopwatch.repository.FirebaseRepository
import com.timebill.stopwatch.utils.PreferenceManager
import com.timebill.stopwatch.utils.AppUtils
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
    private var isFirstRateLoad = true
    private var isFormatting = false
    private var backPressedTime: Long = 0

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
        
        // Set default navigation item
        binding.navigationView.setCheckedItem(R.id.nav_home)

        setupBackPressed()
    }

    private fun setupBackPressed() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    if (backPressedTime + 2000 > System.currentTimeMillis()) {
                        finish()
                    } else {
                        Toast.makeText(this@MainActivity, getString(R.string.msg_exit_confirm), Toast.LENGTH_SHORT).show()
                        backPressedTime = System.currentTimeMillis()
                    }
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        binding.navigationView.setCheckedItem(R.id.nav_home)
    }

    private fun setupViewModel() {
        val guestId = PreferenceManager(this).getGuestId()
        val repository = FirebaseRepository(guestId)
        val factory = ViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[StopwatchViewModel::class.java]
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.defaultHourlyRate.collectLatest { rate ->
                val currentText = binding.homeHero.etHourlyRate.text.toString()
                
                if (rate != null && rate > 0.0) {
                    if (isFirstRateLoad || (currentText.isEmpty() && !binding.homeHero.etHourlyRate.isFocused)) {
                        if (!binding.homeHero.etHourlyRate.isFocused) {
                            isFormatting = true
                            binding.homeHero.etHourlyRate.setText(formatRate(rate))
                            binding.homeHero.tilHourlyRate.prefixText = getString(R.string.prefix_currency)
                            isFormatting = false
                        }
                    }
                } else {
                    if (!binding.homeHero.etHourlyRate.isFocused) {
                        isFormatting = true
                        binding.homeHero.etHourlyRate.setText("")
                        binding.homeHero.tilHourlyRate.prefixText = null
                        isFormatting = false
                    }
                }
                isFirstRateLoad = false
            }
        }

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
        binding.homeHero.etHourlyRate.addTextChangedListener { text ->
            val hasText = !text.isNullOrEmpty()
            binding.homeHero.tilHourlyRate.prefixText = if (hasText) getString(R.string.prefix_currency) else null

            if (isFormatting) return@addTextChangedListener
            val rate = parseRate(text.toString()) ?: 0.0
            viewModel.updateDefaultHourlyRate(rate)
        }

        binding.homeHero.etHourlyRate.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.homeHero.tilHourlyRate.prefixText = getString(R.string.prefix_currency)
            } else {
                val text = binding.homeHero.etHourlyRate.text.toString()
                val rate = parseRate(text)
                if (rate != null && rate > 0.0) {
                    isFormatting = true
                    binding.homeHero.etHourlyRate.setText(formatRate(rate))
                    binding.homeHero.tilHourlyRate.prefixText = getString(R.string.prefix_currency)
                    isFormatting = false
                } else {
                    isFormatting = true
                    binding.homeHero.etHourlyRate.setText("")
                    binding.homeHero.tilHourlyRate.prefixText = null
                    isFormatting = false
                }
            }
        }

        binding.homeHeader.btnMenu.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    // Already on Home
                }
                R.id.nav_history -> {
                    startActivity(Intent(this, HistoryActivity::class.java))
                }
                R.id.nav_reports -> {
                    startActivity(Intent(this, ReportsActivity::class.java))
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                }
                R.id.nav_help -> {
                    startActivity(WebViewActivity.createIntent(this, getString(R.string.nav_help), "https://timebill.indiacybercafe.com/help-faq.html"))
                }
                R.id.nav_contact -> {
                    startActivity(WebViewActivity.createIntent(this, getString(R.string.nav_contact), "https://timebill.indiacybercafe.com/contact-support.html"))
                }
                R.id.nav_issue -> {
                    startActivity(WebViewActivity.createIntent(this, getString(R.string.nav_issue), "https://timebill.indiacybercafe.com/report-issue.html"))
                }
                R.id.nav_privacy -> {
                    startActivity(WebViewActivity.createIntent(this, getString(R.string.nav_privacy), "https://timebill.indiacybercafe.com/privacy-policy.html"))
                }
                R.id.nav_terms -> {
                    startActivity(WebViewActivity.createIntent(this, getString(R.string.nav_terms), "https://timebill.indiacybercafe.com/terms-and-conditions.html"))
                }
                R.id.nav_disclaimer -> {
                    startActivity(WebViewActivity.createIntent(this, getString(R.string.nav_disclaimer), "https://timebill.indiacybercafe.com/disclaimer.html"))
                }
                R.id.nav_about -> {
                    startActivity(Intent(this, AboutActivity::class.java))
                }
                R.id.nav_rate -> {
                    AppUtils.openPlayStore(this)
                }
                R.id.nav_share -> {
                    AppUtils.shareApp(this)
                }
                R.id.nav_update -> {
                    AppUtils.openPlayStore(this)
                }
                else -> {
                    startActivity(Intent(this, ComingSoonActivity::class.java))
                }
            }
            true
        }

        binding.homeHeader.btnHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        binding.homeHeader.btnHeaderPlayPause.setOnClickListener {
            val rate = parseRate(binding.homeHero.etHourlyRate.text.toString()) ?: 0.0
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
                val rate = parseRate(binding.homeHero.etHourlyRate.text.toString()) ?: 0.0
                val name = binding.homeHero.etClientName.text.toString()
                viewModel.startTimer(rate, name)
            }
        }
    }

    private fun showSuccessDialog(session: Session, duration: String) {
        val dialogBinding = LayoutSessionSuccessDialogBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(this)
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
        ViewCompat.setOnApplyWindowInsetsListener(binding.drawerLayout) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            val isImeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            
            binding.homeHeader.headerRoot.updatePadding(top = systemBars.top)

            val headerView = binding.navigationView.getHeaderView(0)
            headerView?.updatePadding(top = systemBars.top)
            
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

    private fun formatRate(rate: Double): String {
        return if (rate % 1.0 == 0.0) {
            DecimalFormat("0").format(rate)
        } else {
            DecimalFormat("0.00").format(rate)
        }
    }

    private fun parseRate(text: String): Double? {
        return text.replace(',', '.').toDoubleOrNull()
    }

    private fun findParentTextInputLayout(view: View): TextInputLayout? {
        var parent = view.parent
        while (parent is ViewGroup) {
            if (parent is TextInputLayout) return parent
            parent = parent.parent
        }
        return null
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev?.action == MotionEvent.ACTION_DOWN) {
            val v = currentFocus
            if (v is EditText) {
                val outRect = Rect()
                v.getGlobalVisibleRect(outRect)
                if (!outRect.contains(ev.rawX.toInt(), ev.rawY.toInt())) {
                    v.clearFocus()
                    val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(v.windowToken, 0)
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }
}