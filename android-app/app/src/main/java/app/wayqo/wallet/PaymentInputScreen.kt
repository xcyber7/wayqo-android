// SPDX-FileCopyrightText: 2026 Black Flag International Limited
// SPDX-License-Identifier: GPL-3.0-only
package app.wayqo.wallet

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Currency
import java.util.Locale
import kotlin.math.roundToInt

/**
 * The single, self-contained payment screen: a Moreta-style black focus screen carrying
 * the app's purple/green accents. The hero is a large amount driven by a compact in-app
 * keypad (no system keyboard), grouped in the country's own convention, white when the
 * wallet can likely cover it and red when it can't. Pay opens the exact quote for
 * review before the wallet signs anything.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PaymentInputScreen(
    state: AppState,
    onAmount: (String) -> Unit,
    onAmountAppend: (String) -> Unit,
    onAmountDelete: () -> Unit,
    onPaymentPurpose: (String) -> Unit,
    onPaymentNote: (String) -> Unit,
    onToggleBalanceHidden: () -> Unit,
    onQuote: () -> Unit,
    onRegister: () -> Unit,
    onSaveMerchant: () -> Unit,
    onBack: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    var noteFocused by remember { mutableStateOf(false) }

    val currency = state.parsed?.currency?.takeIf { it.isNotBlank() }
        ?: state.paymentMarkets.firstOrNull { it.country == state.paymentCountry }?.fiatCurrency?.takeIf { it.isNotBlank() }
        ?: runCatching { Currency.getInstance(Locale.Builder().setRegion(state.paymentCountry).build()).currencyCode }
            .getOrDefault("")
    // Prefer the currency's own glyph (e.g. VND -> "₫") over the ISO code.
    val symbol = runCatching { Currency.getInstance(currency).getSymbol(fiatLocale(state.paymentCountry)) }
        .getOrDefault(currency).ifBlank { currency }
    val currencyAfterNumber = runCatching {
        val formatted = formatFiatCurrency(1, currency, state.paymentCountry)
        formatted.indexOf(symbol) > formatted.indexOfFirst(Char::isDigit)
    }.getOrDefault(true)

    // A fixed-amount QR (VietQR/PIX with an encoded amount) must be paid exactly; lock
    // the field to it so the keypad can't drift the value and fail server validation.
    val encodedAmount = state.parsed?.amount?.substringBefore('.')?.toLongOrNull()?.takeIf { it > 0 }
    val amountLocked = encodedAmount != null
    LaunchedEffect(amountLocked, encodedAmount) {
        if (amountLocked && state.amount != encodedAmount.toString()) onAmount(encodedAmount.toString())
    }

    val amount = state.amount.toLongOrNull()?.takeIf { it > 0 }
    val marketRate = state.paymentMarkets.firstOrNull { it.country == state.paymentCountry }?.exchangeRate
    val available = state.balanceUsd.removePrefix("$").toBigDecimalOrNull()
    val approximateUsd = amount?.let { fiat ->
        runCatching {
            val rate = BigDecimal(marketRate ?: "")
            if (rate.signum() <= 0) null else BigDecimal(fiat).divide(rate, 2, RoundingMode.HALF_UP)
        }.getOrNull()
    }
    val insufficient = available != null && approximateUsd != null && available < approximateUsd
    // This bound protects the keypad/layout; the provider decides actual limits.
    val capReached = state.amount.length >= PAYMENT_MAX_DIGITS
    val heroColor = if (insufficient) PayRed else Color.White

    val recipientResolved = recipientNameResolved(state.parsed?.merchant)
    val recipient = state.parsed?.merchant?.takeIf { recipientResolved } ?: "Recipient unavailable"
    val bank = state.parsed?.let { financialInstitution(it.country, it.bankBin) }
    val corridor = paymentRailBrand(state.paymentCountry, state.parsed?.scheme.orEmpty(), state.paymentNetwork)
    val payoutCountry = runCatching {
        Locale.Builder().setRegion(state.paymentCountry).build().getDisplayCountry(Locale.ENGLISH)
    }.getOrDefault(state.paymentCountry)

    val shake = remember { Animatable(0f) }
    // Initial buzz + shake the moment funds fall short.
    LaunchedEffect(insufficient) {
        if (insufficient) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            for (v in listOf(-16f, 13f, -10f, 7f, -3f, 0f)) shake.animateTo(v, tween(45))
        } else {
            shake.snapTo(0f)
        }
    }
    // Reminder buzz every 2 digits typed while still short.
    LaunchedEffect(state.amount) {
        if (insufficient && state.amount.isNotEmpty() && state.amount.length % 2 == 0) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    val grouped = groupDigits(state.amount, state.paymentCountry)
    // Keep the amount on one line and reserve its height so a grouping separator
    // cannot reflow the column or move the keypad during entry.
    val heroStyle = MaterialTheme.typography.displaySmall.copy(
        fontSize = when {
            grouped.length >= 13 -> 26.sp
            grouped.length >= 10 -> 32.sp
            else -> 40.sp
        },
        lineHeight = 46.sp,
        fontWeight = FontWeight.Bold,
    )

    Column(
        Modifier.fillMaxSize().background(PayBg).padding(horizontal = 10.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Balance • ${localBalanceLabel(state.balanceUsd, marketRate, currency, state.paymentCountry, state.balanceHidden)}",
                style = MaterialTheme.typography.bodyMedium, color = PayTextMuted,
            )
            IconButton(onClick = onToggleBalanceHidden, modifier = Modifier.size(26.dp)) {
                Icon(
                    if (state.balanceHidden) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = if (state.balanceHidden) "Show balance" else "Hide balance",
                    tint = PayTextMuted, modifier = Modifier.size(16.dp),
                )
            }
        }

        // Amount sits just under the balance; a single flexible gap below it (Moreta
        // style) keeps the details + controls packed at the bottom with no scattered gaps.
        Spacer(Modifier.height(10.dp))
        Box(
            Modifier.fillMaxWidth().height(68.dp).offset { IntOffset(shake.value.roundToInt(), 0) },
            contentAlignment = Alignment.Center,
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!currencyAfterNumber) Text(symbol, style = heroStyle, color = heroColor)
                Text(grouped, style = heroStyle, color = heroColor, maxLines = 1, overflow = TextOverflow.Clip)
                if (!amountLocked) BlinkingCaret(color = BrandPurple, height = 42.dp)
                if (currencyAfterNumber) Text(symbol, style = heroStyle, color = heroColor)
            }
        }
        Box(Modifier.fillMaxWidth().height(44.dp), contentAlignment = Alignment.TopCenter) {
            val statusText = when {
                insufficient -> "You're short on funds. Check balance or add more funds."
                amountLocked -> "Amount is set by this QR and can't be changed."
                capReached -> "Input is full. The provider checks payment limits when you continue."
                else -> ""
            }
            if (statusText.isNotBlank()) Text(
                statusText,
                color = if (insufficient) PayRed else PayTextMuted,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
        }

        Spacer(Modifier.weight(1f))

        PayLine("From", "${state.walletName} • ${maskedUsd(state.balanceUsd, state.balanceHidden)}",
            valueColor = if (insufficient) PayRed else Color.White)

        // Keep the verified destination, corridor and account in one stable block.
        Column(Modifier.fillMaxWidth().height(82.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("To • ${corridor.name} • $payoutCountry", color = PayTextMuted,
                style = MaterialTheme.typography.labelMedium, maxLines = 1)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                PaymentInstitutionMark(bank, corridor)
                Text(recipient, color = Color.White, style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            val institutionLabel = bank?.shortName
                ?: state.parsed?.bankBin?.takeIf(String::isNotBlank)?.let { "Participant ID $it" }
            val accountLine = listOfNotNull(institutionLabel, state.parsed?.account?.takeIf(String::isNotBlank)?.let { "Account $it" })
                .joinToString(" • ")
            if (accountLine.isNotBlank()) Text(accountLine, color = PayTextMuted,
                style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }

        PayCategoryChips(state.paymentPurpose, onPaymentPurpose)
        PayReasonField(state.paymentNote, onPaymentNote, onFocusChanged = { noteFocused = it })

        PayTotalRow(
            headline = "You'll pay",
            value = approximateUsd?.let { "≈ $${formatUsdWestern(it)}" } ?: "≈ — USD",
            subtitle = "Indicative USDC • exact debit on the next screen",
        )

        if (!recipientResolved) {
            Text("Recipient name is unverified. Scan another QR before paying.", color = PayRed,
                style = MaterialTheme.typography.labelSmall)
        }
        if (state.message.isNotBlank()) {
            Text(state.message, color = PayTextMuted, style = MaterialTheme.typography.labelSmall, maxLines = 2)
        }

        if (state.userId.isBlank()) {
            OutlinedButton(onClick = onRegister, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                Text("Register payment profile", color = Color.White)
            }
        }
        Button(
            onClick = onQuote,
            enabled = !state.busy && amount != null && !insufficient && state.userId.isNotBlank() && recipientResolved,
            colors = ButtonDefaults.buttonColors(
                containerColor = BrandPurple,
                disabledContainerColor = PaySurface,
                disabledContentColor = PayTextMuted,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Pay") }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            TextButton(onClick = onSaveMerchant, enabled = !state.busy && recipientResolved) { Text("Save payee", color = PayTextMuted) }
            TextButton(onClick = onBack, enabled = !state.busy) { Text("Cancel", color = PayTextMuted) }
        }
        if (!noteFocused && !amountLocked) {
            NumericKeypad(
                onDigit = { d -> if (!capReached) onAmountAppend(d) },
                onBackspace = onAmountDelete,
            )
        }
    }
}
