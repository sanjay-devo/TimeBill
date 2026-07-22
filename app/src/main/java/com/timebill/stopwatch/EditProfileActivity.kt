package com.timebill.stopwatch

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.timebill.stopwatch.databinding.ActivityEditProfileBinding
import com.timebill.stopwatch.model.UserProfile
import com.timebill.stopwatch.repository.FirebaseRepository
import com.timebill.stopwatch.utils.PreferenceManager
import com.timebill.stopwatch.viewmodel.EditProfileViewModel
import com.timebill.stopwatch.viewmodel.ViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    private lateinit var viewModel: EditProfileViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Ensure the content doesn't draw behind the navigation bar/keyboard improperly
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true

        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupWindowInsets()
        setupViewModel()
        observeViewModel()
    }

    private fun setupUI() {
        binding.editProfileHeader.tvTitle.text = getString(R.string.title_edit_profile)
        binding.editProfileHeader.btnBack.setOnClickListener { finish() }

        setupCollapsibleSections()

        binding.btnSaveProfile.setOnClickListener {
            saveProfile()
        }
    }

    private fun setupCollapsibleSections() {
        binding.headerPersonal.setOnClickListener {
            toggleSection(binding.contentPersonal, binding.ivExpandPersonal)
        }
        binding.headerBusiness.setOnClickListener {
            toggleSection(binding.contentBusiness, binding.ivExpandBusiness)
        }
        binding.headerBilling.setOnClickListener {
            toggleSection(binding.contentBilling, binding.ivExpandBilling)
        }
        binding.headerTax.setOnClickListener {
            toggleSection(binding.contentTax, binding.ivExpandTax)
        }
    }

    private fun toggleSection(content: View, icon: android.widget.ImageView) {
        if (content.visibility == View.VISIBLE) {
            content.visibility = View.GONE
            icon.setImageResource(R.drawable.ic_expand_more)
        } else {
            content.visibility = View.VISIBLE
            icon.setImageResource(R.drawable.ic_expand_less)
        }
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.editProfileRoot) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            val isImeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            
            binding.editProfileHeader.headerRoot.updatePadding(top = systemBars.top)
            
            view.updatePadding(
                left = systemBars.left,
                right = systemBars.right,
                bottom = if (isImeVisible) ime.bottom else systemBars.bottom
            )

            insets
        }
    }

    private fun setupViewModel() {
        val preferenceManager = PreferenceManager(this)
        val repository = FirebaseRepository(preferenceManager.getGuestId())
        val factory = ViewModelFactory(repository, preferenceManager)
        viewModel = ViewModelProvider(this, factory)[EditProfileViewModel::class.java]
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.isLoading.collectLatest { isLoading ->
                binding.loadingOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }

        lifecycleScope.launch {
            viewModel.profile.collectLatest { profile ->
                profile?.let { populateFields(it) }
            }
        }

        lifecycleScope.launch {
            viewModel.event.collectLatest { event ->
                when (event) {
                    is EditProfileViewModel.EditProfileEvent.SaveSuccess -> {
                        Toast.makeText(this@EditProfileActivity, R.string.msg_profile_updated, Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    is EditProfileViewModel.EditProfileEvent.Error -> {
                        Toast.makeText(this@EditProfileActivity, event.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun populateFields(profile: UserProfile) {
        binding.etFullName.setText(profile.fullName)
        binding.etMobile.setText(profile.mobile)
        binding.etEmail.setText(profile.email)
        binding.etBusinessName.setText(profile.businessName)
        binding.etAddress1.setText(profile.addressLine1)
        binding.etAddress2.setText(profile.addressLine2)
        binding.etCity.setText(profile.city)
        binding.etState.setText(profile.state)
        binding.etPinCode.setText(profile.pinCode)
        binding.etGstNumber.setText(profile.gstNumber)
    }

    private fun saveProfile() {
        val profile = UserProfile(
            fullName = binding.etFullName.text.toString().trim(),
            mobile = binding.etMobile.text.toString().trim(),
            email = binding.etEmail.text.toString().trim(),
            businessName = binding.etBusinessName.text.toString().trim(),
            addressLine1 = binding.etAddress1.text.toString().trim(),
            addressLine2 = binding.etAddress2.text.toString().trim(),
            city = binding.etCity.text.toString().trim(),
            state = binding.etState.text.toString().trim(),
            pinCode = binding.etPinCode.text.toString().trim(),
            gstNumber = binding.etGstNumber.text.toString().trim(),
            updatedAt = System.currentTimeMillis()
        )
        viewModel.saveProfile(profile)
    }
}