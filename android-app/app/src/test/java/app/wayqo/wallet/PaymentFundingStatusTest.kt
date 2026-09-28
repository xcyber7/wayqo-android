// SPDX-FileCopyrightText: 2026 Black Flag International Limited
// SPDX-License-Identifier: GPL-3.0-only
package app.wayqo.wallet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class PaymentFundingStatusTest {
    @Test fun comparesFullUsdcPrecisionBeforeSigning() {
        val short = paymentFundingStatus("$18.770000", true, "18.770001", "USDC")
        assertTrue(short.insufficient)
        assertEquals(BigDecimal("18.770001"), short.requiredUsdc)
        assertFalse(paymentFundingStatus("$18.770001", true, "18.770001", "USDC").insufficient)
    }

    @Test fun unknownBalanceOrDifferentAssetIsNotDeclaredInsufficient() {
        assertFalse(paymentFundingStatus("$0.00", false, "1.00", "USDC").insufficient)
        assertFalse(paymentFundingStatus("$0.00", true, "1.00", "USDT").insufficient)
        assertFalse(paymentFundingStatus("$0.00", true, "invalid", "USDC").insufficient)
    }
}
