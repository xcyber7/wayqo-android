// SPDX-FileCopyrightText: 2026 Black Flag International Limited
// SPDX-License-Identifier: GPL-3.0-only
package app.wayqo.wallet

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PaymentMarketTest {
    @Test
    fun `provider approval aliases match backend gate`() {
        for (status in listOf("APPROVED", "enabled", "ACTIVE", "verified", "AVAILABLE")) {
            assertTrue(market(status).approved)
        }
        for (status in listOf("", "PENDING", "REJECTED", "UNAVAILABLE")) {
            assertFalse(market(status).approved)
        }
    }

    @Test
    fun `only documented corridors parse before KYC`() {
        assertTrue(canParseQrBeforeKyc("VN"))
        assertTrue(canParseQrBeforeKyc("ph"))
        for (country in listOf("BR", "AR", "PE", "BO", "TH", "KH", "CO")) {
            assertFalse("$country must wait for verified identity", canParseQrBeforeKyc(country))
        }
    }

    private fun market(status: String) = PaymentMarket(
        country = "VN",
        fiatCurrency = "VND",
        settlementCurrency = "USDC",
        scheme = "vietqr",
        exchangeRate = "1",
        status = status,
    )
}
