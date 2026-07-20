package com.timebill.stopwatch.model

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class Session(
    val id: String? = null,
    val clientName: String? = "",
    val hourlyRate: Double? = 0.0,
    val startTime: Long? = 0L,
    val durationMillis: Long? = 0L,
    val earnings: Double? = 0.0,
    val timestamp: Long? = System.currentTimeMillis()
)