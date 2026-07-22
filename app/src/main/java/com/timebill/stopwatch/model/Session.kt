package com.timebill.stopwatch.model

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class Session(
    val id: String? = null,
    val receiptNumber: String? = "",
    val clientName: String? = "",
    val clientMobile: String? = "",
    val clientEmail: String? = "",
    val clientAddress: String? = "",
    val status: String? = "Work Completed",
    val startTime: Long? = 0L,
    val endTime: Long? = 0L,
    val durationMillis: Long? = 0L,
    val hourlyRate: Double? = 0.0,
    val earnings: Double? = 0.0,
    val createdAt: Long? = System.currentTimeMillis(),
    val timestamp: Long? = System.currentTimeMillis()
)