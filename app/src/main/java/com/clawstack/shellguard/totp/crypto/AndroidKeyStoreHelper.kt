package com.clawstack.shellguard.totp.crypto

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Android KeyStore Hardware Security Wrapper for ShellGuard-TOTP.
 *
 * Generates and manages the hardware-backed AES-256-GCM key (`sg_totp_biometric_wrapper`)
 * inside the Android KeyStore / StrongBox with user authentication requirement (`setUserAuthenticationRequired(true)`).
 */
object AndroidKeyStoreHelper {

    /**
     * Standard Key Alias for the biometric wrapper key.
     */
    const val KEY_ALIAS_BIOMETRIC_WRAPPER = "sg_totp_biometric_wrapper"

    private const val ANDROID_KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val TRANSFORMATION = "${KeyProperties.KEY_ALGORITHM_AES}/${KeyProperties.BLOCK_MODE_GCM}/${KeyProperties.ENCRYPTION_PADDING_NONE}"
    private const val TAG_LENGTH_BITS = 128
    private const val GCM_IV_LENGTH_BYTES = 12

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(ANDROID_KEYSTORE_PROVIDER).apply {
            load(null)
        }
    }

    /**
     * Retrieves or generates the hardware-backed AES-256-GCM SecretKey with user authentication enforcement.
     *
     * @param alias The KeyStore alias name (defaults to `sg_totp_biometric_wrapper`).
     * @param userAuthenticationRequired Enforces biometric or device credential authentication to unlock the key.
     * @param authTimeoutSeconds Timeout duration in seconds, or <= 0 for per-use authentication (requires CryptoObject).
     * @return SecretKey residing in Android KeyStore / TEE / StrongBox.
     */
    @Synchronized
    fun getOrCreateBiometricKey(
        alias: String = KEY_ALIAS_BIOMETRIC_WRAPPER,
        userAuthenticationRequired: Boolean = true,
        authTimeoutSeconds: Int = -1
    ): SecretKey {
        if (keyStore.containsAlias(alias)) {
            val entry = keyStore.getEntry(alias, null) as? KeyStore.SecretKeyEntry
            if (entry != null) {
                return entry.secretKey
            }
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE_PROVIDER
        )

        val specBuilder = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(userAuthenticationRequired)

        if (userAuthenticationRequired) {
            try {
                specBuilder.setInvalidatedByBiometricEnrollment(true)
            } catch (ignored: Throwable) {}

            if (authTimeoutSeconds > 0) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    specBuilder.setUserAuthenticationParameters(
                        authTimeoutSeconds,
                        KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL
                    )
                } else {
                    @Suppress("DEPRECATION")
                    specBuilder.setUserAuthenticationValidityDurationSeconds(authTimeoutSeconds)
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    specBuilder.setUserAuthenticationParameters(
                        0,
                        KeyProperties.AUTH_BIOMETRIC_STRONG
                    )
                }
            }
        }

        keyGenerator.init(specBuilder.build())
        return keyGenerator.generateKey()
    }

    /**
     * Alias for getOrCreateBiometricKey with default parameters.
     */
    fun getOrCreateBiometricSecretKey(): SecretKey = getOrCreateBiometricKey()

    /**
     * Obtains an initialized Cipher for BiometricPrompt.CryptoObject authentication.
     */
    fun getBiometricCipher(mode: Int, iv: ByteArray? = null): Cipher {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val key = getOrCreateBiometricSecretKey()
        if (mode == Cipher.ENCRYPT_MODE) {
            cipher.init(mode, key)
        } else {
            requireNotNull(iv) { "IV must not be null for decryption mode" }
            cipher.init(mode, key, GCMParameterSpec(TAG_LENGTH_BITS, iv))
        }
        return cipher
    }

    /**
     * Initializes a Cipher in ENCRYPT_MODE suitable for wrapping inside a `BiometricPrompt.CryptoObject`.
     */
    fun createEncryptCipher(alias: String = KEY_ALIAS_BIOMETRIC_WRAPPER): Cipher {
        val secretKey = getOrCreateBiometricKey(alias)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        return cipher
    }

    /**
     * Initializes a Cipher in DECRYPT_MODE using the provided initialization vector (IV).
     * Suitable for wrapping inside a `BiometricPrompt.CryptoObject`.
     */
    fun createDecryptCipher(iv: ByteArray, alias: String = KEY_ALIAS_BIOMETRIC_WRAPPER): Cipher {
        val secretKey = getOrCreateBiometricKey(alias)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        return cipher
    }

    /**
     * Checks if the KeyStore contains the specified key alias.
     */
    fun hasKey(alias: String = KEY_ALIAS_BIOMETRIC_WRAPPER): Boolean {
        return keyStore.containsAlias(alias)
    }

    /**
     * Deletes the specified key from the Android KeyStore.
     */
    fun deleteKey(alias: String = KEY_ALIAS_BIOMETRIC_WRAPPER) {
        if (keyStore.containsAlias(alias)) {
            keyStore.deleteEntry(alias)
        }
    }
}
