package org.leafvibe.leafbridge

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.Executors
import kotlin.math.abs

class MainActivity : Activity() {
    private val executor = Executors.newSingleThreadExecutor()
    private val prefs by lazy { getSharedPreferences("leafbridge", MODE_PRIVATE) }
    private lateinit var content: LinearLayout
    private lateinit var status: TextView
    private lateinit var spaceLabel: TextView
    private lateinit var addressBar: EditText
    private lateinit var webHost: LinearLayout
    private val webViews = mutableListOf<WebView>()
    private var activeTab = 0
    private var currentPage = "Accueil"
    private var space = "Personnel"
    private var selectedBrowser: String
        get() = prefs.getString("browser_package", "") ?: ""
        set(value) { prefs.edit().putString("browser_package", value).apply() }
    private val green = Color.rgb(34, 197, 94)
    private val navy = Color.rgb(15, 27, 43)
    private val panel = Color.rgb(25, 43, 59)
    private val light = Color.rgb(229, 238, 245)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildShell()
        showHome()
        handleSpotifyCallback(intent)
    }

    private fun buildShell() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(navy) }
        val header = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(18, 14, 18, 10); setBackgroundColor(Color.rgb(20, 35, 51)) }
        header.addView(TextView(this).apply { text = "LEAFBRIDGE  •  LEAFVIBE"; textSize = 19f; setTextColor(Color.WHITE); setTypeface(null, 1) })
        spaceLabel = TextView(this).apply { text = "Espace $space  ·  balayez ← → pour changer"; textSize = 12f; setTextColor(green); setPadding(0, 4, 0, 0) }
        header.addView(spaceLabel)
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(14, 12, 14, 14) }
        val scroll = ScrollView(this).apply { isFillViewport = true; addView(content) }
        val nav = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER; setBackgroundColor(Color.rgb(20, 35, 51)) }
        listOf("Accueil", "Auto", "Web", "Apps", "Comptes", "Réglages").forEach { page ->
            nav.addView(Button(this).apply {
                text = page; textSize = 9f; isAllCaps = false; setTextColor(light)
                setBackgroundColor(Color.TRANSPARENT); setPadding(1, 8, 1, 8)
                setOnClickListener { navigate(page) }
            }, LinearLayout.LayoutParams(0, -2, 1f))
        }
        root.addView(header); root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f)); root.addView(nav)
        setContentView(root)
    }

    private fun navigate(page: String) {
        currentPage = page; content.removeAllViews()
        when (page) {
            "Accueil" -> showHome()
            "Auto" -> showAutomations()
            "Web" -> showBrowser()
            "Apps" -> showApps()
            "Comptes" -> showConnections()
            else -> showSettings()
        }
    }

    private fun heading(s: String) = TextView(this).apply { text = s; textSize = 23f; setTextColor(Color.WHITE); setTypeface(null, 1); setPadding(0, 8, 0, 12) }
    private fun para(s: String) = TextView(this).apply { text = s; textSize = 14f; setTextColor(light); setPadding(0, 4, 0, 9) }
    private fun action(s: String, f: () -> Unit) = Button(this).apply { text = s; isAllCaps = false; setTextColor(Color.WHITE); setBackgroundColor(Color.rgb(37, 75, 78)); setOnClickListener { f() } }
    private fun addCard(title: String, description: String, button: String? = null, f: (() -> Unit)? = null) {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(14, 12, 14, 12); setBackgroundColor(panel) }
        box.addView(TextView(this).apply { text = title; textSize = 17f; setTextColor(Color.WHITE); setTypeface(null, 1) })
        box.addView(para(description))
        if (button != null && f != null) box.addView(action(button, f))
        content.addView(box, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 10 })
    }

    private fun showHome() {
        spaceLabel.text = "Espace $space  ·  balayez ← → pour changer"
        content.addView(heading("Tableau de bord"))
        content.addView(para("Espace $space · glissez horizontalement pour passer de Personnel à Professionnel."))
        status = para("Vérification du moteur Termux…"); content.addView(status)
        addCard("Lanceur Android", "LeafBridge peut être utilisé comme application ET comme écran d’accueil Android.", "Choisir LeafBridge comme lanceur") { openHomeSettings() }
        addCard("Shazam → Spotify", "Automatisation en arrière-plan : les notifications Shazam sont capturées sans ouvrir leur écran et transmises au moteur local.", "Configurer l’automatisation") { navigate("Auto") }
        addCard("Bibliothèque web", "Enregistrez des pages, créez des catégories, ajoutez des favoris et choisissez votre navigateur.", "Ouvrir la bibliothèque") { navigate("Web") }
        addCard("Applications", "Retrouvez les applications installées, ouvrez-les et consultez leurs widgets et liens associés.", "Voir les applications") { navigate("Apps") }
        addCard("Espaces", "Personnel et Professionnel. Balayez l’écran d’accueil vers la gauche ou la droite pour basculer.", "Basculer d’espace") {
            space = if (space == "Personnel") "Professionnel" else "Personnel"; showHome()
        }
        content.addView(heading("Raccourcis de l’espace $space"))
        val widgets = widgetList(); var hasSpaceWidget = false
        for (i in 0 until widgets.length()) {
            val item = widgets.optJSONObject(i) ?: continue
            if (item.optString("space") == space && item.optString("package").isBlank()) {
                hasSpaceWidget = true
                addCard(item.optString("title"), item.optString("target"), "Ouvrir") {
                    val target = item.optString("target")
                    if (target.startsWith("http://") || target.startsWith("https://")) openInSelectedBrowser(target)
                    else Toast.makeText(this, "Ce raccourci ne contient pas de destination.", Toast.LENGTH_SHORT).show()
                }
            }
        }
        if (!hasSpaceWidget) content.addView(para("Aucun raccourci pour cet espace. Créez-en dans Applications."))
        refreshStatus()
    }

    private fun openHomeSettings() {
        try { startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) }
        catch (_: Exception) {
            try { startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)) }
            catch (_: Exception) { Toast.makeText(this, "Choisissez LeafBridge dans les applications par défaut Android.", Toast.LENGTH_LONG).show() }
        }
    }

    private fun showAutomations() {
        content.addView(heading("Dock d’automatisation"))
        content.addView(para("Schémas de flux · modules indépendants · exécution locale. Les modules externes attendent leur autorisation."))
        flowDiagram()
        addCard("Shazam → Spotify", "1. Détection silencieuse → 2. Extraction titre/artiste → 3. Recherche Spotify → 4. Ajout à la playlist. Aucun écran de notification ne doit s’ouvrir.", "Configurer Spotify / playlist") { spotifySetupDialog() }
        addCard("État Shazam", "L’écoute des notifications est un service Android en arrière-plan.", "Autoriser l’accès Shazam") { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
        listOf("shazam_spotify" to "Shazam vers Spotify", "github_sync" to "Synchronisation GitHub", "music_sort" to "Classement musical", "routine" to "Routine LeafBridge").forEach { (id, label) ->
            val enabled = prefs.getBoolean("auto_$id", id == "shazam_spotify")
            addCard(label, if (enabled) "Automatisation activée." else "Automatisation en pause.", if (enabled) "Désactiver" else "Activer") {
                val next = !enabled; prefs.edit().putBoolean("auto_$id", next).apply()
                if (id == "shazam_spotify") requestEngine("POST", "/automation", JSONObject().put("id", id).put("command", if (next) "start" else "stop").toString()) { _, _ -> }
                navigate("Auto")
            }
        }
        content.addView(action("Tester le moteur Termux") { checkTermux() })
    }

    private fun flowDiagram() {
        val steps = listOf("ÉVÉNEMENT\nShazam", "TRAITEMENT\nTitre + artiste", "RECHERCHE\nSpotify", "ACTION\nPlaylist")
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER; setPadding(0, 8, 0, 14) }
        steps.forEachIndexed { i, s ->
            val node = TextView(this).apply {
                text = s; textSize = 10f; gravity = Gravity.CENTER; setTextColor(Color.WHITE); setPadding(5, 12, 5, 12)
                setBackgroundColor(if (i == 3) Color.rgb(31, 105, 70) else Color.rgb(36, 64, 82))
            }
            row.addView(node, LinearLayout.LayoutParams(0, -2, 1f))
            if (i < 3) row.addView(TextView(this).apply { text = "›"; textSize = 20f; setTextColor(green) })
        }
        content.addView(row)
    }

    private fun spotifySetupDialog() {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(20, 10, 20, 0) }
        val client = EditText(this).apply { hint = "Spotify Client ID"; setSingleLine(true); setText(prefs.getString("spotify_client_id", "")) }
        val playlist = EditText(this).apply { hint = "ID de la playlist Spotify"; setSingleLine(true); setText(prefs.getString("spotify_playlist", "3cPjGCqbeDaIH95Vvofjwm")) }
        box.addView(client); box.addView(playlist)
        android.app.AlertDialog.Builder(this).setTitle("Configuration Spotify")
            .setMessage("Client ID Spotify Developer + identifiant de playlist. Ne saisissez jamais votre mot de passe ici. Redirect URI à déclarer : leafbridge://spotify-callback")
            .setView(box).setNegativeButton("Annuler", null)
            .setNeutralButton("Connecter Spotify") { _, _ ->
                prefs.edit().putString("spotify_client_id", client.text.toString().trim()).putString("spotify_playlist", playlist.text.toString().trim()).apply()
                startSpotifyOAuth()
            }.setPositiveButton("Enregistrer") { _, _ ->
                prefs.edit().putString("spotify_client_id", client.text.toString().trim()).putString("spotify_playlist", playlist.text.toString().trim()).apply()
                Toast.makeText(this, "Paramètres Spotify enregistrés. Appuyez sur Connecter Spotify pour autoriser l’accès.", Toast.LENGTH_LONG).show()
            }.show()
    }

    private fun showBrowser() {
        content.addView(heading("Recherche et bibliothèque web"))
        val controls = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        addressBar = EditText(this).apply { hint = "Adresse ou recherche"; setSingleLine(true); setTextColor(Color.WHITE); setHintTextColor(Color.LTGRAY); setText("https://www.google.com") }
        controls.addView(addressBar, LinearLayout.LayoutParams(0, -2, 1f)); controls.addView(action("Aller") { loadAddress() }); content.addView(controls)
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row.addView(action("Ajouter à la bibliothèque") { saveCurrentPage() }); row.addView(action("Choisir navigateur") { chooseBrowser() }); content.addView(row)
        val tabs = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        tabs.addView(action("+") { addTab("https://www.google.com") })
        tabs.addView(action("Suivant") { if (webViews.isNotEmpty()) { activeTab = (activeTab + 1) % webViews.size; displayActiveTab() } })
        tabs.addView(action("Fermer") { closeActiveTab() }); tabs.addView(action("↻") { if (webViews.isNotEmpty()) webViews[activeTab].reload() }); content.addView(tabs)
        webHost = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE) }
        content.addView(webHost, LinearLayout.LayoutParams(-1, (resources.displayMetrics.density * 390).toInt()))
        content.addView(heading("Ma bibliothèque")); showLibraryItems()
        if (webViews.isEmpty()) addTab("https://www.google.com") else displayActiveTab()
    }

    private fun addTab(url: String) {
        val w = WebView(this)
        w.settings.javaScriptEnabled = true; w.settings.domStorageEnabled = true
        w.settings.allowFileAccess = false; w.settings.allowContentAccess = false
        w.settings.javaScriptCanOpenWindowsAutomatically = false; w.settings.setSupportMultipleWindows(false); w.settings.setSafeBrowsingEnabled(true)
        w.webViewClient = object : WebViewClient() {
            override fun onPageFinished(v: WebView, url: String) {
                if (webViews.getOrNull(activeTab) === v && ::addressBar.isInitialized) addressBar.setText(url)
            }
        }
        w.webChromeClient = WebChromeClient(); webViews.add(w); activeTab = webViews.lastIndex; w.loadUrl(url); displayActiveTab()
    }
    private fun displayActiveTab() {
        if (!::webHost.isInitialized || webViews.isEmpty()) return
        activeTab = activeTab.coerceIn(0, webViews.lastIndex); webHost.removeAllViews()
        webHost.addView(webViews[activeTab], LinearLayout.LayoutParams(-1, -1))
        if (::addressBar.isInitialized) addressBar.setText(webViews[activeTab].url ?: "")
    }
    private fun closeActiveTab() {
        if (webViews.isEmpty()) return
        val w = webViews.removeAt(activeTab); webHost.removeView(w); w.stopLoading(); w.destroy()
        if (webViews.isEmpty()) addTab("https://www.google.com") else { activeTab = (activeTab - 1).coerceAtLeast(0); displayActiveTab() }
    }
    private fun loadAddress() {
        if (webViews.isEmpty()) addTab("https://www.google.com")
        var target = addressBar.text.toString().trim(); if (target.isBlank()) return
        if (!target.startsWith("http://") && !target.startsWith("https://")) {
            target = if (target.contains(".") && !target.contains(" ")) "https://$target"
            else "https://www.google.com/search?q=" + URLEncoder.encode(target, "UTF-8")
        }
        val u = Uri.parse(target)
        if (u.scheme !in listOf("http", "https") || u.host.isNullOrBlank()) { Toast.makeText(this, "Adresse HTTP/HTTPS uniquement", Toast.LENGTH_SHORT).show(); return }
        webViews[activeTab].loadUrl(target)
    }

    private fun library(): JSONArray = try { JSONArray(prefs.getString("web_library", "[]")) } catch (_: Exception) { JSONArray() }
    private fun saveCurrentPage() {
        val url = if (webViews.isNotEmpty()) webViews[activeTab].url.orEmpty() else addressBar.text.toString()
        if (url.isBlank()) return
        val title = if (webViews.isNotEmpty()) webViews[activeTab].title ?: url else url
        val category = EditText(this).apply { hint = "Catégorie (ex. Travail, Musique, Outils)" }
        android.app.AlertDialog.Builder(this).setTitle("Enregistrer la page").setMessage(title).setView(category)
            .setNegativeButton("Annuler", null).setPositiveButton("Enregistrer") { _, _ ->
                val a = library(); a.put(JSONObject().put("title", title).put("url", url).put("category", category.text.toString().ifBlank { "Général" }).put("favorite", false))
                prefs.edit().putString("web_library", a.toString()).apply(); navigate("Web")
            }.show()
    }
    private fun showLibraryItems(filter: String = "Tous") {
        val a = library()
        if (a.length() == 0) { content.addView(para("Aucune page enregistrée. Utilisez ‘Ajouter à la bibliothèque’ pendant une recherche.")); return }
        val categories = (0 until a.length()).mapNotNull { a.optJSONObject(it)?.optString("category") }.filter { it.isNotBlank() }.distinct().sorted()
        val filters = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        (listOf("Tous", "Favoris") + categories).forEach { cat -> filters.addView(action(cat) { showLibraryItems(cat) }) }
        content.addView(filters)
        for (i in 0 until a.length()) {
            val item = a.optJSONObject(i) ?: continue
            val url = item.optString("url"); val fav = item.optBoolean("favorite"); val category = item.optString("category")
            if (filter == "Favoris" && !fav) continue
            if (filter != "Tous" && filter != "Favoris" && category != filter) continue
            addCard((if (fav) "★ " else "☆ ") + item.optString("title"), "$category · $url", "Ouvrir") { openInSelectedBrowser(url) }
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            row.addView(action(if (fav) "Retirer favori" else "Favori") { item.put("favorite", !fav); prefs.edit().putString("web_library", a.toString()).apply(); navigate("Web") })
            row.addView(action("Supprimer") { a.remove(i); prefs.edit().putString("web_library", a.toString()).apply(); navigate("Web") })
            content.addView(row)
        }
    }
    private fun chooseBrowser() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com"))
        val handlers = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).distinctBy { it.activityInfo.packageName }
        val labels = handlers.map { it.loadLabel(packageManager).toString() }.toTypedArray()
        val pkgs = handlers.map { it.activityInfo.packageName }
        if (labels.isEmpty()) { Toast.makeText(this, "Aucun navigateur détecté", Toast.LENGTH_SHORT).show(); return }
        android.app.AlertDialog.Builder(this).setTitle("Choisir le navigateur").setItems(labels) { _, which ->
            selectedBrowser = pkgs[which]; Toast.makeText(this, "Navigateur enregistré", Toast.LENGTH_SHORT).show()
        }.setNeutralButton("Toujours demander") { _, _ -> selectedBrowser = "" }.show()
    }
    private fun openInSelectedBrowser(url: String) {
        try { val i = Intent(Intent.ACTION_VIEW, Uri.parse(url)); if (selectedBrowser.isNotBlank()) i.setPackage(selectedBrowser); startActivity(i) }
        catch (_: Exception) { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }

    private data class AppEntry(val label: String, val pkg: String, val icon: android.graphics.drawable.Drawable)
    private fun installedApps(): List<AppEntry> {
        val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return packageManager.queryIntentActivities(i, 0).mapNotNull { r ->
            val a = r.activityInfo.applicationInfo
            if (a.packageName == packageName) null else AppEntry(r.loadLabel(packageManager).toString(), a.packageName, r.loadIcon(packageManager))
        }.distinctBy { it.pkg }.sortedBy { it.label.lowercase() }
    }
    private fun showApps() {
        content.addView(heading("Applications"))
        content.addView(para("Ouvrez une application ou consultez sa fiche. Widgets LeafBridge et widgets Android natifs sont présentés séparément."))
        val apps = installedApps(); val search = EditText(this).apply { hint = "Rechercher une application"; setSingleLine(true) }; content.addView(search)
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }; content.addView(list)
        fun populate(q: String = "") {
            list.removeAllViews()
            apps.filter { it.label.contains(q, true) || it.pkg.contains(q, true) }.forEach { app ->
                val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(8, 8, 8, 8); setBackgroundColor(panel) }
                row.addView(ImageView(this).apply { setImageDrawable(app.icon) }, LinearLayout.LayoutParams(48, 48))
                row.addView(TextView(this).apply { text = app.label; setTextColor(Color.WHITE); textSize = 15f; setPadding(12, 0, 0, 0) }, LinearLayout.LayoutParams(0, -2, 1f))
                row.addView(action("Fiche") { showAppDetail(app) }); list.addView(row, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 6 })
            }
        }
        search.addTextChangedListener(SimpleWatcher { populate(it) }); populate()
        content.addView(heading("Widgets LeafBridge")); content.addView(para("Raccourcis personnalisés créés dans LeafBridge."))
        content.addView(action("Créer un widget LeafBridge") { createLeafWidget() })
        content.addView(heading("Widgets Android natifs"))
        val providers = try { AppWidgetManager.getInstance(this).installedProviders } catch (_: Exception) { emptyList() }
        content.addView(para(if (providers.isEmpty()) "Aucun fournisseur de widget détecté." else providers.take(30).joinToString("\n") { it.loadLabel(packageManager).toString() + " · " + it.provider.packageName }))
    }
    private fun showAppDetail(app: AppEntry) {
        content.removeAllViews(); content.addView(action("← Retour aux applications") { navigate("Apps") })
        content.addView(heading(app.label))
        content.addView(action("Ouvrir l’application") {
            val launch = packageManager.getLaunchIntentForPackage(app.pkg)
            if (launch != null) startActivity(launch) else Toast.makeText(this, "Aucune activité de lancement", Toast.LENGTH_SHORT).show()
        })
        content.addView(para("Package : ${app.pkg}"))
        content.addView(heading("Widgets LeafBridge"))
        val a = widgetList(); var found = false
        for (i in 0 until a.length()) {
            val o = a.optJSONObject(i) ?: continue
            if (o.optString("package") == app.pkg) {
                found = true
                addCard(o.optString("title"), o.optString("target"), "Supprimer") { a.remove(i); prefs.edit().putString("leaf_widgets", a.toString()).apply(); showAppDetail(app) }
            }
        }
        if (!found) content.addView(para("Aucun widget LeafBridge créé pour cette application."))
        val native = try { AppWidgetManager.getInstance(this).installedProviders.filter { it.provider.packageName == app.pkg } } catch (_: Exception) { emptyList() }
        content.addView(heading("Widgets Android fournis"))
        content.addView(para(if (native.isEmpty()) "Aucun widget Android natif déclaré par cette application." else native.joinToString("\n") { it.loadLabel(packageManager).toString() }))
        content.addView(action("Créer un raccourci/widget") { createLeafWidget(app) })
        content.addView(heading("Liens associés"))
        val links = linkList(); var any = false
        for (i in 0 until links.length()) {
            val o = links.optJSONObject(i) ?: continue
            if (o.optString("package") == app.pkg) { any = true; addCard(o.optString("title"), o.optString("url"), "Ouvrir") { openInSelectedBrowser(o.optString("url")) } }
        }
        if (!any) content.addView(para("Aucun lien associé."))
        content.addView(action("Ajouter un lien") {
            val e = EditText(this).apply { hint = "https://…" }
            android.app.AlertDialog.Builder(this).setTitle("Lien associé").setView(e).setNegativeButton("Annuler", null)
                .setPositiveButton("Ajouter") { _, _ ->
                    val u = e.text.toString().trim()
                    if (u.startsWith("https://") || u.startsWith("http://")) {
                        val l = linkList(); l.put(JSONObject().put("package", app.pkg).put("title", u).put("url", u))
                        prefs.edit().putString("app_links", l.toString()).apply(); showAppDetail(app)
                    }
                }.show()
        })
    }
    private fun widgetList() = try { JSONArray(prefs.getString("leaf_widgets", "[]")) } catch (_: Exception) { JSONArray() }
    private fun linkList() = try { JSONArray(prefs.getString("app_links", "[]")) } catch (_: Exception) { JSONArray() }
    private fun createLeafWidget(app: AppEntry? = null) {
        val title = EditText(this).apply { hint = "Nom du widget" }
        val target = EditText(this).apply { hint = "URL https://… (facultatif)" }
        val form = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; addView(title); addView(target) }
        android.app.AlertDialog.Builder(this).setTitle("Créer un widget LeafBridge")
            .setMessage("Ce raccourci apparaîtra dans l’espace sélectionné ou dans la fiche de l’application.")
            .setView(form).setNegativeButton("Annuler", null).setPositiveButton("Créer") { _, _ ->
                val a = widgetList()
                a.put(JSONObject().put("package", app?.pkg.orEmpty()).put("space", space).put("title", title.text.toString().ifBlank { "Raccourci" }).put("target", target.text.toString()))
                prefs.edit().putString("leaf_widgets", a.toString()).apply()
                if (app != null) showAppDetail(app) else Toast.makeText(this, "Widget LeafBridge créé", Toast.LENGTH_SHORT).show()
            }.show()
    }

    private fun showConnections() {
        content.addView(heading("Comptes et connexions"))
        content.addView(para("Chaque service est autorisé séparément par son mécanisme officiel. Ne saisissez jamais un mot de passe de service dans LeafBridge."))
        val spotifyReady = !prefs.getString("spotify_refresh_token", "").isNullOrBlank()
        addCard("Spotify", if (spotifyReady) "Compte autorisé · jetons transmis au moteur local." else "Non connecté · nécessaire pour envoyer automatiquement les titres Shazam vers votre playlist.", "Configurer / connecter Spotify") { spotifySetupDialog() }
        addCard("GitHub", "Connexion officielle à préparer pour la gestion des dépôts et synchronisations.", "Ouvrir GitHub") { openInSelectedBrowser("https://github.com/login") }
        addCard("Instagram", "Connexion officielle uniquement pour les fonctions permises par les API Meta.", "Ouvrir Instagram") { openInSelectedBrowser("https://www.instagram.com/accounts/login/") }
        addCard("TikTok", "Connexion officielle uniquement pour les fonctions permises par les API TikTok.", "Ouvrir TikTok") { openInSelectedBrowser("https://www.tiktok.com/login") }
    }
    private fun showSettings() {
        content.addView(heading("Réglages"))
        content.addView(para("Thème Leaf : bleu nuit, vert Leaf et texte clair. Les espaces Personnel et Professionnel sont accessibles par balayage horizontal sur l’accueil."))
        content.addView(action("Choisir LeafBridge comme lanceur par défaut") { openHomeSettings() })
        content.addView(action("Changer d’espace") { space = if (space == "Personnel") "Professionnel" else "Personnel"; navigate("Accueil") })
        content.addView(action("Choisir le navigateur web") { chooseBrowser() })
        content.addView(action("Autoriser les notifications Shazam") { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) })
        content.addView(action("Tester le moteur Termux") { checkTermux() })
        content.addView(para("Moteur local : http://127.0.0.1:8765 · Termux doit rester actif. Les données de bibliothèque et préférences sont conservées localement."))
    }

    private fun refreshStatus() {
        requestEngine("GET", "/status", null) { ok, _ ->
            if (::status.isInitialized && currentPage == "Accueil") status.text = if (ok) "Moteur Termux : connecté" else "Moteur Termux : hors ligne"
        }
    }
    private fun checkTermux() { requestEngine("GET", "/status", null) { _, message -> Toast.makeText(this, message, Toast.LENGTH_LONG).show() } }
    private fun requestEngine(method: String, path: String, body: String?, done: (Boolean, String) -> Unit) {
        executor.execute {
            var ok = false; var message = "Termux inaccessible. Démarrez le moteur LeafBridge."; var c: HttpURLConnection? = null
            try {
                c = URL("http://127.0.0.1:8765$path").openConnection() as HttpURLConnection
                c.requestMethod = method; c.connectTimeout = 2500; c.readTimeout = 2500
                if (body != null) { c.doOutput = true; c.setRequestProperty("Content-Type", "application/json; charset=utf-8"); c.outputStream.use { it.write(body.toByteArray()) } }
                val code = c.responseCode; val stream = if (code in 200..299) c.inputStream else c.errorStream
                val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                val json = try { JSONObject(response) } catch (_: Exception) { JSONObject() }
                ok = code in 200..299; message = json.optString("message", if (ok) "Termux connecté" else "Erreur HTTP $code")
            } catch (e: Exception) { message = "Termux inaccessible : ${e.localizedMessage}" }
            finally { c?.disconnect() }
            runOnUiThread { if (!isFinishing && !isDestroyed) done(ok, message) }
        }
    }

    private fun startSpotifyOAuth() {
        val client = prefs.getString("spotify_client_id", "").orEmpty(); val playlist = prefs.getString("spotify_playlist", "").orEmpty()
        if (client.isBlank() || playlist.isBlank()) { Toast.makeText(this, "Renseignez le Client ID et l’identifiant de playlist.", Toast.LENGTH_LONG).show(); return }
        try {
            val bytes = ByteArray(64); java.security.SecureRandom().nextBytes(bytes)
            val verifier = android.util.Base64.encodeToString(bytes, android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING)
            val digest = java.security.MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII))
            val challenge = android.util.Base64.encodeToString(digest, android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING)
            val state = java.util.UUID.randomUUID().toString()
            prefs.edit().putString("spotify_pkce_verifier", verifier).putString("spotify_oauth_state", state).apply()
            val uri = Uri.parse("https://accounts.spotify.com/authorize").buildUpon()
                .appendQueryParameter("client_id", client).appendQueryParameter("response_type", "code")
                .appendQueryParameter("redirect_uri", "leafbridge://spotify-callback")
                .appendQueryParameter("code_challenge_method", "S256").appendQueryParameter("code_challenge", challenge)
                .appendQueryParameter("scope", "playlist-modify-private playlist-modify-public").appendQueryParameter("state", state).build()
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (e: Exception) { Toast.makeText(this, "Ouverture OAuth impossible : ${e.localizedMessage}", Toast.LENGTH_LONG).show() }
    }
    private fun handleSpotifyCallback(i: Intent?) {
        val uri = i?.data ?: return
        if (uri.scheme != "leafbridge" || uri.host != "spotify-callback") return
        val error = uri.getQueryParameter("error")
        if (error != null) { Toast.makeText(this, "Autorisation Spotify refusée : $error", Toast.LENGTH_LONG).show(); return }
        val returnedState = uri.getQueryParameter("state"); val expectedState = prefs.getString("spotify_oauth_state", null)
        if (returnedState.isNullOrBlank() || returnedState != expectedState) { Toast.makeText(this, "Réponse Spotify refusée : état OAuth invalide.", Toast.LENGTH_LONG).show(); return }
        prefs.edit().remove("spotify_oauth_state").apply()
        val code = uri.getQueryParameter("code") ?: return
        val client = prefs.getString("spotify_client_id", "").orEmpty(); val verifier = prefs.getString("spotify_pkce_verifier", "").orEmpty()
        if (client.isBlank() || verifier.isBlank()) return
        executor.execute {
            var c: HttpURLConnection? = null
            try {
                c = URL("https://accounts.spotify.com/api/token").openConnection() as HttpURLConnection
                c.requestMethod = "POST"; c.connectTimeout = 10000; c.readTimeout = 10000; c.doOutput = true
                c.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                val body = listOf("client_id" to client, "grant_type" to "authorization_code", "code" to code,
                    "redirect_uri" to "leafbridge://spotify-callback", "code_verifier" to verifier)
                    .joinToString("&") { URLEncoder.encode(it.first, "UTF-8") + "=" + URLEncoder.encode(it.second, "UTF-8") }
                c.outputStream.use { it.write(body.toByteArray()) }
                val codeStatus = c.responseCode; val stream = if (codeStatus in 200..299) c.inputStream else c.errorStream
                val json = JSONObject(stream?.bufferedReader()?.use { it.readText() }.orEmpty())
                if (codeStatus !in 200..299) throw IllegalStateException(json.optString("error_description", "OAuth HTTP $codeStatus"))
                val expires = System.currentTimeMillis() / 1000 + json.optLong("expires_in", 3600)
                val config = JSONObject().put("client_id", client).put("playlist_id", prefs.getString("spotify_playlist", ""))
                    .put("access_token", json.optString("access_token")).put("refresh_token", json.optString("refresh_token")).put("expires_at", expires)
                prefs.edit().putString("spotify_refresh_token", json.optString("refresh_token"))
                    .putString("spotify_access_token", json.optString("access_token")).putLong("spotify_expires_at", expires).apply()
                val result = URL("http://127.0.0.1:8765/spotify/config").openConnection() as HttpURLConnection
                try {
                    result.requestMethod = "POST"; result.connectTimeout = 3000; result.readTimeout = 3000
                    result.doOutput = true; result.setRequestProperty("Content-Type", "application/json")
                    result.outputStream.use { it.write(config.toString().toByteArray()) }
                    if (result.responseCode !in 200..299) throw IllegalStateException("Moteur Termux HTTP ${result.responseCode}")
                } finally { result.disconnect() }
                runOnUiThread { Toast.makeText(this, "Spotify autorisé et transmis au moteur LeafBridge.", Toast.LENGTH_LONG).show() }
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this, "Spotify autorisé, mais transfert au moteur impossible : ${e.localizedMessage}. Vérifiez que Termux est démarré.", Toast.LENGTH_LONG).show() }
            } finally { c?.disconnect() }
        }
    }

    private var swipeStartX = 0f; private var swipeStartY = 0f
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) { swipeStartX = ev.x; swipeStartY = ev.y }
        if (ev.action == MotionEvent.ACTION_UP && currentPage == "Accueil") {
            val dx = ev.x - swipeStartX; val dy = ev.y - swipeStartY
            if (abs(dx) > 120 && abs(dx) > abs(dy) * 1.4f) { space = if (dx < 0) "Professionnel" else "Personnel"; showHome(); return true }
        }
        return super.dispatchTouchEvent(ev)
    }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); handleSpotifyCallback(intent) }
    override fun onResume() { super.onResume(); if (::status.isInitialized && currentPage == "Accueil") refreshStatus() }
    override fun onDestroy() {
        webViews.forEach { (it.parent as? ViewGroup)?.removeView(it); it.stopLoading(); it.destroy() }
        webViews.clear(); executor.shutdown(); super.onDestroy()
    }
    private class SimpleWatcher(val changed: (String) -> Unit) : android.text.TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { changed(s?.toString().orEmpty()) }
        override fun afterTextChanged(s: android.text.Editable?) {}
    }
}