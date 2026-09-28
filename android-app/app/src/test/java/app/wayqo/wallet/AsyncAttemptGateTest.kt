// SPDX-FileCopyrightText: 2026 Black Flag International Limited
// SPDX-License-Identifier: GPL-3.0-only
package app.wayqo.wallet

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AsyncAttemptGateTest {
    @Test
    fun newerImageInvalidatesEarlierDecodeCallback() {
        val gate = AsyncAttemptGate()
        val first = gate.begin()
        val second = gate.begin()

        assertFalse(gate.isCurrent(first))
        assertTrue(gate.isCurrent(second))
    }
}
