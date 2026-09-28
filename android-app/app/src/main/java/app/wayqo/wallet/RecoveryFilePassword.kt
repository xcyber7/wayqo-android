// SPDX-FileCopyrightText: 2026 Black Flag International Limited
// SPDX-License-Identifier: GPL-3.0-only
package app.wayqo.wallet

import java.security.SecureRandom

/** Portable-file password: 24 independent characters, grouped for easier reading. */
internal object RecoveryFilePassword {
    private const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789"
    private val random = SecureRandom()

    fun generate(): String = (0 until 4).joinToString("-") {
        (0 until 6).map { ALPHABET[random.nextInt(ALPHABET.length)] }.joinToString("")
    }
}
