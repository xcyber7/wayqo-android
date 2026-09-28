// SPDX-FileCopyrightText: 2026 Black Flag International Limited
// SPDX-License-Identifier: GPL-3.0-only
package app.wayqo.wallet

import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

/**
 * Fail-closed validation for backend/provider-built Solana transactions.
 * Versioned messages/address lookup tables are rejected until their resolved account
 * list can be verified. Merchant payments and peer transfers have separate policies.
 */
object SolanaTransactionValidator {
    private const val SYSTEM_PROGRAM = "11111111111111111111111111111111"
    private const val TOKEN_PROGRAM = "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA"
    private const val TOKEN_2022_PROGRAM = "TokenzQdBNbLqP5VEhdkAS6EPFLC1PHnBqCXEpPxuEb"
    private const val ASSOCIATED_TOKEN_PROGRAM = "ATokenGPvbdGVxr1b2hvZbsiqW5xWH25efTNsLJA8knL"
    private const val COMPUTE_BUDGET_PROGRAM = "ComputeBudget111111111111111111111111111111"
    private const val MEMO_PROGRAM = "MemoSq4gqABAXKb96qnH8TysNcWxMyWCqXgDLGmfcHr"
    private const val MEMO_V1_PROGRAM = "Memo1UhkJRfHyvLMcVucJwxXeuD728EqVDDwQDxFMNo"

    data class Summary(val mint: String, val destination: String, val baseUnits: BigInteger)

    private data class ParsedInstruction(val program: String, val accounts: List<String>, val data: ByteArray)
    private data class ParsedTransaction(
        val signatureCount: Int,
        val requiredSignatures: Int,
        val readOnlySigned: Int,
        val keys: List<String>,
        val instructions: List<ParsedInstruction>
    )

    /** Validates the existing provider-built merchant-settlement transaction policy. */
    fun validate(
        transaction: ByteArray,
        wallet: String,
        depositAddress: String,
        settlementAmount: String,
        allowedMints: Set<String>
    ): Summary {
        require(depositAddress.isNotBlank()) { "Provider did not supply a deposit address" }
        require(allowedMints.isNotEmpty()) { "No settlement token mint is allowlisted for this build" }
        val parsed = parse(transaction)
        require(parsed.keys.first() == wallet) { "Connected wallet is not the transaction fee payer" }

        var transfer: Summary? = null
        parsed.instructions.forEach { instruction ->
            when (instruction.program) {
                COMPUTE_BUDGET_PROGRAM, MEMO_PROGRAM, MEMO_V1_PROGRAM -> Unit
                TOKEN_PROGRAM, TOKEN_2022_PROGRAM -> {
                    require(transfer == null) { "Transaction contains multiple value transfers" }
                    transfer = validateTokenTransfer(
                        instruction.data,
                        instruction.accounts,
                        wallet,
                        depositAddress,
                        settlementAmount,
                        allowedMints
                    )
                }
                SYSTEM_PROGRAM -> error("Native SOL transfers are not valid for a stablecoin quote")
                else -> error("Transaction invokes an unapproved program: ${instruction.program}")
            }
        }
        return requireNotNull(transfer) { "Transaction does not contain the expected stablecoin transfer" }
    }

    /**
     * Validates the stricter peer-transfer policy. Exactly two instructions are allowed:
     * an idempotent recipient ATA creation followed by TransferChecked to that same ATA.
     * The ATA program derives and verifies the destination on-chain, while this method
     * binds its owner to the address the user reviewed. A backend cannot redirect funds
     * without making the transaction fail this check or fail atomically on-chain.
     */
    fun validatePeerTransfer(
        transaction: ByteArray,
        wallet: String,
        recipient: String,
        amount: String,
        mint: String,
        allowedMints: Set<String>
    ): Summary {
        require(addressBytes(wallet) != null) { "Connected wallet address is invalid" }
        require(addressBytes(recipient) != null) { "Recipient wallet address is invalid" }
        require(wallet != recipient) { "Sender and recipient must differ" }
        require(mint in allowedMints && allowedMints.isNotEmpty()) { "Transfer token mint is not allowlisted" }

        val parsed = parse(transaction)
        require(parsed.signatureCount == 1 && parsed.requiredSignatures == 1 && parsed.readOnlySigned == 0) {
            "Peer transfer must have exactly one writable signer"
        }
        require(parsed.keys.first() == wallet) { "Connected wallet is not the transaction fee payer" }
        require(parsed.instructions.size == 2) { "Peer transfer must contain exactly two instructions" }

        val create = parsed.instructions[0]
        require(create.program == ASSOCIATED_TOKEN_PROGRAM) { "Recipient token-account creation is missing" }
        require(create.data.contentEquals(byteArrayOf(1))) { "Only idempotent token-account creation is allowed" }
        require(create.accounts.size == 6) { "Recipient token-account instruction layout is invalid" }
        val destination = create.accounts[1]
        require(create.accounts[0] == wallet) { "Connected wallet is not the token-account payer" }
        require(create.accounts[2] == recipient) { "Token-account owner does not match the reviewed recipient" }
        require(create.accounts[3] == mint) { "Token-account mint does not match the reviewed token" }
        require(create.accounts[4] == SYSTEM_PROGRAM && create.accounts[5] == TOKEN_PROGRAM) {
            "Token-account instruction uses an unexpected program"
        }

        val transfer = parsed.instructions[1]
        require(transfer.program == TOKEN_PROGRAM) { "Peer transfer must use the classic SPL Token program" }
        require(transfer.accounts.size == 4) { "Peer TransferChecked layout is invalid" }
        require(transfer.accounts[0] != destination) { "Source and destination token accounts must differ" }
        val summary = validateTokenTransfer(
            transfer.data,
            transfer.accounts,
            wallet,
            destination,
            amount,
            allowedMints
        )
        require(summary.mint == mint) { "Transfer mint does not match the reviewed token" }
        return summary
    }

    private fun validateTokenTransfer(
        data: ByteArray,
        accounts: List<String>,
        wallet: String,
        depositAddress: String,
        settlementAmount: String,
        allowedMints: Set<String>
    ): Summary {
        require(data.size == 10 && data[0].toInt() and 0xff == 12) {
            "Only SPL Token TransferChecked is allowed"
        }
        require(accounts.size in 4..8) { "TransferChecked account layout is invalid" }
        val mint = accounts[1]
        val destination = accounts[2]
        val owner = accounts[3]
        require(owner == wallet) { "Connected wallet is not the token authority" }
        require(destination == depositAddress) { "Transaction destination does not match the order" }
        require(mint in allowedMints) { "Transaction token mint is not allowlisted" }

        val amount = littleEndianUInt64(data, 1)
        val decimals = data[9].toInt() and 0xff
        require(decimals in 0..18) { "Token decimals are invalid" }
        val expected = runCatching {
            BigDecimal(settlementAmount)
                .setScale(decimals, RoundingMode.UNNECESSARY)
                .movePointRight(decimals)
                .toBigIntegerExact()
        }.getOrElse { error("Quoted settlement amount has invalid precision") }
        require(expected.signum() > 0) { "Transfer amount must be positive" }
        require(amount == expected) { "Transaction amount does not match the reviewed amount" }
        return Summary(mint, destination, amount)
    }

    private fun parse(transaction: ByteArray): ParsedTransaction {
        require(transaction.size in 134..1232) { "Transaction size is invalid" }
        val reader = Reader(transaction)
        val signatureCount = reader.shortVec()
        require(signatureCount in 1..16) { "Signature count is invalid" }
        repeat(signatureCount) { reader.bytes(64) }

        val firstHeader = reader.u8()
        require(firstHeader and 0x80 == 0) { "Versioned Solana transactions are not enabled" }
        val requiredSignatures = firstHeader
        val readOnlySigned = reader.u8()
        val readOnlyUnsigned = reader.u8()
        require(requiredSignatures == signatureCount) { "Signature header does not match transaction" }
        require(readOnlySigned < requiredSignatures) { "Transaction fee payer must be writable" }

        val keyCount = reader.shortVec()
        require(keyCount in requiredSignatures..64) { "Account key count is invalid" }
        require(readOnlyUnsigned <= keyCount - requiredSignatures) { "Read-only account header is invalid" }
        val keys = ArrayList<String>(keyCount)
        repeat(keyCount) { keys += Base58.encode(reader.bytes(32)) }
        require(keys.toSet().size == keys.size) { "Transaction contains duplicate account keys" }
        reader.bytes(32) // recent blockhash

        val instructionCount = reader.shortVec()
        require(instructionCount in 1..8) { "Transaction instruction count is invalid" }
        val instructions = ArrayList<ParsedInstruction>(instructionCount)
        repeat(instructionCount) {
            val programIndex = reader.u8()
            require(programIndex < keys.size) { "Instruction program index is invalid" }
            val accountCount = reader.shortVec()
            require(accountCount <= 16) { "Instruction account count is invalid" }
            val accounts = List(accountCount) {
                val index = reader.u8()
                require(index < keys.size) { "Instruction account index is invalid" }
                keys[index]
            }
            val dataLength = reader.shortVec()
            require(dataLength <= 1024) { "Instruction data is too large" }
            instructions += ParsedInstruction(keys[programIndex], accounts, reader.bytes(dataLength))
        }
        require(reader.finished()) { "Transaction has trailing data" }
        return ParsedTransaction(signatureCount, requiredSignatures, readOnlySigned, keys, instructions)
    }

    private fun addressBytes(value: String): ByteArray? = runCatching { Base58.decode(value) }
        .getOrNull()
        ?.takeIf { it.size == 32 && Base58.encode(it) == value }

    private fun littleEndianUInt64(bytes: ByteArray, offset: Int): BigInteger {
        require(offset + 8 <= bytes.size)
        val bigEndian = ByteArray(9)
        for (i in 0 until 8) bigEndian[8 - i] = bytes[offset + i]
        return BigInteger(bigEndian)
    }

    private class Reader(private val bytes: ByteArray) {
        private var offset = 0

        fun u8(): Int {
            require(offset < bytes.size) { "Transaction is truncated" }
            return bytes[offset++].toInt() and 0xff
        }

        fun bytes(length: Int): ByteArray {
            require(length >= 0 && offset + length <= bytes.size) { "Transaction is truncated" }
            return bytes.copyOfRange(offset, offset + length).also { offset += length }
        }

        fun shortVec(): Int {
            var value = 0
            var shift = 0
            repeat(3) { byteIndex ->
                val current = u8()
                value = value or ((current and 0x7f) shl shift)
                if (current and 0x80 == 0) {
                    require(byteIndex == 0 || value >= (1 shl (7 * byteIndex))) { "Compact length is not canonical" }
                    return value
                }
                shift += 7
            }
            error("Compact length is too large")
        }

        fun finished() = offset == bytes.size
    }
}
