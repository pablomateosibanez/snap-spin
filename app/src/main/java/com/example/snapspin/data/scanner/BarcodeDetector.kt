package com.example.snapspin.data.scanner

import androidx.camera.core.ImageProxy

/**
 * Detector de códigos de barras sobre fotogramas de la cámara.
 *
 * La interfaz permite sustituir ML Kit por otro lector (o por un doble en tests) sin tocar la UI.
 */
interface BarcodeDetector : AutoCloseable {

    /** Devuelve el primer código legible del fotograma, o `null` si no hay ninguno. */
    suspend fun detect(frame: ImageProxy): String?
}
