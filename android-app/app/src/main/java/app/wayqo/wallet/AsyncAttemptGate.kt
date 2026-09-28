// SPDX-FileCopyrightText: 2026 Black Flag International Limited
// SPDX-License-Identifier: GPL-3.0-only
package app.wayqo.wallet

/**
 * Invalidates callbacks from asynchronous work that has been superseded by a
 * newer attempt. Gallery QR decoding uses this so a slow, older image can never
 * replace the image the user most recently selected.
 */
internal class AsyncAttemptGate {
    private var current = 0L

    fun begin(): Long {
        current += 1
        return current
    }

    fun isCurrent(attempt: Long): Boolean = attempt == current
}
