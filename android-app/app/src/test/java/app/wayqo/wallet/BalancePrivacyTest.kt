// SPDX-FileCopyrightText: 2026 Black Flag International Limited
// SPDX-License-Identifier: GPL-3.0-only
package app.wayqo.wallet

import org.junit.Assert.assertEquals
import org.junit.Test

class BalancePrivacyTest {
    @Test
    fun hiddenBalanceNeverIncludesTheAmount() {
        assertEquals("••••••", displayBalance("\$12,345.67", true))
    }

    @Test
    fun visibleBalanceIsUnchanged() {
        assertEquals("\$12.34", displayBalance("\$12.34", false))
    }
}
