package com.timebill.stopwatch

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.timebill.stopwatch.utils.AppUtils
import com.timebill.stopwatch.databinding.ActivityAboutBinding

class AboutActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()
        setupContent()
        setupListeners()
        
        binding.navigationView.setCheckedItem(R.id.nav_about)
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.drawerLayout) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.aboutHeader.headerRoot.updatePadding(top = systemBars.top)
            
            val headerView = binding.navigationView.getHeaderView(0)
            headerView?.updatePadding(top = systemBars.top)
            
            binding.aboutRoot.updatePadding(left = systemBars.left, right = systemBars.right, bottom = systemBars.bottom)
            insets
        }
    }

    private fun setupContent() {
        // Premium Features
        binding.featureStopwatch.featureIcon.setImageResource(R.drawable.ic_play_rounded)
        binding.featureStopwatch.featureTitle.text = getString(R.string.label_feature_stopwatch)
        binding.featureStopwatch.featureDesc.text = getString(R.string.desc_feature_stopwatch)

        binding.featureTracking.featureIcon.setImageResource(R.drawable.ic_history_rounded)
        binding.featureTracking.featureTitle.text = getString(R.string.label_feature_tracking)
        binding.featureTracking.featureDesc.text = getString(R.string.desc_feature_tracking)

        binding.featureBilling.featureIcon.setImageResource(R.drawable.ic_payment_rounded)
        binding.featureBilling.featureTitle.text = getString(R.string.label_feature_billing)
        binding.featureBilling.featureDesc.text = getString(R.string.desc_feature_billing)

        binding.featureCalc.featureIcon.setImageResource(R.drawable.ic_trending_up_rounded)
        binding.featureCalc.featureTitle.text = getString(R.string.label_feature_calc)
        binding.featureCalc.featureDesc.text = getString(R.string.desc_feature_calc)

        binding.featureReports.featureIcon.setImageResource(R.drawable.ic_reports_rounded)
        binding.featureReports.featureTitle.text = getString(R.string.label_feature_reports)
        binding.featureReports.featureDesc.text = getString(R.string.desc_feature_reports)

        binding.featureHistory.featureIcon.setImageResource(R.drawable.ic_history_rounded)
        binding.featureHistory.featureTitle.text = getString(R.string.label_feature_history)
        binding.featureHistory.featureDesc.text = getString(R.string.desc_feature_history)

        binding.featureSync.featureIcon.setImageResource(R.drawable.ic_refresh_rounded)
        binding.featureSync.featureTitle.text = getString(R.string.label_feature_sync)
        binding.featureSync.featureDesc.text = getString(R.string.desc_feature_sync)

        binding.featureGuest.featureIcon.setImageResource(R.drawable.ic_person_rounded)
        binding.featureGuest.featureTitle.text = getString(R.string.label_feature_guest)
        binding.featureGuest.featureDesc.text = getString(R.string.desc_feature_guest)

        // App Information
        binding.infoVersion.infoLabel.text = getString(R.string.label_info_version)
        binding.infoVersion.infoValue.text = "1.0" // Fallback since BuildConfig might not be ready

        binding.infoPackage.infoLabel.text = getString(R.string.label_info_package)
        binding.infoPackage.infoValue.text = packageName

        binding.infoDeveloper.infoLabel.text = getString(R.string.label_info_developer)
        binding.infoDeveloper.infoValue.text = getString(R.string.val_info_developer)

        binding.infoWebsite.infoLabel.text = getString(R.string.label_info_website)
        binding.infoWebsite.infoValue.text = getString(R.string.val_info_website)

        binding.infoSupport.infoLabel.text = getString(R.string.label_info_support)
        binding.infoSupport.infoValue.text = getString(R.string.val_info_support)

        binding.infoPhone.infoLabel.text = getString(R.string.label_info_phone)
        binding.infoPhone.infoValue.text = getString(R.string.val_info_phone)
    }

    private fun setupListeners() {
        binding.aboutHeader.btnBack.setOnClickListener {
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
                    // Already here
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
}