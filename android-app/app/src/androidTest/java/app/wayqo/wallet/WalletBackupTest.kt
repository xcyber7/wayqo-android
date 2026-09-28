// SPDX-FileCopyrightText: 2026 Black Flag International Limited
// SPDX-License-Identifier: GPL-3.0-only
package app.wayqo.wallet

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONObject
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WalletBackupTest {
    @Test fun encryptedExportRestoresOnlyWithTheCorrectPassphrase() {
        val seed = ByteArray(32) { index -> index.toByte() }
        val backup = WalletBackup.create(seed, "four vivid planets orbit safely".toCharArray())
        assertTrue(!backup.contains(seed.joinToString("") { "%02x".format(it) }))
        assertArrayEquals(seed, WalletBackup.restore(backup, "four vivid planets orbit safely".toCharArray()))
        val wrong = runCatching { WalletBackup.restore(backup, "wrong passphrase".toCharArray()) }
        assertTrue(wrong.isFailure)
        val corrupted = JSONObject(backup).put("iter", 999999999).toString()
        assertTrue(runCatching { WalletBackup.restore(corrupted, "four vivid planets orbit safely".toCharArray()) }.isFailure)
    }

    @Test fun phraseFileRestoresAllBip39WordsWithoutPlaintextInTheFile() {
        val phrase = "abandon ".repeat(23) + "art"
        val backup = WalletBackup.createPhrase(phrase, "four vivid planets orbit safely".toCharArray())
        assertTrue(!backup.contains("abandon"))
        val restored = WalletBackup.restoreMaterial(backup, "four vivid planets orbit safely".toCharArray())
        assertTrue(restored is WalletRecoveryMaterial.Phrase)
        assertTrue((restored as WalletRecoveryMaterial.Phrase).words == phrase)
        assertTrue(runCatching { WalletBackup.restoreMaterial(backup, "incorrect password".toCharArray()) }.isFailure)
    }
}
