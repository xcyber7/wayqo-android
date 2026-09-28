// SPDX-FileCopyrightText: 2026 Black Flag International Limited
// SPDX-License-Identifier: GPL-3.0-only
package app.wayqo.wallet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class WalletMnemonicTest {
    private val words = File("src/main/assets/bip39_english.txt").readLines()
    private val mnemonic = WalletMnemonic(words)

    @Test fun zeroEntropyMatchesBip39AndSolanaVector() {
        val phrase = "abandon ".repeat(23) + "art"
        assertEquals(phrase, mnemonic.fromEntropy(ByteArray(32)))
        assertTrue(mnemonic.isValid(phrase))
        assertEquals("7c139e1a603ca04f5f7cff194e1bb6f6d1b9098470ea90695ab628488a9f921b",
            mnemonic.solanaSeed(phrase).joinToString("") { "%02x".format(it) })
        assertEquals("3Cy3YNTFywCmxoxt8n7UH6hg6dLo5uACowX3CFceaSnx", mnemonic.address(phrase))
    }

    @Test fun wrongWordOrChecksumCannotRestore() {
        val phrase = "abandon ".repeat(23) + "art"
        assertFalse(mnemonic.isValid(phrase.replace("art", "about")))
        assertFalse(mnemonic.isValid(phrase.replaceFirst("abandon", "potato")))
    }

    @Test fun trezorBip39VectorAndPastedWordFormatsRestoreTheSameWallet() {
        // trezor/python-mnemonic vectors.json, English 256-bit 0x7f vector.
        val phrase = "legal winner thank year wave sausage worth useful ".repeat(2) + "legal winner thank year wave sausage worth title"
        val entropy = ByteArray(32) { 0x7f.toByte() }
        assertEquals(phrase, mnemonic.fromEntropy(entropy))
        val expectedSeed = "bc09fca1804f7e69da93c2f2028eb238c227f2e9dda30cd63699232578480a4021b146ad717fbb7e451ce9eb835f43620bf5c514db0f8add49f5d121449d3e87"
        assertEquals(expectedSeed, mnemonic.bip39Seed(phrase, "TREZOR").joinToString("") { "%02x".format(it) })
        val numbered = phrase.split(' ').mapIndexed { index, word -> "${index + 1}. $word" }.joinToString("\n")
        val commaSeparated = phrase.replace(' ', ',')
        assertTrue(mnemonic.isValid(numbered))
        assertTrue(mnemonic.isValid(commaSeparated))
        assertEquals(mnemonic.address(phrase), mnemonic.address(numbered))
        assertEquals(mnemonic.address(phrase), mnemonic.address(commaSeparated))
    }
}
