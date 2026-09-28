// SPDX-FileCopyrightText: 2026 Black Flag International Limited
// SPDX-License-Identifier: GPL-3.0-only
package app.wayqo.wallet

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SavedPayeeSafetyTest {
    @Test fun staticQrCanBeRevalidatedButDynamicQrKeepsOnlyAReference() {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val context = object : ContextWrapper(base) {
            override fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
                base.getSharedPreferences("isolated_payee_safety_$name", mode)
        }
        context.getSharedPreferences("secure_session", Context.MODE_PRIVATE).edit().clear().commit()
        val payees = SavedPayeeStore(SecureStore(context))
        val thai = ParsedQr("promptpay", "TH", "THB", "", "TEST THAI SHOP", "", "thai-alias", false, "fingerprint")
        val philippine = ParsedQr("qrph", "PH", "PHP", "", "TEST PH SHOP", "", "ph-alias", true, "fingerprint")

        payees.saveMerchant("static-qr", thai, "TH", "")
        payees.saveMerchant("one-use-qr-secret", philippine, "PH", "")
        val saved = payees.list()
        assertEquals("static-qr", saved.single { it.country == "TH" }.payload)
        assertEquals("merchant", saved.single { it.country == "TH" }.kind)
        assertEquals("", saved.single { it.country == "PH" }.payload)
        assertEquals("merchant_reference", saved.single { it.country == "PH" }.kind)

        val unknown = philippine.copy(merchant = "UNKNOWN")
        assertTrue(runCatching { payees.saveMerchant("unknown-qr", unknown, "PH", "") }.isFailure)
        context.getSharedPreferences("secure_session", Context.MODE_PRIVATE).edit().clear().commit()
    }
}
