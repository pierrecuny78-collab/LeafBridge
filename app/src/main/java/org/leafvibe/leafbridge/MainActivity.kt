package org.leafvibe.leafbridge

import android.app.Activity
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var root: LinearLayout
    private lateinit var content: LinearLayout
    private lateinit var status: TextView
    private lateinit var browser: LinearLayout
    private lateinit var addressBar: EditText
    private lateinit var webHost: LinearLayout
    private val webViews = mutableListOf<WebView>()
    private var activeTab = 0
    private var currentPage = "Accueil"

    override fun onStop() {
        LeafAutoBackupScheduler.requestSoon(this)
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LeafAutoBackupScheduler.install(this)
        buildShell()
        showHome()
    }

    private fun buildShell() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(12, 20, 29))
        }
        val header = TextView(this).apply {
            text = "LEAFBRIDGE  •  V1.1"
            textSize = 19f
            setTextColor(Color.WHITE)
            setPadding(22, 20, 16, 16)
            setBackgroundColor(Color.rgb(20, 35, 47))
        }
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 14, 16, 12)
        }
        val scroll = ScrollView(this).apply { addView(content) }
        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.rgb(20, 35, 47))
            gravity = Gravity.CENTER
        }
        listOf("Accueil", "Automatisations", "Web", "Connexions", "Réglages").forEach { page ->
            val b = Button(this).apply {
                text = when (page) {
                    "Automatisations" -> "Auto"
                    "Réglages" -> "Réglages"
                    else -> page
                }
                textSize = 10f
                isAllCaps = false
                setPadding(2, 8, 2, 8)
                setOnClickListener { navigate(page) }
            }
            nav.addView(b, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }
        root.addView(header)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(nav)
        setContentView(root)
    }

    private fun navigate(page: String) {
        currentPage = page
        content.removeAllViews()
        when (page) {
            "Accueil" -> showHome()
            "Automatisations" -> showAutomations()
            "Web" -> showBrowser()
            "Connexions" -> showConnections()
            else -> showSettings()
        }
    }

    private fun heading(value: String) = TextView(this).apply {
        text = value
        textSize = 23f
        setTextColor(Color.WHITE)
        setPadding(0, 8, 0, 12)
    }

    private fun paragraph(value: String) = TextView(this).apply {
        text = value
        textSize = 14f
        setTextColor(Color.rgb(205, 216, 225))
        setPadding(0, 4, 0, 10)
    }

    private fun action(label: String, callback: () -> Unit) = Button(this).apply {
        text = label
        isAllCaps = false
        setOnClickListener { callback() }
    }

    private fun card(title: String, description: String, actionLabel: String? = null, callback: (() -> Unit)? = null) {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(14, 12, 14, 12)
            setBackgroundColor(Color.rgb(25, 39, 52))
        }
        panel.addView(TextView(this).apply {
            text = title
            textSize = 17f
            setTextColor(Color.WHITE)
        })
        panel.addView(paragraph(description))
        if (actionLabel != null && callback != null) panel.addView(action(actionLabel, callback))
        content.addView(panel, LinearLayout.LayoutParams(-1, -2).apply {
            bottomMargin = 12
        })
    }

    private fun showHome() {
        content.addView(heading("Tableau de bord"))
        status = paragraph("Vérification de Termux…")
        content.addView(status)
        card("Shazam", "Réception des notifications Shazam sur Android.", "Gérer l’accès") {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
        card("Moteur Termux", "Le moteur local exécute les tâches autorisées en arrière-plan.", "Tester la connexion") {
            checkTermux()
        }
        card("Accès notifications", "Autoriser LeafBridge à lire les notifications Shazam.", "Ouvrir les autorisations") {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
        card("Ouvrir les automatisations", "Démarrer ou arrêter les modules depuis leurs propres boutons.", "Voir les automatisations") {
            navigate("Automatisations")
        }
        refreshStatus()
    }

    private fun showAutomations() {
        content.addView(heading("Automatisations"))
        content.addView(paragraph("Chaque bouton envoie une commande précise au moteur local Termux. Le moteur refuse les modules qui ne sont pas encore configurés."))
        automationCard("Shazam → Spotify", "Transmettre les titres détectés vers la file de traitement Spotify.", "shazam_spotify")
        automationCard("Synchronisation GitHub", "Préparer une synchronisation de projets et de fichiers autorisés.", "github_sync")
        automationCard("Classement musical", "Classer les titres reçus par style et catégorie.", "music_sort")
        automationCard("Routine LeafBridge", "Lancer les vérifications locales programmées.", "routine")
        content.addView(action("Actualiser l'état du moteur") { refreshStatus() })
    }

    private fun automationCard(title: String, description: String, id: String) {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(14, 12, 14, 12)
            setBackgroundColor(Color.rgb(25, 39, 52))
        }
        panel.addView(TextView(this).apply {
            text = title
            textSize = 17f
            setTextColor(Color.WHITE)
        })
        panel.addView(paragraph(description))
        val buttons = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        buttons.addView(action("Démarrer") { sendAutomation(id, "start") }, LinearLayout.LayoutParams(0, -2, 1f))
        buttons.addView(action("Arrêter") { sendAutomation(id, "stop") }, LinearLayout.LayoutParams(0, -2, 1f))
        panel.addView(buttons)
        content.addView(panel, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 12 })
    }

    private fun showConnections() {
        content.addView(heading("Connexions"))
        content.addView(paragraph("LeafBridge ne crée aucun compte. Chaque service sera autorisé séparément, avec ses propres permissions."))
        listOf(
            Triple("Spotify", "Compte musical. L'autorisation officielle sera nécessaire pour ajouter des titres aux playlists.", "https://accounts.spotify.com/"),
            Triple("GitHub", "Dépôts, fichiers et synchronisation selon les autorisations accordées.", "https://github.com/login"),
            Triple("Instagram", "Connexion via les mécanismes officiels et uniquement pour les fonctions permises par l'API.", "https://www.instagram.com/accounts/login/"),
            Triple("TikTok", "Connexion officielle selon les produits et permissions API disponibles.", "https://www.tiktok.com/login")
        ).forEach { (name, description, url) ->
            card(name, description, "Ouvrir le service") {
                startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)))
            }
        }
    }

    private fun showSettings() {
        content.addView(heading("Réglages"))
        content.addView(paragraph("Moteur local : http://127.0.0.1:8765. Le serveur doit être démarré dans Termux."))
        content.addView(action("Tester Termux") { checkTermux() })
        content.addView(action("Autoriser les notifications Shazam") {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        })
        content.addView(action("Ouvrir les paramètres de batterie") {
            startActivity(Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        })
        content.addView(paragraph("Pour une exécution durable, configure Termux:Boot et retire Termux des restrictions de batterie si Android le permet."))
    }

    private fun showBrowser() {
        content.addView(heading("Navigateur LeafBridge"))
        val controls = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        addressBar = EditText(this).apply {
            hint = "Adresse web ou recherche"
            setSingleLine(true)
            setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY)
            setText("https://www.google.com")
        }
        controls.addView(addressBar, LinearLayout.LayoutParams(0, -2, 1f))
        controls.addView(action("Aller") { loadAddress() })
        content.addView(controls)
        val tabControls = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        tabControls.addView(action("Nouvel onglet +") { addTab("https://www.google.com") })
        tabControls.addView(action("Onglet suivant") {
            if (webViews.isNotEmpty()) {
                activeTab = (activeTab + 1) % webViews.size
                displayActiveTab()
            }
        })
        tabControls.addView(action("Fermer") { closeActiveTab() })
        tabControls.addView(action("Actualiser") { if (webViews.isNotEmpty()) webViews[activeTab].reload() })
        tabControls.addView(action("←") { if (webViews.isNotEmpty() && webViews[activeTab].canGoBack()) webViews[activeTab].goBack() })
        content.addView(tabControls)
        webHost = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
        }
        content.addView(webHost, LinearLayout.LayoutParams(-1, 0, 1f).apply { height = 900 })
        if (webViews.isEmpty()) addTab("https://www.google.com") else displayActiveTab()
    }

    private fun addTab(url: String) {
        val view = WebView(this)
        view.settings.javaScriptEnabled = true
        view.settings.domStorageEnabled = true
        view.settings.allowFileAccess = false
        view.settings.allowContentAccess = false
        view.settings.javaScriptCanOpenWindowsAutomatically = false
        view.settings.setSupportMultipleWindows(false)
        view.settings.allowFileAccessFromFileURLs = false
        view.settings.allowUniversalAccessFromFileURLs = false
        view.webViewClient = object : WebViewClient() {
            override fun onPageFinished(webView: WebView, url: String) {
                if (webViews.getOrNull(activeTab) === webView && ::addressBar.isInitialized) {
                    addressBar.setText(url)
                }
            }
        }
        view.webChromeClient = WebChromeClient()
        webViews.add(view)
        activeTab = webViews.lastIndex
        view.loadUrl(url)
        displayActiveTab()
    }

    private fun displayActiveTab() {
        if (!::webHost.isInitialized || webViews.isEmpty()) return
        activeTab = activeTab.coerceIn(0, webViews.lastIndex)
        webHost.removeAllViews()
        webHost.addView(webViews[activeTab], LinearLayout.LayoutParams(-1, -1))
        if (::addressBar.isInitialized) addressBar.setText(webViews[activeTab].url ?: "")
    }

    private fun closeActiveTab() {
        if (webViews.isEmpty()) return
        val removed = webViews.removeAt(activeTab)
        webHost.removeView(removed)
        removed.stopLoading()
        removed.destroy()
        if (webViews.isEmpty()) {
            activeTab = 0
            addTab("https://www.google.com")
        } else {
            activeTab = (activeTab - 1).coerceAtLeast(0)
            displayActiveTab()
        }
    }

    private fun loadAddress() {
        if (webViews.isEmpty()) addTab("https://www.google.com")
        var target = addressBar.text.toString().trim()
        if (target.isBlank()) return
        if (!target.startsWith("http://") && !target.startsWith("https://")) {
            target = if (target.contains(".") && !target.contains(" ")) "https://$target"
                else "https://www.google.com/search?q=" + URLEncoder.encode(target, "UTF-8")
        }
        webViews[activeTab].loadUrl(target)
    }

    private fun checkTermux() {
        requestEngine("GET", "/status", null) { ok, message ->
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }

    private fun refreshStatus() {
        requestEngine("GET", "/status", null) { ok, message ->
            if (::status.isInitialized) status.text = if (ok) "Moteur Termux : connecté" else "Moteur Termux : hors ligne"
        }
    }

    private fun sendAutomation(id: String, command: String) {
        val payload = JSONObject().put("id", id).put("command", command).toString()
        requestEngine("POST", "/automation", payload) { ok, message ->
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }

    private fun requestEngine(method: String, path: String, body: String?, done: (Boolean, String) -> Unit) {
        executor.execute {
            var ok = false
            var message = "Termux inaccessible. Démarre le moteur LeafBridge dans Termux."
            try {
                val connection = URL("http://127.0.0.1:8765$path").openConnection() as HttpURLConnection
                connection.requestMethod = method
                connection.connectTimeout = 2500
                connection.readTimeout = 2500
                if (body != null) {
                    connection.doOutput = true
                    connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                }
                val code = connection.responseCode
                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                val json = try { JSONObject(response) } catch (_: Exception) { JSONObject() }
                ok = code in 200..299
                message = json.optString("message", if (ok) "Termux connecté" else "Erreur moteur : HTTP $code")
                connection.disconnect()
            } catch (_: Exception) { }
            runOnUiThread { done(ok, message) }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::status.isInitialized && currentPage == "Accueil") refreshStatus()
    }

    override fun onDestroy() {
        webViews.forEach { it.destroy() }
        executor.shutdown()
        super.onDestroy()
    }
}
