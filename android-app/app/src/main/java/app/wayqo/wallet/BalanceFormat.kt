// SPDX-FileCopyrightText: 2026 Black Flag International Limited
// SPDX-License-Identifier: GPL-3.0-only
package app.wayqo.wallet

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

// fiatLocale maps a payout country to the locale whose grouping/decimal conventions
// should render its amounts (e.g. VN and BR group with '.', PH/TH with ',').
internal fun fiatLocale(country: String): Locale = when (country.uppercase()) {
    "VN" -> Locale.forLanguageTag("vi-VN")
    "TH" -> Locale.forLanguageTag("th-TH")
    "ID" -> Locale.forLanguageTag("id-ID")
    "MY" -> Locale.forLanguageTag("ms-MY")
    "PH" -> Locale.forLanguageTag("en-PH")
    "SG" -> Locale.forLanguageTag("en-SG")
    "KH" -> Locale.forLanguageTag("km-KH")
    "BR" -> Locale.forLanguageTag("pt-BR")
    "AR" -> Locale.forLanguageTag("es-AR")
    "CO" -> Locale.forLanguageTag("es-CO")
    "PE" -> Locale.forLanguageTag("es-PE")
    "BO" -> Locale.forLanguageTag("es-BO")
    "IN" -> Locale.forLanguageTag("en-IN")
    else -> Locale.US
}

// formatFiat renders a whole-unit fiat amount with the country's own thousands
// separator, e.g. 20000 -> "20.000" (VN/BR) or "20,000" (PH/TH). The backend sends
// fiatAmount in whole currency units (VND has no decimals).
internal fun formatFiat(amount: Long, country: String): String =
    NumberFormat.getIntegerInstance(fiatLocale(country)).format(amount)

/** Use the payout locale's separators, currency symbol, placement, and decimal digits. */
internal fun formatFiatCurrency(amount: Long, currencyCode: String, country: String): String {
    val currency = Currency.getInstance(currencyCode.uppercase())
    val digits = currency.defaultFractionDigits.coerceAtLeast(0)
    return NumberFormat.getCurrencyInstance(fiatLocale(country)).apply {
        this.currency = currency
        minimumFractionDigits = digits
        maximumFractionDigits = digits
    }.format(amount)
}

/**
 * Groups the digits typed into a payment amount using the payout country's own
 * thousands separator as the user types (VN/BR "20.000", PH/TH "20,000"), while the
 * underlying value stays digits-only. Whole units only — these corridors carry no
 * minor unit at input. An optional currency [symbol] is rendered inline (before or
 * after the number) so it tracks the amount; the text cursor stays aligned to the
 * digits via [OffsetMapping].
 */
internal class FiatInputTransformation(
    country: String,
    private val symbol: String = "",
    private val symbolAfter: Boolean = true,
) : VisualTransformation {
    private val sep: Char = DecimalFormatSymbols(fiatLocale(country)).groupingSeparator

    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text
        val n = digits.length
        if (n == 0) return TransformedText(text, OffsetMapping.Identity)

        val out = StringBuilder()
        for (i in 0 until n) {
            if (i > 0 && (n - i) % 3 == 0) out.append(sep)
            out.append(digits[i])
        }
        val grouped = out.toString()
        val pre = if (symbol.isNotEmpty() && !symbolAfter) "$symbol " else ""
        val suf = if (symbol.isNotEmpty() && symbolAfter) " $symbol" else ""
        val transformed = pre + grouped + suf

        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val o = offset.coerceIn(0, n)
                var seps = 0
                for (i in 1 until n) if (i <= o && (n - i) % 3 == 0) seps++
                return pre.length + o + seps
            }

            override fun transformedToOriginal(offset: Int): Int {
                val start = pre.length
                val end = pre.length + grouped.length
                val o = offset.coerceIn(start, end) - start
                var seen = 0
                for (i in 0 until o) if (grouped[i] != sep) seen++
                return seen
            }
        }
        return TransformedText(AnnotatedString(transformed), mapping)
    }
}

// Western grouping for USD amounts, e.g. 1688.07 -> "1,688.07".
internal fun formatUsdWestern(amount: BigDecimal): String =
    NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2; maximumFractionDigits = 2
    }.format(amount)

internal data class UsdcBalanceDisplay(val main: String, val raisedDigits: String, val fullValue: String)

internal fun formatUsdcBalance(amount: BigDecimal): UsdcBalanceDisplay {
    val full = "$" + amount.toPlainString() + " USDC"
    if (amount > BigDecimal.ZERO && amount < BigDecimal("0.01")) {
        val digits = amount.setScale(6, RoundingMode.DOWN).toPlainString()
            .substringAfter('.').padEnd(6, '0')
        return UsdcBalanceDisplay("$0.00", digits.substring(2, 6), full)
    }
    return UsdcBalanceDisplay("$" + amount.setScale(2, RoundingMode.HALF_UP).toPlainString(), "", full)
}
