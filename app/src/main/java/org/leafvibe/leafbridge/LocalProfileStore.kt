package org.leafvibe.leafbridge

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Offline profile metadata. The PIN itself is never persisted. */
class LocalProfileStore(context: Context) {
    private val prefs = context.getSharedPreferences("leafvibe_local_profile", Context.MODE_PRIVATE)

    fun exists(): Boolean = prefs.contains("pin_hash") && prefs.contains("pin_salt")
    fun profileName(): String = prefs.getString("profile_name", "Utilisateur LeafVibe") ?: "Utilisateur LeafVibe"

    fun create(name: String, pin: CharArray): Boolean {
        if (exists() || pin.size < 6) return false
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = derive(pin, salt)
        return prefs.edit()
            .putString("profile_name", name.trim().ifBlank { "Utilisateur LeafVibe" })
            .putString("pin_salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString("pin_hash", Base64.encodeToString(hash, Base64.NO_WRAP))
            .putLong("created_at", System.currentTimeMillis())
            .commit()
    }

    fun verify(pin: CharArray): Boolean {
        val saltText = prefs.getString("pin_salt", null) ?: return false
        val hashText = prefs.getString("pin_hash", null) ?: return false
        return try {
            MessageDigest.isEqual(
                Base64.decode(hashText, Base64.NO_WRAP),
                derive(pin, Base64.decode(saltText, Base64.NO_WRAP))
            )
        } catch (_: Exception) { false }
    }

    private fun derive(pin: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin, salt, 210_000, 256)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally { spec.clearPassword() }
    }
}
