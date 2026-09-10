package com.example.snapspin.presentation.camera

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.snapspin.SnapSpinApp
import com.example.snapspin.R
import com.example.snapspin.data.image.CoverPhoto
import com.example.snapspin.data.scanner.BarcodeDetector
import com.example.snapspin.domain.model.ScanMode
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Vista de cámara. Según el modo, analiza fotogramas buscando un código de barras o hace una
 * única foto de la portada.
 *
 * En modo código de barras no hay botón de disparo: en cuanto se lee un código válido se
 * continúa solo, que es justo el paso manual que sobra en la app oficial.
 */
@Composable
fun CameraCapture(
    mode: ScanMode,
    onBarcode: (String) -> Unit,
    onCover: (ByteArray) -> Unit,
    onCancel: () -> Unit,
    onFailure: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val cameraErrorMessage = stringResource(R.string.camera_error)

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val detector = remember(mode) {
        if (mode == ScanMode.BARCODE) {
            (context.applicationContext as SnapSpinApp).container.createBarcodeDetector()
        } else {
            null
        }
    }
    val imageCapture = remember(mode) {
        if (mode == ScanMode.COVER) {
            ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()
        } else {
            null
        }
    }
    /** Evita procesar varias veces el mismo disco mientras se navega fuera de la cámara. */
    val alreadyEmitted = remember(mode) { AtomicBoolean(false) }
    val boundProvider = remember { mutableStateOf<ProcessCameraProvider?>(null) }

    DisposableEffect(mode) {
        onDispose {
            boundProvider.value?.unbindAll()
            detector?.close()
            analysisExecutor.shutdown()
        }
    }

    LaunchedEffect(mode) {
        val provider = runCatching {
            ProcessCameraProvider.getInstance(context).await(ContextCompat.getMainExecutor(context))
        }
            .getOrElse {
                onFailure(cameraErrorMessage)
                return@LaunchedEffect
            }

        val preview = Preview.Builder().build().apply {
            setSurfaceProvider(previewView.surfaceProvider)
        }

        val useCases = buildList {
            add(preview)
            imageCapture?.let(::add)
            if (detector != null) {
                add(barcodeAnalysis(detector, analysisExecutor, alreadyEmitted) { code ->
                    scope.launch { onBarcode(code) }
                })
            }
        }

        runCatching {
            provider.unbindAll()
            provider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                *useCases.toTypedArray(),
            )
            boundProvider.value = provider
        }.onFailure { onFailure(cameraErrorMessage) }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize(),
        )

        Surface(
            color = Color.Black.copy(alpha = 0.55f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(
                        if (mode == ScanMode.BARCODE) {
                            R.string.camera_hint_barcode
                        } else {
                            R.string.camera_hint_cover
                        }
                    ),
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                )

                if (mode == ScanMode.COVER && imageCapture != null) {
                    Button(
                        onClick = {
                            if (alreadyEmitted.compareAndSet(false, true)) {
                                imageCapture.capture(
                                    executor = analysisExecutor,
                                    onCaptured = { jpeg -> scope.launch { onCover(jpeg) } },
                                    onCaptureFailed = {
                                        alreadyEmitted.set(false)
                                        scope.launch { onFailure(cameraErrorMessage) }
                                    },
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.action_capture))
                    }
                }

                TextButton(onClick = onCancel) {
                    Text(stringResource(R.string.action_cancel), color = Color.White)
                }
            }
        }
    }
}

/** Analiza fotogramas en segundo plano y emite el primer código de barras encontrado. */
private fun barcodeAnalysis(
    detector: BarcodeDetector,
    executor: Executor,
    alreadyEmitted: AtomicBoolean,
    onBarcode: (String) -> Unit,
): ImageAnalysis = ImageAnalysis.Builder()
    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
    .build()
    .apply {
        setAnalyzer(executor) { frame: ImageProxy ->
            // El analizador no es suspend: se bloquea este hilo dedicado a la espera del
            // resultado de ML Kit. STRATEGY_KEEP_ONLY_LATEST garantiza un fotograma en vuelo.
            try {
                if (!alreadyEmitted.get()) {
                    val code = runBlocking { detector.detect(frame) }
                    if (code != null && alreadyEmitted.compareAndSet(false, true)) {
                        onBarcode(code)
                    }
                }
            } catch (error: Exception) {
                // Un fotograma ilegible no debe interrumpir el escaneo.
            } finally {
                frame.close()
            }
        }
    }

/** Envuelve el callback de CameraX y prepara la imagen para el reconocimiento. */
private fun ImageCapture.capture(
    executor: Executor,
    onCaptured: (ByteArray) -> Unit,
    onCaptureFailed: (ImageCaptureException) -> Unit,
) = takePicture(
    executor,
    object : ImageCapture.OnImageCapturedCallback() {
        override fun onCaptureSuccess(image: ImageProxy) {
            val jpeg = try {
                CoverPhoto.prepare(CoverPhoto.bytesOf(image), image.imageInfo.rotationDegrees)
            } finally {
                image.close()
            }
            onCaptured(jpeg)
        }

        override fun onError(exception: ImageCaptureException) = onCaptureFailed(exception)
    },
)
