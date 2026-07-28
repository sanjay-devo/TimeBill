package com.timebill.stopwatch.utils

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.timebill.stopwatch.model.Session
import com.timebill.stopwatch.model.UserProfile
import java.text.SimpleDateFormat
import java.util.*

object PdfUtils {

    fun createPdfDocument(session: Session, profile: UserProfile): PdfDocument {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val gold = Color.parseColor("#D4AE7A")
        val darkBrown = Color.parseColor("#2E1A17")
        val labelColor = Color.parseColor("#7A6A63")

        canvas.drawColor(Color.WHITE)

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

        // Header - Right (Invoice Info)
        val rightX = 545f
        titlePaint.textAlign = Paint.Align.RIGHT
        titlePaint.textSize = 28f
        titlePaint.color = darkBrown
        canvas.drawText("INVOICE", rightX, 60f, titlePaint)

        normalPaint.textAlign = Paint.Align.RIGHT
        normalPaint.color = Color.BLACK
        normalPaint.textSize = 12f
        val displayInvoiceNum = if (!session.invoiceNumber.isNullOrEmpty()) session.invoiceNumber else session.receiptNumber ?: "-"
        canvas.drawText("Invoice No. : $displayInvoiceNum", rightX, 85f, normalPaint)
        
        // Date and Time on the same row, both on the right side
        val invoiceTs = if (session.invoiceTimestamp != 0L) session.invoiceTimestamp!! else if (session.receiptTimestamp != 0L) session.receiptTimestamp!! else (session.createdAt ?: session.timestamp ?: 0L)
        val dateStr = "Date : ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(invoiceTs))}"
        val timeStr = "Time : ${SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(invoiceTs))}"
        val combinedDateTime = "$dateStr     $timeStr"
        
        canvas.drawText(combinedDateTime, rightX, 105f, normalPaint)
        
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
        canvas.drawText(AppUtils.formatDuration(session.durationMillis ?: 0L), 280f, yPos + 25f, normalPaint)
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
        canvas.drawText("Automatically Generated Invoice", 297f, yPos, labelPaint)

        pdfDocument.finishPage(page)
        return pdfDocument
    }
}
