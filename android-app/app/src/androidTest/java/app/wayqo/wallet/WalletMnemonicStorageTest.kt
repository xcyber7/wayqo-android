// SPDX-FileCopyrightText: 2026 Black Flag International Limited
// SPDX-License-Identifier: GPL-3.0-only
package app.wayqo.wallet

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileInputStream

@RunWith(AndroidJUnit4::class)
class WalletMnemonicStorageTest {
    @Test fun encryptedRecoveryWordsStayWithTheirOwnWallet() {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val root = File(base.cacheDir, "wallet-mnemonic-storage-test").apply { deleteRecursively(); mkdirs() }
        val context = object : ContextWrapper(base) {
            override fun getFilesDir(): File = root
            override fun getFileStreamPath(name: String): File = File(root, name)
            override fun openFileInput(name: String): FileInputStream = FileInputStream(File(root, name))
            override fun deleteFile(name: String): Boolean = File(root, name).delete()
            override fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
                base.getSharedPreferences("isolated_mnemonic_$name", mode)
        }
        context.getSharedPreferences("wayqo_wallets", Context.MODE_PRIVATE).edit().clear().commit()
        try {
            val oldSeed = ByteArray(32) { 7 }
            val oldAddress = EmbeddedWallet.importSeed(context, oldSeed)
            val phrase = "abandon ".repeat(23) + "art"
            val words = base.assets.open("bip39_english.txt").bufferedReader().use { it.readLines() }
            val newSeed = WalletMnemonic(words).solanaSeed(phrase)
            val newAddress = EmbeddedWallet.importMnemonic(context, newSeed, phrase)
            assertTrue(newAddress != oldAddress)
            assertTrue(EmbeddedWallet.hasMnemonic(context, newAddress))
            assertFalse(EmbeddedWallet.hasMnemonic(context, oldAddress))
            assertEquals(phrase, EmbeddedWallet.exportMnemonic(context, newAddress))
            assertTrue(File(root, "wayqo_mnemonic_$newAddress.bin").readBytes().decodeToString().contains("abandon").not())

            EmbeddedWallet.select(context, oldAddress)
            assertArrayEquals(oldSeed, EmbeddedWallet.exportSeed(context))
            assertTrue(runCatching { EmbeddedWallet.importMnemonic(context, newSeed, "legal winner") }.isFailure)
            assertEquals(oldAddress, EmbeddedWallet.activeAddress(context))
            EmbeddedWallet.select(context, newAddress)
            assertEquals(phrase, EmbeddedWallet.exportMnemonic(context, newAddress))
            assertArrayEquals(newSeed, EmbeddedWallet.exportSeed(context))
            newSeed.fill(0)
        } finally { root.deleteRecursively() }
    }
}
