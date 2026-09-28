// SPDX-FileCopyrightText: 2026 Black Flag International Limited
// SPDX-License-Identifier: GPL-3.0-only
package app.wayqo.wallet

import java.math.BigDecimal

internal data class PaymentFundingStatus(
    val availableUsdc: BigDecimal?,
    val requiredUsdc: BigDecimal?,
) {
    val insufficient: Boolean
        get() = availableUsdc != null && requiredUsdc != null && availableUsdc < requiredUsdc
}

internal fun paymentFundingStatus(
    balanceUsd: String,
    balanceLoaded: Boolean,
    settlementAmount: String,
    settlementCurrency: String,
): PaymentFundingStatus {
    val available = if (balanceLoaded) balanceUsd.removePrefix("$").toBigDecimalOrNull()?.takeIf { it >= BigDecimal.ZERO }
        else null
    val required = if (settlementCurrency.equals("USDC", ignoreCase = true)) {
        settlementAmount.toBigDecimalOrNull()?.takeIf { it > BigDecimal.ZERO }
    } else null
    return PaymentFundingStatus(available, required)
}
