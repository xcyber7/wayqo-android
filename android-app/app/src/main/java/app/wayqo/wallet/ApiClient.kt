// SPDX-FileCopyrightText: 2026 Black Flag International Limited
// SPDX-License-Identifier: GPL-3.0-only
package app.wayqo.wallet

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.UUID

data class Challenge(val id: String, val message: String)
data class Capabilities(
    val environment: String,
    val paymentMode: String,
    val walletAuthorized: Boolean,
    val provider: String,
    val merchantPayments: Boolean,
    val peerTransfers: Boolean,
)
data class Onboarding(val userId: String, val kycStatus: String, val markets: JSONObject) {
    // Whether the given market (ISO 3166-1 alpha-2) is approved for this user.
    fun isApproved(country: String): Boolean {
        val value = markets.opt(country.uppercase())
        if (value == true) return true
        val status = (value as? JSONObject)?.optString("status")?.uppercase().orEmpty()
        return status in setOf("APPROVED", "ENABLED", "ACTIVE", "VERIFIED", "AVAILABLE")
    }
}
data class ParsedQr(
    val scheme: String,
    val country: String,
    val currency: String,
    val amount: String,
    val merchant: String,
    val bankBin: String,
    val account: String,
    val dynamic: Boolean,
    val payloadFingerprint: String,
)
data class DetectedQr(
    val detected: Boolean,
    val country: String,
    val scheme: String,
    val known: Boolean,
    val enabled: Boolean,
    val networkRequired: Boolean
)
data class PaymentMarket(
    val country: String,
    val fiatCurrency: String,
    val settlementCurrency: String,
    val scheme: String,
    val exchangeRate: String,
    val status: String
) {
    val approved: Boolean get() = status.uppercase() in setOf("APPROVED", "ENABLED", "ACTIVE", "VERIFIED", "AVAILABLE")
}
data class Quote(
    val id: String,
    val fiatAmount: Long,
    val fiatCurrency: String,
    val settlementAmount: String,
    val settlementCurrency: String,
    val exchangeRate: String,
    val protocolFeeUsd: String,
    val minimumFeeUsd: String,
    val expiresAt: String
)
data class Order(
    val id: String,
    val status: Int,
    val label: String,
    val chainId: Int,
    val depositAddress: String,
    val encodedTransaction: String,
    val transactionHash: String
)
data class PreparedTransfer(
    val transferId: String,
    val encodedTransaction: String,
    val recipient: String,
    val recipientTokenAccount: String,
    val mint: String,
    val amount: String,
    val baseUnits: String,
    val decimals: Int,
    val lastValidBlockHeight: Long
)
data class ActivityItem(
    val id: String,
    val kind: String,
    val status: String,
    val recipient: String,
    val fiatAmount: Long,
    val fiatCurrency: String,
    val settlementAmount: String,
    val settlementCurrency: String,
    val exchangeRate: String,
    val protocolFeeUsd: String,
    val minimumFeeUsd: String,
    val transactionHash: String,
    val repeatable: Boolean,
    val createdAt: String
)
data class ActivityPage(val items: List<ActivityItem>, val nextCursor: String)

class ApiException(
    val code: String = "",
    val status: Int = 0,
    message: String,
) : Exception(message) {
    val requiresKyc: Boolean get() = code == "kyc_required"
}

class ApiClient(private val baseUrl: String, private val store: SecureStore) {
    suspend fun challenge(wallet: String, publicKey: String): Challenge {
        val json = request("POST", "/v1/auth/challenge", JSONObject().put("walletAddress", wallet).put("publicKey", publicKey), false)
        return Challenge(json.getString("challengeId"), json.getString("message"))
    }

    suspend fun verify(challenge: Challenge, wallet: String, publicKey: String, signature: String): String {
        val body = JSONObject()
            .put("challengeId", challenge.id)
            .put("walletAddress", wallet)
            .put("publicKey", publicKey)
            .put("signature", signature)
        val token = request("POST", "/v1/auth/verify", body, false).getString("token")
        store.put("apiToken", token)
        return token
    }

    suspend fun capabilities(): Capabilities {
        val json = request("GET", "/v1/capabilities", null)
        val features = json.optJSONObject("supportedFeatures") ?: JSONObject()
        return Capabilities(
            environment = json.optString("environment", "sandbox"),
            paymentMode = json.optString("paymentMode", "disabled"),
            walletAuthorized = json.optBoolean("walletAuthorized"),
            provider = json.optString("provider"),
            merchantPayments = features.optBoolean("merchantPayments"),
            peerTransfers = features.optBoolean("peerTransfers"),
        )
    }

    suspend fun onboard(residencyCountry: String? = null): Onboarding {
        val create = residencyCountry != null
        val body = residencyCountry?.let { JSONObject().put("residencyCountry", it.uppercase()) }
        val json = request(if (create) "POST" else "GET", "/v1/onboarding", body)
        val user = json.getJSONObject("user")
        return Onboarding(user.getString("userId"), user.optString("kycStatus"), json.getJSONObject("marketAccess").getJSONObject("markets"))
    }

    suspend fun kycLink(): String = request(
        "POST",
        "/v1/onboarding/kyc",
        JSONObject().put("callbackUrl", BuildConfig.KYC_CALLBACK_URL)
    ).getString("url")

    suspend fun deleteAccount() {
        request("POST", "/v1/account/delete", null)
    }

    // Returns the signed subject-access-request envelope verbatim so the exact
    // signed bytes are preserved for the PDF and for independent verification.
    suspend fun exportAccount(): JSONObject = request("GET", "/v1/account/export", null)

    suspend fun markets(): List<PaymentMarket> {
        val array = request("GET", "/v1/markets?settlementCurrency=USDC", null).getJSONArray("markets")
        return buildList {
            for (index in 0 until array.length()) {
                val value = array.getJSONObject(index)
                add(PaymentMarket(
                    country = value.getString("country"),
                    fiatCurrency = value.getString("fiatCurrency"),
                    settlementCurrency = value.optString("settlementCurrency", "USDC"),
                    scheme = value.optString("scheme", "provider"),
                    exchangeRate = value.optString("exchangeRate"),
                    status = value.optString("status", "UNAVAILABLE")
                ))
            }
        }
    }

    suspend fun detectQr(payload: String): DetectedQr {
        val json = request("POST", "/v1/qr/detect", JSONObject().put("payload", payload))
        return DetectedQr(
            detected = json.optBoolean("detected"),
            country = json.optString("country").uppercase(),
            scheme = json.optString("scheme"),
            known = json.optBoolean("known"),
            enabled = json.optBoolean("enabled"),
            networkRequired = json.optBoolean("networkRequired")
        )
    }

    suspend fun parseQr(payload: String, country: String, network: String = ""): ParsedQr {
        val body = JSONObject().put("payload", payload).put("country", country.uppercase())
        if (network.isNotBlank()) body.put("network", network.uppercase())
        val json = request("POST", "/v1/qr/parse", body)
        val expectedFingerprint = qrPayloadFingerprint(payload)
        val returnedFingerprint = json.optString("payloadFingerprint")
        if (returnedFingerprint != expectedFingerprint) {
            throw ApiException(message = "The server returned details for a different QR. Scan again.")
        }
        if (!recipientNameResolved(json.optString("merchantName"))) {
            throw ApiException(code = "recipient_unverified", message = "The payment provider could not verify the recipient name. Ask for another QR code.")
        }
        return ParsedQr(
            scheme = json.optString("scheme"),
            country = json.optString("country"),
            currency = json.optString("currency"),
            amount = json.optString("amount"),
            merchant = json.optString("merchantName"),
            bankBin = json.optString("bankBin"),
            account = json.optString("accountNumber"),
            dynamic = json.optBoolean("dynamic"),
            payloadFingerprint = returnedFingerprint,
        )
    }

    suspend fun quote(payload: String, amount: Long, userId: String, country: String, network: String, paymentPurpose: String): Quote {
        val body = JSONObject()
            .put("qrString", payload)
            .put("amount", amount)
            .put("chainId", 101)
            .put("settlementCurrency", "USDC")
            .put("userId", userId)
            .put("country", country.uppercase())
            .put("sourceOfFundsConfirmed", true)
            .put("paymentPurpose", paymentPurpose)
        if (network.isNotBlank()) body.put("network", network.uppercase())
        val json = request("POST", "/v1/quotes", body)
        return Quote(
            id = json.getString("quoteId"),
            fiatAmount = json.getLong("fiatAmount"),
            fiatCurrency = json.getString("fiatCurrency"),
            settlementAmount = json.getString("settlementAmount"),
            settlementCurrency = json.getString("settlementCurrency"),
            exchangeRate = json.optString("exchangeRate"),
            protocolFeeUsd = json.optString("protocolFeeUsd"),
            minimumFeeUsd = json.optString("minimumFeeUsd"),
            expiresAt = json.optString("expiresAt")
        )
    }

    suspend fun placeOrder(quoteId: String): Order {
        val storageKey = "orderAttempt:$quoteId"
        val idempotencyKey = store.get(storageKey) ?: UUID.randomUUID().toString().also { store.put(storageKey, it) }
        return order(request("POST", "/v1/orders", JSONObject().put("quoteId", quoteId), extraHeaders = mapOf("Idempotency-Key" to idempotencyKey)))
    }

    suspend fun verifyOrder(orderId: String, hash: String): Order = order(
        request("POST", "/v1/orders/${URLEncoder.encode(orderId, "UTF-8")}/verify", JSONObject().put("transactionHash", hash))
    )

    suspend fun orderStatus(orderId: String): Order = order(request("GET", "/v1/orders/${URLEncoder.encode(orderId, "UTF-8")}", null))

    suspend fun prepareTransfer(recipient: String, amount: String, mint: String): PreparedTransfer {
        val body = JSONObject()
            .put("recipient", recipient)
            .put("amount", amount)
            .put("mint", mint)
        val json = request("POST", "/v1/transfers/prepare", body)
        return PreparedTransfer(
            transferId = json.getString("transferId"),
            encodedTransaction = json.getString("encodedTransaction"),
            recipient = json.getString("recipient"),
            recipientTokenAccount = json.getString("recipientTokenAccount"),
            mint = json.getString("mint"),
            amount = json.getString("amount"),
            baseUnits = json.getString("baseUnits"),
            decimals = json.getInt("decimals"),
            lastValidBlockHeight = json.getLong("lastValidBlockHeight")
        )
    }

    suspend fun markTransferSubmitted(transferId: String, transactionHash: String): ActivityItem = activity(
        request(
            "POST",
            "/v1/transfers/$transferId/submitted",
            JSONObject().put("transactionHash", transactionHash)
        )
    )

    suspend fun activity(cursor: String = "", limit: Int = 50): ActivityPage {
        val query = buildString {
            append("/v1/activity?limit=").append(limit.coerceIn(1, 100))
            if (cursor.isNotBlank()) append("&cursor=").append(java.net.URLEncoder.encode(cursor, Charsets.UTF_8.name()))
        }
        val json = request("GET", query, null)
        val array = json.getJSONArray("items")
        val items = buildList {
            for (index in 0 until array.length()) add(activity(array.getJSONObject(index)))
        }
        return ActivityPage(items, json.optString("nextCursor"))
    }

    private fun activity(json: JSONObject) = ActivityItem(
        id = json.getString("id"),
        kind = json.optString("kind"),
        status = json.optString("status"),
        recipient = json.optString("recipient"),
        fiatAmount = json.optLong("fiatAmount"),
        fiatCurrency = json.optString("fiatCurrency"),
        settlementAmount = json.optString("settlementAmount"),
        settlementCurrency = json.optString("settlementCurrency"),
        exchangeRate = json.optString("exchangeRate"),
        protocolFeeUsd = json.optString("protocolFeeUsd"),
        minimumFeeUsd = json.optString("minimumFeeUsd"),
        transactionHash = json.optString("transactionHash"),
        repeatable = json.optBoolean("repeatable"),
        createdAt = json.optString("createdAt")
    )

    private fun order(json: JSONObject) = Order(
        json.getString("orderId"),
        json.optInt("status"),
        json.optString("statusLabel"),
        json.optInt("chainId"),
        json.optString("depositAddress"),
        json.optString("encodedTransaction"),
        json.optString("transactionHash")
    )

    private suspend fun request(
        method: String,
        path: String,
        body: JSONObject?,
        authenticated: Boolean = true,
        extraHeaders: Map<String, String> = emptyMap()
    ): JSONObject = withContext(Dispatchers.IO) {
        val connection = URL(baseUrl.trimEnd('/') + path).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 8_000
            connection.readTimeout = 12_000
            connection.useCaches = false
            connection.setRequestProperty("Cache-Control", "no-store")
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Content-Type", "application/json")
            if (authenticated) {
                val token = store.get("apiToken") ?: throw ApiException(message = "Wallet session missing")
                connection.setRequestProperty("Authorization", "Bearer $token")
            }
            extraHeaders.forEach(connection::setRequestProperty)
            if (body != null) {
                connection.doOutput = true
                connection.outputStream.use { it.write(body.toString().toByteArray()) }
            }
            val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
            val responseBytes = stream?.use(::readBoundedResponse) ?: ByteArray(0)
            val raw = responseBytes.toString(Charsets.UTF_8)
            val json = runCatching { JSONObject(raw) }.getOrElse { throw ApiException(message = "Server returned an invalid response") }
            if (connection.responseCode !in 200..299) {
                val apiError = json.optJSONObject("error")
                val message = apiError?.optString("message").orEmpty()
                throw ApiException(
                    code = apiError?.optString("code").orEmpty(),
                    status = connection.responseCode,
                    message = message.ifBlank { "Request failed (${connection.responseCode})" },
                )
            }
            json
        } finally {
            connection.disconnect()
        }
    }

    private fun qrPayloadFingerprint(payload: String): String = MessageDigest
        .getInstance("SHA-256")
        .digest(payload.toByteArray(Charsets.UTF_8))
        .take(8)
        .joinToString("") { "%02x".format(it.toInt() and 0xff) }

    private fun readBoundedResponse(input: InputStream): ByteArray {
        val maximum = 1_048_576
        val output = ByteArrayOutputStream(8_192)
        val buffer = ByteArray(8_192)
        var total = 0
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            if (read == 0) continue
            total += read
            if (total > maximum) throw ApiException(message = "Server response exceeded the size limit")
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }
}
