package com.timebill.stopwatch

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.timebill.stopwatch.adapter.ClientsAdapter
import com.timebill.stopwatch.databinding.ActivitySelectClientBinding
import com.timebill.stopwatch.repository.FirebaseRepository
import com.timebill.stopwatch.utils.PreferenceManager
import com.timebill.stopwatch.viewmodel.ClientsViewModel
import com.timebill.stopwatch.viewmodel.ViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SelectClientActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySelectClientBinding
    private lateinit var viewModel: ClientsViewModel
    private lateinit var adapter: ClientsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySelectClientBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()
        setupViewModel()
        setupRecyclerView()
        setupListeners()
        observeViewModel()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.selectClientRoot) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.appBarLayout.updatePadding(top = systemBars.top)
            binding.selectClientRoot.updatePadding(
                left = systemBars.left,
                right = systemBars.right,
                bottom = systemBars.bottom
            )
            insets
        }
    }

    private fun setupViewModel() {
        val repository = FirebaseRepository(PreferenceManager(this).getGuestId())
        val factory = ViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[ClientsViewModel::class.java]
    }

    private fun setupRecyclerView() {
        adapter = ClientsAdapter(
            onSelect = { client ->
                val resultIntent = Intent()
                resultIntent.putExtra("SELECTED_CLIENT_NAME", client.clientName)
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            }
        )
        binding.rvClients.layoutManager = LinearLayoutManager(this)
        binding.rvClients.adapter = adapter
    }

    private fun setupListeners() {
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                filterClients(newText)
                return true
            }
        })
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.clients.collectLatest { clients ->
                adapter.submitList(clients)
                binding.emptyState.visibility = if (clients.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun filterClients(query: String?) {
        val currentList = viewModel.clients.value
        if (query.isNullOrEmpty()) {
            adapter.submitList(currentList)
        } else {
            val filtered = currentList.filter {
                it.clientName?.contains(query, ignoreCase = true) == true ||
                it.mobile?.contains(query, ignoreCase = true) == true ||
                it.email?.contains(query, ignoreCase = true) == true
            }
            adapter.submitList(filtered)
        }
    }
}