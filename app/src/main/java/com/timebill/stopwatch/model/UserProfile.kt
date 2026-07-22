package com.timebill.stopwatch.model

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class UserProfile(
    val fullName: String? = "",
    val mobile: String? = "",
    val email: String? = "",
    val businessName: String? = "",
    val addressLine1: String? = "",
    val addressLine2: String? = "",
    val city: String? = "",
    val state: String? = "",
    val pinCode: String? = "",
    val gstNumber: String? = "",
    val updatedAt: Long? = System.currentTimeMillis()
)