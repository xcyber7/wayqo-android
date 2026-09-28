// SPDX-FileCopyrightText: 2026 Black Flag International Limited
// SPDX-License-Identifier: GPL-3.0-only
package app.wayqo.wallet

import java.security.SecureRandom
import java.util.Random

/** Selects fresh, distinct, one-based positions for each new wallet check. */
internal fun chooseRecoveryWordPositions(
    wordCount: Int = 24,
    checkCount: Int = 3,
    random: Random = SecureRandom(),
): List<Int> {
    require(wordCount > 0 && checkCount in 1..wordCount)
    val positions = (1..wordCount).toMutableList()
    for (index in 0 until checkCount) {
        val other = index + random.nextInt(wordCount - index)
        val value = positions[index]
        positions[index] = positions[other]
        positions[other] = value
    }
    return positions.take(checkCount).sorted()
}
