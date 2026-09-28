// SPDX-FileCopyrightText: 2026 Black Flag International Limited
// SPDX-License-Identifier: GPL-3.0-only
package app.wayqo.wallet

import java.net.URI
import java.util.Base64

internal enum class ScannedQrRoute { SOLANA, LOCAL_PAYMENT, PROVIDER_PAYMENT, UNKNOWN }

/** Offline route hint only. The backend remains authoritative for recipient verification. */
internal fun classifyScannedQr(payload: String): ScannedQrRoute {
    if (runCatching { PeerTransferInput.parse(payload) }.isSuccess) return ScannedQrRoute.SOLANA
    val text = payload.trim()
    if (text.length > 4096) return ScannedQrRoute.UNKNOWN
    // Observed encrypted bank QR envelope. Its country and beneficiary cannot be
    // decoded locally: require an explicit market choice and provider verification.
    if (isEncryptedBankQrCandidate(text)) return ScannedQrRoute.PROVIDER_PAYMENT
    val link = runCatching { URI(text) }.getOrNull()
    val protocol = link?.scheme.orEmpty().lowercase()
    val host = link?.host.orEmpty().lowercase()
    val path = link?.path.orEmpty()
    val web = protocol in setOf("http", "https")
    val paymentLink = link?.userInfo == null && (
        (protocol == "wxp" && host.isNotEmpty()) ||
        (web && host == "weixin.qq.com" && path.startsWith("/g/")) ||
        (protocol in setOf("alipays", "alipayqr") && host.isNotEmpty()) ||
        (web && host == "qr.alipay.com" && path.length > 1) ||
        (web && host == "render.alipay.com" && path.startsWith("/p/")) ||
        (web && host == "qr.95516.com" && path.length > 1)
    )
    if (paymentLink) {
        return ScannedQrRoute.LOCAL_PAYMENT
    }
    if (!text.startsWith("000201") && !text.startsWith("000202")) return ScannedQrRoute.UNKNOWN
    var offset = 0
    var country = ""
    var checksumSeen = false
    while (offset + 4 <= text.length) {
        val tag = text.substring(offset, offset + 2)
        val length = text.substring(offset + 2, offset + 4).toIntOrNull() ?: return ScannedQrRoute.UNKNOWN
        if (!tag.all(Char::isDigit) || offset + 4 + length > text.length) return ScannedQrRoute.UNKNOWN
        val value = text.substring(offset + 4, offset + 4 + length)
        if (tag == "58") country = value
        if (tag == "63") checksumSeen = length == 4 && offset + 8 == text.length
        offset += 4 + length
    }
    return if (offset == text.length && checksumSeen && country.matches(Regex("[A-Z]{2}")))
        ScannedQrRoute.LOCAL_PAYMENT else ScannedQrRoute.UNKNOWN
}

internal fun isEncryptedBankQrCandidate(text: String): Boolean {
    val parts = text.split('|')
    if (parts.size != 2 || text.length > 4096) return false
    val decoded = parts.map { part ->
        if (!part.matches(Regex("[A-Za-z0-9+/]+={0,2}"))) return false
        val bytes = runCatching { Base64.getDecoder().decode(part) }.getOrNull() ?: return false
        if (Base64.getEncoder().encodeToString(bytes) != part) return false
        bytes
    }
    return decoded[0].size in 128..3072 && decoded[0].size % 16 == 0 && decoded[1].size in 16..32
}
