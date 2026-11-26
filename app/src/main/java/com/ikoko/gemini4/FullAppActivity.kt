package com.ikoko.gemini4

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.Executors

class FullAppActivity : Activity() {
    private val bg = Color.rgb(15, 12, 10)
    private val panel = Color.rgb(32, 25, 19)
    private val gold = Color.rgb(220, 184, 105)
    private val ink = Color.rgb(245, 237, 222)
    private val muted = Color.rgb(177, 165, 146)
    private val io = Executors.newSingleThreadExecutor()
    private lateinit var db: ChatDb
    private lateinit var secrets: SecretStore
    private lateinit var body: LinearLayout
    private lateinit var transcript: LinearLayout
    private lateinit var input: EditText
    private var session = ""
    private val prefs by lazy { getSharedPreferences("gikoko_settings", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        db = ChatDb(this)
        secrets = SecretStore(this)
        session = prefs.getString("active_session", null) ?: UUID.randomUUID().toString().also { prefs.edit().putString("active_session", it).apply() }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
            setPadding(dp(16), dp(18), dp(16), dp(12))
        }
        root.addView(label("G.I GEMINI", 25f, gold, true))
        root.addView(label("Powered by G.I KO KO  •  AI WORKSPACE", 12f, muted))
        root.addView(space(12))
        val nav = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        nav.addView(navButton("CHAT") { chat() }, weight())
        nav.addView(navButton("HYDROGEN") { openGemini() }, weight())
        nav.addView(navButton("API KEYS") { settings() }, weight())
        nav.addView(navButton("HISTORY") { history() }, weight())
        root.addView(nav)
        root.addView(space(8))
        body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(body, LinearLayout.LayoutParams(-1, 0, 1f))
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(dp(16) + bars.left, dp(18) + bars.top, dp(16) + bars.right, dp(12) + bars.bottom)
            insets
        }
        setContentView(root)
        ViewCompat.requestApplyInsets(root)
        chat()
    }

    private fun chat() {
        body.removeAllViews()
        body.addView(label("AI CHAT", 18f, gold, true))
        body.addView(label("Native chat via your API provider. Hydrogen Website opens with your Google Account.", 12f, muted))
        body.addView(space(8))
        val scroll = ScrollView(this).apply { setBackgroundColor(Color.rgb(20, 17, 15)) }
        transcript = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), dp(10), dp(8), dp(10))
        }
        scroll.addView(transcript)
        body.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        db.recent(session).forEach { bubble(it.role, it.content) }
        input = EditText(this).apply {
            hint = "Message G.I..."
            setHintTextColor(muted); setTextColor(ink)
            textSize = 15f; minLines = 2; maxLines = 4
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = rounded(panel, gold)
        }
        body.addView(input, LinearLayout.LayoutParams(-1, -2))
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row.addView(action("＋ NEW CHAT") { session = UUID.randomUUID().toString(); prefs.edit().putString("active_session", session).apply(); chat() }, weight())
        row.addView(action("SEND  ➤") { send() }, weight())
        body.addView(row)
    }

    private fun send() {
        val prompt = input.text.toString().trim()
        if (prompt.isEmpty()) return
        val base = prefs.getString("base", "")?.trim().orEmpty()
        val model = prefs.getString("model", "")?.trim().orEmpty()
        val key = secrets.read()
        if (base.isBlank() || model.isBlank() || key.isBlank()) {
            Toast.makeText(this, "Set Base URL, Model ID and API Key first.", Toast.LENGTH_LONG).show()
            settings(); return
        }
        input.setText("")
        val activeSession = session
        val activeTranscript = transcript
        db.add(activeSession, "user", prompt)
        bubble("user", prompt)
        val wait = label("G.I is thinking…", 13f, gold)
        activeTranscript.addView(wait)
        io.execute {
            val answer = complete(base, model, key, activeSession)
            runOnUiThread {
                activeTranscript.removeView(wait)
                if (answer.startsWith("!ERR!")) {
                    if (session == activeSession) bubble("error", answer.removePrefix("!ERR!"))
                } else {
                    db.add(activeSession, "assistant", answer)
                    if (session == activeSession) bubble("assistant", answer)
                }
            }
        }
    }

    private fun complete(base: String, model: String, key: String, activeSession: String): String {
        var conn: HttpURLConnection? = null
        return try {
            val root = base.trimEnd('/')
            val endpoint = when {
                root.endsWith("/chat/completions") -> root
                Regex("/v[0-9]+(\\.[0-9]+)?$").containsMatchIn(root) -> "$root/chat/completions"
                else -> "$root/v1/chat/completions"
            }
            val msgs = JSONArray()
            db.recent(activeSession, 30).forEach { msgs.put(JSONObject().put("role", it.role).put("content", it.content)) }
            val payload = JSONObject().put("model", model).put("messages", msgs)
            conn = URL(endpoint).openConnection() as HttpURLConnection
            conn.requestMethod = "POST"; conn.connectTimeout = 20000; conn.readTimeout = 90000
            conn.doOutput = true
            conn.setRequestProperty("Authorization", "Bearer $key")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val raw = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                val detail = try { JSONObject(raw).optJSONObject("error")?.optString("message") ?: raw } catch (_: Exception) { raw }
                "!ERR!HTTP $code — $detail"
            } else {
                JSONObject(raw).optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")
                    ?.optString("content")?.takeIf { it.isNotBlank() } ?: "Empty response from provider."
            }
        } catch (e: Exception) { "!ERR!${e.message ?: "Request failed"}" }
        finally { conn?.disconnect() }
    }

    private fun settings() {
        body.removeAllViews()
        body.addView(label("API PROVIDER", 18f, gold, true))
        body.addView(label("OpenAI-compatible API. Key is encrypted locally using Android Keystore.", 12f, muted))
        body.addView(space(10))
        val base = field("Base URL (https://provider.example/v1)", prefs.getString("base", "https://api.openai.com/v1") ?: "")
        val model = field("Model ID supplied by provider", prefs.getString("model", "") ?: "")
        val key = field("API Key", secrets.read(), true)
        body.addView(base); body.addView(space(8)); body.addView(model); body.addView(space(8)); body.addView(key)
        body.addView(space(12))
        body.addView(action("SAVE API SETTINGS") {
            prefs.edit().putString("base", base.text.toString().trim())
                .putString("model", model.text.toString().trim()).apply()
            secrets.save(key.text.toString().trim())
            Toast.makeText(this, "Saved. API key is encrypted on this device.", Toast.LENGTH_SHORT).show()
        })
        body.addView(space(8))
        body.addView(action("LOAD API MODELS") {
            val endpointBase = base.text.toString().trim().trimEnd('/')
            val token = key.text.toString().trim()
            io.execute {
                val found = fetchModels(endpointBase, token)
                runOnUiThread {
                    if (found.isEmpty()) Toast.makeText(this, "No models found. Check URL, key and provider compatibility.", Toast.LENGTH_LONG).show()
                    else AlertDialog.Builder(this).setTitle("Select API Model")
                        .setItems(found.toTypedArray()) { _, index -> model.setText(found[index]) }.show()
                }
            }
        })
        body.addView(space(8))
        body.addView(action("TEST API CONNECTION") {
            val endpointBase = base.text.toString().trim().trimEnd('/')
            val token = key.text.toString().trim()
            val selectedModel = model.text.toString().trim()
            if (endpointBase.isBlank() || selectedModel.isBlank()) {
                Toast.makeText(this, "Enter Base URL and Model ID first.", Toast.LENGTH_LONG).show()
            } else {
                io.execute {
                    val found = fetchModels(endpointBase, token)
                    runOnUiThread {
                        val matched = found.any { it == selectedModel }
                        val message = when {
                            found.isEmpty() -> "Could not verify. Check URL, API key and /models support."
                            matched -> "Connected. Model found: $selectedModel"
                            else -> "Provider reachable: ${found.size} model(s), but selected model was not listed. Verify its exact ID."
                        }
                        AlertDialog.Builder(this).setTitle("G.I API Test").setMessage(message)
                            .setPositiveButton("OK", null).show()
                    }
                }
            }
        })
        body.addView(space(8))
        body.addView(action("OPEN GEMINI WEBSITE") { openGemini() })
        body.addView(space(8))
        body.addView(label("Hydrogen availability is controlled by Google Account plan, mode and quota. The Gemini Website login is not a public API key, so it is kept separate from this API client.", 12f, muted))
    }

    private fun fetchModels(base: String, key: String): List<String> {
        var conn: HttpURLConnection? = null
        return try {
            val endpoint = if (Regex("/v[0-9]+(\\.[0-9]+)?$").containsMatchIn(base)) "$base/models" else "$base/v1/models"
            conn = URL(endpoint).openConnection() as HttpURLConnection
            conn.connectTimeout = 15000
            conn.readTimeout = 20000
            if (key.isNotBlank()) conn.setRequestProperty("Authorization", "Bearer $key")
            val code = conn.responseCode
            if (code !in 200..299) return emptyList()
            val raw = conn.inputStream.bufferedReader().use { it.readText() }
            val data = JSONObject(raw).optJSONArray("data") ?: return emptyList()
            (0 until data.length()).mapNotNull { data.optJSONObject(it)?.optString("id")?.takeIf { id -> id.isNotBlank() } }
        } catch (_: Exception) { emptyList() }
        finally { conn?.disconnect() }
    }

    private fun history() {
        body.removeAllViews()
        body.addView(label("CHAT HISTORY", 18f, gold, true))
        body.addView(label("Select a conversation to continue where you left off.", 12f, muted))
        val scroll = ScrollView(this)
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(4, 8, 4, 8) }
        val sessions = db.sessions(100)
        if (sessions.isEmpty()) list.addView(label("No conversations yet.", 14f, muted))
        sessions.forEach { item ->
            val date = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(item.updatedAt))
            val preview = item.lastMessage.replace("\n", " ").take(72)
            list.addView(action("${date}  •  ${item.messageCount} messages\n${preview}") {
                session = item.sessionId
                prefs.edit().putString("active_session", session).apply()
                chat()
            })
            list.addView(space(8))
        }
        scroll.addView(list)
        body.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
    }

    private fun openGemini() {
        val uri = Uri.parse("https://gemini.google.com/")
        try { CustomTabsIntent.Builder().setShowTitle(true).setToolbarColor(panel).build().launchUrl(this, uri) }
        catch (_: Exception) { startActivity(Intent(Intent.ACTION_VIEW, uri)) }
    }

    private fun bubble(role: String, value: String) {
        val mine = role == "user"
        val fill = if (mine) Color.rgb(69, 49, 23) else if (role == "error") Color.rgb(75, 30, 24) else panel
        val view = TextView(this).apply {
            text = (if (mine) "YOU  " else if (role == "assistant") "G.I  " else "NOTICE  ") + value
            textSize = 14f; setTextColor(ink); setPadding(dp(12), dp(10), dp(12), dp(10))
            background = rounded(fill, gold)
        }
        val lp = LinearLayout.LayoutParams(if (mine) (resources.displayMetrics.widthPixels * .82f).toInt() else -1, -2)
        lp.gravity = if (mine) Gravity.END else Gravity.START
        lp.bottomMargin = dp(8)
        transcript.addView(view, lp)
    }

    private fun field(h: String, v: String, secret: Boolean = false) = EditText(this).apply {
        hint = h; setHintTextColor(muted); setTextColor(ink); textSize = 14f; setSingleLine(true)
        setPadding(dp(12), dp(10), dp(12), dp(10)); background = rounded(panel, gold); setText(v)
        if (secret) inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
    }
    private fun navButton(s: String, f: () -> Unit) = Button(this).apply {
        text = s; textSize = 10f; isAllCaps = false; setTextColor(gold); setBackgroundColor(panel)
        setPadding(dp(1), 0, dp(1), 0); setOnClickListener { f() }
    }
    private fun action(s: String, f: () -> Unit) = Button(this).apply {
        text = s; textSize = 12f; isAllCaps = false; typeface = Typeface.DEFAULT_BOLD
        setTextColor(Color.rgb(25, 19, 10)); background = rounded(gold, gold); setOnClickListener { f() }
    }
    private fun label(s: String, size: Float, color: Int, bold: Boolean = false) = TextView(this).apply {
        text = s; textSize = size; setTextColor(color); if (bold) typeface = Typeface.DEFAULT_BOLD
        setLineSpacing(dp(2).toFloat(), 1f)
    }
    private fun rounded(fill: Int, stroke: Int) = GradientDrawable().apply {
        setColor(fill); cornerRadius = dp(14).toFloat(); setStroke(dp(1), stroke)
    }
    private fun space(h: Int) = View(this).apply { layoutParams = LinearLayout.LayoutParams(1, dp(h)) }
    private fun weight() = LinearLayout.LayoutParams(0, dp(42), 1f)
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    override fun onDestroy() { io.shutdown(); super.onDestroy() }
}
