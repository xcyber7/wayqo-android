// SPDX-FileCopyrightText: 2026 Black Flag International Limited
// SPDX-License-Identifier: GPL-3.0-only
package app.wayqo.wallet

import android.app.KeyguardManager
import android.content.Context
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer
import java.io.File

/** Device authenticated, encrypted local seeds. Addresses identify independent wallet files. */
object EmbeddedWallet {
    private const val LEGACY_FILE = "unbound_wallet.bin"
    private const val LEGACY_ADDRESS = "unbound_wallet_addr"
    private const val PREFIX = "wayqo_wallet_"
    private const val MNEMONIC_PREFIX = "wayqo_mnemonic_"
    private const val ACTIVE = "active_local_wallet"

    private fun prefs(context: Context) = context.getSharedPreferences("wayqo_wallets", Context.MODE_PRIVATE)
    private fun file(context: Context, address: String): File {
        require(address.matches(Regex("[1-9A-HJ-NP-Za-km-z]{32,44}"))) { "Invalid wallet address" }
        return File(context.filesDir, PREFIX + address + ".bin")
    }

    private fun mnemonicFile(context: Context, address: String): File {
        require(address.matches(Regex("[1-9A-HJ-NP-Za-km-z]{32,44}"))) { "Invalid wallet address" }
        return File(context.filesDir, MNEMONIC_PREFIX + address + ".bin")
    }

    /** Copies the old ciphertext verbatim. The old file remains until the copy is verified. */
    fun migrate(context: Context) {
        val legacy = context.getFileStreamPath(LEGACY_FILE)
        if (!legacy.exists()) return
        val address = runCatching {
            context.openFileInput(LEGACY_ADDRESS).use { it.readBytes().decodeToString().trim() }
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: runCatching {
            val seed = KeystoreCrypto.decrypt(legacy.readBytes())
            Base58.encode(Ed25519PrivateKeyParameters(seed, 0).generatePublicKey().encoded).also { seed.fill(0) }
        }.getOrNull() ?: return
        val target = file(context, address)
        if (!target.exists()) {
            val staging = File(context.filesDir, target.name + ".tmp")
            staging.writeBytes(legacy.readBytes())
            require(staging.readBytes().contentEquals(legacy.readBytes())) { "Wallet migration copy failed" }
            require(staging.renameTo(target)) { "Wallet migration could not finish" }
        }
        if (prefs(context).getString(ACTIVE, null) == null) prefs(context).edit().putString(ACTIVE, address).apply()
        legacy.delete()
        context.deleteFile(LEGACY_ADDRESS)
    }

    fun addresses(context: Context): List<String> {
        migrate(context)
        return context.filesDir.listFiles().orEmpty().mapNotNull { candidate ->
            candidate.name.takeIf { it.startsWith(PREFIX) && it.endsWith(".bin") }
                ?.removePrefix(PREFIX)?.removeSuffix(".bin")
        }.sorted()
    }

    fun activeAddress(context: Context): String? {
        val available = addresses(context)
        return prefs(context).getString(ACTIVE, null)?.takeIf { it in available } ?: available.firstOrNull()
    }

    fun select(context: Context, address: String) {
        require(address in addresses(context)) { "Wallet is not stored on this device" }
        prefs(context).edit().putString(ACTIVE, address).apply()
    }

    fun exists(context: Context): Boolean = activeAddress(context) != null
    fun isHardwareProtected(context: Context): Boolean = exists(context) && deviceSecure(context)
    fun publicAddress(context: Context): String? = activeAddress(context)

    fun publicKeyBytes(context: Context): ByteArray = privateKey(context).generatePublicKey().encoded
    fun address(context: Context): String = activeAddress(context) ?: error("No local wallet")

    fun sign(context: Context, message: ByteArray): ByteArray {
        val signer = Ed25519Signer()
        signer.init(true, privateKey(context))
        signer.update(message, 0, message.size)
        return signer.generateSignature()
    }

    fun exportSeed(context: Context): ByteArray {
        val address = activeAddress(context) ?: error("No wallet to back up")
        return KeystoreCrypto.decrypt(file(context, address).readBytes())
    }

    fun hasMnemonic(context: Context, address: String): Boolean = mnemonicFile(context, address).exists()

    /** New wallets retain their original BIP39 words under the same device-bound key. */
    fun importMnemonic(context: Context, seed: ByteArray, phrase: String): String {
        require(seed.size == 32 && phrase.isNotBlank()) { "Invalid wallet recovery material" }
        val words = context.assets.open("bip39_english.txt").bufferedReader().use { it.readLines() }
        val derived = WalletMnemonic(words).solanaSeed(phrase)
        try { require(derived.contentEquals(seed)) { "Recovery words do not match this wallet key" } }
        finally { derived.fill(0) }
        val address = Base58.encode(Ed25519PrivateKeyParameters(seed, 0).generatePublicKey().encoded)
        val target = mnemonicFile(context, address)
        if (target.exists()) {
            val existing = KeystoreCrypto.decrypt(target.readBytes())
            val candidate = phrase.toByteArray(Charsets.UTF_8)
            try { require(existing.contentEquals(candidate)) { "Existing recovery material differs" } }
            finally { existing.fill(0); candidate.fill(0) }
            return importSeed(context, seed)
        }
        val plaintext = phrase.toByteArray(Charsets.UTF_8)
        val encrypted = try { KeystoreCrypto.encrypt(plaintext, requireAuth = deviceSecure(context)) }
            finally { plaintext.fill(0) }
        val staging = File(context.filesDir, target.name + ".tmp")
        val oldActive = activeAddress(context)
        val hadSeed = file(context, address).exists()
        try {
            staging.writeBytes(encrypted)
            val imported = importSeed(context, seed)
            require(staging.renameTo(target)) { "Could not store wallet recovery material" }
            return imported
        } catch (error: Exception) {
            if (!hadSeed) file(context, address).delete()
            if (oldActive != null && oldActive in addresses(context)) select(context, oldActive)
            throw error
        } finally {
            staging.delete()
            encrypted.fill(0)
        }
    }

    fun exportMnemonic(context: Context, address: String = activeAddress(context) ?: error("No wallet")): String {
        require(address == activeAddress(context)) { "Select this wallet before exporting recovery words" }
        val target = mnemonicFile(context, address)
        require(target.exists()) { "This wallet has no BIP39 recovery words; export its Solana key instead" }
        val plaintext = KeystoreCrypto.decrypt(target.readBytes())
        return try { plaintext.toString(Charsets.UTF_8) } finally { plaintext.fill(0) }
    }

    /** Adds a wallet without replacing any existing file or changing selection on failure. */
    fun importSeed(context: Context, seed: ByteArray): String {
        require(seed.size == 32) { "Backup did not contain a valid wallet key" }
        migrate(context)
        val address = Base58.encode(Ed25519PrivateKeyParameters(seed, 0).generatePublicKey().encoded)
        val target = file(context, address)
        if (target.exists()) {
            val existing = KeystoreCrypto.decrypt(target.readBytes())
            try { require(existing.contentEquals(seed)) { "Existing wallet has different key material" } }
            finally { existing.fill(0) }
        } else {
            val encrypted = KeystoreCrypto.encrypt(seed, requireAuth = deviceSecure(context))
            val staging = File(context.filesDir, target.name + ".tmp")
            staging.writeBytes(encrypted)
            require(staging.renameTo(target)) { "Could not store wallet" }
        }
        select(context, address)
        return address
    }

    private fun privateKey(context: Context): Ed25519PrivateKeyParameters {
        val seed = exportSeed(context)
        return try { Ed25519PrivateKeyParameters(seed, 0) } finally { seed.fill(0) }
    }

    private fun deviceSecure(context: Context): Boolean =
        (context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager)?.isDeviceSecure == true
}
