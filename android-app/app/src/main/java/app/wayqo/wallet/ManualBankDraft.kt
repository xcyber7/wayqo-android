// SPDX-FileCopyrightText: 2026 Black Flag International Limited
// SPDX-License-Identifier: GPL-3.0-only
package app.wayqo.wallet

/** A manual draft is never a provider-verified or payable recipient. */
internal fun manualBankLabel(country: String): String = when (country.uppercase()) {
    "VN" -> "Receiving bank BIN (6 digits)"
    "BR" -> "Receiving bank ISPB (8 digits)"
    "TH" -> "Receiving bank code"
    "SG" -> "Receiving bank code"
    "PH" -> "Receiving bank identifier or name"
    else -> "Receiving bank identifier or name"
}

internal fun manualDraftComplete(country: String, bank: String, account: String): Boolean {
    if (country.length != 2 || bank.isBlank() || account.isBlank()) return false
    val validBank = when (country.uppercase()) {
        "VN" -> bank.matches(Regex("[0-9]{6}"))
        "BR" -> bank.matches(Regex("[0-9]{8}"))
        else -> bank.trim().length in 2..80
    }
    return validBank && account.trim().length in 4..100
}
