// SPDX-FileCopyrightText: 2026 Black Flag International Limited
// SPDX-License-Identifier: GPL-3.0-only
package app.wayqo.wallet

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Wraps secrets with an AES-256-GCM key held in the Android hardware Keystore
 * (StrongBox secure element when available, TEE otherwise). The wrapping key is
 * non-exportable and device-bound.
 *
 * When [encrypt] is called with `requireAuth = true` (device has a secure lock), the
 * wrapping key is created as **user-authentication-required**: the OS will not let it
 * decrypt unless the user has authenticated (biometric or device PIN/pattern) within
 * [AUTH_VALIDITY_SECONDS]. That is hardware-enforced protection of the wallet seed, not
 * just a UI prompt. If the device has no secure lock, a non-auth key is used so the
 * wallet still works (graceful fallback).
 */
object KeystoreCrypto {
    private const val ALIAS = "unbound_wallet_wrap_key"
    private const val KEYSTORE = "AndroidKeyStore"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_BITS = 128
    private const val IV_LEN = 12
    private const val AUTH_VALIDITY_SECONDS = 30

    fun encrypt(plaintext: ByteArray, requireAuth: Boolean): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, wrappingKey(requireAuth))
        val iv = cipher.iv
        return iv + cipher.doFinal(plaintext)
    }

    fun decrypt(blob: ByteArray): ByteArray {
        val key = existingKey() ?: error("Wallet key is missing")
        val iv = blob.copyOfRange(0, IV_LEN)
        val ciphertext = blob.copyOfRange(IV_LEN, blob.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(ciphertext)
    }

    private fun existingKey(): SecretKey? {
        val keystore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        return (keystore.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey
    }

    private fun wrappingKey(requireAuth: Boolean): SecretKey =
        existingKey() ?: generateKey(requireAuth)

    private fun generateKey(requireAuth: Boolean): SecretKey {
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        val useStrongBox = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
        return try {
            generator.init(spec(strongBox = useStrongBox, requireAuth = requireAuth))
            generator.generateKey()
        } catch (_: Exception) {
            generator.init(spec(strongBox = false, requireAuth = requireAuth))
            generator.generateKey()
        }
    }

    private fun spec(strongBox: Boolean, requireAuth: Boolean): KeyGenParameterSpec {
        val builder = KeyGenParameterSpec.Builder(
            ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
        if (strongBox && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            builder.setIsStrongBoxBacked(true)
        }
        if (requireAuth) {
            builder.setUserAuthenticationRequired(true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                builder.setUserAuthenticationParameters(
                    AUTH_VALIDITY_SECONDS,
                    KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL,
                )
            } else {
                @Suppress("DEPRECATION")
                builder.setUserAuthenticationValidityDurationSeconds(AUTH_VALIDITY_SECONDS)
            }
        }
        return builder.build()
    }
}
