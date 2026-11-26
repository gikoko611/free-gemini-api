package com.ikoko.gemini4

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.browser.customtabs.CustomTabsIntent

class MainActivity : Activity() {
    private val background = Color.rgb(16, 13, 10)
    private val panel = Color.rgb(31, 25, 18)
    private val gold = Color.rgb(216, 181, 106)
    private val muted = Color.rgb(190, 177, 151)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = background
        window.navigationBarColor = background
        setContentView(buildHome())
    }

    private fun buildHome(): View {
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(this@MainActivity.background)
            setPadding(dp(22), dp(30), dp(22), dp(24))
        }

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            addView(page)
        }

        page.addView(label("G.I", 42f, gold, true))
        page.addView(label("GEMINI", 22f, Color.WHITE, true))
        page.addView(label("Powered by G.I KO KO", 13f, muted, false))
        page.addView(space(30))

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(20), dp(18), dp(20))
            setBackgroundColor(panel)
        }
        card.addView(label("Gemini Website", 20f, gold, true))
        card.addView(space(8))
        card.addView(label(
            "Sign in with your Google Account and use the Gemini experience available to your account. Your browser session stays on this device; G.I Gemini does not copy Google session cookies to a server.",
            14f, muted, false
        ))
        card.addView(space(18))

        val open = Button(this).apply {
            text = "OPEN GEMINI"
            setTextColor(Color.rgb(25, 19, 10))
            setBackgroundColor(gold)
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            isAllCaps = false
            setOnClickListener { openGemini() }
        }
        card.addView(open, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(52)
        ))
        page.addView(card)

        page.addView(space(22))
        page.addView(label("HYDROGEN ACCESS", 15f, gold, true))
        page.addView(space(8))
        page.addView(label(
            "On a free Google Account, Google's current model guide lists Hydrogen in Task mode and Auto in Chat mode. Manual Hydrogen selection in Chat mode requires an eligible Google AI plan. Availability and usage limits are controlled by Google.",
            14f, muted, false
        ))
        page.addView(space(20))
        page.addView(label("API KEY MODE", 15f, gold, true))
        page.addView(space(6))
        page.addView(label(
            "A separate API-provider screen will be added in the next stage. Gemini Website sign-in is not the same as Gemini API authentication.",
            14f, muted, false
        ))
        page.addView(space(24))
        page.addView(label("G.I GEMINI • Android 10+", 12f, muted, false))
        return scroll
    }

    private fun openGemini() {
        val uri = Uri.parse("https://gemini.google.com/")
        try {
            CustomTabsIntent.Builder()
                .setShowTitle(true)
                .setToolbarColor(panel)
                .build()
                .launchUrl(this, uri)
        } catch (_: Exception) {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
    }

    private fun label(text: String, size: Float, color: Int, bold: Boolean): TextView =
        TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(color)
            if (bold) typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.START
            setLineSpacing(dp(3).toFloat(), 1f)
        }

    private fun space(height: Int) = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(1, dp(height))
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
