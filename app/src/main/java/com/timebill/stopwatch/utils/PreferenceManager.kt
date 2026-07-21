package com.timebill.stopwatch.utils

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

class PreferenceManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("timebill_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_GUEST_ID = "guest_id"
    }

    fun getGuestId(): String {
        var guestId = prefs.getString(KEY_GUEST_ID, null)
        if (guestId == null) {
            guestId = "guest_${UUID.randomUUID().toString().take(8)}"
            prefs.edit().putString(KEY_GUEST_ID, guestId).apply()
        }
        return guestId
    }

    fun clearAllData() {
        prefs.edit().clear().apply()
    }
}