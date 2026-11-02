package org.leafvibe.leafbridge

import android.content.Context
import android.net.Uri
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import java.util.concurrent.Executors

/** Creates portable ZIP archives through the local Termux engine. */
object WorkspaceBackup {
    private val executor = Executors.newSingleThreadExecutor()
    private const val BASE = "http://127.0.0.1:8765"

    fun export(context: Context, destination: Uri, done: (Boolean, String) -> Unit) {
        executor.execute {
            try {
                val payload = request("GET", "/backup", null)
                val output = context.contentResolver.openOutputStream(destination)
                    ?: throw IllegalStateException("Impossible d'ouvrir la destination")
                ZipOutputStream(output).use { zip ->
                    zip.putNextEntry(ZipEntry("leafbridge-backup.json"))
                    zip.write(payload.toByteArray(Charsets.UTF_8))
                    zip.closeEntry()
                    zip.putNextEntry(ZipEntry("README.txt"))
                    zip.write(("Sauvegarde LeafBridge\nFormat : leafbridge-backup-v1\n" +
                        "Contient la bibliothèque Shazam locale, les favoris, les automatisations et le journal.\n" +
                        "Le profil PIN reste propre à chaque appareil et n'est pas exporté.\n").toByteArray(Charsets.UTF_8))
                    zip.closeEntry()
                }
                done(true, "Sauvegarde ZIP créée.")
            } catch (e: Exception) {
                done(false, e.message ?: "Échec de la sauvegarde")
            }
        }
    }

    fun restore(context: Context, source: Uri, done: (Boolean, String) -> Unit) {
        executor.execute {
            try {
                val input = context.contentResolver.openInputStream(source)
                    ?: throw IllegalStateException("Impossible de lire le fichier")
                var payload: String? = null
                ZipInputStream(input).use { zip ->
                    while (true) {
                        val entry = zip.nextEntry ?: break
                        if (entry.name == "leafbridge-backup.json") {
                            val bytes = zip.readBytes()
                            if (bytes.size > 32 * 1024 * 1024) throw IllegalArgumentException("Archive trop volumineuse")
                            payload = String(bytes, Charsets.UTF_8)
                            break
                        }
                    }
                }
                val json = payload ?: throw IllegalArgumentException("Archive LeafBridge introuvable")
                val result = JSONObject(request("POST", "/restore", json))
                if (!result.optBoolean("ok")) throw IllegalStateException(result.optString("message", "Restauration refusée"))
                done(true, result.optString("message", "Restauration terminée"))
            } catch (e: Exception) {
                done(false, e.message ?: "Échec de la restauration")
            }
        }
    }

    private fun request(method: String, path: String, body: String?): String {
        val connection = (URL(BASE + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 10000
            readTimeout = 30000
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
        }
        try {
            if (body != null) connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                val message = try { JSONObject(text).optString("message") } catch (_: Exception) { "Erreur HTTP $code" }
                throw IllegalStateException(message)
            }
            return text
        } finally {
            connection.disconnect()
        }
    }
}
