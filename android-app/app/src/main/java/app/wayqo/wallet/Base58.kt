// SPDX-FileCopyrightText: 2026 Black Flag International Limited
// SPDX-License-Identifier: GPL-3.0-only
package app.wayqo.wallet

import java.math.BigInteger

object Base58 {
    private const val alphabet = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"

    fun encode(input: ByteArray): String {
        if (input.isEmpty()) return ""
        var value = BigInteger(1, input)
        val base = BigInteger.valueOf(58)
        val output = StringBuilder()
        while (value > BigInteger.ZERO) {
            val parts = value.divideAndRemainder(base)
            output.append(alphabet[parts[1].toInt()])
            value = parts[0]
        }
        input.takeWhile { it == 0.toByte() }.forEach { _ -> output.append('1') }
        return output.reverse().toString()
    }

    fun decode(input: String): ByteArray {
        require(input.isNotEmpty()) { "Base58 value is empty" }
        val base = BigInteger.valueOf(58)
        var value = BigInteger.ZERO
        input.forEach { character ->
            val digit = alphabet.indexOf(character)
            require(digit >= 0) { "Invalid Base58 character" }
            value = value.multiply(base).add(BigInteger.valueOf(digit.toLong()))
        }
        var decoded = value.toByteArray()
        if (value == BigInteger.ZERO) decoded = ByteArray(0)
        else if (decoded[0] == 0.toByte()) decoded = decoded.copyOfRange(1, decoded.size)
        val zeros = input.takeWhile { it == '1' }.length
        return ByteArray(zeros) + decoded
    }
}
