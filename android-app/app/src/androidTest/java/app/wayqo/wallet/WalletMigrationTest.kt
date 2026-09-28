// SPDX-FileCopyrightText: 2026 Black Flag International Limited
// SPDX-License-Identifier: GPL-3.0-only
package app.wayqo.wallet

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class WalletMigrationTest {
    @Test fun legacyAddressSurvivesMigrationAndOtherWalletCannotBeOverwritten() {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val root = File(base.cacheDir, "wallet-migration-test").apply { deleteRecursively(); mkdirs() }
        val context = object : ContextWrapper(base) {
            override fun getFilesDir(): File = root
            override fun getFileStreamPath(name: String): File = File(root, name)
            override fun openFileInput(name: String): FileInputStream = FileInputStream(File(root, name))
            override fun openFileOutput(name: String, mode: Int): FileOutputStream = FileOutputStream(File(root, name))
            override fun deleteFile(name: String): Boolean = File(root, name).delete()
            override fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
                base.getSharedPreferences("isolated_migration_$name", mode)
        }
        context.getSharedPreferences("wayqo_wallets", Context.MODE_PRIVATE).edit().clear().commit()
        val oldSeed = ByteArray(32) { 7 }
        val oldAddress = Base58.encode(Ed25519PrivateKeyParameters(oldSeed, 0).generatePublicKey().encoded)
        File(root, "unbound_wallet.bin").writeBytes(KeystoreCrypto.encrypt(oldSeed, false))
        File(root, "unbound_wallet_addr").writeText(oldAddress)

        assertEquals(listOf(oldAddress), EmbeddedWallet.addresses(context))
        assertEquals(oldAddress, EmbeddedWallet.activeAddress(context))
        assertArrayEquals(oldSeed, EmbeddedWallet.exportSeed(context))
        assertFalse(File(root, "unbound_wallet.bin").exists())

        val newSeed = ByteArray(32) { 9 }
        val newAddress = EmbeddedWallet.importSeed(context, newSeed)
        assertTrue(newAddress != oldAddress)
        assertEquals(setOf(oldAddress, newAddress), EmbeddedWallet.addresses(context).toSet())
        EmbeddedWallet.select(context, oldAddress)
        assertArrayEquals(oldSeed, EmbeddedWallet.exportSeed(context))
        EmbeddedWallet.select(context, newAddress)
        assertArrayEquals(newSeed, EmbeddedWallet.exportSeed(context))
        EmbeddedWallet.importSeed(context, newSeed)
        assertArrayEquals(newSeed, EmbeddedWallet.exportSeed(context))
        root.deleteRecursively()
    }
}
