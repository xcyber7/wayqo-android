// SPDX-FileCopyrightText: 2026 Black Flag International Limited
// SPDX-License-Identifier: GPL-3.0-only
package app.wayqo.wallet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class BalanceFormatTest {
    @Test fun subCentBalanceKeepsSixDecimalsAccessible() {
        val shown = formatUsdcBalance(BigDecimal("0.001234"))
        assertEquals("$0.00", shown.main)
        assertEquals("1234", shown.raisedDigits)
        assertEquals("$0.001234 USDC", shown.fullValue)
    }

    @Test fun ordinaryBalancesShowCents() {
        assertEquals("$12.35", formatUsdcBalance(BigDecimal("12.345678")).main)
        assertEquals("$0.00", formatUsdcBalance(BigDecimal.ZERO).main)
    }

    @Test fun fiatUsesDestinationSeparatorsAndSymbol() {
        val vietnam = formatFiatCurrency(20_000, "VND", "VN")
        assertTrue(vietnam.contains("20.000"))
        assertTrue(vietnam.contains("₫"))
        val brazil = formatFiatCurrency(20_000, "BRL", "BR")
        assertTrue(brazil.contains("20.000,00"))
        assertTrue(brazil.contains("R$"))
    }
}
