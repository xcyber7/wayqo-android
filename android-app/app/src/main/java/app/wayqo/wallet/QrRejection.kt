// SPDX-FileCopyrightText: 2026 Black Flag International Limited
// SPDX-License-Identifier: GPL-3.0-only
package app.wayqo.wallet

import java.net.URI

internal const val SCANNER_ERROR_PAUSE_MS = 3_000L

/** Format hints only: never open a decoded link or treat it as a verified recipient. */
internal fun unsupportedQrReason(payload: String): String {
    val text = payload.trim()
    if (text.length > 4096) return "This code contains more data than a supported payment QR. Ask for a payment QR instead."
    val scheme = text.substringBefore(':', "").lowercase()
    when (scheme) {
        "bitcoin" -> return "Bitcoin payment QRs aren’t supported. Use a Solana USDC or supported local payment QR."
        "lightning", "lnurl" -> return "Bitcoin Lightning QRs aren’t supported. Use a Solana USDC or supported local payment QR."
        "ethereum" -> return "Ethereum / EVM payment QRs aren’t supported by this scanner yet."
        "litecoin", "dogecoin", "monero", "bitcoincash", "tron", "ripple" ->
            return "This cryptocurrency payment format isn’t supported. Use a Solana USDC or supported local payment QR."
        "wc" -> return "This is a WalletConnect connection QR, rather than a payment QR."
        "wifi" -> return "This is a Wi-Fi setup QR, rather than a payment QR."
        "mailto", "tel", "sms", "smsto", "geo" -> return "This is a contact or location QR, rather than a payment QR."
        "solana" -> return "This Solana QR doesn’t contain a supported wallet recipient. Request a Solana USDC wallet QR."
    }
    if (text.startsWith("BEGIN:VCARD", true)) return "This is a contact card QR, rather than a payment QR."
    if (text.startsWith("lnbc", true) || text.startsWith("lntb", true) || text.startsWith("lnurl1", true))
        return "This looks like a Bitcoin Lightning code. Lightning payments aren’t supported."
    if (text.matches(Regex("0[xX][0-9a-fA-F]{40}")))
        return "This looks like an Ethereum / EVM wallet address. EVM transfers aren’t supported by this scanner yet."
    val link = runCatching { URI(text) }.getOrNull()
    if (link?.scheme?.lowercase() in setOf("http", "https")) {
        if (link?.host.equals("weixin.qq.com", true) && link?.path.orEmpty().startsWith("/r/"))
            return "This is a WeChat profile or social link. Ask for the merchant’s payment QR."
        return "This is a web link without a recognized payment instruction. Ask for the merchant’s payment QR."
    }
    if (text.startsWith("000201") || text.startsWith("000202"))
        return "The local payment data is incomplete or uses an unsupported format. Ask for another payment QR."
    return "No supported wallet recipient or local payment instruction was found. Ask for a payment QR or choose another image."
}
