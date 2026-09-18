package com.aisupercleaner.ultimate.data.vault

import android.content.Context
import android.content.SharedPreferences
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VaultAuthManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        EncryptedSharedPreferences.create(
            context, "vault_auth_prefs", masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun canUseBiometric(): Boolean {
        val manager = BiometricManager.from(context)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
        return manager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun isPinSet(): Boolean = prefs.contains(KEY_PIN_HASH)
    fun isBiometricEnabled(): Boolean = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, true)
    fun setBiometricEnabled(enabled: Boolean) { prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply() }

    @Synchronized
    fun setPin(pin: String): Boolean {
        if (pin.length < 4) return false
        val salt = ByteArray(SALT_BYTES).apply { SecureRandom().nextBytes(this) }
        return try {
            val hash = derivePin(pin, salt)
            try {
                prefs.edit()
                    .putString(KEY_PIN_SALT, salt.toHex())
                    .putString(KEY_PIN_HASH, hash.toHex())
                    .remove(KEY_FAILED_ATTEMPTS)
                    .remove(KEY_LOCKOUT_UNTIL)
                    .apply()
                true
            } finally { hash.fill(0) }
        } finally { salt.fill(0) }
    }

    /** Returns false while rate-limited; failed attempts use persisted exponential backoff. */
    @Synchronized
    fun verifyPin(pin: String): Boolean {
        val now = System.currentTimeMillis()
        if (prefs.getLong(KEY_LOCKOUT_UNTIL, 0L) > now) return false
        val salt = prefs.getString(KEY_PIN_SALT, null)?.hexToBytes() ?: return false
        val expected = prefs.getString(KEY_PIN_HASH, null)?.hexToBytes() ?: run {
            salt.fill(0)
            return false
        }
        return try {
            val derived = derivePin(pin, salt)
            try {
                var matches = MessageDigest.isEqual(derived, expected)
                var legacy = false
                if (!matches && expected.size == LEGACY_HASH_BYTES) {
                    val legacyHash = legacyHashPin(pin, salt)
                    try {
                        legacy = MessageDigest.isEqual(legacyHash, expected)
                        matches = legacy
                    } finally { legacyHash.fill(0) }
                }
                if (matches) {
                    if (legacy) migratePin(pin, salt)
                    prefs.edit().remove(KEY_FAILED_ATTEMPTS).remove(KEY_LOCKOUT_UNTIL).apply()
                } else registerFailedAttempt(now)
                matches
            } finally { derived.fill(0) }
        } finally {
            salt.fill(0)
            expected.fill(0)
        }
    }

    fun clearPin() {
        prefs.edit().remove(KEY_PIN_HASH).remove(KEY_PIN_SALT)
            .remove(KEY_FAILED_ATTEMPTS).remove(KEY_LOCKOUT_UNTIL).apply()
    }

    fun authenticate(activity: FragmentActivity, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val executor = ContextCompat.getMainExecutor(context)
        val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) { onSuccess() }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) { onError(errString.toString()) }
            override fun onAuthenticationFailed() {}
        })
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("الخزنة الخاصة")
            .setSubtitle("أكّد هويتك للوصول")
            .setNegativeButtonText("استخدام PIN")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .build()
        prompt.authenticate(info)
    }

    private fun derivePin(pin: String, salt: ByteArray): ByteArray {
        val chars = pin.toCharArray()
        val spec = PBEKeySpec(chars, salt, PBKDF2_ITERATIONS, DERIVED_KEY_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
            chars.fill('\u0000')
        }
    }

    private fun legacyHashPin(pin: String, salt: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        digest.update(pin.toByteArray(Charsets.UTF_8))
        return digest.digest()
    }

    private fun migratePin(pin: String, salt: ByteArray) {
        val hash = derivePin(pin, salt)
        try { prefs.edit().putString(KEY_PIN_HASH, hash.toHex()).apply() }
        finally { hash.fill(0) }
    }

    private fun registerFailedAttempt(now: Long) {
        val attempts = (prefs.getInt(KEY_FAILED_ATTEMPTS, 0) + 1).coerceAtMost(MAX_FAILED_ATTEMPTS)
        val delay = if (attempts >= MAX_FAILED_ATTEMPTS) MAX_LOCKOUT_MS
        else (BASE_BACKOFF_MS * (1L shl (attempts - 1))).coerceAtMost(MAX_BACKOFF_MS)
        prefs.edit().putInt(KEY_FAILED_ATTEMPTS, attempts).putLong(KEY_LOCKOUT_UNTIL, now + delay).apply()
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
    private fun String.hexToBytes(): ByteArray? {
        if (length % 2 != 0) return null
        return try { ByteArray(length / 2) { i -> substring(i * 2, i * 2 + 2).toInt(16).toByte() } } catch (_: Exception) { null }
    }

    companion object {
        private const val KEY_PIN_HASH = "vault_pin_hash"
        private const val KEY_PIN_SALT = "vault_pin_salt"
        private const val KEY_BIOMETRIC_ENABLED = "vault_biometric_enabled"
        private const val KEY_FAILED_ATTEMPTS = "vault_pin_failed_attempts"
        private const val KEY_LOCKOUT_UNTIL = "vault_pin_lockout_until"
        private const val SALT_BYTES = 16
        private const val PBKDF2_ITERATIONS = 210_000
        private const val DERIVED_KEY_BITS = 256
        private const val LEGACY_HASH_BYTES = 32
        private const val MAX_FAILED_ATTEMPTS = 5
        private const val BASE_BACKOFF_MS = 1_000L
        private const val MAX_BACKOFF_MS = 30_000L
        private const val MAX_LOCKOUT_MS = 5 * 60 * 1_000L
    }
}
