// SPDX-FileCopyrightText: 2026 Black Flag International Limited
// SPDX-License-Identifier: GPL-3.0-only
package app.wayqo.wallet

import org.junit.Assert.assertEquals
import org.junit.Test

class ScannedQrRouteTest {
    @Test fun encryptedEnvelopeNeedsCountrySelectionAndProviderVerification() {
        val envelope = java.util.Base64.getEncoder().encodeToString(ByteArray(256) { (it % 256).toByte() }) + "|" +
            java.util.Base64.getEncoder().encodeToString(ByteArray(18) { it.toByte() })
        assertEquals(ScannedQrRoute.PROVIDER_PAYMENT, classifyScannedQr(envelope))
        assertEquals(ScannedQrRoute.UNKNOWN, classifyScannedQr("ordinary|text"))
        assertEquals(ScannedQrRoute.UNKNOWN, classifyScannedQr(envelope + "|extra"))
        assertEquals(ScannedQrRoute.UNKNOWN, classifyScannedQr(envelope.replace('|', ':')))
    }
    @Test fun distinguishesWalletLocalPaymentAndUnrelatedQr() {
        assertEquals(ScannedQrRoute.SOLANA,
            classifyScannedQr("solana:11111111111111111111111111111111"))
        assertEquals(ScannedQrRoute.LOCAL_PAYMENT,
            classifyScannedQr("0002015802VN6304ABCD"))
        assertEquals(ScannedQrRoute.LOCAL_PAYMENT,
            classifyScannedQr("00020101021238600010A00000072701300006970407011697040000000000180208QRIBFTTA53037045802VN63047A5B"))
        assertEquals(ScannedQrRoute.LOCAL_PAYMENT,
            classifyScannedQr("wxp://example"))
        assertEquals(ScannedQrRoute.UNKNOWN, classifyScannedQr("https://example.com"))
        assertEquals(ScannedQrRoute.UNKNOWN, classifyScannedQr("0002015802VN"))
    }

    @Test fun socialLinksAndLookalikeHostsAreNotPaymentQrs() {
        listOf(
            "http://weixin.qq.com/r/mp/dlocal-social-account",
            "https://wechat.com/",
            "https://qr.alipay.com.evil.example/abc",
            "https://example.com/?url=qr.95516.com/pay",
            "https://weixin.qq.com@evil.example/g/abc",
        ).forEach { assertEquals(ScannedQrRoute.UNKNOWN, classifyScannedQr(it)) }
        assertEquals(ScannedQrRoute.LOCAL_PAYMENT, classifyScannedQr("https://qr.alipay.com/abc"))
    }
}
