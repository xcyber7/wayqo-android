// SPDX-FileCopyrightText: 2026 Black Flag International Limited
// SPDX-License-Identifier: GPL-3.0-only
package app.wayqo.wallet

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.net.HttpURLConnection
import java.net.URL

/**
 * Minimal Solana JSON-RPC client: broadcasts locally-signed transactions (embedded
 * wallet path) and reads SPL token balances for the USD balance display.
 */
class SolanaRpc(private val endpoint: String) {

    /** Broadcasts a fully-signed transaction; returns the base58 transaction signature. */
    suspend fun sendTransaction(signedTx: ByteArray): String {
        val base64 = Base64.encodeToString(signedTx, Base64.NO_WRAP)
        val params = JSONArray().apply {
            put(base64)
            put(
                JSONObject().apply {
                    put("encoding", "base64")
                    put("skipPreflight", false)
                    put("preflightCommitment", "confirmed")
                    put("maxRetries", 5)
                },
            )
        }
        val json = call("sendTransaction", params)
        return json.optString("result").ifBlank { error("Solana RPC did not return a signature") }
    }

    /** Total UI balance (already decimal-adjusted) of [mint] tokens owned by [owner]. */
    suspend fun tokenBalance(owner: String, mint: String): BigDecimal {
        val params = JSONArray().apply {
            put(owner)
            put(JSONObject().put("mint", mint))
            put(JSONObject().put("encoding", "jsonParsed"))
        }
        val json = call("getTokenAccountsByOwner", params)
        val accounts = json.optJSONObject("result")?.optJSONArray("value") ?: return BigDecimal.ZERO
        var total = BigDecimal.ZERO
        for (i in 0 until accounts.length()) {
            val amount = runCatching {
                accounts.getJSONObject(i)
                    .getJSONObject("account").getJSONObject("data")
                    .getJSONObject("parsed").getJSONObject("info")
                    .getJSONObject("tokenAmount").optString("uiAmountString", "0")
            }.getOrElse { "0" }
            total = total.add(runCatching { BigDecimal(amount.ifBlank { "0" }) }.getOrElse { BigDecimal.ZERO })
        }
        return total
    }

    private suspend fun call(method: String, params: JSONArray): JSONObject = withContext(Dispatchers.IO) {
        val body = JSONObject().apply {
            put("jsonrpc", "2.0")
            put("id", 1)
            put("method", method)
            put("params", params)
        }.toString()
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 15_000
            readTimeout = 30_000
            setRequestProperty("Content-Type", "application/json")
        }
        val text = try {
            connection.outputStream.use { it.write(body.toByteArray()) }
            val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
            stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        } finally {
            connection.disconnect()
        }
        val json = runCatching { JSONObject(text) }
            .getOrElse { error("Solana RPC returned an unreadable response") }
        json.optJSONObject("error")?.let { err ->
            error("Solana RPC error: ${err.optString("message", err.toString())}")
        }
        json
    }
}
