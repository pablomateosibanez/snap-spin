package com.example.snapspin.data.scanner

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

/**
 * Lector de códigos de barras con ML Kit, limitado a los formatos que usan los discos
 * (EAN-13/EAN-8 en Europa, UPC en ediciones americanas) para acelerar la detección.
 */
class MlKitBarcodeDetector : BarcodeDetector {

    private val scanner: BarcodeScanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
            )
            .build()
    )

    @OptIn(ExperimentalGetImage::class)
    override suspend fun detect(frame: ImageProxy): String? {
        val image = frame.image ?: return null
        val input = InputImage.fromMediaImage(image, frame.imageInfo.rotationDegrees)
        return scanner.process(input).await()
            .firstNotNullOfOrNull { it.rawValue?.trim()?.takeIf(String::isNotEmpty) }
    }

    override fun close() = scanner.close()
}
