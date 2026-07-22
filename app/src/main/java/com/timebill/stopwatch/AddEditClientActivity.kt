package com.timebill.stopwatch

import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.timebill.stopwatch.databinding.ActivityAddEditClientBinding
import com.timebill.stopwatch.model.Client
import com.timebill.stopwatch.repository.FirebaseRepository
import com.timebill.stopwatch.utils.PreferenceManager
import com.timebill.stopwatch.viewmodel.ClientsViewModel
import com.timebill.stopwatch.viewmodel.ViewModelFactory
import kotlinx.coroutines.launch

class AddEditClientActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditClientBinding
    private lateinit var viewModel: ClientsViewModel
    private var clientId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAddEditClientBinding.inflate(layoutInflater)
        setContentView(binding.root)

        clientId = intent.getStringExtra("CLIENT_ID")
        setupWindowInsets()
        setupViewModel()
        setupUI()
        setupListeners()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.addEditClientRoot) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.appBarLayout.updatePadding(top = systemBars.top)
            binding.addEditClientRoot.updatePadding(
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

    private fun setupUI() {
        if (clientId != null) {
            binding.toolbar.title = getString(R.string.title_edit_client)
            binding.btnSaveClient.text = getString(R.string.action_save_changes)
            binding.btnDeleteClient.visibility = View.VISIBLE

            binding.etClientName.setText(intent.getStringExtra("CLIENT_NAME"))
            binding.etMobile.setText(intent.getStringExtra("CLIENT_MOBILE"))
            binding.etEmail.setText(intent.getStringExtra("CLIENT_EMAIL"))
            binding.etAddress.setText(intent.getStringExtra("CLIENT_ADDRESS"))
        }
    }

    private fun setupListeners() {
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.btnSaveClient.setOnClickListener {
            saveClient()
        }

        binding.btnDeleteClient.setOnClickListener {
            showDeleteConfirmationDialog()
        }
    }

    private fun saveClient() {
        val name = binding.etClientName.text.toString().trim()
        val mobile = binding.etMobile.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val address = binding.etAddress.text.toString().trim()

        if (email.isNotEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = getString(R.string.msg_invalid_email)
            return
        } else {
            binding.tilEmail.error = null
        }

        val client = Client(
            clientId = clientId,
            clientName = name,
            mobile = mobile,
            email = email,
            address = address
        )

        lifecycleScope.launch {
            val repository = FirebaseRepository(PreferenceManager(this@AddEditClientActivity).getGuestId())
            repository.saveClient(client)
            Toast.makeText(this@AddEditClientActivity, R.string.msg_client_saved, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun showDeleteConfirmationDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_delete_client_title)
            .setMessage(R.string.dialog_delete_client_msg)
            .setNegativeButton(R.string.label_cancel, null)
            .setPositiveButton(R.string.label_delete) { _, _ ->
                clientId?.let {
                    lifecycleScope.launch {
                        viewModel.deleteClient(it)
                        Toast.makeText(this@AddEditClientActivity, R.string.msg_client_deleted, Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            }
            .show()
    }
}