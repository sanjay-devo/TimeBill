package com.timebill.stopwatch

import android.content.Intent
import android.os.Bundle
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
import com.timebill.stopwatch.databinding.ActivityProfileBinding
import com.timebill.stopwatch.repository.FirebaseRepository
import com.timebill.stopwatch.utils.PreferenceManager
import com.timebill.stopwatch.viewmodel.ProfileViewModel
import com.timebill.stopwatch.viewmodel.ViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var viewModel: ProfileViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()
        setupViewModel()
        setupListeners()
        observeViewModel()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.profileRoot) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.profileHeader.headerRoot.updatePadding(top = systemBars.top)
            binding.profileRoot.updatePadding(
                left = systemBars.left,
                right = systemBars.right,
                bottom = systemBars.bottom
            )
            insets
        }
    }

    private fun setupViewModel() {
        val preferenceManager = PreferenceManager(this)
        val guestId = preferenceManager.getGuestId()
        val repository = FirebaseRepository(guestId)
        val factory = ViewModelFactory(repository, preferenceManager)
        viewModel = ViewModelProvider(this, factory)[ProfileViewModel::class.java]
    }

    private fun setupListeners() {
        binding.profileHeader.btnBack.setOnClickListener {
            finish()
        }

        binding.btnEditProfile.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        binding.btnDeleteAllData.setOnClickListener {
            showDeleteAllDataDialog()
        }

        binding.btnDeleteGuestAccount.setOnClickListener {
            showDeleteGuestAccountDialog()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.guestId.collectLatest { guestId ->
                binding.tvGuestId.text = getString(R.string.label_guest_id, guestId)
            }
        }

        lifecycleScope.launch {
            viewModel.isLoading.collectLatest { isLoading ->
                binding.loadingOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }

        lifecycleScope.launch {
            viewModel.event.collectLatest { event ->
                when (event) {
                    is ProfileViewModel.ProfileEvent.DataDeleted -> {
                        showSuccessDialog(getString(R.string.msg_success_data_deleted))
                    }
                    is ProfileViewModel.ProfileEvent.AccountDeleted -> {
                        showSuccessDialog(getString(R.string.msg_success_account_deleted))
                    }
                    is ProfileViewModel.ProfileEvent.Error -> {
                        Toast.makeText(this@ProfileActivity, event.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun showDeleteAllDataDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_delete_all_title)
            .setMessage(R.string.dialog_delete_all_msg)
            .setNegativeButton(R.string.label_cancel, null)
            .setPositiveButton(R.string.dialog_delete_action) { _, _ ->
                viewModel.deleteAllData()
            }
            .show()
    }

    private fun showDeleteGuestAccountDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_delete_account_title)
            .setMessage(R.string.dialog_delete_account_msg)
            .setNegativeButton(R.string.label_cancel, null)
            .setPositiveButton(R.string.dialog_delete_account_action) { _, _ ->
                viewModel.deleteGuestAccount()
            }
            .show()
    }

    private fun showSuccessDialog(message: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.label_success)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }
}