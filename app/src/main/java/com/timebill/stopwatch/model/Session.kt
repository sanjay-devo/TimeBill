package com.timebill.stopwatch.model

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class Session(
    val id: String? = null,
    val invoiceNumber: String? = "",
    val receiptNumber: String? = "", // Deprecated: use invoiceNumber
    val clientName: String? = "",
    val clientMobile: String? = "",
    val clientEmail: String? = "",
    val clientAddress: String? = "",
    val workName: String? = "",
    val hasClientDetails: Boolean? = false,
    val invoiceTimestamp: Long? = 0L,
    val receiptTimestamp: Long? = 0L, // Deprecated: use invoiceTimestamp
    val status: String? = "Work Completed",
    val startTime: Long? = 0L,
    val endTime: Long? = 0L,
    val durationMillis: Long? = 0L,
    val hourlyRate: Double? = 0.0,
    val earnings: Double? = 0.0,
    val createdAt: Long? = System.currentTimeMillis(),
    val timestamp: Long? = System.currentTimeMillis()
)
