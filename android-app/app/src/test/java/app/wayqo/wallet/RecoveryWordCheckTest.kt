// SPDX-FileCopyrightText: 2026 Black Flag International Limited
// SPDX-License-Identifier: GPL-3.0-only
package app.wayqo.wallet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

class RecoveryWordCheckTest {
    @Test fun usesDistinctPositionsAndDoesNotAlwaysAskTheSameWords() {
        val random = Random(731)
        val draws = (1..20).map { chooseRecoveryWordPositions(random = random) }
        draws.forEach { positions ->
            assertEquals(3, positions.size)
            assertEquals(3, positions.distinct().size)
            assertTrue(positions.all { it in 1..24 })
        }
        assertTrue(draws.distinct().size > 1)
    }
}
