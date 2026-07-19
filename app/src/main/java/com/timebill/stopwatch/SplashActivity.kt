package com.timebill.stopwatch

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AlphaAnimation
import android.widget.ImageView
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsControllerCompat
import java.io.IOException

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge with transparent bars.
        // Using SystemBarStyle.light since the splash background (#F5ECDD) is light.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        
        setContentView(R.layout.activity_splash)

        // Ensure system bar icons are dark to be visible against the light background
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.isAppearanceLightStatusBars = true
        controller.isAppearanceLightNavigationBars = true

        val splashImage: ImageView = findViewById(R.id.splash_image)

        // Load logo.png from assets
        try {
            assets.open("logo.png").use { inputStream ->
                val drawable = Drawable.createFromStream(inputStream, null)
                splashImage.setImageDrawable(drawable)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        // Smooth Fade In animation (200ms)
        val fadeIn = AlphaAnimation(0f, 1f)
        fadeIn.duration = 200
        splashImage.startAnimation(fadeIn)

        // Total splash duration: 1200ms
        Handler(Looper.getMainLooper()).postDelayed({
            startMainActivity()
        }, 1200)
    }

    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        
        // Professional fade transition
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, android.R.anim.fade_in, android.R.anim.fade_out)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
        finish()
    }
}