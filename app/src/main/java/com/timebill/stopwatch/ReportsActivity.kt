package com.timebill.stopwatch

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.timebill.stopwatch.databinding.ActivityReportsBinding
import com.timebill.stopwatch.repository.FirebaseRepository
import com.timebill.stopwatch.utils.PreferenceManager
import com.timebill.stopwatch.viewmodel.ReportsState
import com.timebill.stopwatch.viewmodel.ReportsViewModel
import com.timebill.stopwatch.viewmodel.ViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ReportsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReportsBinding
    private lateinit var viewModel: ReportsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityReportsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()
        setupViewModel()
        setupListeners()
        observeViewModel()
        
        binding.navigationView.setCheckedItem(R.id.nav_reports)
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.drawerLayout) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.reportsHeader.headerRoot.updatePadding(top = systemBars.top)
            
            val headerView = binding.navigationView.getHeaderView(0)
            headerView?.updatePadding(top = systemBars.top)
            
            binding.reportsRoot.updatePadding(left = systemBars.left, right = systemBars.right, bottom = systemBars.bottom)
            insets
        }
    }

    private fun setupViewModel() {
        val guestId = PreferenceManager(this).getGuestId()
        val repository = FirebaseRepository(guestId)
        val factory = ViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[ReportsViewModel::class.java]
    }

    private fun setupListeners() {
        binding.reportsHeader.btnBack.setOnClickListener {
            finish()
        }

        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    startActivity(intent)
                    finish()
                }
                R.id.nav_history -> {
                    startActivity(Intent(this, HistoryActivity::class.java))
                    finish()
                }
                R.id.nav_reports -> {
                    // Already here
                }
                else -> {
                    startActivity(Intent(this, ComingSoonActivity::class.java))
                }
            }
            true
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                when (state) {
                    is ReportsState.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.layoutContent.visibility = View.GONE
                        binding.layoutEmpty.visibility = View.GONE
                    }
                    is ReportsState.Success -> {
                        binding.progressBar.visibility = View.GONE
                        binding.layoutContent.visibility = View.VISIBLE
                        binding.layoutEmpty.visibility = View.GONE
                        
                        binding.tvTotalHoursValue.text = state.totalHours
                        binding.tvTotalEarningsValue.text = state.totalEarnings
                        binding.tvTotalSessionsValue.text = state.totalSessions
                        binding.tvAvgEarningsValue.text = state.todayEarnings
                    }
                    is ReportsState.Empty -> {
                        binding.progressBar.visibility = View.GONE
                        binding.layoutContent.visibility = View.GONE
                        binding.layoutEmpty.visibility = View.VISIBLE
                    }
                    is ReportsState.Error -> {
                        binding.progressBar.visibility = View.GONE
                        Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}