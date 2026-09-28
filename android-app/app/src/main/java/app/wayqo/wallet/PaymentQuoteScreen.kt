// SPDX-FileCopyrightText: 2026 Black Flag International Limited
// SPDX-License-Identifier: GPL-3.0-only
package app.wayqo.wallet

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * The exact-debit review: the same black focus screen as amount entry, but the hero
 * amount is now fixed to the provider quote and the total is the exact USDC debit.
 * Final approval signs the transaction with the wallet.
 */
@Composable
fun PaymentQuoteScreen(
    state: AppState,
    onPay: () -> Unit,
    onFunding: () -> Unit,
    onRefreshBalance: () -> Unit,
    onBack: () -> Unit,
    onPaymentPurpose: (String) -> Unit,
) {
    val quote = state.quote
    val funding = paymentFundingStatus(
        state.balanceUsd, state.balanceLoaded,
        quote?.settlementAmount.orEmpty(), quote?.settlementCurrency.orEmpty(),
    )
    val insufficient = funding.insufficient
    val heroColor = if (insufficient) PayRed else Color.White
    val marketRate = state.paymentMarkets.firstOrNull { it.country == state.paymentCountry }?.exchangeRate
    val currency = quote?.fiatCurrency.orEmpty()
    val heroText = quote?.let {
        runCatching { formatFiatCurrency(it.fiatAmount, it.fiatCurrency, state.paymentCountry) }
            .getOrElse { _ -> "${formatFiat(it.fiatAmount, state.paymentCountry)} ${it.fiatCurrency}" }
    }.orEmpty()
    val recipientResolved = recipientNameResolved(state.parsed?.merchant)
    val recipient = state.parsed?.merchant?.takeIf { recipientResolved } ?: "Recipient unavailable"
    val bank = state.parsed?.let { financialInstitution(it.country, it.bankBin) }
    val brand = paymentRailBrand(state.paymentCountry, state.parsed?.scheme.orEmpty(), state.paymentNetwork)

    val shake = remember { Animatable(0f) }
    LaunchedEffect(insufficient, quote?.id) {
        if (insufficient) for (v in listOf(-16f, 13f, -10f, 7f, -3f, 0f)) shake.animateTo(v, tween(45))
        else shake.snapTo(0f)
    }

    Column(
        Modifier.fillMaxSize().background(PayBg).padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            "Balance • ${localBalanceLabel(state.balanceUsd, marketRate, currency, state.paymentCountry, state.balanceHidden)}",
            style = MaterialTheme.typography.bodyMedium,
            color = PayTextMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Box(
            Modifier.fillMaxWidth().weight(1f).offset { IntOffset(shake.value.roundToInt(), 0) },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                heroText,
                color = heroColor,
                fontSize = 46.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (insufficient) {
            Text(
                "You're short on funds. Check balance or add more funds.",
                color = PayRed, style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
            )
        }

        PayLine("From", "${state.walletName} • ${maskedUsd(state.balanceUsd, state.balanceHidden)}", valueColor = if (insufficient) PayRed else Color.White)
        PayLine("To", recipient)
        bank?.let { PayLine("", it.name, valueColor = PayTextMuted) }
        state.parsed?.account?.takeIf(String::isNotBlank)?.let { PayLine("Account", it, valueColor = PayTextMuted) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            PaymentInstitutionMark(bank, brand)
            Spacer(Modifier.size(6.dp))
            Text(brand.name + (state.parsed?.let { if (it.dynamic) " • single-use QR" else " • reusable QR" } ?: ""),
                color = PayTextMuted, style = MaterialTheme.typography.labelMedium)
        }
        if (!recipientResolved) Text("Recipient name is unverified. Scan another QR before paying.",
            color = PayRed, style = MaterialTheme.typography.labelSmall)

        PayLine("Purpose", PAYMENT_PURPOSES.firstOrNull { it.first == state.paymentPurpose }?.second ?: "Other", valueColor = PayTextMuted)

        PayTotalRow(
            headline = "You'll pay exactly",
            value = "${quote?.settlementAmount.orEmpty()} ${quote?.settlementCurrency.orEmpty()}",
            subtitle = "Total with fees • rate ${quote?.exchangeRate.orEmpty()}" +
                (quote?.protocolFeeUsd?.takeIf { it.isNotBlank() }?.let { " • fee $it USD" } ?: ""),
        )

        if (!state.walletAuthorized) {
            Text(
                "Preview only — signing is unavailable for this wallet.",
                color = PayTextMuted, style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(2.dp))
        if (insufficient) {
            OutlinedButton(onClick = onFunding, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                Text("Add money", color = PayBlue)
            }
        }
        Button(
            onClick = onPay,
            enabled = !state.busy && state.walletAuthorized && recipientResolved && !insufficient && quote != null &&
                funding.availableUsdc != null && funding.requiredUsdc != null,
            colors = ButtonDefaults.buttonColors(
                containerColor = PayBlue,
                disabledContainerColor = PaySurface,
                disabledContentColor = PayTextMuted,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Confirm payment") }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            TextButton(onClick = onRefreshBalance, enabled = !state.busy) { Text("Check balance", color = PayTextMuted) }
            TextButton(onClick = onBack, enabled = !state.busy) { Text("Cancel", color = PayTextMuted) }
        }
    }
}
