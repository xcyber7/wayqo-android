// SPDX-FileCopyrightText: 2026 Black Flag International Limited
// SPDX-License-Identifier: GPL-3.0-only
package app.wayqo.wallet

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.camera.core.ImageProxy

/** All QR processing is local, using the Apache-licensed ZXing-C++ engine. */
internal fun newQrImageDecoder(): QrImageDecoder = object : QrImageDecoder {
    override suspend fun decode(context: Context, uri: Uri) = decodeFallbackImage(context, uri)
    override suspend fun decodeBitmap(bitmap: Bitmap) = decodeFallbackBitmap(bitmap)
    override fun close() = Unit
}

internal fun newCameraQrDecoder(onZoom: (Float) -> Boolean): CameraQrDecoder = object : CameraQrDecoder {
    private val reader = fallbackQrReader(1)
    override fun process(image: ImageProxy, onResult: (String?) -> Unit, onComplete: () -> Unit) {
        try { onResult(runCatching { reader.read(image).firstOrNull()?.text }.getOrNull()) }
        finally { onComplete() }
    }
    override fun close() = Unit
}
