// SPDX-FileCopyrightText: 2026 Black Flag International Limited
// SPDX-License-Identifier: GPL-3.0-only
package app.wayqo.wallet

/**
 * Signs a legacy Solana transaction locally, for the embedded wallet path (no external
 * wallet app). The connected wallet is the fee payer — account key index 0, i.e.
 * signature slot 0 — which [SolanaTransactionValidator] has already asserted
 * (`keys.first() == wallet`). Only that single slot is filled; our flows have exactly
 * one signer.
 *
 * Wire format: [shortVec signatureCount][signatureCount × 64-byte signatures][message].
 * The ed25519 signature is over the message bytes (everything after the signatures).
 */
object SolanaTx {
    fun signLegacy(unsigned: ByteArray, sign: (ByteArray) -> ByteArray): ByteArray {
        val (signatureCount, headerLen) = readShortVec(unsigned, 0)
        require(signatureCount in 1..16) { "Invalid transaction signature count" }
        val messageOffset = headerLen + signatureCount * 64
        require(messageOffset <= unsigned.size) { "Transaction is truncated" }
        val message = unsigned.copyOfRange(messageOffset, unsigned.size)
        val signature = sign(message)
        require(signature.size == 64) { "Signer returned an invalid signature" }
        val signed = unsigned.copyOf()
        System.arraycopy(signature, 0, signed, headerLen, 64) // slot 0 (fee payer)
        return signed
    }

    /** Reads a compact-u16 (shortvec) at [offset]; returns (value, bytesConsumed). */
    private fun readShortVec(bytes: ByteArray, offset: Int): Pair<Int, Int> {
        var value = 0
        var shift = 0
        var index = offset
        repeat(3) {
            require(index < bytes.size) { "Transaction is truncated" }
            val current = bytes[index].toInt() and 0xff
            index++
            value = value or ((current and 0x7f) shl shift)
            if (current and 0x80 == 0) return value to (index - offset)
            shift += 7
        }
        error("Compact length is too large")
    }
}
