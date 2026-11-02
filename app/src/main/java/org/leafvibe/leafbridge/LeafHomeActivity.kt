package org.leafvibe.leafbridge

import android.app.Activity
import android.app.WallpaperManager
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import kotlin.math.abs

class LeafHomeActivity : Activity() {
    private lateinit var prefs: SharedPreferences
    private lateinit var root: LinearLayout
    private lateinit var desktop: LinearLayout
    private lateinit var dock: LinearLayout
    private var page = 0
    private var drawer = false
    private var category = "Tout"
    private var apps: List<ApplicationInfo> = emptyList()
    private var downX = 0f
    private var downY = 0f
    private val wallpaperRequest = 9201
    private val themes = listOf("Leaf Dark", "Leaf Light", "Ocean", "Forest", "Amber")
    private val accent = Color.rgb(190, 242, 115)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("leafvibe_desktop", MODE_PRIVATE)
        page = prefs.getInt("last_page", 1).coerceIn(0, 3)
        renderDesktop()
        Thread {
            val loaded = loadApps()
            runOnUiThread {
                if (!isFinishing) {
                    apps = loaded
                    if (!drawer) renderDesktop()
                }
            }
        }.start()
    }

    private fun loadApps(): List<ApplicationInfo> = try {
        packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            .asSequence()
            .filter { it.packageName != packageName }
            .filter { packageManager.getLaunchIntentForPackage(it.packageName) != null }
            .sortedBy { packageManager.getApplicationLabel(it).toString().lowercase() }
            .toList()
    } catch (_: Exception) {
        emptyList()
    }

    private fun bg(): Int = when (prefs.getString("theme", "Leaf Dark")) {
        "Leaf Light" -> Color.rgb(238, 243, 238)
        "Ocean" -> Color.rgb(10, 35, 55)
        "Forest" -> Color.rgb(12, 43, 35)
        "Amber" -> Color.rgb(49, 34, 20)
        else -> Color.rgb(10, 18, 25)
    }
    private fun panelColor(): Int = when (prefs.getString("theme", "Leaf Dark")) {
        "Leaf Light" -> Color.rgb(255, 255, 255)
        "Ocean" -> Color.rgb(23, 58, 78)
        "Forest" -> Color.rgb(27, 61, 49)
        "Amber" -> Color.rgb(70, 49, 31)
        else -> Color.rgb(24, 39, 49)
    }
    private fun fg(): Int = if (prefs.getString("theme", "Leaf Dark") == "Leaf Light") Color.rgb(25, 40, 34) else Color.WHITE
    private fun muted(): Int = if (prefs.getString("theme", "Leaf Dark") == "Leaf Light") Color.DKGRAY else Color.rgb(169, 188, 198)
    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
    private fun shape(color: Int, radius: Int = 18) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat()
    }
    private fun text(value: String, size: Float, color: Int = fg(), bold: Boolean = false) =
        TextView(this).apply {
            text = value; textSize = size; setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER_VERTICAL
        }
    private fun button(value: String, action: () -> Unit) = TextView(this).apply {
        text = value; textSize = 13f; setTextColor(fg()); gravity = Gravity.CENTER
        setPadding(dp(12), dp(10), dp(12), dp(10)); background = shape(panelColor(), 14)
        setOnClickListener { action() }
    }
    private fun appName(info: ApplicationInfo) = packageManager.getApplicationLabel(info).toString()
    private fun appIcon(info: ApplicationInfo) = try { packageManager.getApplicationIcon(info) } catch (_: Exception) { null }

    private fun renderDesktop() {
        drawer = false
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(24), dp(16), dp(12)); setBackgroundColor(bg()) }
        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val brand = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        brand.addView(text("LEAFVIBE", 22f, fg(), true))
        brand.addView(text("ANDROID DESKTOP  •  ESPACE ${page + 1}/4", 10f, muted(), true))
        top.addView(brand, LinearLayout.LayoutParams(0, -2, 1f))
        top.addView(button("LeafBridge") { startActivity(Intent(this, MainActivity::class.java)) })
        top.addView(button("⌕ Apps", ::renderDrawer))
        top.addView(button("⚙", ::renderSettings))
        root.addView(top)
        val webSearch = EditText(this).apply {
            hint = "Rechercher sur le Web…"
            setSingleLine(true)
            textSize = 14f
            setTextColor(fg())
            setHintTextColor(muted())
            background = shape(panelColor(), 16)
            setPadding(dp(14), dp(8), dp(14), dp(8))
            setOnEditorActionListener { field, _, _ ->
                val query = field.text.toString().trim()
                if (query.isNotEmpty()) {
                    val intent = Intent(Intent.ACTION_WEB_SEARCH).putExtra(android.app.SearchManager.QUERY, query)
                    try { startActivity(intent) } catch (_: Exception) {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + Uri.encode(query))))
                    }
                }
                true
            }
        }
        root.addView(webSearch, LinearLayout.LayoutParams(-1, dp(48)).apply { topMargin = dp(10); bottomMargin = dp(6) })
        val pages = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        for (p in 0..3) pages.addView(buildPage(p), LinearLayout.LayoutParams(resources.displayMetrics.widthPixels - dp(32), -1))
        val horizontal = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(pages)
            post { smoothScrollTo(page * (resources.displayMetrics.widthPixels - dp(32)), 0) }
            setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN) { downX = event.x; downY = event.y }
                if (event.action == MotionEvent.ACTION_UP) {
                    val dx = event.x - downX
                    val dy = event.y - downY
                    if (dy < -dp(65) && abs(dy) > abs(dx)) {
                        renderDrawer()
                        return@setOnTouchListener true
                    }
                    if (abs(dx) > dp(55)) {
                        page = (page + if (dx < 0) 1 else -1).coerceIn(0, 3)
                        prefs.edit().putInt("last_page", page).apply()
                        smoothScrollTo(page * (resources.displayMetrics.widthPixels - dp(32)), 0)
                        updatePageIndicator()
                    }
                }
                false
            }
        }
        root.addView(horizontal, LinearLayout.LayoutParams(-1, 0, 1f))
        val indicator = LinearLayout(this).apply { gravity = Gravity.CENTER; tag = "page_indicator" }
        for (p in 0..3) indicator.addView(text(if (p == page) "●" else "○", 13f, accent).apply {
            setPadding(dp(8), dp(4), dp(8), dp(4))
            setOnClickListener { page = p; prefs.edit().putInt("last_page", page).apply(); horizontal.smoothScrollTo(page * (resources.displayMetrics.widthPixels - dp(32)), 0); updatePageIndicator() }
        })
        root.addView(indicator)
        root.addView(button("▣  Fenêtres Android  ·  multitâche") { Toast.makeText(this, "Balaye vers le haut et maintiens pour afficher les applications récentes Android.", Toast.LENGTH_LONG).show() })
        val dockPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; background = shape(panelColor(), 22)
            setPadding(dp(10), dp(8), dp(10), dp(8))
        }
        dockPanel.addView(text("DOCK  •  FAVORIS", 9f, muted(), true))
        dock = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        dockPanel.addView(dock)
        root.addView(dockPanel)
        setContentView(root)
        renderDock()
    }

    private fun buildPage(index: Int): View {
        val pageView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(4), dp(18), dp(4), dp(12))
        }
        fun card(label: String, detail: String, action: () -> Unit) {
            val item = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = shape(panelColor(), 18)
                setPadding(dp(15), dp(13), dp(15), dp(13))
                setOnClickListener { action() }
            }
            item.addView(text(label, 17f, fg(), true))
            item.addView(text(detail, 12f, muted()))
            pageView.addView(item, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(9) })
        }
        when (index) {
            0 -> {
                pageView.addView(text("Privé & professionnel", 27f, fg(), true))
                pageView.addView(text("Deux espaces distincts, accessibles depuis le côté gauche.", 12f, muted()))
                card("🔒  Vie privée", "Photos, fichiers personnels, communication et agenda privé") {
                    category = "Médias"; renderDrawer()
                }
                card("💼  Vie professionnelle", "Documents, projets, bureautique et outils de travail") {
                    category = "Outils"; renderDrawer()
                }
                card("Ouvrir les projets LeafVibe", "Retrouver le centre de contrôle et les modules") {
                    startActivity(Intent(this, MainActivity::class.java))
                }
            }
            1 -> {
                pageView.addView(text("LEAFVIBE", 30f, fg(), true))
                pageView.addView(text("TON ESPACE PERSONNEL", 12f, accent, true))
                pageView.addView(text("Centre de périphériques", 21f, fg(), true).apply { setPadding(0, dp(12), 0, dp(8)) })
                card("Modes de périphérique", "Téléphone • ordinateur • montre • clé USB") { renderDeviceModes() }
                card("Partage & transferts", "Bluetooth, Wi-Fi, partage Android et périphériques USB") { renderSharing() }
                card("Bibliothèque d'extensions", "Ouvrir le catalogue et gérer les fiches d'outils") {
                    startActivity(Intent(this, ExtensionLibraryActivity::class.java))
                }
                card("Réglages LeafVibe", "Thèmes, fond d'écran et préférences du bureau") { renderSettings() }
                card("Centre de contrôle LeafBridge", "Automatisations, musique, journal et modules") {
                    startActivity(Intent(this, MainActivity::class.java))
                }
            }
            2 -> {
                pageView.addView(text("Organisation", 28f, fg(), true))
                pageView.addView(text("Le côté droit : planifier, suivre et construire tes habitudes.", 12f, muted()))
                card("Planning & tâches", "Agenda, listes et organisation quotidienne") {
                    try { startActivity(Intent(Intent.ACTION_MAIN).addCategory("android.intent.category.APP_CALENDAR")) }
                    catch (_: Exception) { category = "Outils"; renderDrawer() }
                }
                card("Routines", "Automatisations et contrôles périodiques LeafBridge") {
                    startActivity(Intent(this, MainActivity::class.java))
                }
                card("Habitudes", "Accéder aux outils de suivi et de régularité") { category = "Outils"; renderDrawer() }
                card("Tracker", "Suivi personnel, activité et progression") { category = "Outils"; renderDrawer() }
                card("Applications d'organisation", "Toutes les applications de planification et de suivi") {
                    category = "Outils"; renderDrawer()
                }
            }
            else -> {
                pageView.addView(text("Santé & périphériques", 26f, fg(), true))
                pageView.addView(text("Accès rapide aux appareils et services compatibles.", 12f, muted()))
                card("Samsung Health", "Rechercher et ouvrir Samsung Health si installé") {
                    openMatchingApp("health")
                }
                card("Samsung Wearable", "Rechercher et ouvrir Samsung Wearable si installé") {
                    openMatchingApp("wear")
                }
                card("Téléphone & appareils connectés", "Voir les réglages Bluetooth et les connexions") {
                    openBluetoothSettings()
                }
                card("Clés USB & stockage externe", "Ouvrir le sélecteur Android pour parcourir un support") {
                    openUsbPicker()
                }
            }
        }
        val suggestions = when (index) {
            0 -> apps.filter { appName(it).contains(Regex("photo|camera|gallery|message|mail|office|word|excel", RegexOption.IGNORE_CASE)) }
            1 -> apps.take(8)
            2 -> apps.filter { appName(it).contains(Regex("calendar|agenda|task|todo|note|keep|clock|health", RegexOption.IGNORE_CASE)) }
            else -> apps.filter { appName(it).contains(Regex("health|wear|watch|bluetooth|files", RegexOption.IGNORE_CASE)) }
        }.take(8)
        if (suggestions.isNotEmpty()) {
            pageView.addView(text("APPLICATIONS SUGGÉRÉES", 11f, accent, true).apply {
                setPadding(0, dp(10), 0, dp(6))
            })
            val grid = GridLayout(this).apply { columnCount = 4 }
            suggestions.forEach { info ->
                grid.addView(appTile(info, true), GridLayout.LayoutParams().apply {
                    width = (resources.displayMetrics.widthPixels - dp(64)) / 4
                    height = dp(88)
                    setMargins(dp(2), dp(3), dp(2), dp(3))
                })
            }
            pageView.addView(grid)
        }
        pageView.addView(View(this), LinearLayout.LayoutParams(1, 0, 1f))
        pageView.addView(button("＋ Personnaliser cet espace", ::renderSettings))
        return ScrollView(this).apply { isFillViewport = true; addView(pageView) }
    }

    private fun openMatchingApp(term: String) {
        val match = apps.firstOrNull { appName(it).contains(term, true) }
        if (match != null) launch(match)
        else Toast.makeText(this, "Application non trouvée. Vérifie qu'elle est installée.", Toast.LENGTH_LONG).show()
    }

    private fun renderDeviceModes() {
        val page = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(24), dp(18), dp(16)); setBackgroundColor(bg()) }
        page.addView(text("Modes de périphérique", 25f, fg(), true))
        listOf("Téléphone" to "Interface mobile LeafVibe", "Ordinateur" to "Préparer la continuité avec LeafCore", "Montre" to "Services compatibles Samsung Wear", "Clé USB" to "Stockage et modules portables").forEach { (name, detail) ->
            val item = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; background = shape(panelColor(), 16); setPadding(dp(14), dp(12), dp(14), dp(12)) }
            item.addView(text(name, 18f, fg(), true)); item.addView(text(detail, 12f, muted()))
            page.addView(item, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
        }
        page.addView(button("Réglages Android des appareils") { openBluetoothSettings() })
        page.addView(button("← Retour au bureau") { renderDesktop() })
        setContentView(ScrollView(this).apply { addView(page) })
    }

    private fun renderSharing() {
        val page = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(24), dp(18), dp(16)); setBackgroundColor(bg()) }
        page.addView(text("Partage & transferts", 25f, fg(), true))
        page.addView(text("LeafVibe utilise les panneaux et sélecteurs Android officiels : aucune connexion n'est activée sans ton choix.", 12f, muted()))
        page.addView(button("Bluetooth") { openBluetoothSettings() })
        page.addView(button("Wi-Fi & partage de connexion") {
            try { startActivity(Intent(android.provider.Settings.Panel.ACTION_INTERNET_CONNECTIVITY)) }
            catch (_: Exception) { startActivity(Intent(android.provider.Settings.ACTION_WIFI_SETTINGS)) }
        })
        page.addView(button("Partager un élément…") {
            val send = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, "Partagé depuis LeafVibe") }
            startActivity(Intent.createChooser(send, "Partager avec"))
        })
        page.addView(button("Parcourir une clé USB / un dossier") { openUsbPicker() })
        page.addView(button("← Retour au bureau") { renderDesktop() })
        setContentView(ScrollView(this).apply { addView(page) })
    }

    private fun openBluetoothSettings() {
        try { startActivity(Intent(android.provider.Settings.Panel.ACTION_BLUETOOTH)) }
        catch (_: Exception) { startActivity(Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS)) }
    }

    private fun openUsbPicker() {
        val pick = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        try { startActivity(pick) }
        catch (_: Exception) { Toast.makeText(this, "Sélecteur de stockage indisponible", Toast.LENGTH_SHORT).show() }
    }

    private fun appTile(info: ApplicationInfo, canPin: Boolean): View {
        val tile = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(dp(3), dp(5), dp(3), dp(5))
            setOnClickListener { launch(info) }
            if (canPin) setOnLongClickListener { pin(info); true }
        }
        val icon = ImageView(this).apply {
            setImageDrawable(appIcon(info)); scaleType = ImageView.ScaleType.FIT_CENTER
        }
        tile.addView(icon, LinearLayout.LayoutParams(dp(42), dp(42)))
        tile.addView(text(appName(info), 10f, fg()).apply { gravity = Gravity.CENTER; maxLines = 2; ellipsize = android.text.TextUtils.TruncateAt.END })
        return tile
    }

    private fun launch(info: ApplicationInfo) {
        packageManager.getLaunchIntentForPackage(info.packageName)?.let { startActivity(it) }
    }
    private fun pin(info: ApplicationInfo) {
        val current = prefs.getStringSet("dock", emptySet())?.toMutableSet() ?: mutableSetOf()
        current.add(info.packageName)
        prefs.edit().putStringSet("dock", current).apply()
        renderDock()
        Toast.makeText(this, "${appName(info)} ajouté au dock", Toast.LENGTH_SHORT).show()
    }
    private fun renderDock() {
        dock.removeAllViews()
        val pins = prefs.getStringSet("dock", emptySet()) ?: emptySet()
        val selected = pins.mapNotNull { p -> apps.firstOrNull { it.packageName == p } }.take(5)
        selected.forEach { info ->
            val tile = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setOnClickListener { launch(info) } }
            tile.addView(ImageView(this).apply { setImageDrawable(appIcon(info)) }, LinearLayout.LayoutParams(dp(34), dp(34)))
            tile.addView(text(appName(info), 9f, fg()).apply { gravity = Gravity.CENTER; maxLines = 1 })
            dock.addView(tile, LinearLayout.LayoutParams(0, dp(55), 1f))
        }
        dock.addView(button("＋", ::renderDrawer), LinearLayout.LayoutParams(dp(48), dp(48)))
    }

    private fun renderDrawer() {
        drawer = true
        val pageView = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(24), dp(18), dp(12)); setBackgroundColor(bg()) }
        val head = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        head.addView(text("Applications", 24f, fg(), true), LinearLayout.LayoutParams(0, -2, 1f))
        head.addView(button("← Bureau") { renderDesktop() })
        pageView.addView(head)
        val categories = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf("Tout", "Privé", "Pro", "Organisation", "Communication", "Médias", "Santé", "Outils").forEach { cat ->
            categories.addView(button(if (category == cat) "● $cat" else cat) { category = cat; renderDrawer() })
        }
        pageView.addView(HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(categories)
        })
        val search = EditText(this).apply {
            hint = "Rechercher une application"; setTextColor(fg()); setHintTextColor(muted())
            background = shape(panelColor(), 14); setPadding(dp(14), dp(8), dp(14), dp(8))
        }
        pageView.addView(search, LinearLayout.LayoutParams(-1, dp(50)).apply { topMargin = dp(12); bottomMargin = dp(10) })
        val grid = GridLayout(this).apply { columnCount = 4 }
        val scroll = ScrollView(this).apply { addView(grid) }
        fun fill(filter: String) {
            grid.removeAllViews()
            val filtered = apps.filter {
                val name = appName(it)
                val matchesCategory = when(category) {
                    "Communication" -> name.contains(Regex("message|mail|phone|contact|whats|telegram|signal", RegexOption.IGNORE_CASE))
                    "Médias" -> name.contains(Regex("music|spotify|shazam|photo|camera|video|youtube", RegexOption.IGNORE_CASE))
                    "Santé" -> name.contains(Regex("health|wear|fit|watch|fitness", RegexOption.IGNORE_CASE))
                    "Organisation" -> name.contains(Regex("calendar|agenda|task|todo|note|keep|clock|tracker", RegexOption.IGNORE_CASE))
                    "Privé" -> name.contains(Regex("photo|camera|gallery|message|phone|health", RegexOption.IGNORE_CASE))
                    "Pro" -> name.contains(Regex("office|word|excel|mail|drive|teams|github|termux", RegexOption.IGNORE_CASE))
                    "Outils" -> name.contains(Regex("file|drive|settings|termux|calculator|clock|github", RegexOption.IGNORE_CASE))
                    else -> true
                }
                matchesCategory && name.contains(filter, true)
            }
            filtered.forEach { info ->
                grid.addView(appTile(info, false), GridLayout.LayoutParams().apply {
                    width = (resources.displayMetrics.widthPixels - dp(48)) / 4
                    height = dp(92); setMargins(dp(1), dp(4), dp(1), dp(4))
                })
            }
        }
        fill("")
        search.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { fill(s?.toString().orEmpty()) }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
        pageView.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        pageView.addView(button("Thèmes et fonds d'écran", ::renderSettings))
        setContentView(pageView)
    }

    private fun renderSettings() {
        val pageView = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(24), dp(18), dp(16)); setBackgroundColor(bg()) }
        pageView.addView(text("Personnaliser LeafVibe", 24f, fg(), true))
        pageView.addView(text("Thèmes", 16f, accent, true))
        themes.forEach { theme ->
            pageView.addView(button(if (prefs.getString("theme", "Leaf Dark") == theme) "✓  $theme" else theme) {
                prefs.edit().putString("theme", theme).apply(); renderSettings()
            }, LinearLayout.LayoutParams(-1, dp(48)).apply { topMargin = dp(6) })
        }
        pageView.addView(text("Fond d'écran", 16f, accent, true).apply { setPadding(0, dp(18), 0, dp(8)) })
        pageView.addView(button("Choisir une image dans mes fichiers") {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply { type = "image/*"; addCategory(Intent.CATEGORY_OPENABLE) }
            startActivityForResult(intent, wallpaperRequest)
        })
        pageView.addView(text("Le fond d'écran système Android sera modifié uniquement si tu confirmes ton choix.", 11f, muted()))
        pageView.addView(text("Bureaux", 16f, accent, true).apply { setPadding(0, dp(18), 0, dp(8)) })
        pageView.addView(button("Retour au bureau 1") { page = 0; prefs.edit().putInt("last_page", page).apply(); renderDesktop() })
        pageView.addView(button("Retour au bureau 2") { page = 1; prefs.edit().putInt("last_page", page).apply(); renderDesktop() })
        pageView.addView(button("Retour au bureau 3") { page = 2; prefs.edit().putInt("last_page", page).apply(); renderDesktop() })
        pageView.addView(button("Retour au bureau 4") { page = 3; prefs.edit().putInt("last_page", page).apply(); renderDesktop() })
        pageView.addView(View(this), LinearLayout.LayoutParams(1, 0, 1f))
        pageView.addView(button("← Retour") { renderDesktop() })
        setContentView(ScrollView(this).apply { addView(pageView) })
    }

    @Deprecated("Uses the Android document picker")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == wallpaperRequest && resultCode == RESULT_OK) {
            val uri: Uri = data?.data ?: return
            try {
                contentResolver.openInputStream(uri).use { stream ->
                    val bitmap = android.graphics.BitmapFactory.decodeStream(stream)
                        ?: throw IllegalArgumentException("Image illisible")
                    WallpaperManager.getInstance(this).setBitmap(bitmap)
                    Toast.makeText(this, "Fond d'écran appliqué", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, e.message ?: "Impossible d'appliquer le fond", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun updatePageIndicator() {
        val indicator = root.findViewWithTag<LinearLayout>("page_indicator") ?: return
        indicator.removeAllViews()
        for (p in 0..3) indicator.addView(text(if (p == page) "●" else "○", 13f, accent).apply {
            setPadding(dp(8), dp(4), dp(8), dp(4))
            setOnClickListener { this@LeafHomeActivity.page = p; prefs.edit().putInt("last_page", p).apply(); renderDesktop() }
        })
    }

    @Deprecated("Back returns to the desktop")
    override fun onBackPressed() {
        if (drawer) renderDesktop() else super.onBackPressed()
    }
}
