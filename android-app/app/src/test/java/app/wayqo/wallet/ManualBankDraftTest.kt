// SPDX-FileCopyrightText: 2026 Black Flag International Limited
// SPDX-License-Identifier: GPL-3.0-only
package app.wayqo.wallet

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ManualBankDraftTest {
    @Test fun draftNeedsCountryCorridorBankCodeAndAccount() {
        assertFalse(manualDraftComplete("", "970436", "123456789"))
        assertFalse(manualDraftComplete("VN", "Vietcombank", "123456789"))
        assertFalse(manualDraftComplete("VN", "970436", ""))
        assertTrue(manualDraftComplete("VN", "970436", "123456789"))
        assertFalse(manualDraftComplete("BR", "1234", "123456789"))
        assertTrue(manualDraftComplete("BR", "12345678", "123456789"))
    }
}
