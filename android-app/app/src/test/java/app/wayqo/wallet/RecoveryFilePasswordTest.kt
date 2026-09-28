// SPDX-FileCopyrightText: 2026 Black Flag International Limited
// SPDX-License-Identifier: GPL-3.0-only
package app.wayqo.wallet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecoveryFilePasswordTest {
    @Test fun generatedPasswordsAreDistinctAndEasyToRead() {
        val generated = (1..32).map { RecoveryFilePassword.generate() }
        assertEquals(32, generated.distinct().size)
        assertTrue(generated.all { it.matches(Regex("[A-HJ-NP-Za-km-z2-9]{6}(-[A-HJ-NP-Za-km-z2-9]{6}){3}")) })
    }
}
