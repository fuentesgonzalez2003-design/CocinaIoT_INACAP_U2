package cl.inacap.cocinaiot.data

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Autenticación LOCAL educativa, sin usuarios ni contraseñas incrustadas en código. */
class LocalAuth(context: Context) {
    private val prefs = context.getSharedPreferences("iot_auth_local", Context.MODE_PRIVATE)
    val hasUser: Boolean get() = prefs.contains("user")
    val username: String get() = prefs.getString("user", "") ?: ""

    fun register(user: String, password: String): String? {
        val clean = user.trim()
        if (hasUser) return "Ya existe un usuario local en este dispositivo."
        if (!clean.matches(Regex("[A-Za-z0-9._-]{3,30}")))
            return "Usuario: 3 a 30 caracteres, sin espacios."
        if (password.length < 8) return "La contraseña debe tener al menos 8 caracteres."
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = derive(password, salt)
        val stored = prefs.edit()
            .putString("user", clean)
            .putString("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString("hash", Base64.encodeToString(hash, Base64.NO_WRAP))
            .commit()
        return if (stored) null else "No se pudieron guardar las credenciales."
    }

    fun login(user: String, password: String): Boolean {
        if (!hasUser || user.trim() != username) return false
        return try {
            val salt = Base64.decode(prefs.getString("salt", ""), Base64.NO_WRAP)
            val expected = Base64.decode(prefs.getString("hash", ""), Base64.NO_WRAP)
            MessageDigest.isEqual(expected, derive(password, salt))
        } catch (_: Exception) { false }
    }

    private fun derive(password: String, salt: ByteArray): ByteArray {
        val chars = password.toCharArray()
        val spec = PBEKeySpec(chars, salt, 150_000, 256)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
            chars.fill('\u0000')
        }
    }
}
