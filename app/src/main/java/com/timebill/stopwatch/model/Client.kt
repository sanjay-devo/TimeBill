package com.timebill.stopwatch.model

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class Client(
    val clientId: String? = null,
    val clientName: String? = "",
    val mobile: String? = "",
    val email: String? = "",
    val address: String? = "",
    val createdAt: Long? = System.currentTimeMillis(),
    val updatedAt: Long? = System.currentTimeMillis()
)