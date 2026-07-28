package com.timebill.stopwatch.repository

import android.util.Log
import com.timebill.stopwatch.model.Session
import com.timebill.stopwatch.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EmailRepository {
    private val client = OkHttpClient()
    private val apiKey = "TB_7f9K@2xP!TimeBill#2026"
    private val apiUrl = "https://timebill.indiacybercafe.com/api/send-email.php"
    private val TAG = "EmailRepository"

    suspend fun sendInvoiceEmail(session: Session, profile: UserProfile, pdfFile: File): Result<String> {
        return withContext(Dispatchers.IO) {
            val invoiceNo = session.invoiceNumber ?: session.receiptNumber ?: ""
            val invoiceTimestamp = session.invoiceTimestamp ?: 0L
            val sdfDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val sdfTime = SimpleDateFormat("hh:mm a", Locale.getDefault())
            
            val formattedDate = sdfDate.format(Date(invoiceTimestamp))
            val formattedTime = sdfTime.format(Date(invoiceTimestamp))

            val businessName = if (!profile.businessName.isNullOrEmpty()) profile.businessName else profile.fullName ?: ""
            val businessAddress = "${profile.addressLine1 ?: ""}, ${profile.addressLine2 ?: ""}, ${profile.city ?: ""}, ${profile.state ?: ""} - ${profile.pinCode ?: ""}"

            val builder = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                // Original fields
                .addFormDataPart("client_name", session.clientName ?: "")
                .addFormDataPart("client_email", session.clientEmail ?: "")
                .addFormDataPart("work_name", session.workName ?: "")
                .addFormDataPart("invoice_no", invoiceNo)
                .addFormDataPart("invoice_date", formattedDate)
                .addFormDataPart("total_amount", String.format(Locale.getDefault(), "%.2f", session.earnings ?: 0.0))
                
                // Business Details
                .addFormDataPart("business_name", businessName)
                .addFormDataPart("business_email", profile.email ?: "")
                .addFormDataPart("business_mobile", profile.mobile ?: "")
                .addFormDataPart("business_address", businessAddress)
                .addFormDataPart("business_gst", profile.gstNumber ?: "")
                
                // New required fields from API
                .addFormDataPart("status", session.status ?: "Work Completed")
                .addFormDataPart("duration", formatDuration(session.durationMillis ?: 0L))
                .addFormDataPart("rate", String.format(Locale.getDefault(), "%.2f", session.hourlyRate ?: 0.0))
                .addFormDataPart("amount", String.format(Locale.getDefault(), "%.2f", session.earnings ?: 0.0))
                .addFormDataPart("subtotal", String.format(Locale.getDefault(), "%.2f", session.earnings ?: 0.0))
                .addFormDataPart("tax", "0.00")
                .addFormDataPart("total", String.format(Locale.getDefault(), "%.2f", session.earnings ?: 0.0))
                .addFormDataPart("generated_date", formattedDate)
                .addFormDataPart("generated_time", formattedTime)
                .addFormDataPart("business_phone", profile.mobile ?: "")

                // PDF File
                .addFormDataPart(
                    "invoice_pdf",
                    "Invoice-$invoiceNo.pdf",
                    pdfFile.asRequestBody("application/pdf".toMediaTypeOrNull())
                )

            val requestBody = builder.build()

            // Logging request fields for debugging (excluding API key)
            Log.d(TAG, "--- Outgoing Email API Request ---")
            requestBody.parts.forEach { part ->
                val contentDisposition = part.headers?.get("Content-Disposition")
                if (contentDisposition != null && !contentDisposition.contains("filename=")) {
                    val name = contentDisposition.substringAfter("name=\"").substringBefore("\"")
                    // Note: This is simplified as part.body.writeTo requires a buffer, 
                    // for logging we'll just track the keys being sent.
                    Log.d(TAG, "Field: $name")
                }
            }
            Log.d(TAG, "API URL: $apiUrl")
            Log.d(TAG, "----------------------------------")

            val request = Request.Builder()
                .url(apiUrl)
                .addHeader("X-API-Key", apiKey)
                .post(requestBody)
                .build()

            try {
                val response = client.newCall(request).execute()
                val bodyString = response.body?.string()
                if (response.isSuccessful) {
                    Result.success(bodyString ?: "Success")
                } else {
                    Log.e(TAG, "API Error: ${response.code} - $bodyString")
                    Result.failure(Exception("Error ${response.code}: ${response.message}\n$bodyString"))
                }
            } catch (e: IOException) {
                Log.e(TAG, "Network Error", e)
                Result.failure(e)
            }
        }
    }

    private fun formatDuration(millis: Long): String {
        val seconds = (millis / 1000) % 60
        val minutes = (millis / (1000 * 60)) % 60
        val hours = (millis / (1000 * 60 * 60))
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    }
}
