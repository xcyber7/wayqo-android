// SPDX-FileCopyrightText: 2026 Black Flag International Limited
// SPDX-License-Identifier: GPL-3.0-only
package app.wayqo.wallet

import android.annotation.SuppressLint
import android.util.Size
import android.view.MotionEvent
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.max

@SuppressLint("UnsafeOptInUsageError")
@Composable
fun QrScanner(skip: String = "", enabled: Boolean = true, onQr: (String) -> Unit) {
    val currentEnabled = rememberUpdatedState(enabled)
    val currentSkip = rememberUpdatedState(skip)
    val currentOnQr = rememberUpdatedState(onQr)
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val boundCamera = remember { AtomicReference<Camera?>(null) }
    val boundAnalysis = remember { AtomicReference<ImageAnalysis?>(null) }
    val disposed = remember { AtomicBoolean(false) }
    val previewView = remember {
        PreviewView(context).also { view ->
            // Keep CameraX on the OEM-selected primary/logical rear camera. On devices
            // such as Pixel this preserves the manufacturer's multi-camera selection,
            // stabilization and focus behavior instead of forcing the lower-quality
            // ultra-wide physical sensor merely because it is a second rear camera.
            view.implementationMode = PreviewView.ImplementationMode.PERFORMANCE
            view.scaleType = PreviewView.ScaleType.FILL_CENTER
            view.setOnTouchListener { target, event ->
                if (event.action != MotionEvent.ACTION_UP) return@setOnTouchListener true
                val camera = boundCamera.get() ?: return@setOnTouchListener true
                val point = view.meteringPointFactory.createPoint(event.x, event.y)
                camera.cameraControl.startFocusAndMetering(
                    FocusMeteringAction.Builder(point)
                        .setAutoCancelDuration(3, TimeUnit.SECONDS)
                        .build()
                )
                target.performClick()
                true
            }
        }
    }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val delivered = remember { AtomicBoolean(false) }
    val lastValue = remember { AtomicReference<String?>(null) }
    val lastValueAt = remember { AtomicLong(0) }
    val consecutiveMatches = remember { AtomicInteger(0) }
    LaunchedEffect(enabled) {
        if (enabled) {
            delivered.set(false)
            lastValue.set(null)
            consecutiveMatches.set(0)
        }
    }
    val autoTorch = remember { AutoTorchController() }
    val mainExecutor = remember { ContextCompat.getMainExecutor(context) }
    val scanner = remember {
        newCameraQrDecoder { requestedRatio ->
            val camera = boundCamera.get()
            if (camera == null) false else {
                val zoom = camera.cameraInfo.zoomState.value
                camera.cameraControl.setZoomRatio(requestedRatio.coerceIn(zoom?.minZoomRatio ?: 1f, zoom?.maxZoomRatio ?: requestedRatio))
                true
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        disposed.set(false)
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            if (disposed.get()) return@addListener
            val provider = future.get()
            val resolutionSelector = ResolutionSelector.Builder()
                .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
                .setResolutionStrategy(
                    ResolutionStrategy(
                        Size(1920, 1080),
                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                    )
                )
                .build()
            val preview = Preview.Builder()
                .setResolutionSelector(resolutionSelector)
                .build()
                .also { it.surfaceProvider = previewView.surfaceProvider }
            // Analysis at 720p is materially cheaper than processing every 1080p
            // preview frame, while retaining ample QR detail. KEEP_ONLY_LATEST and
            // ML Kit auto-zoom handle distant codes without building a frame backlog.
            val analysisResolutionSelector = ResolutionSelector.Builder()
                .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
                .setResolutionStrategy(
                    ResolutionStrategy(
                        Size(1280, 720),
                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                    )
                )
                .build()
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setResolutionSelector(analysisResolutionSelector)
                .build()
            analysis.setAnalyzer(executor) { imageProxy ->
                val mediaImage = imageProxy.image
                if (mediaImage == null || delivered.get() || !currentEnabled.value) {
                    imageProxy.close()
                    return@setAnalyzer
                }
                val camera = boundCamera.get()
                if (camera?.cameraInfo?.hasFlashUnit() == true) {
                    // Enable in sustained low light; turn back off when ambient light
                    // returns (hysteresis in AutoTorchController prevents flapping).
                    autoTorch.observe(averageLuminance(imageProxy))?.let { camera.cameraControl.enableTorch(it) }
                }
                scanner.process(imageProxy, onResult = { value ->
                    mainExecutor.execute {
                        if (!disposed.get() && currentEnabled.value && !value.isNullOrBlank() && value != currentSkip.value) {
                            val now = System.currentTimeMillis()
                            val matches = if (lastValue.get() == value && now - lastValueAt.get() <= 1_500) {
                                consecutiveMatches.incrementAndGet()
                            } else {
                                lastValue.set(value)
                                consecutiveMatches.set(1)
                                1
                            }
                            lastValueAt.set(now)
                            if (matches >= 1 && delivered.compareAndSet(false, true)) currentOnQr.value(value)
                        }
                    }
                }, onComplete = { imageProxy.close() })
            }
            provider.unbindAll()
            if (!disposed.get()) {
                boundAnalysis.set(analysis)
                val camera = provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                boundCamera.set(camera)
                // Start on the primary 1x lens. Beginning at the ultra-wide minimum
                // materially reduces QR pixels on multi-camera Pixels; ML Kit will
                // still zoom in automatically when a distant code needs it.
                camera.cameraInfo.zoomState.value?.let { zoom ->
                    camera.cameraControl.setZoomRatio(1f.coerceIn(zoom.minZoomRatio, zoom.maxZoomRatio))
                }
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            disposed.set(true)
            boundAnalysis.getAndSet(null)?.clearAnalyzer()
            boundCamera.getAndSet(null)?.let { camera ->
                camera.cameraControl.enableTorch(false)
            }
            if (future.isDone) runCatching { future.get().unbindAll() }
            scanner.close()
            executor.shutdownNow()
        }
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
    }
}

private fun averageLuminance(image: ImageProxy): Int {
    val plane = image.planes.firstOrNull() ?: return 255
    val buffer = plane.buffer
    val rowStride = plane.rowStride
    val pixelStride = plane.pixelStride
    val startX = image.width / 5
    val endX = image.width * 4 / 5
    val startY = image.height / 5
    val endY = image.height * 4 / 5
    val stepX = max(1, image.width / 40)
    val stepY = max(1, image.height / 40)
    var total = 0L
    var samples = 0
    var y = startY
    while (y < endY) {
        var x = startX
        while (x < endX) {
            val index = y * rowStride + x * pixelStride
            if (index < buffer.limit()) {
                total += buffer.get(index).toInt() and 0xFF
                samples++
            }
            x += stepX
        }
        y += stepY
    }
    return if (samples == 0) 255 else (total / samples).toInt()
}

internal class AutoTorchController(
    private val darkThreshold: Int = 32,
    // Turn the torch back off only when ambient light is clearly bright and sustained.
    // The threshold is well above darkThreshold (hysteresis) so it doesn't flap, and high
    // enough that the torch's own contribution in a dark room doesn't trip it.
    private val brightThreshold: Int = 115,
    private val requiredDarkFrames: Int = 6,
    private val exposureWarmupMs: Long = 2_000,
    private val requiredDarkMs: Long = 1_200,
    private val requiredBrightMs: Long = 1_500,
    private val clockMs: () -> Long = { System.nanoTime() / 1_000_000 },
) {
    private var consecutiveDarkFrames = 0
    private var firstFrameAtMs: Long? = null
    private var darkSinceMs: Long? = null
    private var brightSinceMs: Long? = null
    private var enabled = false

    /**
     * Waits for exposure to settle, then: with the torch off, requires sustained low
     * light before returning true (enable); with the torch on, requires sustained bright
     * light before returning false (disable). Returns null when nothing should change.
     */
    fun observe(luminance: Int): Boolean? {
        val now = clockMs()
        val firstFrame = firstFrameAtMs ?: now.also { firstFrameAtMs = it }
        if (now - firstFrame < exposureWarmupMs) return null
        if (!enabled) {
            if (luminance > darkThreshold) {
                consecutiveDarkFrames = 0
                darkSinceMs = null
                return null
            }
            if (darkSinceMs == null) darkSinceMs = now
            consecutiveDarkFrames++
            if (consecutiveDarkFrames < requiredDarkFrames || now - darkSinceMs!! < requiredDarkMs) return null
            enabled = true
            brightSinceMs = null
            return true
        } else {
            if (luminance < brightThreshold) {
                brightSinceMs = null
                return null
            }
            if (brightSinceMs == null) brightSinceMs = now
            if (now - brightSinceMs!! < requiredBrightMs) return null
            enabled = false
            consecutiveDarkFrames = 0
            darkSinceMs = null
            return false
        }
    }
}
