// SPDX-FileCopyrightText: 2026 Black Flag International Limited
// SPDX-License-Identifier: GPL-3.0-only
package app.wayqo.wallet

import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

data class SavedPayee(
    val id: String,
    val kind: String,
    val label: String,
    val destination: String,
    val payload: String = "",
    val mint: String = "",
    val country: String = "",
    val network: String = "",
    val note: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Device-local payee store. The complete JSON document is protected by SecureStore's
 * Android Keystore-backed AES-GCM key and excluded from backup/device transfer.
 * Dynamic payment QR payloads are never retained or replayed. They can only be
 * saved as a recipient reference that prompts a fresh scan.
 */
class SavedPayeeStore(private val secureStore: SecureStore) {
    private val storageKey = "savedPayees:v1"

    fun list(): List<SavedPayee> {
        val raw = secureStore.get(storageKey) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val value = array.getJSONObject(index)
                    val kind = value.optString("kind")
                    val destination = value.optString("destination")
                    if (kind !in setOf("merchant", "merchant_reference", "wallet") || destination.isBlank()) continue
                    add(
                        SavedPayee(
                            id = value.getString("id"),
                            kind = kind,
                            label = value.optString("label").take(80),
                            destination = destination.take(512),
                            payload = value.optString("payload").take(4096),
                            mint = value.optString("mint").take(64),
                            country = value.optString("country").take(2).uppercase(),
                            network = value.optString("network").take(8).uppercase(),
                            note = value.optString("note").take(40),
                            updatedAt = value.optLong("updatedAt")
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun saveMerchant(payload: String, parsed: ParsedQr, country: String, network: String, note: String = "") {
        require(recipientNameResolved(parsed.merchant)) { "The recipient name must be verified before saving" }
        require(payload.length in 1..4096 && parsed.account.isNotBlank()) { "Merchant payment details are incomplete" }
        val kind = if (parsed.dynamic) "merchant_reference" else "merchant"
        upsert(
            SavedPayee(
                id = if (parsed.dynamic) identifier(kind, country.uppercase(), parsed.scheme, parsed.account)
                    else identifier("merchant", parsed.scheme, parsed.account),
                kind = kind,
                label = parsed.merchant,
                destination = listOf(parsed.bankBin, parsed.account).filter(String::isNotBlank).joinToString(" · "),
                payload = if (parsed.dynamic) "" else payload,
                country = country.uppercase(),
                network = network.uppercase(),
                note = note.take(40),
            )
        )
    }

    fun saveWallet(address: String, mint: String) {
        val addressValid = runCatching { Base58.decode(address).size == 32 }.getOrDefault(false)
        val mintValid = runCatching { Base58.decode(mint).size == 32 }.getOrDefault(false)
        require(addressValid && mintValid) { "Wallet payment details are invalid" }
        upsert(
            SavedPayee(
                id = identifier("wallet", address, mint),
                kind = "wallet",
                label = "${address.take(6)}…${address.takeLast(6)}",
                destination = address,
                mint = mint
            )
        )
    }

    fun delete(id: String) {
        persist(list().filterNot { it.id == id })
    }

    /** Sets a short user note/nickname (e.g. "RENT") shown beside the payee. */
    fun rename(id: String, note: String) {
        persist(list().map { if (it.id == id) it.copy(note = note.take(40)) else it })
    }

    private fun upsert(payee: SavedPayee) {
        persist(listOf(payee) + list().filterNot { it.id == payee.id }.take(49))
    }

    private fun persist(payees: List<SavedPayee>) {
        val array = JSONArray()
        payees.take(50).forEach { payee ->
            array.put(
                JSONObject()
                    .put("id", payee.id)
                    .put("kind", payee.kind)
                    .put("label", payee.label)
                    .put("destination", payee.destination)
                    .put("payload", payee.payload)
                    .put("mint", payee.mint)
                    .put("country", payee.country)
                    .put("network", payee.network)
                    .put("note", payee.note)
                    .put("updatedAt", payee.updatedAt)
            )
        }
        secureStore.put(storageKey, array.toString())
    }

    private fun identifier(vararg parts: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(parts.joinToString("\u0000").toByteArray(Charsets.UTF_8))
        return digest.take(12).joinToString("") { "%02x".format(it) }
    }
}
