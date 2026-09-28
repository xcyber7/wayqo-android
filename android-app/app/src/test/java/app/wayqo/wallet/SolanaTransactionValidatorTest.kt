// SPDX-FileCopyrightText: 2026 Black Flag International Limited
// SPDX-License-Identifier: GPL-3.0-only
package app.wayqo.wallet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.Base64

class SolanaTransactionValidatorTest {
    private val walletBytes = ByteArray(32) { 1 }
    private val sourceBytes = ByteArray(32) { 2 }
    private val destinationBytes = ByteArray(32) { 3 }
    private val mintBytes = ByteArray(32) { 4 }
	private val recipientBytes = ByteArray(32) { 5 }
    private val wallet = Base58.encode(walletBytes)
    private val destination = Base58.encode(destinationBytes)
    private val mint = Base58.encode(mintBytes)
	private val recipient = Base58.encode(recipientBytes)

    @Test
    fun acceptsExactAllowlistedTransferChecked() {
        val summary = SolanaTransactionValidator.validate(
            transaction = transferChecked(1_250_000),
            wallet = wallet,
            depositAddress = destination,
            settlementAmount = "1.25",
            allowedMints = setOf(mint)
        )
        assertEquals(mint, summary.mint)
        assertEquals(destination, summary.destination)
    }

    @Test
    fun rejectsMismatchedAmount() {
        assertThrows(IllegalArgumentException::class.java) {
            SolanaTransactionValidator.validate(
                transferChecked(1_250_001), wallet, destination, "1.25", setOf(mint)
            )
        }
    }

    @Test
    fun rejectsUnapprovedMint() {
        assertThrows(IllegalArgumentException::class.java) {
            SolanaTransactionValidator.validate(
                transferChecked(1_250_000), wallet, destination, "1.25", setOf(Base58.encode(ByteArray(32) { 9 }))
            )
        }
    }

	@Test
	fun acceptsStrictPeerTransfer() {
		val summary = SolanaTransactionValidator.validatePeerTransfer(
			peerTransferChecked(1_250_000), wallet, recipient, "1.25", mint, setOf(mint)
		)
		assertEquals(mint, summary.mint)
		assertEquals(destination, summary.destination)
	}

	@Test
	fun rejectsPeerTransferWhoseAtaOwnerIsNotReviewedRecipient() {
		assertThrows(IllegalArgumentException::class.java) {
			SolanaTransactionValidator.validatePeerTransfer(
				peerTransferChecked(1_250_000, ataOwner = ByteArray(32) { 9 }),
				wallet,
				recipient,
				"1.25",
				mint,
				setOf(mint)
			)
		}
	}

	@Test
	fun rejectsPeerTransferToDifferentDestination() {
		assertThrows(IllegalArgumentException::class.java) {
			SolanaTransactionValidator.validatePeerTransfer(
				peerTransferChecked(1_250_000, transferDestinationIndex = 4),
				wallet,
				recipient,
				"1.25",
				mint,
				setOf(mint)
			)
		}
	}

	@Test
    fun rejectsPeerTransferWithExtraInstruction() {
		val transaction = peerTransferChecked(1_250_000)
		// The instruction count sits after signatures, header, eight keys, and blockhash.
		transaction[1 + 64 + 3 + 1 + (8 * 32) + 32] = 3
		assertThrows(IllegalArgumentException::class.java) {
			SolanaTransactionValidator.validatePeerTransfer(
				transaction, wallet, recipient, "1.25", mint, setOf(mint)
			)
		}
	}

    private fun transferChecked(amount: Long): ByteArray {
        val tokenProgram = Base58.decode("TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA")
        val out = ByteArrayOutputStream()
        out.write(1) // one signature
        out.write(ByteArray(64))
        out.write(byteArrayOf(1, 0, 2)) // message header
        out.write(5)
        listOf(walletBytes, sourceBytes, destinationBytes, mintBytes, tokenProgram).forEach(out::write)
        out.write(ByteArray(32))
        out.write(1) // instructions
        out.write(4) // token program index
        out.write(4)
        out.write(byteArrayOf(1, 3, 2, 0)) // source, mint, destination, authority
        out.write(10)
        out.write(12) // TransferChecked
        repeat(8) { shift -> out.write(((amount ushr (shift * 8)) and 0xff).toInt()) }
        out.write(6)
        return out.toByteArray()
    }

    @Test
    fun acceptsTransactionFixtureProducedByGoBackend() {
        val backendSender = "AKnL4NNf3DGWZJS6cPknBuEGnVsV4A4m5tgebLHaRSZ9"
        val backendRecipient = "9hSR6S7WPtxmTojgo6GG3k4yDPecgJY292j7xrsUGWBu"
        val usdc = "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v"
        val encoded = "AQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAABAAUIiojj3XQJ8ZX9UtstPLpdcspnCb8dlBIb83SIAbQPb1wryQI4aGkCwmHzZMYU8eyrT/PgX1gaHAgFP8dT003nbIxE6dOLQfkqpDMEQDsQJEW33I+Jy32RDC01S0/+en4TAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAG3fbh12Whk9nL4UbO63msHLSF7V9bN5E6jPWFfv8AqYE5dw6ofRdfVqNUZsNMfszLjYqRtO43ol32D1uPybOUjJclj04kifG7PRApFI4NgwtaE5na/xCEBI572Nvp+FnG+nrzvtutOj1l82qryXQxsbvkwtL24OR8pgIDRS9dYQMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAgYGAAIFBwMEAQEEBAEHAgAKDGDsUwAAAAAABg=="
        val summary = SolanaTransactionValidator.validatePeerTransfer(
            Base64.getDecoder().decode(encoded),
            backendSender,
            backendRecipient,
            "5.5",
            usdc,
            setOf(usdc)
        )
        assertEquals("5500000", summary.baseUnits.toString())
    }

	private fun peerTransferChecked(
		amount: Long,
		ataOwner: ByteArray = recipientBytes,
		transferDestinationIndex: Int = 2
	): ByteArray {
		val systemProgram = Base58.decode("11111111111111111111111111111111")
		val tokenProgram = Base58.decode("TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA")
		val ataProgram = Base58.decode("ATokenGPvbdGVxr1b2hvZbsiqW5xWH25efTNsLJA8knL")
		val out = ByteArrayOutputStream()
		out.write(1)
		out.write(ByteArray(64))
		out.write(byteArrayOf(1, 0, 5))
		out.write(8)
		listOf(walletBytes, sourceBytes, destinationBytes, mintBytes, ataOwner, systemProgram, tokenProgram, ataProgram).forEach(out::write)
		out.write(ByteArray(32))
		out.write(2)
		out.write(7)
		out.write(6)
		out.write(byteArrayOf(0, 2, 4, 3, 5, 6))
		out.write(1)
		out.write(1)
		out.write(6)
		out.write(4)
		out.write(byteArrayOf(1, 3, transferDestinationIndex.toByte(), 0))
		out.write(10)
		out.write(12)
		repeat(8) { shift -> out.write(((amount ushr (shift * 8)) and 0xff).toInt()) }
		out.write(6)
		return out.toByteArray()
	}
}
