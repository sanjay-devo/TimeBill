package com.timebill.stopwatch

import android.content.ContentValues
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
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
import com.timebill.stopwatch.model.Client
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
    private var allClients: List<Client> = emptyList()

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

        binding.cbMoreDetails.setOnCheckedChangeListener { _, isChecked ->
            binding.layoutMoreDetails.visibility = if (isChecked) View.VISIBLE else View.GONE
            if (isChecked) {
                autoFillClientDetails(binding.etClientName.text.toString())
            }
        }

        binding.etClientName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                autoFillClientDetails(s.toString())
            }
        })

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

        lifecycleScope.launch {
            viewModel.getClients().collectLatest { clients ->
                allClients = clients
            }
        }
    }

    private fun autoFillClientDetails(name: String) {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) return
        val client = allClients.find { it.clientName?.trim()?.equals(trimmedName, ignoreCase = true) == true }
        client?.let {
            if (binding.etClientMobileSD.text.isNullOrEmpty()) binding.etClientMobileSD.setText(it.mobile)
            if (binding.etClientEmailSD.text.isNullOrEmpty()) binding.etClientEmailSD.setText(it.email)
            if (binding.etClientAddressSD.text.isNullOrEmpty()) binding.etClientAddressSD.setText(it.address)
        }
    }

    private fun populateSessionData(session: Session) {
        binding.etClientName.setText(session.clientName)
        binding.etClientMobileSD.setText(session.clientMobile)
        binding.etClientEmailSD.setText(session.clientEmail)
        binding.etClientAddressSD.setText(session.clientAddress)
        binding.etWorkName.setText(session.workName)

        binding.cbMoreDetails.isChecked = session.hasClientDetails ?: false
        binding.layoutMoreDetails.visibility = if (binding.cbMoreDetails.isChecked) View.VISIBLE else View.GONE

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

        // Capture current values from UI
        val name = binding.etClientName.text.toString().trim()
        val hasDetails = binding.cbMoreDetails.isChecked
        
        val mobile = if (hasDetails) binding.etClientMobileSD.text.toString().trim() else ""
        val email = if (hasDetails) binding.etClientEmailSD.text.toString().trim() else ""
        val address = if (hasDetails) binding.etClientAddressSD.text.toString().trim() else ""
        val workName = binding.etWorkName.text.toString().trim()

        val updates = mutableMapOf<String, Any?>()
        updates["clientName"] = name
        updates["clientMobile"] = mobile
        updates["clientEmail"] = email
        updates["clientAddress"] = address
        updates["workName"] = workName
        updates["hasClientDetails"] = hasDetails

        sessionId?.let { id ->
            viewModel.updateSessionDetails(id, updates)
            // Update local snapshot for PDF generation
            currentSession = session.copy(
                clientName = name,
                clientMobile = mobile,
                clientEmail = email,
                clientAddress = address,
                workName = workName,
                hasClientDetails = hasDetails
            )
            
            // Auto create/update client
            if (hasDetails) {
                autoCreateOrUpdateClient(name, mobile, email, address)
            }
        }

        val updatedSession = currentSession!!
        val receiptNum = if (updatedSession.receiptNumber.isNullOrEmpty()) {
            val newNum = generateReceiptNumber()
            sessionId?.let { viewModel.updateSessionStatus(it, updatedSession.status ?: "Work Completed", newNum) }
            newNum
        } else {
            updatedSession.receiptNumber!!
        }

        val sessionForPdf = updatedSession.copy(receiptNumber = receiptNum)
        val pdfDocument = createPdfDocument(sessionForPdf, profile)
        handlePdfAction(pdfDocument, receiptNum, action)
    }

    private fun autoCreateOrUpdateClient(name: String, mobile: String, email: String, address: String) {
        if (name.isEmpty()) return
        val existingClient = allClients.find { it.clientName?.equals(name, ignoreCase = true) == true }
        if (existingClient == null) {
            val newClient = Client(
                clientName = name,
                mobile = mobile,
                email = email,
                address = address
            )
            lifecycleScope.launch {
                FirebaseRepository(PreferenceManager(this@SessionDetailsActivity).getGuestId()).saveClient(newClient)
            }
        } else {
            // Update only empty fields
            val updatedMobile = if (existingClient.mobile.isNullOrEmpty()) mobile else existingClient.mobile
            val updatedEmail = if (existingClient.email.isNullOrEmpty()) email else existingClient.email
            val updatedAddress = if (existingClient.address.isNullOrEmpty()) address else existingClient.address

            if (updatedMobile != existingClient.mobile || updatedEmail != existingClient.email || updatedAddress != existingClient.address) {
                val updatedClient = existingClient.copy(
                    mobile = updatedMobile,
                    email = updatedEmail,
                    address = updatedAddress
                )
                lifecycleScope.launch {
                    FirebaseRepository(PreferenceManager(this@SessionDetailsActivity).getGuestId()).saveClient(updatedClient)
                }
            }
        }
    }

    private fun createPdfDocument(session: Session, profile: UserProfile): PdfDocument {
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
            textSize = 10f
            color = labelColor
            letterSpacing = 0.1f
        }

        var yPos = 60f

        // Header - Left (Brand)
        titlePaint.textSize = 24f
        canvas.drawText("TimeBill", 50f, yPos, titlePaint)
        yPos += 20f
        normalPaint.textSize = 12f
        normalPaint.color = labelColor
        canvas.drawText("Stopwatch & Billing", 50f, yPos, normalPaint)

        // Header - Right (Receipt Info)
        val rightX = 545f
        titlePaint.textAlign = Paint.Align.RIGHT
        titlePaint.textSize = 28f
        titlePaint.color = darkBrown
        canvas.drawText("RECEIPT", rightX, 60f, titlePaint)

        normalPaint.textAlign = Paint.Align.RIGHT
        normalPaint.color = Color.BLACK
        normalPaint.textSize = 12f
        canvas.drawText("Receipt No: ${session.receiptNumber ?: "-"}", rightX, 85f, normalPaint)
        canvas.drawText("Receipt Date: ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(session.createdAt ?: session.timestamp ?: 0L))}", rightX, 105f, normalPaint)
        
        titlePaint.textAlign = Paint.Align.LEFT
        normalPaint.textAlign = Paint.Align.LEFT
        normalPaint.color = Color.BLACK

        yPos = 130f
        paint.color = gold
        canvas.drawRect(50f, yPos, 545f, yPos + 2f, paint)
        yPos += 40f

        val hasDetails = session.hasClientDetails == true

        // FROM & BILL TO (Side by Side)
        val rightColX = 300f

        var leftY = yPos
        var rightY = yPos

        // FROM Section (Always visible)
        canvas.drawText("FROM", 50f, leftY, labelPaint)
        leftY += 20f
        titlePaint.textSize = 14f
        canvas.drawText(profile.fullName ?: "Your Name", 50f, leftY, titlePaint)
        leftY += 18f
        normalPaint.textSize = 11f
        canvas.drawText(profile.mobile ?: "", 50f, leftY, normalPaint)
        leftY += 15f
        canvas.drawText(profile.email ?: "", 50f, leftY, normalPaint)
        leftY += 15f
        canvas.drawText(profile.addressLine1 ?: "", 50f, leftY, normalPaint)
        if (!profile.addressLine2.isNullOrEmpty()) {
            leftY += 15f
            canvas.drawText(profile.addressLine2!!, 50f, leftY, normalPaint)
        }
        leftY += 15f
        canvas.drawText("${profile.city ?: ""}, ${profile.state ?: ""} - ${profile.pinCode ?: ""}", 50f, leftY, normalPaint)
        if (!profile.gstNumber.isNullOrEmpty()) {
            leftY += 15f
            canvas.drawText("GST: ${profile.gstNumber}", 50f, leftY, normalPaint)
        }

        // BILL TO Section
        canvas.drawText("BILL TO", rightColX, rightY, labelPaint)
        rightY += 20f
        titlePaint.textSize = 14f
        canvas.drawText(session.clientName ?: "Unnamed Client", rightColX, rightY, titlePaint)

        if (hasDetails) {
            // Add captured client details from session to PDF if enabled
            rightY += 18f
            normalPaint.textSize = 11f
            if (!session.clientMobile.isNullOrEmpty()) {
                canvas.drawText(session.clientMobile, rightColX, rightY, normalPaint)
                rightY += 15f
            }
            if (!session.clientEmail.isNullOrEmpty()) {
                canvas.drawText(session.clientEmail, rightColX, rightY, normalPaint)
                rightY += 15f
            }
            if (!session.clientAddress.isNullOrEmpty()) {
                canvas.drawText(session.clientAddress, rightColX, rightY, normalPaint)
                rightY += 15f
            }
        }

        // Status Badge Section
        rightY += 15f
        canvas.drawText("Status:", rightColX, rightY + 12.5f, labelPaint)
        val labelWidth = labelPaint.measureText("Status:")
        val badgeStartX = rightColX + labelWidth + 5f

        val status = session.status ?: "Draft"
        val (bgColor, textColor) = when (status) {
            "Work Completed" -> Color.parseColor("#E8F5E9") to Color.parseColor("#2E7D32")
            "Payment Pending" -> Color.parseColor("#FFF3E0") to Color.parseColor("#E65100")
            "Payment Received" -> Color.parseColor("#E3F2FD") to Color.parseColor("#0D47A1")
            "Work In Progress", "In Progress" -> Color.parseColor("#F3E5F5") to Color.parseColor("#4A148C")
            "On Hold" -> Color.parseColor("#FFFDE7") to Color.parseColor("#F57F17")
            "Cancelled" -> Color.parseColor("#FFEBEE") to Color.parseColor("#C62828")
            "Draft" -> Color.parseColor("#F5F5F5") to Color.parseColor("#616161")
            else -> Color.parseColor("#EFEBE9") to Color.parseColor("#4E342E")
        }

        val badgePaint = Paint().apply {
            color = bgColor
            style = Paint.Style.FILL
        }
        val statusTextPaint = Paint().apply {
            color = textColor
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val textWidth = statusTextPaint.measureText(status)
        val badgeWidth = textWidth + 20f
        val badgeHeight = 18f
        
        val rectF = android.graphics.RectF(badgeStartX, rightY, badgeStartX + badgeWidth, rightY + badgeHeight)
        canvas.drawRoundRect(rectF, 9f, 9f, badgePaint)
        canvas.drawText(status, badgeStartX + 10f, rightY + 12.5f, statusTextPaint)
        
        rightY += badgeHeight + 5f
        
        yPos = maxOf(leftY, rightY) + 50f

        // SESSION DETAILS TABLE
        // Header
        paint.color = darkBrown
        canvas.drawRect(50f, yPos, 545f, yPos + 30f, paint)
        
        normalPaint.color = Color.WHITE
        normalPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        normalPaint.textSize = 11f
        canvas.drawText("Description", 60f, yPos + 20f, normalPaint)
        canvas.drawText("Duration", 280f, yPos + 20f, normalPaint)
        canvas.drawText("Rate", 400f, yPos + 20f, normalPaint)
        canvas.drawText("Amount", 480f, yPos + 20f, normalPaint)
        
        yPos += 30f
        // Row
        paint.color = Color.WHITE
        canvas.drawRect(50f, yPos, 545f, yPos + 40f, paint)
        
        // Borders
        paint.color = Color.LTGRAY
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 0.5f
        canvas.drawRect(50f, yPos - 30f, 545f, yPos + 40f, paint)
        canvas.drawLine(270f, yPos - 30f, 270f, yPos + 40f, paint)
        canvas.drawLine(390f, yPos - 30f, 390f, yPos + 40f, paint)
        canvas.drawLine(470f, yPos - 30f, 470f, yPos + 40f, paint)
        paint.style = Paint.Style.FILL

        normalPaint.color = Color.BLACK
        normalPaint.typeface = Typeface.DEFAULT
        val description = if (session.workName.isNullOrEmpty()) "General Work" else session.workName
        canvas.drawText(description, 60f, yPos + 25f, normalPaint)
        canvas.drawText(formatDuration(session.durationMillis ?: 0L), 280f, yPos + 25f, normalPaint)
        canvas.drawText(String.format(Locale.getDefault(), "₹%.0f/hr", session.hourlyRate ?: 0.0), 400f, yPos + 25f, normalPaint)
        canvas.drawText(String.format(Locale.getDefault(), "₹%.2f", session.earnings ?: 0.0), 480f, yPos + 25f, normalPaint)

        yPos += 70f

        // PAYMENT SUMMARY
        val summaryX = 350f
        val summaryWidth = 195f
        
        canvas.drawText("Payment Summary", summaryX, yPos, labelPaint)
        yPos += 15f
        
        paint.color = Color.WHITE
        canvas.drawRect(summaryX, yPos, summaryX + summaryWidth, yPos + 80f, paint)
        paint.color = Color.LTGRAY
        paint.style = Paint.Style.STROKE
        canvas.drawRect(summaryX, yPos, summaryX + summaryWidth, yPos + 80f, paint)
        paint.style = Paint.Style.FILL
        
        yPos += 20f
        normalPaint.textSize = 12f
        canvas.drawText("Subtotal", summaryX + 10f, yPos, normalPaint)
        val subtotal = String.format(Locale.getDefault(), "₹%.2f", session.earnings ?: 0.0)
        canvas.drawText(subtotal, 545f - 10f - normalPaint.measureText(subtotal), yPos, normalPaint)
        
        yPos += 20f
        canvas.drawText("Tax", summaryX + 10f, yPos, normalPaint)
        canvas.drawText("₹0.00", 545f - 10f - normalPaint.measureText("₹0.00"), yPos, normalPaint)
        
        yPos += 25f
        paint.color = gold
        canvas.drawRect(summaryX + 5f, yPos - 18f, summaryX + summaryWidth - 5f, yPos + 7f, paint)
        
        titlePaint.textSize = 14f
        titlePaint.color = darkBrown
        canvas.drawText("Total", summaryX + 10f, yPos, titlePaint)
        val totalStr = String.format(Locale.getDefault(), "₹%.2f", session.earnings ?: 0.0)
        canvas.drawText(totalStr, 545f - 10f - titlePaint.measureText(totalStr), yPos, titlePaint)

        // Footer
        yPos = 760f
        paint.color = gold
        canvas.drawRect(50f, yPos, 545f, yPos + 0.5f, paint)
        yPos += 25f
        normalPaint.textAlign = Paint.Align.CENTER
        normalPaint.textSize = 10f
        normalPaint.color = labelColor
        canvas.drawText("Generated by TimeBill – Stopwatch & Billing", 297f, yPos, normalPaint)
        yPos += 15f
        canvas.drawText("https://timebill.indiacybercafe.com | support@timebill.indiacybercafe.com", 297f, yPos, normalPaint)
        yPos += 15f
        labelPaint.textAlign = Paint.Align.CENTER
        labelPaint.textSize = 10f
        canvas.drawText("Automatically Generated Receipt", 297f, yPos, labelPaint)

        pdfDocument.finishPage(page)
        return pdfDocument
    }

    private fun handlePdfAction(pdfDocument: PdfDocument, receiptNumber: String, action: Action) {
        val fileName = "Receipt_$receiptNumber.pdf"
        
        // Save to cache for VIEW and SHARE
        val tempFile = File(cacheDir, fileName)
        try {
            FileOutputStream(tempFile).use { pdfDocument.writeTo(it) }
            
            when (action) {
                Action.VIEW -> {
                    openInternalPdfViewer(tempFile)
                    pdfDocument.close()
                }
                Action.SHARE -> {
                    sharePdfFile(tempFile)
                    pdfDocument.close()
                }
                Action.DOWNLOAD -> {
                    savePdfToDownloads(pdfDocument, fileName)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Action failed: ${e.message}", Toast.LENGTH_SHORT).show()
            pdfDocument.close()
        }
    }

    private fun savePdfToDownloads(pdfDocument: PdfDocument, fileName: String) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/TimeBill")
                }
                val resolver = contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { outputStream ->
                        pdfDocument.writeTo(outputStream)
                    }
                    Toast.makeText(this, "Receipt saved successfully.", Toast.LENGTH_LONG).show()
                } else {
                    throw Exception("Failed to create MediaStore entry")
                }
            } else {
                val directory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "TimeBill")
                if (!directory.exists() && !directory.mkdirs()) {
                    throw Exception("Failed to create directory")
                }
                val file = File(directory, fileName)
                FileOutputStream(file).use { outputStream ->
                    pdfDocument.writeTo(outputStream)
                }
                if (file.exists()) {
                    Toast.makeText(this, "Receipt saved successfully.", Toast.LENGTH_LONG).show()
                } else {
                    throw Exception("File not found after saving")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Save failed: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            pdfDocument.close()
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