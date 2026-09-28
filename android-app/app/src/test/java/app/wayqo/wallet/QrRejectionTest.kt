// SPDX-FileCopyrightText: 2026 Black Flag International Limited
// SPDX-License-Identifier: GPL-3.0-only
package app.wayqo.wallet

import org.junit.Assert.assertTrue
import org.junit.Test

class QrRejectionTest {
    @Test fun explainsFormatsWithoutOpeningLinksOrEchoingQrContents() {
        mapOf(
            "bitcoin:private-recipient" to "Bitcoin",
            "ethereum:private-recipient" to "Ethereum",
            "wc:private-session-secret" to "WalletConnect",
            "WIFI:S:private-network;P:private-password;;" to "Wi-Fi",
            "BEGIN:VCARD\nprivate-contact\nEND:VCARD" to "contact card",
            "https://weixin.qq.com/r/mp/private-profile" to "WeChat profile",
            "https://example.com/private-secret" to "web link",
            "solana:not-a-wallet" to "Solana",
            "000201broken-private-data" to "incomplete",
        ).forEach { (payload, expected) ->
            val reason = unsupportedQrReason(payload)
            assertTrue(reason, reason.contains(expected))
            assertTrue("QR data must not be echoed", !reason.contains("private"))
            assertTrue(classifyScannedQr(payload) == ScannedQrRoute.UNKNOWN)
        }
        assertTrue(unsupportedQrReason("lnbc123secret").contains("Lightning"))
        assertTrue(unsupportedQrReason("0x" + "a".repeat(40)).contains("EVM"))
        assertTrue(unsupportedQrReason("x".repeat(4097)).contains("more data"))
    }
}
