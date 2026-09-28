// SPDX-FileCopyrightText: 2026 Black Flag International Limited
// SPDX-License-Identifier: GPL-3.0-only
package app.wayqo.wallet

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipientVerificationTest {
    @Test fun placeholderNamesAreNotPayable() {
        listOf(null, "", " unknown ", "UNVERIFIED", "N/A", "not available", "\t").forEach {
            assertFalse(recipientNameResolved(it))
        }
        assertTrue(recipientNameResolved("JUAN DELA CRUZ"))
    }
}
