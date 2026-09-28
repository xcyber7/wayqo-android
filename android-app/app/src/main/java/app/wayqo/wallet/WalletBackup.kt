// SPDX-FileCopyrightText: 2026 Black Flag International Limited
// SPDX-License-Identifier: GPL-3.0-only
package app.wayqo.wallet

import android.util.Base64
import org.json.JSONObject
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * User-controlled, passphrase-encrypted backup of the wallet seed.
 *
 * The seed is encrypted with a key derived from a **user passphrase** (PBKDF2-HMAC-SHA256)
 * — NOT the device's Keystore key — so the backup is portable to a new device (the device
 * key can't travel). The resulting blob is meaningless without the passphrase; the
 * operator never sees the seed or the passphrase, and the blob must go only to the user's
 * own storage. This mirrors how MetaMask/Coinbase-style encrypted backups work.
 */
internal sealed interface WalletRecoveryMaterial {
    class Seed(val bytes: ByteArray) : WalletRecoveryMaterial
    class Phrase(val words: String) : WalletRecoveryMaterial
}

object WalletBackup {
    private const val LEGACY_VERSION = 1
    private const val PHRASE_VERSION = 2
    private const val KDF = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS = 210_000
    private const val KEY_BITS = 256
    private const val GCM_TAG_BITS = 128
    private const val SALT_LEN = 16
    private const val IV_LEN = 12
    private const val MAX_BLOB_CHARS = 16_384

    /** Encrypts [seed] under [passphrase]; returns a self-describing JSON backup blob. */
    fun create(seed: ByteArray, passphrase: CharArray): String {
        require(seed.size == 32) { "Nothing to back up" }
        return encrypt(seed, passphrase, LEGACY_VERSION)
    }

    /** New-wallet file contains the BIP39 words, preserving future chain derivation. */
    fun createPhrase(phrase: String, passphrase: CharArray): String {
        val plaintext = phrase.toByteArray(Charsets.UTF_8)
        return try { encrypt(plaintext, passphrase, PHRASE_VERSION) }
        finally { plaintext.fill(0) }
    }

    private fun encrypt(plaintext: ByteArray, passphrase: CharArray, version: Int): String {
        require(plaintext.isNotEmpty() && plaintext.size <= 512) { "Nothing to back up" }
        val salt = ByteArray(SALT_LEN).also { SecureRandom().nextBytes(it) }
        val key = deriveKey(passphrase, salt, ITERATIONS)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"))
        val iv = cipher.iv
        val ciphertext = try { cipher.doFinal(plaintext) } finally { key.fill(0) }
        return JSONObject().apply {
            put("v", version)
            put("kdf", KDF)
            put("iter", ITERATIONS)
            put("salt", b64(salt))
            put("iv", b64(iv))
            put("ct", b64(ciphertext))
        }.toString()
    }

    /** Decrypts a backup blob with [passphrase]; throws on a wrong passphrase or bad blob. */
    fun restore(blob: String, passphrase: CharArray): ByteArray {
        val material = restoreMaterial(blob, passphrase)
        require(material is WalletRecoveryMaterial.Seed) { "This backup contains recovery words; use the wallet restore flow" }
        return material.bytes
    }

    internal fun restoreMaterial(blob: String, passphrase: CharArray): WalletRecoveryMaterial {
        require(blob.length <= MAX_BLOB_CHARS) { "Backup file is too large" }
        val json = runCatching { JSONObject(blob.trim()) }
            .getOrElse { error("This doesn't look like a WAYQO wallet backup") }
        val version = json.optInt("v")
        require(version == LEGACY_VERSION || version == PHRASE_VERSION) { "Unsupported backup version" }
        require(json.optString("kdf") == KDF) { "Unsupported backup encryption" }
        val iterations = json.optInt("iter", ITERATIONS)
        require(iterations in ITERATIONS..1_000_000) { "Unsupported backup work factor" }
        val salt = unb64(json.getString("salt"))
        val iv = unb64(json.getString("iv"))
        val ciphertext = unb64(json.getString("ct"))
        require(salt.size == SALT_LEN && iv.size == IV_LEN && ciphertext.size in 48..528) { "Invalid backup file" }
        if (version == LEGACY_VERSION) require(ciphertext.size == 48) { "Invalid legacy backup file" }
        val key = deriveKey(passphrase, salt, iterations)
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(GCM_TAG_BITS, iv))
            val plaintext = runCatching { cipher.doFinal(ciphertext) }
                .getOrElse { error("Incorrect passphrase or corrupted backup") }
            if (version == LEGACY_VERSION) WalletRecoveryMaterial.Seed(plaintext)
            else try { WalletRecoveryMaterial.Phrase(plaintext.toString(Charsets.UTF_8)) }
                finally { plaintext.fill(0) }
        } finally { key.fill(0) }
    }

    private fun deriveKey(passphrase: CharArray, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(passphrase, salt, iterations, KEY_BITS)
        return try { SecretKeyFactory.getInstance(KDF).generateSecret(spec).encoded }
        finally { spec.clearPassword() }
    }

    private fun b64(bytes: ByteArray) = Base64.encodeToString(bytes, Base64.NO_WRAP)
    private fun unb64(value: String) = Base64.decode(value, Base64.NO_WRAP)
}
