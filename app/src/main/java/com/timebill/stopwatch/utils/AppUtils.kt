package com.timebill.stopwatch.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.timebill.stopwatch.R

object AppUtils {

    fun openPlayStore(context: Context) {
        val packageName = context.packageName
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    fun shareApp(context: Context) {
        val packageName = context.packageName
        val shareMessage = "Track your work hours professionally with TimeBill.\n\nDownload TimeBill:\nhttps://play.google.com/store/apps/details?id=$packageName"
        
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_TEXT, shareMessage)
        
        context.startActivity(Intent.createChooser(intent, "Share via"))
    }

    fun openWebsite(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (_: Exception) {
            // Handle error
        }
    }

    fun openEmail(context: Context, email: String) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO)
            intent.data = Uri.parse("mailto:$email")
            context.startActivity(Intent.createChooser(intent, "Send Email"))
        } catch (_: Exception) {
            // Handle error
        }
    }

    fun openDialer(context: Context, phone: String) {
        try {
            val intent = Intent(Intent.ACTION_DIAL)
            intent.data = Uri.parse("tel:$phone")
            context.startActivity(intent)
        } catch (_: Exception) {
            // Handle error
        }
    }
}