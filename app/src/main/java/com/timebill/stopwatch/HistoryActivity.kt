package com.timebill.stopwatch

import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.timebill.stopwatch.adapter.SessionsAdapter
import com.timebill.stopwatch.databinding.ActivityHistoryBinding
import com.timebill.stopwatch.repository.FirebaseRepository
import com.timebill.stopwatch.utils.PreferenceManager
import com.timebill.stopwatch.utils.AppUtils
import com.timebill.stopwatch.viewmodel.HistoryViewModel
import com.timebill.stopwatch.viewmodel.ViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private lateinit var viewModel: HistoryViewModel
    private lateinit var adapter: SessionsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()
        setupViewModel()
        setupRecyclerView()
        setupListeners()
        observeViewModel()
        
        binding.navigationView.setCheckedItem(R.id.nav_history)
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.drawerLayout) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.historyHeader.headerRoot.updatePadding(top = systemBars.top)
            
            val headerView = binding.navigationView.getHeaderView(0)
            headerView?.updatePadding(top = systemBars.top)
            
            binding.historyRoot.updatePadding(left = systemBars.left, right = systemBars.right, bottom = systemBars.bottom)
            insets
        }
    }

    private fun setupViewModel() {
        val guestId = PreferenceManager(this).getGuestId()
        val repository = FirebaseRepository(guestId)
        val factory = ViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[HistoryViewModel::class.java]
    }

    private fun setupRecyclerView() {
        adapter = SessionsAdapter { sessionId ->
            viewModel.deleteSession(sessionId)
        }
        binding.rvHistory.layoutManager = LinearLayoutManager(this)
        binding.rvHistory.adapter = adapter
    }

    private fun setupListeners() {
        binding.historyHeader.btnBack.setOnClickListener {
            finish()
        }

        binding.historyHeader.btnDeleteAll.setOnClickListener {
            showDeleteAllDialog()
        }

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadSessions()
        }

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false

            override fun onQueryTextChange(newText: String?): Boolean {
                filterSessions(newText)
                return true
            }
        })

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
                    // Already here
                }
                R.id.nav_reports -> {
                    startActivity(Intent(this, ReportsActivity::class.java))
                    finish()
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
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.sessions.collectLatest { sessions ->
                adapter.submitList(sessions)
                binding.tvEmpty.visibility = if (sessions.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        lifecycleScope.launch {
            viewModel.isRefreshing.collectLatest { isRefreshing ->
                binding.swipeRefresh.isRefreshing = isRefreshing
            }
        }
    }

    private fun filterSessions(query: String?) {
        val currentList = viewModel.sessions.value
        if (query.isNullOrEmpty()) {
            adapter.submitList(currentList)
        } else {
            val filtered = currentList.filter {
                it.clientName?.contains(query, ignoreCase = true) == true
            }
            adapter.submitList(filtered)
        }
    }

    private fun showDeleteAllDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.label_delete_all)
            .setMessage(R.string.msg_delete_all_confirm)
            .setPositiveButton(R.string.label_delete_all) { _, _ ->
                viewModel.deleteAllSessions()
            }
            .setNegativeButton(R.string.label_cancel, null)
            .show()
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