// SPDX-FileCopyrightText: 2026 Black Flag International Limited
// SPDX-License-Identifier: GPL-3.0-only
package app.wayqo.wallet

import android.icu.util.ULocale
import java.util.Locale
import com.google.i18n.phonenumbers.PhoneNumberUtil

/**
 * A selectable country of residence: ISO 3166-1 alpha-2 code, the English name, the
 * name in the country's own primary language/script (endonym), and its flag emoji.
 */
data class Country(
    val code: String,
    val english: String,
    val native: String,
    val flag: String,
    val callingCode: String,
)

/**
 * Jurisdictions refused at onboarding. MUST stay in sync with the backend blocklist in
 * internal/policy/policy.go (US/HK by launch strategy; CU/IR/KP/SY by the
 * current conservative risk policy). The
 * backend is authoritative and re-validates every residency, so this list is only a
 * client-side courtesy to avoid offering a country that will be rejected.
 */
val EXCLUDED_RESIDENCIES = setOf("US", "HK", "CU", "IR", "KP", "SY")

/** Regional-indicator flag emoji for a 2-letter ISO code, e.g. "VN" -> 🇻🇳. */
private fun flagEmoji(code: String): String {
    val cc = code.uppercase()
    if (cc.length != 2 || !cc.all { it in 'A'..'Z' }) return "🏳️"
    val first = 0x1F1E6 + (cc[0] - 'A')
    val second = 0x1F1E6 + (cc[1] - 'A')
    return String(Character.toChars(first)) + String(Character.toChars(second))
}

/**
 * The country's name in its own primary language, via ICU likely-subtags (e.g. VN ->
 * "Việt Nam", SA -> "المملكة العربية السعودية", JP -> "日本"). Falls back to the English
 * name when ICU cannot resolve a native form.
 */
private fun nativeName(code: String, english: String): String = try {
    val region = ULocale.Builder().setRegion(code).build()
    val likely = ULocale.addLikelySubtags(region)
    val displayLocale = ULocale.Builder().setLanguage(likely.language).setRegion(code).build()
    region.getDisplayCountry(displayLocale).ifBlank { english }
} catch (_: Exception) {
    english
}

/**
 * All real ISO 3166-1 countries minus the excluded set, sorted by English name. This
 * is the residency picker's source: residency is confirmed during the Didit/Gaian
 * identity check, so the picker is broad by design rather than limited to the
 * settlement corridors.
 */
val RESIDENCY_COUNTRIES: List<Country> by lazy {
    Locale.getISOCountries()
        .map { it.uppercase() }
        .filter { it !in EXCLUDED_RESIDENCIES }
        .mapNotNull { code ->
            val english = Locale("", code).getDisplayCountry(Locale.ENGLISH)
            if (english.isBlank() || english.equals(code, ignoreCase = true)) return@mapNotNull null
            val dialing = PhoneNumberUtil.getInstance().getCountryCodeForRegion(code)
            Country(code, english, nativeName(code, english), flagEmoji(code), if (dialing > 0) "+$dialing" else "")
        }
        .sortedBy { it.english }
}

/** Look up a selected country by ISO code for display. */
fun countryByCode(code: String): Country? =
    RESIDENCY_COUNTRIES.firstOrNull { it.code.equals(code, ignoreCase = true) }
