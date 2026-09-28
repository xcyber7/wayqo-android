// SPDX-FileCopyrightText: 2026 Black Flag International Limited
// SPDX-License-Identifier: GPL-3.0-only
package app.wayqo.wallet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class PeerTransferInputTest {
    private val recipient = Base58.encode(ByteArray(32) { 7 })
    private val mint = Base58.encode(ByteArray(32) { 8 })

    @Test
    fun parsesRawAddress() {
        val parsed = PeerTransferInput.parse(recipient)
        assertEquals(recipient, parsed.address)
        assertNull(parsed.mint)
    }

    @Test
    fun parsesSolanaPayRecipientAndMint() {
        val parsed = PeerTransferInput.parse("solana:$recipient?spl-token=$mint")
        assertEquals(recipient, parsed.address)
        assertEquals(mint, parsed.mint)
    }

    @Test
    fun rejectsMalformedOrAmbiguousInput() {
        listOf(
            "not-base58",
            "solana://$recipient",
            "solana:$recipient?spl-token=$mint&spl-token=$mint",
            "https://example.test/$recipient"
        ).forEach { input ->
            assertThrows(input, IllegalArgumentException::class.java) { PeerTransferInput.parse(input) }
        }
    }
}
