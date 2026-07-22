package com.timebill.stopwatch

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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.timebill.stopwatch.adapter.ClientsAdapter
import com.timebill.stopwatch.databinding.ActivityClientsBinding
import com.timebill.stopwatch.repository.FirebaseRepository
import com.timebill.stopwatch.utils.PreferenceManager
import com.timebill.stopwatch.viewmodel.ClientsViewModel
import com.timebill.stopwatch.viewmodel.ViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ClientsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityClientsBinding
    private lateinit var viewModel: ClientsViewModel
    private lateinit var adapter: ClientsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityClientsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()
        setupViewModel()
        setupRecyclerView()
        setupListeners()
        observeViewModel()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.clientsRoot) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.appBarLayout.updatePadding(top = systemBars.top)
            binding.clientsRoot.updatePadding(
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
            onEdit = { client ->
                val intent = Intent(this, AddEditClientActivity::class.java)
                intent.putExtra("CLIENT_ID", client.clientId)
                intent.putExtra("CLIENT_NAME", client.clientName)
                intent.putExtra("CLIENT_MOBILE", client.mobile)
                intent.putExtra("CLIENT_EMAIL", client.email)
                intent.putExtra("CLIENT_ADDRESS", client.address)
                startActivity(intent)
            },
            onDelete = { client ->
                showDeleteConfirmationDialog(client.clientId!!)
            }
        )
        binding.rvClients.layoutManager = LinearLayoutManager(this)
        binding.rvClients.adapter = adapter
    }

    private fun setupListeners() {
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.fabAddClient.setOnClickListener {
            startActivity(Intent(this, AddEditClientActivity::class.java))
        }

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

    private fun showDeleteConfirmationDialog(clientId: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_delete_client_title)
            .setMessage(R.string.dialog_delete_client_msg)
            .setNegativeButton(R.string.label_cancel, null)
            .setPositiveButton(R.string.label_delete) { _, _ ->
                viewModel.deleteClient(clientId)
            }
            .show()
    }
}