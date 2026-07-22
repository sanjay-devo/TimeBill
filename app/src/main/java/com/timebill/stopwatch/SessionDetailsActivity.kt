package com.timebill.stopwatch

import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.timebill.stopwatch.databinding.ActivitySessionDetailsBinding
import com.timebill.stopwatch.model.Session
import com.timebill.stopwatch.model.UserProfile
import com.timebill.stopwatch.repository.FirebaseRepository
import com.timebill.stopwatch.utils.PreferenceManager
import com.timebill.stopwatch.viewmodel.SessionDetailsViewModel
import com.timebill.stopwatch.viewmodel.ViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class SessionDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySessionDetailsBinding
    private lateinit var viewModel: SessionDetailsViewModel
    private var currentSession: Session? = null
    private var userProfile: UserProfile? = null
    private var sessionId: String? = null

    private var isSessionSummaryExpanded = true
    private var isBillingInfoExpanded = false
    private var isReceiptInfoExpanded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySessionDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionId = intent.getStringExtra("SESSION_ID")
        if (sessionId == null) {
            finish()
            return
        }

        if (savedInstanceState != null) {
            isSessionSummaryExpanded = savedInstanceState.getBoolean("isSessionSummaryExpanded", true)
            isBillingInfoExpanded = savedInstanceState.getBoolean("isBillingInfoExpanded", false)
            isReceiptInfoExpanded = savedInstanceState.getBoolean("isReceiptInfoExpanded", false)
        }

        setupWindowInsets()
        setupViewModel()
        setupListeners()
        setupStatusDropdown()
        updateCollapseStates()
        observeData()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("isSessionSummaryExpanded", isSessionSummaryExpanded)
        outState.putBoolean("isBillingInfoExpanded", isBillingInfoExpanded)
        outState.putBoolean("isReceiptInfoExpanded", isReceiptInfoExpanded)
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }
    }

    private fun setupViewModel() {
        val guestId = PreferenceManager(this).getGuestId()
        val repository = FirebaseRepository(guestId)
        val factory = ViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[SessionDetailsViewModel::class.java]
    }

    private fun setupListeners() {
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.headerSessionSummary.setOnClickListener {
            isSessionSummaryExpanded = !isSessionSummaryExpanded
            updateCollapseStates()
        }

        binding.headerBillingInfo.setOnClickListener {
            isBillingInfoExpanded = !isBillingInfoExpanded
            updateCollapseStates()
        }

        binding.headerReceiptInfo.setOnClickListener {
            isReceiptInfoExpanded = !isReceiptInfoExpanded
            updateCollapseStates()
        }

        binding.btnSaveProfile.setOnClickListener {
            saveProfileData()
        }

        binding.bottomAppBar.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.action_view_receipt -> {
                    generateAndHandlePdf(Action.VIEW)
                    true
                }
                R.id.action_download_pdf -> {
                    generateAndHandlePdf(Action.DOWNLOAD)
                    true
                }
                R.id.action_share_receipt -> {
                    generateAndHandlePdf(Action.SHARE)
                    true
                }
                R.id.action_delete_session -> {
                    showDeleteConfirmation()
                    true
                }
                else -> false
            }
        }
    }

    private fun updateCollapseStates() {
        toggleSection(binding.contentSessionSummary, binding.ivExpandSessionSummary, isSessionSummaryExpanded)
        toggleSection(binding.contentBillingInfo, binding.ivExpandBillingInfo, isBillingInfoExpanded)
        toggleSection(binding.contentReceiptInfo, binding.ivExpandReceiptInfo, isReceiptInfoExpanded)
    }

    private fun toggleSection(content: View, icon: android.widget.ImageView, expand: Boolean) {
        content.visibility = if (expand) View.VISIBLE else View.GONE
        icon.setImageResource(if (expand) R.drawable.ic_expand_less else R.drawable.ic_expand_more)
    }

    private fun setupStatusDropdown() {
        val statuses = arrayOf(
            getString(R.string.status_work_completed),
            getString(R.string.status_work_in_progress),
            getString(R.string.status_payment_pending),
            getString(R.string.status_payment_received),
            getString(R.string.status_cancelled),
            getString(R.string.status_on_hold)
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, statuses)
        binding.atvStatus.setAdapter(adapter)
        binding.atvStatus.setOnItemClickListener { _, _, position, _ ->
            val newStatus = statuses[position]
            sessionId?.let { id ->
                val receiptNum = if (currentSession?.receiptNumber.isNullOrEmpty()) generateReceiptNumber() else currentSession?.receiptNumber!!
                viewModel.updateSessionStatus(id, newStatus, receiptNum)
            }
        }
    }

    private fun observeData() {
        sessionId?.let { id ->
            lifecycleScope.launch {
                viewModel.getSession(id).collectLatest { session ->
                    session?.let {
                        currentSession = it
                        populateSessionData(it)
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewModel.getProfile().collectLatest { profile ->
                userProfile = profile
                populateProfileData(profile)
            }
        }
    }

    private fun populateSessionData(session: Session) {
        binding.etClientName.setText(session.clientName)
        binding.etSessionId.setText(session.id)
        binding.etReceiptNumberSummary.setText(session.receiptNumber)
        
        val sdfDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val sdfTime = SimpleDateFormat("hh:mm a", Locale.getDefault())
        
        binding.etSessionDate.setText(sdfDate.format(Date(session.startTime ?: 0L)))
        binding.etStartTime.setText(sdfTime.format(Date(session.startTime ?: 0L)))
        binding.etEndTime.setText(if (session.endTime != 0L) sdfTime.format(Date(session.endTime ?: 0L)) else "-")
        binding.etDuration.setText(formatDuration(session.durationMillis ?: 0L))
        binding.etHourlyRateSummary.setText(String.format(Locale.getDefault(), "₹%.2f/hr", session.hourlyRate ?: 0.0))
        binding.etTotalEarnings.setText(String.format(Locale.getDefault(), "₹%.2f", session.earnings ?: 0.0))
        
        binding.atvStatus.setText(session.status, false)

        binding.etReceiptNumber.setText(session.receiptNumber)
        binding.etReceiptDate.setText(sdfDate.format(Date(session.createdAt ?: session.timestamp ?: 0L)))
        binding.etGeneratedTime.setText(sdfTime.format(Date(session.createdAt ?: session.timestamp ?: 0L)))
    }

    private fun populateProfileData(profile: UserProfile?) {
        profile?.let {
            binding.etFullName.setText(it.fullName)
            binding.etMobile.setText(it.mobile)
            binding.etEmail.setText(it.email)
            binding.etAddress1.setText(it.addressLine1)
            binding.etAddress2.setText(it.addressLine2)
            binding.etCity.setText(it.city)
            binding.etState.setText(it.state)
            binding.etPinCode.setText(it.pinCode)
            binding.etGstNumber.setText(it.gstNumber)
        }
    }

    private fun saveProfileData() {
        val profile = UserProfile(
            fullName = binding.etFullName.text.toString(),
            mobile = binding.etMobile.text.toString(),
            email = binding.etEmail.text.toString(),
            addressLine1 = binding.etAddress1.text.toString(),
            addressLine2 = binding.etAddress2.text.toString(),
            city = binding.etCity.text.toString(),
            state = binding.etState.text.toString(),
            pinCode = binding.etPinCode.text.toString(),
            gstNumber = binding.etGstNumber.text.toString(),
            updatedAt = System.currentTimeMillis()
        )
        viewModel.saveProfile(profile)
        Toast.makeText(this, R.string.msg_profile_updated, Toast.LENGTH_SHORT).show()
    }

    private fun generateReceiptNumber(): String {
        val date = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val random = (100000..999999).random()
        return "TB-$date-$random"
    }

    private fun formatDuration(millis: Long): String {
        val seconds = (millis / 1000) % 60
        val minutes = (millis / (1000 * 60)) % 60
        val hours = (millis / (1000 * 60 * 60))
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun showDeleteConfirmation() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_delete_session_title)
            .setMessage(R.string.msg_delete_session_confirm)
            .setPositiveButton(R.string.dialog_delete_action) { _, _ ->
                sessionId?.let {
                    viewModel.deleteSession(it)
                    finish()
                }
            }
            .setNegativeButton(R.string.label_cancel, null)
            .show()
    }

    private enum class Action { VIEW, DOWNLOAD, SHARE }

    private fun generateAndHandlePdf(action: Action) {
        val session = currentSession ?: return
        val profile = userProfile ?: UserProfile()

        if (session.receiptNumber.isNullOrEmpty()) {
            val newReceiptNum = generateReceiptNumber()
            sessionId?.let { viewModel.updateSessionStatus(it, session.status ?: "Work Completed", newReceiptNum) }
            // Wait for update or just use the new number for now
            session.copy(receiptNumber = newReceiptNum).let { updatedSession ->
                createPdf(updatedSession, profile, action)
            }
        } else {
            createPdf(session, profile, action)
        }
    }

    private fun createPdf(session: Session, profile: UserProfile, action: Action) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        
        val cream = Color.parseColor("#F8F4EF")
        val gold = Color.parseColor("#D4AE7A")
        val darkBrown = Color.parseColor("#2E1A17")
        val labelColor = Color.parseColor("#7A6A63")

        canvas.drawColor(cream)

        val paint = Paint()
        val titlePaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 24f
            color = darkBrown
        }
        val normalPaint = Paint().apply {
            textSize = 12f
            color = Color.BLACK
        }
        val labelPaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 12f
            color = labelColor
        }

        var yPos = 60f
        
        // Logo & Brand
        canvas.drawText("TimeBill", 50f, yPos, titlePaint)
        yPos += 25f
        normalPaint.textSize = 14f
        canvas.drawText("Stopwatch & Billing", 50f, yPos, normalPaint)
        
        // Receipt Header
        titlePaint.textSize = 28f
        titlePaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("Receipt", 545f, 70f, titlePaint)
        titlePaint.textAlign = Paint.Align.LEFT
        
        yPos += 40f
        paint.color = gold
        canvas.drawRect(50f, yPos, 545f, yPos + 3f, paint)
        yPos += 30f

        // Receipt Details
        canvas.drawText("Receipt No:", 50f, yPos, labelPaint)
        canvas.drawText(session.receiptNumber ?: "-", 150f, yPos, normalPaint)
        
        yPos += 20f
        canvas.drawText("Receipt Date:", 50f, yPos, labelPaint)
        canvas.drawText(SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(session.createdAt ?: session.timestamp ?: 0L)), 150f, yPos, normalPaint)
        
        yPos += 40f
        canvas.drawRect(50f, yPos, 545f, yPos + 1f, paint)
        yPos += 30f

        // From Section
        canvas.drawText("FROM", 50f, yPos, labelPaint)
        yPos += 25f
        titlePaint.textSize = 16f
        canvas.drawText(profile.fullName ?: "Your Name", 50f, yPos, titlePaint)
        yPos += 20f
        normalPaint.textSize = 12f
        canvas.drawText(profile.mobile ?: "", 50f, yPos, normalPaint)
        yPos += 15f
        canvas.drawText(profile.email ?: "", 50f, yPos, normalPaint)
        yPos += 15f
        canvas.drawText("${profile.addressLine1}${if (profile.addressLine2?.isNotEmpty() == true) ", " + profile.addressLine2 else ""}", 50f, yPos, normalPaint)
        yPos += 15f
        canvas.drawText("${profile.city}, ${profile.state} - ${profile.pinCode}", 50f, yPos, normalPaint)
        if (!profile.gstNumber.isNullOrEmpty()) {
            yPos += 15f
            canvas.drawText("GST: ${profile.gstNumber}", 50f, yPos, normalPaint)
        }

        yPos += 40f
        // Bill To Section
        canvas.drawText("BILL TO", 50f, yPos, labelPaint)
        yPos += 25f
        titlePaint.textSize = 16f
        canvas.drawText(session.clientName ?: "Unnamed Client", 50f, yPos, titlePaint)

        yPos += 40f
        canvas.drawRect(50f, yPos, 545f, yPos + 1f, paint)
        yPos += 30f

        // Session Details Section
        canvas.drawText("SESSION DETAILS", 50f, yPos, labelPaint)
        yPos += 30f
        
        val detailsX1 = 50f
        val detailsX2 = 200f
        
        canvas.drawText("Start Time:", detailsX1, yPos, labelPaint)
        canvas.drawText(SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(session.startTime ?: 0L)), detailsX2, yPos, normalPaint)
        yPos += 20f
        canvas.drawText("End Time:", detailsX1, yPos, labelPaint)
        canvas.drawText(if (session.endTime != 0L) SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(session.endTime ?: 0L)) else "-", detailsX2, yPos, normalPaint)
        yPos += 20f
        canvas.drawText("Duration:", detailsX1, yPos, labelPaint)
        canvas.drawText(formatDuration(session.durationMillis ?: 0L), detailsX2, yPos, normalPaint)
        yPos += 20f
        canvas.drawText("Hourly Rate:", detailsX1, yPos, labelPaint)
        canvas.drawText(String.format(Locale.getDefault(), "₹%.2f/hr", session.hourlyRate ?: 0.0), detailsX2, yPos, normalPaint)
        yPos += 20f
        canvas.drawText("Status:", detailsX1, yPos, labelPaint)
        canvas.drawText(session.status ?: "", detailsX2, yPos, normalPaint)

        yPos += 40f
        canvas.drawRect(50f, yPos, 545f, yPos + 1f, paint)
        yPos += 30f

        // Payment Summary
        canvas.drawText("PAYMENT SUMMARY", 50f, yPos, labelPaint)
        yPos += 30f
        titlePaint.textSize = 20f
        canvas.drawText("Total Earnings:", 50f, yPos, titlePaint)
        titlePaint.textAlign = Paint.Align.RIGHT
        canvas.drawText(String.format(Locale.getDefault(), "₹%.2f", session.earnings ?: 0.0), 545f, yPos, titlePaint)
        titlePaint.textAlign = Paint.Align.LEFT

        // Footer
        yPos = 760f
        canvas.drawRect(50f, yPos, 545f, yPos + 0.5f, paint)
        yPos += 25f
        normalPaint.textAlign = Paint.Align.CENTER
        normalPaint.textSize = 10f
        canvas.drawText("Generated by TimeBill – Stopwatch & Billing", 297f, yPos, normalPaint)
        yPos += 15f
        canvas.drawText("https://timebill.indiacybercafe.com | support@timebill.indiacybercafe.com", 297f, yPos, normalPaint)
        yPos += 15f
        labelPaint.textAlign = Paint.Align.CENTER
        labelPaint.textSize = 10f
        canvas.drawText("Automatically Generated Receipt", 297f, yPos, labelPaint)

        pdfDocument.finishPage(page)

        val fileName = "Receipt_${session.receiptNumber}.pdf"
        val file = if (action == Action.DOWNLOAD) {
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
        } else {
            File(cacheDir, fileName)
        }

        try {
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            
            when (action) {
                Action.VIEW -> openInternalPdfViewer(file)
                Action.DOWNLOAD -> Toast.makeText(this, getString(R.string.msg_pdf_saved), Toast.LENGTH_LONG).show()
                Action.SHARE -> sharePdfFile(file)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openInternalPdfViewer(file: File) {
        val intent = Intent(this, PdfViewerActivity::class.java)
        intent.putExtra("FILE_PATH", file.absolutePath)
        startActivity(intent)
    }

    private fun sharePdfFile(file: File) {
        val uri = FileProvider.getUriForFile(this, "com.timebill.stopwatch.provider", file)
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "application/pdf"
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(Intent.createChooser(intent, "Share Receipt"))
    }
}