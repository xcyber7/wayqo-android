// SPDX-FileCopyrightText: 2026 Black Flag International Limited
// SPDX-License-Identifier: GPL-3.0-only
package app.wayqo.wallet

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.Base64

private val sarInk = Color.rgb(0x14, 0x14, 0x16)
private val sarMuted = Color.rgb(0x6b, 0x6b, 0x72)
private val sarAccent = Color.rgb(0x0a, 0x6e, 0x5a)
private val sarRule = Color.rgb(0xd8, 0xd8, 0xda)

/**
 * Renders a signed subject-access-request envelope (as returned by
 * GET /v1/account/export) into a human-readable PDF. The document's authority
 * comes from the Ed25519 signature over the exact payload bytes — this PDF is a
 * presentation wrapper that also embeds the signed payload and signature
 * verbatim on a final "Verification record" page, plus a QR pointer, so the
 * export can be checked independently against Wayqo's published public key.
 */
object SarPdf {
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 42f

    fun write(context: Context, envelope: JSONObject): Uri {
        val payloadB64 = envelope.optString("payload")
        val signature = envelope.optJSONObject("signature") ?: JSONObject()
        val payloadBytes = runCatching { Base64.getDecoder().decode(payloadB64) }.getOrDefault(ByteArray(0))
        val document = runCatching { JSONObject(String(payloadBytes, Charsets.UTF_8)) }.getOrDefault(JSONObject())

        val pdf = PdfDocument()
        val w = Writer(pdf)
        w.newPage()

        // ---- Header ----
        w.text("WAYQO", w.brand)
        w.text("Subject Access Request — data we hold about you", w.h1)
        w.gap(6f)
        val issuer = document.optJSONObject("issuer") ?: JSONObject()
        w.text("Issued by " + issuer.optString("name", "Black Flag International Limited") +
            " (company no. " + issuer.optString("companyNumber", "3117200") + ", " +
            issuer.optString("jurisdiction", "Hong Kong") + "), acting as data controller.", w.body)
        w.text("Contact: " + issuer.optString("contact", "privacy@wayqo.app"), w.body)
        w.gap(10f)
        w.divider()
        w.gap(8f)
        w.kv("Report ID", document.optString("reportId", "—"))
        w.kv("Issued at (UTC)", document.optString("issuedAt", "—"))
        w.kv("Environment", document.optString("environment", "—"))
        w.kv("Subject wallet", document.optJSONObject("subject")?.optString("wallet") ?: "—")
        w.gap(12f)

        val records = document.optJSONObject("records") ?: JSONObject()

        // ---- Profile ----
        w.heading("Profile")
        val profile = records.optJSONObject("profile")
        if (profile == null || profile.length() == 0) {
            w.text("No profile record is held.", w.body)
        } else {
            renderObject(w, profile)
        }
        w.gap(10f)

        // ---- Activity ----
        w.heading("Payment activity")
        renderArray(w, records.optJSONArray("activity"), "No payment activity is recorded.")
        w.gap(10f)

        // ---- Orders ----
        w.heading("Orders")
        renderArray(w, records.optJSONArray("orders"), "No orders are recorded.")
        w.gap(10f)

        // ---- Quotes ----
        w.heading("Quotes")
        renderArray(w, records.optJSONArray("quotes"), "No quotes are recorded.")
        w.gap(10f)

        // ---- Audit log ----
        w.heading("Audit log (tamper-evident)")
        w.text("Each entry is hash-chained to the previous one; the hashes let us prove the log has not been altered. These records are retained where required by anti-money-laundering obligations.", w.small)
        w.gap(4f)
        renderArray(w, records.optJSONArray("auditEvents"), "No audit entries are recorded.")
        w.gap(10f)

        // ---- Disclosures ----
        w.heading("Disclosures")
        val disclosures = document.optJSONObject("disclosures") ?: JSONObject()
        disclosures.optJSONArray("recipientCategories")?.let { cats ->
            w.text("Categories of recipients:", w.label)
            for (i in 0 until cats.length()) w.text("•  " + cats.optString(i), w.body, indent = 8f)
            w.gap(4f)
        }
        for (key in listOf("retention", "onChainNote", "selfCustodyNote", "rightsNote")) {
            val value = disclosures.optString(key)
            if (value.isNotBlank()) { w.text(value, w.body); w.gap(3f) }
        }

        // ---- Verification page ----
        w.newPage()
        w.text("Verification record", w.h1)
        w.gap(6f)
        w.text("This export is authenticated by an Ed25519 digital signature over the exact data payload below — not by this document's layout. Anyone can confirm it is genuine and unaltered without contacting Wayqo:", w.body)
        w.gap(6f)
        w.text("1.  Obtain Wayqo's public signing key from https://www.wayqo.app/.well-known/wayqo-attestation-key.json (or GET /v1/attestation/key) and confirm its key ID matches below.", w.body)
        w.text("2.  Base64-decode the payload and verify the signature over those exact bytes with the public key.", w.body)
        w.text("3.  The decoded payload is the JSON data shown in this report. If any byte was altered, the signature will not verify.", w.body)
        w.gap(10f)
        w.kv("Algorithm", signature.optString("alg", "Ed25519"))
        w.kv("Key ID", signature.optString("keyId", "—"))
        val sha256 = sha256Hex(payloadBytes)
        w.kv("Payload SHA-256", sha256)
        w.gap(8f)

        // QR pointer: compact crypto bundle (no personal data), for quick capture.
        val qrBundle = JSONObject()
            .put("v", document.optString("format", "wayqo.sar.v1"))
            .put("keyId", signature.optString("keyId"))
            .put("alg", signature.optString("alg", "Ed25519"))
            .put("sha256", sha256)
            .put("verify", "https://www.wayqo.app/verify")
        runCatching { qrBitmap(qrBundle.toString(), 320) }.getOrNull()?.let { bmp ->
            w.image(bmp, 150f)
            w.text("Scan to capture the key ID and payload hash for verification.", w.small)
            w.gap(8f)
        }

        w.text("Public key (base64):", w.label)
        w.mono(signature.optString("publicKey"))
        w.gap(6f)
        w.text("Signature (base64):", w.label)
        w.mono(signature.optString("value"))
        w.gap(6f)
        w.text("Signed payload (base64):", w.label)
        w.mono(payloadB64)

        w.finishPage()

        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val tag = document.optString("reportId").takeIf { it.isNotBlank() }?.take(8) ?: "export"
        val file = File(dir, "wayqo-data-export-$tag.pdf")
        FileOutputStream(file).use { pdf.writeTo(it) }
        pdf.close()
        return FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
    }

    private fun renderArray(w: Writer, array: JSONArray?, emptyText: String) {
        if (array == null || array.length() == 0) {
            w.text(emptyText, w.body)
            return
        }
        for (i in 0 until array.length()) {
            if (i > 0) w.gap(6f)
            w.text("#${i + 1}", w.label)
            val item = array.optJSONObject(i)
            if (item != null) renderObject(w, item) else w.text(array.optString(i), w.body, indent = 8f)
        }
    }

    private fun renderObject(w: Writer, obj: JSONObject) {
        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = obj.opt(key)
            val rendered = when (value) {
                is JSONObject -> value.toString()
                is JSONArray -> value.toString()
                null -> ""
                else -> value.toString()
            }
            if (rendered.isBlank() || rendered == "null") continue
            w.kv(humanize(key), rendered, indent = 8f)
        }
    }

    private fun humanize(key: String): String {
        val spaced = StringBuilder()
        for (ch in key) {
            if (ch.isUpperCase() && spaced.isNotEmpty()) spaced.append(' ')
            spaced.append(ch)
        }
        return spaced.toString().replaceFirstChar { it.uppercase() }
    }

    private fun sha256Hex(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }

    private fun qrBitmap(content: String, size: Int): Bitmap {
        val hints = mapOf(EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M, EncodeHintType.MARGIN to 1)
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bmp.setPixel(x, y, if (matrix.get(x, y)) Color.BLACK else Color.WHITE)
            }
        }
        return bmp
    }

    private class Writer(val pdf: PdfDocument) {
        private var page: PdfDocument.Page? = null
        private var canvas: Canvas? = null
        private var y = 0f
        private var pageNo = 0

        val brand = paint(11f, true, sarAccent, letterSpacingEm = 0.18f)
        val h1 = paint(19f, true, sarInk)
        val body = paint(10.5f, false, sarInk)
        val small = paint(8.5f, false, sarMuted)
        val label = paint(8.5f, true, sarMuted, letterSpacingEm = 0.06f)
        private val heading = paint(13f, true, sarInk)
        private val mono = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.MONOSPACE; textSize = 7.5f; color = sarInk
        }
        private val rulePaint = Paint().apply { color = sarRule; strokeWidth = 0.8f }

        fun newPage() {
            pageNo++
            page = pdf.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNo).create())
            canvas = page!!.canvas
            y = MARGIN
        }

        fun finishPage() {
            page?.let {
                // Footer with page number.
                val footer = small
                canvas?.drawText("Wayqo — Subject Access Request · page $pageNo", MARGIN, PAGE_HEIGHT - MARGIN + 14f, footer)
                pdf.finishPage(it)
            }
            page = null
            canvas = null
        }

        private fun ensure(space: Float) {
            if (y + space > PAGE_HEIGHT - MARGIN) {
                finishPage()
                newPage()
            }
        }

        fun gap(h: Float) { y += h }

        fun divider() {
            ensure(6f)
            canvas?.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, rulePaint)
            y += 6f
        }

        fun heading(title: String) {
            gap(2f)
            text(title, heading)
            divider()
            gap(2f)
        }

        fun text(s: String, p: Paint, indent: Float = 0f, lineGap: Float = 3.2f) {
            if (s.isEmpty()) return
            val maxWidth = PAGE_WIDTH - 2 * MARGIN - indent
            for (line in wrap(s, p, maxWidth)) {
                ensure(p.textSize + lineGap)
                canvas?.drawText(line, MARGIN + indent, y + p.textSize, p)
                y += p.textSize + lineGap
            }
        }

        fun kv(key: String, value: String, indent: Float = 0f) {
            text(key.uppercase(), label, indent = indent, lineGap = 1.5f)
            text(value, body, indent = indent)
            gap(2.5f)
        }

        fun mono(s: String) {
            if (s.isEmpty()) return
            val maxWidth = PAGE_WIDTH - 2 * MARGIN
            for (line in breakToWidth(s, mono, maxWidth)) {
                ensure(mono.textSize + 2f)
                canvas?.drawText(line, MARGIN, y + mono.textSize, mono)
                y += mono.textSize + 2f
            }
        }

        fun image(bmp: Bitmap, size: Float) {
            ensure(size + 4f)
            val dst = Rect(MARGIN.toInt(), y.toInt(), (MARGIN + size).toInt(), (y + size).toInt())
            canvas?.drawBitmap(bmp, null, dst, null)
            y += size + 4f
        }

        private fun wrap(s: String, p: Paint, maxWidth: Float): List<String> {
            val out = ArrayList<String>()
            for (rawLine in s.split("\n")) {
                if (rawLine.isEmpty()) { out.add(""); continue }
                var current = StringBuilder()
                for (word in rawLine.split(" ")) {
                    val candidate = if (current.isEmpty()) word else "$current $word"
                    if (p.measureText(candidate) <= maxWidth) {
                        current = StringBuilder(candidate)
                    } else {
                        if (current.isNotEmpty()) out.add(current.toString())
                        if (p.measureText(word) > maxWidth) {
                            out.addAll(breakToWidth(word, p, maxWidth))
                            current = StringBuilder()
                        } else {
                            current = StringBuilder(word)
                        }
                    }
                }
                if (current.isNotEmpty()) out.add(current.toString())
            }
            return out
        }

        private fun breakToWidth(s: String, p: Paint, maxWidth: Float): List<String> {
            val out = ArrayList<String>()
            var start = 0
            while (start < s.length) {
                var end = start + 1
                while (end <= s.length && p.measureText(s, start, end) <= maxWidth) end++
                end = (end - 1).coerceAtLeast(start + 1)
                out.add(s.substring(start, end))
                start = end
            }
            return out
        }

        companion object {
            fun paint(size: Float, bold: Boolean, color: Int, letterSpacingEm: Float = 0f): Paint =
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    textSize = size
                    this.color = color
                    typeface = Typeface.create(Typeface.SANS_SERIF, if (bold) Typeface.BOLD else Typeface.NORMAL)
                    letterSpacing = letterSpacingEm
                }
        }
    }
}
