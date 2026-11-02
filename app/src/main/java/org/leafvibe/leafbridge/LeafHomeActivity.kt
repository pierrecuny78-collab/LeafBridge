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
    private val wallpaperRequest = 9201
    private val themes = listOf("Leaf Dark", "Leaf Light", "Ocean", "Forest", "Amber")
    private val accent = Color.rgb(190, 242, 115)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("leafvibe_desktop", MODE_PRIVATE)
        loadApps()
        renderDesktop()
    }

    private fun loadApps() {
        apps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { packageManager.getLaunchIntentForPackage(it.packageName) != null && it.packageName != packageName }
            .sortedBy { packageManager.getApplicationLabel(it).toString().lowercase() }
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
        brand.addView(text("ANDROID DESKTOP  •  BUREAU ${page + 1}/3", 10f, muted(), true))
        top.addView(brand, LinearLayout.LayoutParams(0, -2, 1f))
        top.addView(button("LeafBridge") { startActivity(Intent(this, MainActivity::class.java)) })
        top.addView(button("⌕ Apps", ::renderDrawer))
        top.addView(button("⚙", ::renderSettings))
        root.addView(top)
        val pages = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        for (p in 0..3) pages.addView(buildPage(p), LinearLayout.LayoutParams(resources.displayMetrics.widthPixels - dp(32), -1))
        val horizontal = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(pages)
            post { smoothScrollTo(page * (resources.displayMetrics.widthPixels - dp(32)), 0) }
            setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN) downX = event.x
                if (event.action == MotionEvent.ACTION_UP) {
                    val dx = event.x - downX
                    if (abs(dx) > dp(55)) {
                        page = (page + if (dx < 0) 1 else -1).coerceIn(0, 3)
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
            setOnClickListener { page = p; horizontal.smoothScrollTo(page * (resources.displayMetrics.widthPixels - dp(32)), 0); updatePageIndicator() }
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
        val title = when (index) {
            0 -> "Organisation"
            1 -> "Bureau principal"
            2 -> "Vie privée"
            else -> "Vie professionnelle"
        }
        val subtitle = when (index) {
            0 -> "Planifier • classer • suivre • retrouver"
            1 -> "Ton point de départ LeafVibe"
            2 -> "Espace personnel séparé du travail"
            else -> "Espace de travail et projets"
        }
        pageView.addView(text(title, 21f, fg(), true))
        pageView.addView(text(subtitle, 11f, muted()))
        pageView.addView(View(this), LinearLayout.LayoutParams(1, dp(8)))

        fun actionCard(label: String, detail: String, action: () -> Unit) {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = shape(panelColor(), 16)
                setPadding(dp(13), dp(9), dp(13), dp(9))
                setOnClickListener { action() }
            }
            card.addView(text(label, 14f, fg(), true))
            card.addView(text(detail, 10f, muted()))
            pageView.addView(card, LinearLayout.LayoutParams(-1, -2).apply {
                bottomMargin = dp(7)
            })
        }

        when (index) {
            0 -> {
                actionCard("✓  Tâches et suivi", "Ouvrir LeafBridge et ses modules de suivi") {
                    startActivity(Intent(this, MainActivity::class.java))
                }
                actionCard("▤  Documents et classement", "Créer ou importer un document dans un emplacement choisi") {
                    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TITLE, "Nouvelle-note.txt")
                    }
                    try { startActivity(intent) } catch (_: Exception) {
                        Toast.makeText(this, "Gestionnaire de fichiers indisponible", Toast.LENGTH_SHORT).show()
                    }
                }
                actionCard("▦  Planning", "Ouvrir le calendrier Android") {
                    try { startActivity(Intent(Intent.ACTION_MAIN).addCategory("android.intent.category.APP_CALENDAR")) }
                    catch (_: Exception) { Toast.makeText(this, "Aucun calendrier disponible", Toast.LENGTH_SHORT).show() }
                }
                actionCard("⌕  Applications d'organisation", "Fichiers, notes, agenda, tâches et outils") {
                    category = "Outils"; renderDrawer()
                }
            }
            1 -> {
                actionCard("LeafBridge  •  Centre de contrôle", "Automatisations, bibliothèque, Shazam et journal") {
                    startActivity(Intent(this, MainActivity::class.java))
                }
                actionCard("Applications", "Rechercher toutes les applications installées") { renderDrawer() }
                actionCard("Personnaliser", "Thèmes, fond d'écran et bureaux") { renderSettings() }
                actionCard("Ajouter un raccourci", "Choisir une application dans le tiroir puis la maintenir pour l'épingler au dock") {
                    renderDrawer()
                }
            }
            2 -> {
                actionCard("Photos et fichiers personnels", "Ouvrir le sélecteur Android sans donner accès global aux fichiers") {
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE); type = "*/*"
                    }
                    startActivity(intent)
                }
                actionCard("Agenda personnel", "Accéder à ton calendrier personnel") {
                    try { startActivity(Intent(Intent.ACTION_MAIN).addCategory("android.intent.category.APP_CALENDAR")) }
                    catch (_: Exception) { renderDrawer() }
                }
                actionCard("Communication privée", "Messages, téléphone et contacts installés") {
                    category = "Communication"; renderDrawer()
                }
                actionCard("Médias", "Musique, photos, vidéo et Shazam") {
                    category = "Médias"; renderDrawer()
                }
            }
            else -> {
                actionCard("Projets LeafVibe", "Ouvrir le centre LeafBridge et les modules") {
                    startActivity(Intent(this, MainActivity::class.java))
                }
                actionCard("Créer un fichier de travail", "Choisir le dossier et le nom du document") {
                    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE); type = "*/*"
                        putExtra(Intent.EXTRA_TITLE, "Nouveau-document")
                    }
                    startActivity(intent)
                }
                actionCard("Outils professionnels", "Bureautique, développement, fichiers et communication") {
                    category = "Outils"; renderDrawer()
                }
                actionCard("Communication professionnelle", "Ouvrir les applications de messagerie disponibles") {
                    category = "Communication"; renderDrawer()
                }
            }
        }

        val suggestions = when (index) {
            0 -> apps.filter { appName(it).contains(Regex("calendar|agenda|task|todo|note|keep|file|drive", RegexOption.IGNORE_CASE)) }
            1 -> apps.take(8)
            2 -> apps.filter { appName(it).contains(Regex("photo|camera|gallery|music|spotify|shazam|message", RegexOption.IGNORE_CASE)) }
            else -> apps.filter { appName(it).contains(Regex("office|excel|word|github|termux|code|mail|drive", RegexOption.IGNORE_CASE)) }
        }.take(8)
        if (suggestions.isNotEmpty()) {
            pageView.addView(text("RACCOURCIS D'APPLICATIONS", 10f, accent, true).apply {
                setPadding(0, dp(8), 0, dp(6))
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
        listOf("Tout", "Communication", "Médias", "Outils").forEach { cat ->
            categories.addView(button(cat) { category = cat; renderDrawer() }, LinearLayout.LayoutParams(0, -2, 1f))
        }
        pageView.addView(categories)
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
        pageView.addView(button("Retour au bureau 1") { page = 0; renderDesktop() })
        pageView.addView(button("Retour au bureau 2") { page = 1; renderDesktop() })
        pageView.addView(button("Retour au bureau 3") { page = 2; renderDesktop() })
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
            setOnClickListener { this@LeafHomeActivity.page = p; renderDesktop() }
        })
    }

    @Deprecated("Back returns to the desktop")
    override fun onBackPressed() {
        if (drawer) renderDesktop() else super.onBackPressed()
    }
}
