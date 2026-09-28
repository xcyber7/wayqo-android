// SPDX-FileCopyrightText: 2026 Black Flag International Limited
// SPDX-License-Identifier: GPL-3.0-only
package app.wayqo.wallet

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import androidx.camera.core.ImageProxy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import zxingcpp.BarcodeReader

/** Flavor-specific engines share the same camera lifecycle, UI and payment gates. */
internal interface QrImageDecoder : AutoCloseable {
    suspend fun decode(context: Context, uri: Uri): List<String>
    suspend fun decodeBitmap(bitmap: Bitmap): List<String>
}

internal interface CameraQrDecoder : AutoCloseable {
    fun process(image: ImageProxy, onResult: (String?) -> Unit, onComplete: () -> Unit)
}

internal fun fallbackQrReader(maxSymbols: Int) = BarcodeReader().apply {
    options.formats = setOf(BarcodeReader.Format.QR_CODE)
    options.tryHarder = true
    options.tryRotate = true
    options.tryInvert = true
    options.maxNumberOfSymbols = maxSymbols
}

internal suspend fun decodeFallbackBitmap(bitmap: Bitmap): List<String> = withContext(Dispatchers.IO) {
    fallbackQrReader(2).read(bitmap, Rect(0, 0, bitmap.width, bitmap.height), 0)
        .mapNotNull { it.text }.filter(String::isNotBlank).distinct()
}

internal suspend fun decodeFallbackImage(context: Context, uri: Uri): List<String> = withContext(Dispatchers.IO) {
    val bitmap = decodeUploadBitmap(context, uri) ?: return@withContext emptyList()
    try { decodeFallbackBitmap(bitmap) } finally { bitmap.recycle() }
}
