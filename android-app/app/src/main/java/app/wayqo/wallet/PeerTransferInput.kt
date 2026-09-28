// SPDX-FileCopyrightText: 2026 Black Flag International Limited
// SPDX-License-Identifier: GPL-3.0-only
package app.wayqo.wallet

import java.net.URLDecoder
import java.nio.charset.StandardCharsets

data class PeerRecipient(val address: String, val mint: String?)

/** Parses either a raw wallet address or a conservative Solana Pay recipient URI. */
object PeerTransferInput {
    fun parse(raw: String): PeerRecipient {
        val value = raw.trim()
        require(value.isNotEmpty() && value.length <= 512) { "Enter a recipient wallet address" }
        val (address, query) = if (value.startsWith("solana:", ignoreCase = true)) {
            val payload = value.substringAfter(':')
            require(!payload.startsWith("//")) { "Solana Pay recipient URI is invalid" }
            payload.substringBefore('?') to payload.substringAfter('?', "")
        } else {
            require(':' !in value && '?' !in value && '#' !in value) { "Recipient wallet address is invalid" }
            value to ""
        }
        requireCanonicalAddress(address, "Recipient wallet address is invalid")

        val parameters = mutableMapOf<String, String>()
        if (query.isNotEmpty()) {
            query.split('&').filter(String::isNotEmpty).forEach { item ->
                val name = decode(item.substringBefore('='))
                val content = decode(item.substringAfter('=', ""))
                require(name !in parameters) { "Solana Pay URI contains duplicate parameters" }
                parameters[name] = content
            }
        }
        val mint = parameters["spl-token"]?.also {
            requireCanonicalAddress(it, "Solana Pay token mint is invalid")
        }
        return PeerRecipient(address, mint)
    }

    private fun requireCanonicalAddress(value: String, message: String) {
        val bytes = runCatching { Base58.decode(value) }.getOrNull()
        require(bytes?.size == 32 && Base58.encode(bytes) == value) { message }
    }

    private fun decode(value: String): String = runCatching {
        URLDecoder.decode(value, StandardCharsets.UTF_8.name())
    }.getOrElse { error("Solana Pay URI contains invalid encoding") }
}
