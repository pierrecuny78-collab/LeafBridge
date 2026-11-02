package org.leafvibe.leafbridge

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.text.InputType
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private val io = Executors.newSingleThreadExecutor()
    private lateinit var root: LinearLayout
    private lateinit var content: LinearLayout
    private var current = "Accueil"
    private var engineOnline = false
    private lateinit var profileStore: LocalProfileStore
    private var authenticated = false
    private val bg = Color.rgb(11, 20, 28)
    private val surface = Color.rgb(24, 39, 49)
    private val green = Color.rgb(190, 242, 115)
    private val muted = Color.rgb(169, 188, 198)
    private val white = Color.WHITE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        profileStore = LocalProfileStore(this)
        if (profileStore.exists()) showUnlockScreen() else showProfileSetupScreen()
    }


    private fun gateBase(titleText: String, subtitle: String): LinearLayout {
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setBackgroundColor(bg)
            setPadding(dp(26), dp(36), dp(26), dp(28))
        }
        page.addView(label("LEAFBRIDGE", 28f, white, true))
        page.addView(label("LEAFVIBE  •  ESPACE LOCAL", 12f, green, true))
        page.addView(View(this), LinearLayout.LayoutParams(1, dp(34)))
        page.addView(label(titleText, 25f, white, true))
        page.addView(label(subtitle, 14f, muted))
        return page
    }

    private fun gateInput(hintText: String, numeric: Boolean = false): EditText =
        EditText(this).apply {
            hint = hintText
            textSize = 16f
            setTextColor(white)
            setHintTextColor(muted)
            inputType = if (numeric) InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD else InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            background = shape(Color.rgb(31, 48, 59), 14)
            setPadding(dp(14), dp(12), dp(14), dp(12))
        }

    private fun showProfileSetupScreen() {
        authenticated = false
        val page = gateBase("Créer ton profil", "Un profil privé, enregistré uniquement sur cet appareil.")
        val name = gateInput("Nom du profil")
        val pin = gateInput("Code PIN à 6 chiffres minimum", true)
        val confirm = gateInput("Confirmer le code PIN", true)
        page.addView(name, LinearLayout.LayoutParams(-1, dp(58)).apply { topMargin = dp(22) })
        page.addView(pin, LinearLayout.LayoutParams(-1, dp(58)).apply { topMargin = dp(12) })
        page.addView(confirm, LinearLayout.LayoutParams(-1, dp(58)).apply { topMargin = dp(12) })
        page.addView(label("Le code n'est pas stocké en clair. Le profil reste disponible hors ligne.", 12f, muted))
        page.addView(button("Créer mon espace LeafVibe", true) {
            val p = pin.text.toString().toCharArray()
            if (name.text.toString().isBlank()) {
                Toast.makeText(this, "Choisis un nom de profil.", Toast.LENGTH_SHORT).show()
            } else if (p.size < 6 || p.any { !it.isDigit() }) {
                Toast.makeText(this, "Le code doit contenir au moins 6 chiffres.", Toast.LENGTH_SHORT).show()
            } else if (String(p) != confirm.text.toString()) {
                Toast.makeText(this, "Les deux codes ne correspondent pas.", Toast.LENGTH_SHORT).show()
            } else if (profileStore.create(name.text.toString(), p)) {
                p.fill('\u0000')
                authenticated = true
                openWorkspace()
            } else {
                p.fill('\u0000')
                Toast.makeText(this, "Impossible de créer le profil local.", Toast.LENGTH_LONG).show()
            }
        }, LinearLayout.LayoutParams(-1, dp(56)).apply { topMargin = dp(22) })
        page.addView(label("Aucun compte en ligne n'est nécessaire.", 12f, muted))
        setContentView(ScrollView(this).apply { addView(page) })
    }

    private fun showUnlockScreen() {
        authenticated = false
        val page = gateBase("Bon retour, " + profileStore.profileName(), "Déverrouille ton espace LeafVibe pour continuer.")
        val pin = gateInput("Ton code PIN", true)
        page.addView(pin, LinearLayout.LayoutParams(-1, dp(58)).apply { topMargin = dp(24) })
        page.addView(button("Déverrouiller", true) {
            val value = pin.text.toString().toCharArray()
            val valid = profileStore.verify(value)
            value.fill('\u0000')
            pin.text.clear()
            if (valid) { authenticated = true; openWorkspace() }
            else Toast.makeText(this, "Code incorrect. Réessaie.", Toast.LENGTH_SHORT).show()
        }, LinearLayout.LayoutParams(-1, dp(56)).apply { topMargin = dp(16) })
        page.addView(label("Mode hors ligne · Profil enregistré sur cet appareil", 12f, muted))
        setContentView(ScrollView(this).apply { addView(page) })
    }

    private fun openWorkspace() {
        buildShell()
        navigate("Accueil")
        refreshEngine()
    }

    private fun lockWorkspace() { authenticated = false; showUnlockScreen() }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun shape(color: Int, radius: Int = 18): GradientDrawable =
        GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat() }

    private fun label(value: String, size: Float = 14f, color: Int = muted, bold: Boolean = false): TextView =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
            setPadding(0, dp(3), 0, dp(3))
        }

    private fun panel(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = shape(surface, 20)
        setPadding(dp(16), dp(15), dp(16), dp(15))
    }

    private fun addPanel(view: View) {
        content.addView(view, LinearLayout.LayoutParams(-1, -2).apply {
            bottomMargin = dp(12)
        })
    }

    private fun button(title: String, primary: Boolean = false, onClick: () -> Unit): Button =
        Button(this).apply {
            text = title
            isAllCaps = false
            textSize = 13f
            setTextColor(if (primary) Color.rgb(20, 34, 25) else white)
            background = shape(if (primary) green else Color.rgb(42, 62, 73), 14)
            setPadding(dp(12), dp(5), dp(12), dp(5))
            setOnClickListener { onClick() }
        }

    private fun buildShell() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(20), dp(17), dp(20), dp(14))
        }
        val brand = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        brand.addView(label("LEAFBRIDGE", 19f, white, true))
        brand.addView(label("CONTROL CENTER  •  2.0", 10f, muted, true))
        header.addView(brand, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(label("●", 13f, green, true))
        header.addView(label(" LEAFVIBE", 11f, muted, true))
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(18))
        }
        val scroll = ScrollView(this).apply { isFillViewport = true; addView(content) }
        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(5), dp(7), dp(5), dp(7))
            setBackgroundColor(Color.rgb(17, 29, 38))
        }
        listOf("Accueil", "Modules", "Musique", "Activité", "Réglages").forEach { page ->
            val short = when (page) {
                "Accueil" -> "⌂\nAccueil"
                "Modules" -> "◈\nModules"
                "Musique" -> "♫\nMusique"
                "Activité" -> "◷\nActivité"
                else -> "⚙\nRéglages"
            }
            val item = TextView(this).apply {
                text = short
                gravity = Gravity.CENTER
                textSize = 11f
                setLineSpacing(dp(3).toFloat(), 1f)
                setTextColor(if (page == current) green else muted)
                setPadding(dp(2), dp(5), dp(2), dp(5))
                setOnClickListener { navigate(page) }
            }
            nav.addView(item, LinearLayout.LayoutParams(0, -2, 1f))
        }
        root.addView(header)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(nav)
        setContentView(root)
    }

    private fun navigate(page: String) {
        current = page
        content.removeAllViews()
        when (page) {
            "Accueil" -> home()
            "Modules" -> modules()
            "Musique" -> music()
            "Activité" -> activityLog()
            else -> settings()
        }
    }

    private fun title(main: String, sub: String) {
        content.addView(label(main, 26f, white, true))
        content.addView(label(sub, 13f, muted))
        content.addView(View(this).apply { setBackgroundColor(Color.rgb(48, 68, 78)) },
            LinearLayout.LayoutParams(-1, dp(1)).apply { topMargin = dp(12); bottomMargin = dp(16) })
    }

    private fun home() {
        title("Ton espace de contrôle", "Tes automatisations et ta bibliothèque, au même endroit.")
        val hero = panel().apply {
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(35, 67, 57), Color.rgb(24, 42, 51))).apply { cornerRadius = dp(22).toFloat() }
            addView(label("LEAFVIBE ECOSYSTEM", 10f, green, true))
            addView(label("La musique circule.\nTu gardes le contrôle.", 23f, white, true))
            addView(label("Shazam  →  LeafBridge  →  Spotify / LeafMusic", 12f, muted))
            addView(button("Gérer les modules", true) { navigate("Modules") })
        }
        addPanel(hero)
        val stats = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        stats.addView(statCard("MOTEUR", if (engineOnline) "En ligne" else "Hors ligne", if (engineOnline) green else Color.rgb(255, 184, 92)),
            LinearLayout.LayoutParams(0, -2, 1f))
        stats.addView(View(this), LinearLayout.LayoutParams(dp(10), 1))
        stats.addView(statCard("AUTOMATISATIONS", "4 modules", green), LinearLayout.LayoutParams(0, -2, 1f))
        addPanel(stats)
        val quick = panel()
        quick.addView(label("ACCÈS RAPIDE", 11f, green, true))
        quick.addView(label("Réception Shazam", 17f, white, true))
        quick.addView(label("Autorisation Android requise pour lire les notifications Shazam.", 12f))
        quick.addView(button("Configurer les notifications", false) {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        })
        quick.addView(label("Connexion Spotify", 17f, white, true))
        quick.addView(label("L'autorisation Spotify officielle sera intégrée avant tout ajout automatique.", 12f))
        quick.addView(button("Ouvrir les connexions", false) { navigate("Réglages") })
        addPanel(quick)
        val statusPanel = panel()
        statusPanel.addView(label("ÉTAT DU SYSTÈME", 11f, green, true))
        statusPanel.addView(label(if (engineOnline) "Moteur local détecté" else "Moteur local non détecté", 16f, white, true))
        statusPanel.addView(button("Actualiser", true) { refreshEngine() })
        addPanel(statusPanel)
    }

    private fun statCard(name: String, value: String, accent: Int) = panel().apply {
        addView(label(name, 10f, muted, true))
        addView(label(value, 17f, accent, true))
    }

    private fun modules() {
        title("Modules", "Active, configure et surveille chaque fonction.")
        module("01  SHAZAM → SPOTIFY", "Capture des titres Shazam, file d'attente et ajout aux playlists.", "shazam_spotify")
        module("02  CLASSEMENT MUSICAL", "Organisation par genre, ambiance, artiste et playlists.", "music_sort")
        module("03  SYNCHRONISATION GITHUB", "Synchronisation contrôlée des projets et dépôts.", "github_sync")
        module("04  ROUTINES LEAFBRIDGE", "Contrôles périodiques et tâches locales programmées.", "routine")
        addPanel(panel().apply {
            addView(label("ÉTAT RÉEL DES FONCTIONS", 11f, green, true))
            addView(label("Le moteur actuel accepte la réception Shazam. Les trois autres modules nécessitent encore leur implémentation côté moteur.", 13f, white))
            addView(button("Actualiser le moteur", false) { refreshEngine() })
        })
    }

    private fun module(name: String, description: String, id: String) {
        val p = panel()
        p.addView(label(name, 14f, green, true))
        p.addView(label(description, 13f, white))
        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        actions.addView(button("Démarrer", true) { sendAutomation(id, "start") },
            LinearLayout.LayoutParams(0, -2, 1f))
        actions.addView(View(this), LinearLayout.LayoutParams(dp(8), 1))
        actions.addView(button("Arrêter", false) { sendAutomation(id, "stop") },
            LinearLayout.LayoutParams(0, -2, 1f))
        p.addView(actions)
        addPanel(p)
    }

    private fun music() {
        title("Bibliothèque musicale", "Ta collection locale, dédupliquée et consultable.")
        val summary = panel()
        summary.addView(label("COLLECTION LEAFBRIDGE", 11f, green, true))
        summary.addView(label("Chargement de ta bibliothèque…", 14f, white))
        summary.addView(button("Actualiser", true) { navigate("Musique") })
        addPanel(summary)
        request("GET", "/library", null) { ok, message, json ->
            runOnUiThread {
                if (current != "Musique") return@runOnUiThread
                content.removeAllViews()
                title("Bibliothèque musicale", "Ta collection locale, dédupliquée et consultable.")
                val tracks = json?.optJSONArray("tracks")
                val total = json?.optInt("total", 0) ?: 0
                val favorites = json?.optInt("favorites", 0) ?: 0
                addPanel(panel().apply {
                    addView(label("VUE D'ENSEMBLE", 11f, green, true))
                    addView(label("$total titre(s) enregistré(s)", 21f, white, true))
                    addView(label("$favorites favori(s) · Stockage local Termux", 12f))
                    addView(button("Actualiser la bibliothèque", true) { navigate("Musique") })
                })
                if (!ok || tracks == null) {
                    addPanel(panel().apply { addView(label(message, 13f, white)) })
                    return@runOnUiThread
                }
                if (tracks.length() == 0) {
                    addPanel(panel().apply {
                        addView(label("AUCUN TITRE POUR LE MOMENT", 11f, green, true))
                        addView(label("Les notifications Shazam reçues apparaîtront automatiquement ici.", 14f, white))
                        addView(label("Les anciennes entrées de test sont ignorées.", 12f))
                    })
                } else {
                    for (i in 0 until tracks.length()) {
                        val track = tracks.optJSONObject(i) ?: continue
                        val id = track.optString("id")
                        val isFavorite = track.optBoolean("favorite")
                        addPanel(panel().apply {
                            addView(label(track.optString("title", "Titre inconnu"), 17f, white, true))
                            addView(label(track.optString("artist", "Artiste non renseigné"), 13f, muted))
                            addView(label("Reçu : " + track.optString("first_seen", "Date inconnue"), 10f, muted))
                            addView(button(if (isFavorite) "★ Retirer des favoris" else "☆ Ajouter aux favoris", isFavorite) {
                                val body = JSONObject().put("id", id).put("favorite", !isFavorite).toString()
                                request("POST", "/track", body) { success, result, _ ->
                                    runOnUiThread {
                                        Toast.makeText(this@MainActivity, result, Toast.LENGTH_SHORT).show()
                                        if (success && current == "Musique") navigate("Musique")
                                    }
                                }
                            })
                        })
                    }
                }
            }
        }
    }

    private fun activityLog() {
        title("Journal d'activité", "Historique des opérations du moteur LeafBridge.")
        request("GET", "/logs", null) { ok, message, json ->
            runOnUiThread {
                if (current != "Activité") return@runOnUiThread
                addPanel(panel().apply {
                    addView(label(if (ok) "JOURNAL DU MOTEUR" else "MOTEUR INDISPONIBLE", 11f, green, true))
                    addView(label(message, 13f, white))
                    addView(label(json?.toString(2) ?: "Aucun détail disponible.", 11f, muted))
                    addView(button("Actualiser le journal", true) { navigate("Activité") })
                })
            }
        }
    }

    private fun settings() {
        title("Réglages & connexions", "Configure les accès et le fonctionnement local.")
        addPanel(panel().apply {
            addView(label("AUTORISATIONS ANDROID", 11f, green, true))
            addView(label("Accès aux notifications Shazam", 15f, white, true))
            addView(label("Nécessaire pour récupérer les titres identifiés.", 12f))
            addView(button("Ouvrir les autorisations", true) {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            })
            addView(label("Optimisation batterie", 15f, white, true))
            addView(button("Paramètres batterie", false) {
                startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            })
        })
        addPanel(panel().apply {
            addView(label("COMPTES & SERVICES", 11f, green, true))
            listOf("Spotify — OAuth à intégrer", "GitHub — connexion à intégrer", "LeafMusic — stockage local à configurer").forEach {
                addView(label("•  $it", 13f, white))
            }
            addView(label("Aucun mot de passe ne doit être demandé directement dans LeafBridge.", 11f))
        })
        addPanel(panel().apply {
            addView(label("PROFIL LOCAL", 11f, green, true))
            addView(label(profileStore.profileName(), 16f, white, true))
            addView(label("Ce profil fonctionne sans connexion Internet.", 12f))
            addView(button("Verrouiller LeafBridge", false) { lockWorkspace() })
        })
        addPanel(panel().apply {
            addView(label("MOTEUR LOCAL", 11f, green, true))
            addView(label("Adresse : 127.0.0.1:8765", 13f, white))
            addView(label(if (engineOnline) "État : connecté" else "État : hors ligne", 13f))
            addView(button("Tester la connexion", true) { refreshEngine() })
        })
    }

    private fun refreshEngine() {
        request("GET", "/status", null) { ok, message, _ ->
            runOnUiThread {
                engineOnline = ok
                if (current == "Accueil") navigate("Accueil")
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendAutomation(id: String, command: String) {
        val body = JSONObject().put("id", id).put("command", command).toString()
        request("POST", "/automation", body) { ok, message, _ ->
            runOnUiThread { Toast.makeText(this, message, Toast.LENGTH_LONG).show() }
        }
    }

    private fun request(method: String, path: String, body: String?,
                        done: (Boolean, String, JSONObject?) -> Unit) {
        io.execute {
            var ok = false
            var message = "Moteur local inaccessible. Vérifie son démarrage."
            var json: JSONObject? = null
            try {
                val c = URL("http://127.0.0.1:8765$path").openConnection() as HttpURLConnection
                c.requestMethod = method
                c.connectTimeout = 2500
                c.readTimeout = 2500
                if (body != null) {
                    c.doOutput = true
                    c.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    c.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                }
                val code = c.responseCode
                val stream = if (code in 200..299) c.inputStream else c.errorStream
                val raw = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                json = try { JSONObject(raw) } catch (_: Exception) { null }
                ok = code in 200..299
                message = json?.optString("message")?.takeIf { it.isNotBlank() }
                    ?: if (ok) "Opération réussie" else "Erreur HTTP $code"
                c.disconnect()
            } catch (_: Exception) { }
            done(ok, message, json)
        }
    }

    override fun onDestroy() {
        io.shutdown()
        super.onDestroy()
    }
}
