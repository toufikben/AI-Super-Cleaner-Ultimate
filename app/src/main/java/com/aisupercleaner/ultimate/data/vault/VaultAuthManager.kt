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

    fun setPin(pin: String): Boolean {
        if (pin.length < 4) return false
        val salt = ByteArray(16).apply { SecureRandom().nextBytes(this) }
        val hash = hashPin(pin, salt)
        prefs.edit().putString(KEY_PIN_SALT, salt.toHex()).putString(KEY_PIN_HASH, hash).apply()
        return true
    }

    fun verifyPin(pin: String): Boolean {
        val saltHex = prefs.getString(KEY_PIN_SALT, null) ?: return false
        val expectedHash = prefs.getString(KEY_PIN_HASH, null) ?: return false
        val salt = saltHex.hexToBytes() ?: return false
        return hashPin(pin, salt) == expectedHash
    }

    fun clearPin() { prefs.edit().remove(KEY_PIN_HASH).remove(KEY_PIN_SALT).apply() }

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
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
            )
            .build()
        prompt.authenticate(info)
    }

    private fun hashPin(pin: String, salt: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        digest.update(pin.toByteArray(Charsets.UTF_8))
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
    private fun String.hexToBytes(): ByteArray? {
        if (length % 2 != 0) return null
        return try { ByteArray(length / 2) { i -> substring(i * 2, i * 2 + 2).toInt(16).toByte() } } catch (e: Exception) { null }
    }

    companion object {
        private const val KEY_PIN_HASH = "vault_pin_hash"
        private const val KEY_PIN_SALT = "vault_pin_salt"
        private const val KEY_BIOMETRIC_ENABLED = "vault_biometric_enabled"
    }
}
