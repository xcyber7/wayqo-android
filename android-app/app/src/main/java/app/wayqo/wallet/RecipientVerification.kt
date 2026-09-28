// SPDX-FileCopyrightText: 2026 Black Flag International Limited
// SPDX-License-Identifier: GPL-3.0-only
package app.wayqo.wallet

/** A rail or account alias does not establish the recipient's identity. */
internal fun recipientNameResolved(name: String?): Boolean {
    val normalized = name.orEmpty().trim().replace(Regex("\\s+"), " ").uppercase()
    return normalized.isNotEmpty() && normalized !in setOf(
        "UNKNOWN", "UNVERIFIED", "UNAVAILABLE", "N/A", "NA", "NULL", "NOT AVAILABLE", "NOT PROVIDED",
    )
}
