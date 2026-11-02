package org.leafvibe.leafbridge

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.inputmethod.EditorInfo
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class LeafBrowserActivity : Activity() {
    private lateinit var web: WebView
    private lateinit var address: EditText
    private lateinit var status: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(12, 16, 12, 8); setBackgroundColor(android.graphics.Color.rgb(13, 22, 29)) }
        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        top.addView(actionButton("←") { if (web.canGoBack()) web.goBack() else finish() })
        top.addView(actionButton("→") { if (web.canGoForward()) web.goForward() })
        top.addView(actionButton("↻") { web.reload() })
        address = EditText(this).apply {
            setSingleLine(true); hint = "Rechercher ou saisir une adresse"; textSize = 13f
            setTextColor(android.graphics.Color.WHITE); setHintTextColor(android.graphics.Color.LTGRAY)
            setPadding(14, 8, 14, 8); setBackgroundColor(android.graphics.Color.rgb(36, 49, 58))
            imeOptions = EditorInfo.IME_ACTION_GO
            setOnEditorActionListener { _, action, _ -> if (action == EditorInfo.IME_ACTION_GO || action == EditorInfo.IME_ACTION_DONE) { navigate(text.toString()); true } else false }
        }
        top.addView(address, LinearLayout.LayoutParams(0, 48, 1f)); root.addView(top)
        status = TextView(this).apply { text = "Navigation LeafVibe"; setTextColor(android.graphics.Color.LTGRAY); textSize = 11f; setPadding(4, 6, 4, 6) }
        root.addView(status)
        web = WebView(this)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.loadsImagesAutomatically = true
        web.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) { address.setText(url ?: ""); status.text = "Chargement…" }
            override fun onPageFinished(view: WebView?, url: String?) { address.setText(url ?: ""); status.text = view?.title ?: "Page chargée" }
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val uri = request.url
                val scheme = uri.scheme?.lowercase()
                if (scheme == "http" || scheme == "https") return false
                return try { startActivity(Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE)); true }
                catch (_: ActivityNotFoundException) { Toast.makeText(this@LeafBrowserActivity, "Aucune application ne peut ouvrir ce lien.", Toast.LENGTH_SHORT).show(); true }
                catch (_: SecurityException) { true }
            }
        }
        web.webChromeClient = WebChromeClient()
        root.addView(web, LinearLayout.LayoutParams(-1, 0, 1f))
        val bottom = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; orientation = LinearLayout.HORIZONTAL }
        bottom.addView(actionButton("Accueil") { navigate("https://www.google.com") })
        bottom.addView(actionButton("Navigateur externe") {
            try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(web.url ?: "https://www.google.com"))) }
            catch (_: Exception) { Toast.makeText(this@LeafBrowserActivity, "Navigateur indisponible", Toast.LENGTH_SHORT).show() }
        })
        root.addView(bottom); setContentView(root)
        if (savedInstanceState != null) web.restoreState(savedInstanceState) else navigate(intent.getStringExtra("url") ?: "https://www.google.com")
    }
    private fun actionButton(label: String, action: () -> Unit) = TextView(this).apply {
        text = label; textSize = 12f; gravity = Gravity.CENTER; setTextColor(android.graphics.Color.WHITE)
        setPadding(10, 12, 10, 12); setOnClickListener { action() }
    }
    private fun navigate(raw: String) {
        val value = raw.trim(); if (value.isEmpty()) return
        val target = if (value.startsWith("http://") || value.startsWith("https://")) value
            else if (value.contains(".") && !value.contains(" ")) "https://$value"
            else "https://www.google.com/search?q=" + Uri.encode(value)
        try { web.loadUrl(target) } catch (_: Exception) { Toast.makeText(this, "Adresse impossible à ouvrir", Toast.LENGTH_SHORT).show() }
    }
    override fun onSaveInstanceState(outState: Bundle) { web.saveState(outState); super.onSaveInstanceState(outState) }
    @Deprecated("Back navigates within the browser first")
    override fun onBackPressed() { if (::web.isInitialized && web.canGoBack()) web.goBack() else finish() }
    override fun onDestroy() {
        if (::web.isInitialized) { (web.parent as? android.view.ViewGroup)?.removeView(web); web.stopLoading(); web.webChromeClient = null; web.webViewClient = WebViewClient(); web.destroy() }
        super.onDestroy()
    }
}