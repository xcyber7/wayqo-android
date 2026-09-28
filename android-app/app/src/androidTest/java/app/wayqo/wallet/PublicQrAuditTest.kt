// SPDX-FileCopyrightText: 2026 Black Flag International Limited
// SPDX-License-Identifier: GPL-3.0-only
package app.wayqo.wallet

import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Opt-in, read-only audit. No wallet/session writes, order creation, or signing. */
@RunWith(AndroidJUnit4::class)
class PublicQrAuditTest {
    @Test fun downloadedSamplesDecodeAndUseExistingVerificationGates() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val root = File(context.filesDir, "qr-public-audit")
        val manifest = File(root, "downloaded.json")
        assumeTrue("Public audit samples must be supplied explicitly", manifest.isFile)
        val providerChecks = InstrumentationRegistry.getArguments().getString("providerChecks") == "true"
        val api = ApiClient(BuildConfig.API_BASE_URL, SecureStore(context))
        val user = if (providerChecks) api.onboard() else null
        val rows = JSONArray(manifest.readText())
        val results = JSONArray()
        val scanner = newQrImageDecoder()
        try {
            for (i in 0 until rows.length()) {
                val row = rows.getJSONObject(i)
                val image = row.getString("image")
                require(image.matches(Regex("[A-Z]{2}-[a-f0-9]{12}\\.png")))
                val bitmap = BitmapFactory.decodeFile(File(root, image).absolutePath)
                assertTrue("Unreadable downloaded image: $image", bitmap != null)
                val values = scanner.decodeBitmap(bitmap)
                bitmap.recycle()
                val exact = values.size == 1 && values.single() == row.getString("payload")
                val result = JSONObject().put("image", image).put("country", row.getString("country"))
                    .put("source", row.getString("source")).put("imageDecodedExactly", exact)
                    .put("decoder", BuildConfig.QR_DECODER)
                results.put(result)
                if (!exact) continue
                val payload = values.single()
                result.put("androidRoute", classifyScannedQr(payload).name)
                if (!providerChecks) continue
                try {
                    val detected = api.detectQr(payload)
                    result.put("backendDetected", detected.detected).put("backendCountry", detected.country)
                } catch (error: ApiException) {
                    result.put("detectError", error.code).put("detectHttpStatus", error.status)
                }
                val country = row.getString("country")
                val network = row.optString("network")
                try {
                    val parsed = api.parseQr(payload, country, network)
                    result.put("recipientResolved", recipientNameResolved(parsed.merchant))
                        .put("returnedCountry", parsed.country).put("currency", parsed.currency)
                    val amount = parsed.amount.toLongOrNull()?.takeIf { it > 0 } ?: when (country) {
                        "VN" -> 20_000L
                        "AR" -> 10_000L
                        "KH", "CO" -> 10_000L
                        "PH" -> 50L
                        "TH" -> 20L
                        else -> 10L
                    }
                    try {
                        val quote = api.quote(payload, amount, user!!.userId, country, network, "other")
                        result.put("quoteReceived", true).put("quoteFiatCurrency", quote.fiatCurrency)
                            .put("quoteSettlementCurrency", quote.settlementCurrency)
                    } catch (error: ApiException) {
                        result.put("quoteReceived", false).put("quoteError", error.code)
                            .put("quoteHttpStatus", error.status)
                    }
                } catch (error: ApiException) {
                    result.put("recipientResolved", false).put("parseError", error.code)
                        .put("parseHttpStatus", error.status)
                }
            }
        } finally {
            scanner.close()
            // Raw QR data and recipient identifiers are deliberately omitted.
            File(root, "device-results.json").writeText(results.toString(2))
        }
        assertTrue("Downloaded samples must decode independently; see device-results.json",
            (0 until results.length()).all { results.getJSONObject(it).getBoolean("imageDecodedExactly") })
    }
}
