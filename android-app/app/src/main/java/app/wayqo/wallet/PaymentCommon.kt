// SPDX-FileCopyrightText: 2026 Black Flag International Limited
// SPDX-License-Identifier: GPL-3.0-only
package app.wayqo.wallet

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormatSymbols

internal fun categoryIcon(code: String): ImageVector = when (code) {
    "food_dining" -> Icons.Filled.Restaurant
    "groceries" -> Icons.Filled.LocalGroceryStore
    "shopping" -> Icons.Filled.ShoppingBag
    "goods_services" -> Icons.Filled.Storefront
    "transport" -> Icons.Filled.DirectionsCar
    "travel" -> Icons.Filled.Flight
    "accommodation" -> Icons.Filled.Hotel
    "bills_utilities" -> Icons.AutoMirrored.Filled.ReceiptLong
    "entertainment" -> Icons.Filled.Movie
    "health" -> Icons.Filled.LocalHospital
    "education" -> Icons.Filled.School
    "gift" -> Icons.Filled.CardGiftcard
    "personal_transfer" -> Icons.Filled.Person
    else -> Icons.Filled.MoreHoriz
}

/** Groups a digit string with the payout country's own separator (VN/BR "20.000",
 *  PH/TH "20,000"), for read-only display alongside the custom keypad. */
internal fun groupDigits(digits: String, country: String): String {
    if (digits.isEmpty()) return ""
    val sep = DecimalFormatSymbols(fiatLocale(country)).groupingSeparator
    val n = digits.length
    val sb = StringBuilder()
    for (i in 0 until n) {
        if (i > 0 && (n - i) % 3 == 0) sb.append(sep)
        sb.append(digits[i])
    }
    return sb.toString()
}

/** A blinking text caret for the custom-keypad amount field. */
@Composable
internal fun BlinkingCaret(color: Color, height: Dp) {
    val transition = rememberInfiniteTransition(label = "caret")
    val alpha by transition.animateFloat(
        initialValue = 1f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "caretAlpha",
    )
    Box(Modifier.padding(horizontal = 3.dp).width(3.dp).height(height).background(color.copy(alpha = alpha)))
}

/** Compact in-app numeric keypad — the familiar device keypad layout, just smaller and
 *  themed for the dark screen (clean keys with a press ripple, no heavy tiles). Replaces
 *  the system keyboard for the amount so the screen layout is fully controlled. */
@Composable
internal fun NumericKeypad(onDigit: (String) -> Unit, onBackspace: () -> Unit, modifier: Modifier = Modifier) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("", "0", "⌫"),
    )
    val scope = rememberCoroutineScope()
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                row.forEach { key ->
                    val keyModifier = when {
                        key.isEmpty() -> Modifier
                        key == "⌫" -> Modifier.pointerInput(Unit) {
                            // Tap deletes one; press-and-hold auto-repeats (accelerating).
                            detectTapGestures(onPress = {
                                onBackspace()
                                val job = scope.launch {
                                    delay(350)
                                    var interval = 90L
                                    while (true) {
                                        onBackspace()
                                        delay(interval)
                                        if (interval > 35L) interval -= 6L
                                    }
                                }
                                tryAwaitRelease()
                                job.cancel()
                            })
                        }
                        else -> Modifier.clickable { onDigit(key) }
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .then(keyModifier),
                        contentAlignment = Alignment.Center,
                    ) {
                        when {
                            key == "⌫" -> Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Delete", tint = Color.White)
                            key.isNotEmpty() -> Text(key, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Normal)
                        }
                    }
                }
            }
        }
    }
}

/** Horizontally-scrolling category pills (the payment purpose). Compact dark chips;
 *  the selected one takes the brand accent. */
@Composable
internal fun PayCategoryChips(selected: String, onSelect: (String) -> Unit) {
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PAYMENT_PURPOSES.forEach { (code, label) ->
            val active = selected == code
            Row(
                Modifier
                    .clip(RoundedCornerShape(22.dp))
                    .background(if (active) BrandPurple else PaySurface)
                    .clickable { onSelect(if (active) "" else code) }
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Icon(
                    categoryIcon(code),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    label,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

/** Optional local payee note. Reports focus so the caller can hide the custom
 *  keypad while the system keyboard is up. */
@Composable
internal fun PayReasonField(value: String, onValue: (String) -> Unit, onFocusChanged: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(PaySurface)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            value = value,
            onValueChange = { onValue(it.take(40)) },
            singleLine = true,
            textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
            cursorBrush = SolidColor(BrandPurple),
            modifier = Modifier.fillMaxWidth().onFocusChanged { onFocusChanged(it.isFocused) },
            decorationBox = { inner ->
                if (value.isEmpty()) Text("Payee note if saved (optional)", color = PayTextMuted, fontSize = 15.sp)
                inner()
            },
        )
    }
}

/** A left label / right value line (From, To …). */
@Composable
internal fun PayLine(label: String, value: String, valueColor: Color = Color.White) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = PayTextMuted, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(
            value,
            color = valueColor,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** The "You'll pay exactly / Total with fees" summary with the USD figure. */
@Composable
internal fun PayTotalRow(headline: String, value: String, subtitle: String = "Total with fees") {
    Row(Modifier.fillMaxWidth().height(54.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(headline, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(subtitle, color = PayTextMuted, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(value, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = PayTextMuted)
    }
}

/** Balance rendered in the payout country's own currency, e.g. "255,618 VND", from
 *  the USD balance and the indicative market rate (local units per USD). */
internal fun localBalanceLabel(balanceUsd: String, marketRate: String?, currency: String, country: String, hidden: Boolean): String {
    if (hidden) return "••••"
    val usd = balanceUsd.removePrefix("$").toBigDecimalOrNull() ?: return "$balanceUsd USDC"
    val rate = marketRate?.toBigDecimalOrNull()
    if (rate == null || rate.signum() <= 0) return "$balanceUsd USDC"
    val local = usd.multiply(rate).setScale(0, RoundingMode.DOWN)
    return "${formatFiat(local.toLong(), country)} $currency"
}

/** USD balance for the From line, masked when the user has hidden the balance. */
internal fun maskedUsd(balanceUsd: String, hidden: Boolean): String =
    if (hidden) "•••• USD" else "${balanceUsd.removePrefix("$")} USD"
