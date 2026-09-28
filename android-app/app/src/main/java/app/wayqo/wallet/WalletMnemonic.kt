// SPDX-FileCopyrightText: 2026 Black Flag International Limited
// SPDX-License-Identifier: GPL-3.0-only
package app.wayqo.wallet

import org.bouncycastle.crypto.digests.SHA512Digest
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator
import org.bouncycastle.crypto.params.KeyParameter
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.security.SecureRandom
import java.text.Normalizer
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/** BIP39 English, followed by hardened SLIP-0010 ed25519 m/44'/501'/0'/0'. */
internal class WalletMnemonic(private val words: List<String>) {
    init { require(words.size == 2048 && words.distinct().size == 2048) }

    /** Accept words separated by spaces, newlines or pasted list punctuation. */
    fun wordsFromInput(input: String): List<String> = input
        .replace(Regex("(?m)(?<!\\S)\\d{1,2}[.)]\\s*"), " ")
        .trim().lowercase()
        .split(Regex("[\\s,;]+"))

    fun generate(): String {
        val entropy = ByteArray(32).also(SecureRandom()::nextBytes)
        return try { fromEntropy(entropy) } finally { entropy.fill(0) }
    }

    fun fromEntropy(entropy: ByteArray): String {
        require(entropy.size == 32) { "A new wallet requires 256 bits of entropy" }
        val checksum = MessageDigest.getInstance("SHA-256").digest(entropy)[0].toInt() and 255
        return (0 until 24).joinToString(" ") { index ->
            var value = 0
            for (bit in index * 11 until index * 11 + 11) {
                value = (value shl 1) or if (bit < 256) {
                    (entropy[bit / 8].toInt() ushr (7 - bit % 8)) and 1
                } else {
                    (checksum ushr (7 - (bit - 256))) and 1
                }
            }
            words[value]
        }
    }

    fun isValid(phrase: String): Boolean {
        val tokens = wordsFromInput(phrase)
        if (tokens.size != 24) return false
        val entropy = ByteArray(32)
        var checksum = 0
        for ((index, word) in tokens.withIndex()) {
            val value = words.binarySearch(word)
            if (value < 0) return false
            for (offset in 0 until 11) {
                val bit = index * 11 + offset
                val digit = (value ushr (10 - offset)) and 1
                if (bit < 256) entropy[bit / 8] = (entropy[bit / 8].toInt() or (digit shl (7 - bit % 8))).toByte()
                else checksum = (checksum shl 1) or digit
            }
        }
        return checksum == (MessageDigest.getInstance("SHA-256").digest(entropy)[0].toInt() and 255)
    }

    fun solanaSeed(phrase: String): ByteArray {
        val bip39Seed = bip39Seed(phrase)
        try {
            var node = hmac("ed25519 seed".toByteArray(), bip39Seed)
            for (index in intArrayOf(44, 501, 0, 0)) {
                val data = byteArrayOf(0) + node.copyOfRange(0, 32) +
                    ByteBuffer.allocate(4).putInt(index or Int.MIN_VALUE).array()
                val next = hmac(node.copyOfRange(32, 64), data)
                node.fill(0)
                node = next
            }
            return node.copyOfRange(0, 32).also { node.fill(0) }
        } finally { bip39Seed.fill(0) }
    }

    /** BIP39 seed, independently checked against Trezor's published vectors. */
    fun bip39Seed(phrase: String, passphrase: String = ""): ByteArray {
        require(isValid(phrase)) { "Invalid 24-word recovery phrase or checksum" }
        val normalized = Normalizer.normalize(wordsFromInput(phrase).joinToString(" "), Normalizer.Form.NFKD)
        val generator = PKCS5S2ParametersGenerator(SHA512Digest())
        val phraseBytes = normalized.toByteArray(Charsets.UTF_8)
        val saltBytes = Normalizer.normalize("mnemonic$passphrase", Normalizer.Form.NFKD).toByteArray(Charsets.UTF_8)
        return try {
            generator.init(phraseBytes, saltBytes, 2048)
            (generator.generateDerivedParameters(512) as KeyParameter).key
        } finally {
            phraseBytes.fill(0)
            saltBytes.fill(0)
        }
    }

    fun address(phrase: String): String {
        val seed = solanaSeed(phrase)
        return try { Base58.encode(Ed25519PrivateKeyParameters(seed, 0).generatePublicKey().encoded) }
        finally { seed.fill(0) }
    }

    private fun hmac(key: ByteArray, data: ByteArray): ByteArray =
        Mac.getInstance("HmacSHA512").apply { init(SecretKeySpec(key, "HmacSHA512")) }.doFinal(data)
}
