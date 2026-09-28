// SPDX-FileCopyrightText: 2026 Black Flag International Limited
// SPDX-License-Identifier: GPL-3.0-only
package app.wayqo.wallet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PaymentBrandingTest {
    @Test
    fun resolvesPublishedVietQrBankBin() {
        val institution = financialInstitution("VN", "970407")
        assertEquals("Techcombank", institution?.shortName)
        assertEquals("https://cdn.vietqr.io/img/TCB.png", institution?.logoUrl)
    }

    @Test
    fun doesNotGuessInstitutionForAnotherCountry() {
        assertNull(financialInstitution("PH", "970407"))
    }

    @Test
    fun peruBrandUsesChosenNetwork() {
        assertEquals("Yape", paymentRailBrand("PE", "provider", "YAPE").name)
        assertEquals("Plin", paymentRailBrand("PE", "provider", "PLIN").name)
    }
}
