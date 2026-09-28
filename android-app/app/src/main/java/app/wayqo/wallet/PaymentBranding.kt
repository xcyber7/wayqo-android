// SPDX-FileCopyrightText: 2026 Black Flag International Limited
// SPDX-License-Identifier: GPL-3.0-only
package app.wayqo.wallet

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp

internal data class PaymentRailBrand(
    val name: String,
    val mark: String,
    val color: Color,
    // Drawable resource name for the official rail logo (resolved at runtime via
    // getIdentifier so a missing asset simply falls back to the coloured mark). Drop a
    // transparent PNG named e.g. "rail_pix.png" into res/drawable-nodpi to enable it.
    val asset: String = "",
)

internal data class FinancialInstitution(
    val name: String,
    val shortName: String,
    val logoUrl: String,
)

/** Both payment steps use the same verified institution logo or an honest rail mark. */
@Composable
internal fun PaymentInstitutionMark(institution: FinancialInstitution?, rail: PaymentRailBrand) {
    val logo by produceState<Bitmap?>(initialValue = null, key1 = institution?.logoUrl) {
        value = institution?.logoUrl?.let { loadTrustedInstitutionLogo(it) }
    }
    if (logo != null) {
        Surface(color = Color.White, shape = RoundedCornerShape(4.dp)) {
            Image(bitmap = logo!!.asImageBitmap(), contentDescription = institution?.name,
                modifier = Modifier.size(24.dp).padding(2.dp))
        }
    } else {
        Surface(color = rail.color, shape = RoundedCornerShape(4.dp)) {
            Text(rail.mark, color = Color.White, style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp))
        }
    }
}

/** Visual identity for the rail, independent of the recipient's country label. */
internal fun paymentRailBrand(country: String, scheme: String, network: String = ""): PaymentRailBrand {
    val providerName = scheme.trim().takeIf { it.isNotBlank() && !it.equals("provider", true) }
    return when (country.uppercase()) {
        "VN" -> PaymentRailBrand("VietQR", "VietQR", Color(0xFF075AA8), "rail_vietqr")
        "PH" -> PaymentRailBrand("QR Ph", "QRPh", Color(0xFF1356A2), "rail_qrph")
        "TH" -> PaymentRailBrand("PromptPay", "PP", Color(0xFF163A71), "rail_promptpay")
        "KH" -> PaymentRailBrand("KHQR", "KHQR", Color(0xFFE21B2D), "rail_khqr")
        "MY" -> PaymentRailBrand("DuitNow QR", "DN", Color(0xFFE21C2A), "rail_duitnow")
        "ID" -> PaymentRailBrand("QRIS", "QRIS", Color(0xFFB51F2E), "rail_qris")
        "SG" -> PaymentRailBrand("SGQR / PayNow", "SGQR", Color(0xFF7B2D8E), "rail_paynow")
        "BR" -> PaymentRailBrand("Pix", "PIX", Color(0xFF00A99D), "rail_pix")
        "PE" -> when (network.uppercase()) {
            "YAPE" -> PaymentRailBrand("Yape", "YAPE", Color(0xFF6C2383), "rail_yape")
            "PLIN" -> PaymentRailBrand("Plin", "PLIN", Color(0xFF00A9CE), "rail_plin")
            else -> PaymentRailBrand("Yape / Plin", "PE", Color(0xFF6C2383))
        }
        "CN" -> PaymentRailBrand("Weixin business pay", "微信", Color(0xFF07C160), "rail_weixin")
        "AR", "BO", "CO" -> PaymentRailBrand(providerName ?: "Interoperable QR", country.uppercase(), BrandPurple)
        else -> PaymentRailBrand(providerName ?: "Local QR", country.uppercase().ifBlank { "QR" }, BrandPurple)
    }
}

/**
 * VietQR exposes the receiving bank's BIN in the QR and publishes a bank/logo
 * directory. Keep common institutions available offline; unknown/current BINs
 * are still displayed honestly as participant identifiers by the UI.
 */
internal fun financialInstitution(country: String, bankBin: String): FinancialInstitution? {
    if (!country.equals("VN", ignoreCase = true)) return null
    return vietnamInstitutions[bankBin.trim()]
}

private fun vietQrBank(name: String, shortName: String, code: String) =
    FinancialInstitution(name, shortName, "https://cdn.vietqr.io/img/$code.png")

private val vietnamInstitutions = mapOf(
    "970415" to vietQrBank("Vietnam Joint Stock Commercial Bank for Industry and Trade", "VietinBank", "ICB"),
    "970436" to vietQrBank("Joint Stock Commercial Bank for Foreign Trade of Vietnam", "Vietcombank", "VCB"),
    "970418" to vietQrBank("Bank for Investment and Development of Vietnam", "BIDV", "BIDV"),
    "970405" to vietQrBank("Vietnam Bank for Agriculture and Rural Development", "Agribank", "VBA"),
    "970448" to vietQrBank("Orient Commercial Joint Stock Bank", "OCB", "OCB"),
    "970422" to vietQrBank("Military Commercial Joint Stock Bank", "MBBank", "MB"),
    "970407" to vietQrBank("Vietnam Technological and Commercial Joint Stock Bank", "Techcombank", "TCB"),
    "970416" to vietQrBank("Asia Commercial Joint Stock Bank", "ACB", "ACB"),
    "970432" to vietQrBank("Vietnam Prosperity Joint Stock Commercial Bank", "VPBank", "VPB"),
    "970423" to vietQrBank("Tien Phong Commercial Joint Stock Bank", "TPBank", "TPB"),
    "970403" to vietQrBank("Saigon Thuong Tin Commercial Joint Stock Bank", "Sacombank", "STB"),
    "970437" to vietQrBank("Ho Chi Minh City Development Joint Stock Commercial Bank", "HDBank", "HDB"),
    "970441" to vietQrBank("Vietnam International Commercial Joint Stock Bank", "VIB", "VIB"),
    "970443" to vietQrBank("Saigon-Hanoi Commercial Joint Stock Bank", "SHB", "SHB"),
    "970431" to vietQrBank("Vietnam Export Import Commercial Joint Stock Bank", "Eximbank", "EIB"),
    "970426" to vietQrBank("Vietnam Maritime Commercial Joint Stock Bank", "MSB", "MSB"),
    "970412" to vietQrBank("Vietnam Public Joint Stock Commercial Bank", "PVcomBank", "PVCB"),
    "970419" to vietQrBank("National Citizen Commercial Joint Stock Bank", "NCB", "NCB"),
    "970424" to vietQrBank("Shinhan Bank Vietnam", "ShinhanBank", "SHBVN"),
    "970449" to vietQrBank("Loc Phat Vietnam Commercial Joint Stock Bank", "LPBank", "LPB"),
)
