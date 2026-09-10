package com.example.snapspin.data.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream

/**
 * Prepara la foto de la portada antes de enviarla a reconocer.
 *
 * Una captura a resolución completa ronda los 4-6 MB: reducirla a 1280 px de lado mayor recorta
 * el tiempo de subida drásticamente sin perder precisión de reconocimiento, y de paso corrige la
 * orientación con la que se tomó.
 */
object CoverPhoto {

    private const val MAX_SIDE = 1280
    private const val JPEG_QUALITY = 85

    /** Extrae los bytes JPEG del fotograma capturado (CameraX los entrega ya comprimidos). */
    fun bytesOf(image: ImageProxy): ByteArray {
        val buffer = image.planes[0].buffer
        return ByteArray(buffer.remaining()).also { buffer.get(it) }
    }

    fun prepare(jpeg: ByteArray, rotationDegrees: Int): ByteArray {
        val bitmap = decodeScaled(jpeg) ?: return jpeg
        val rotated = if (rotationDegrees % 360 != 0) {
            val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                .also { if (it != bitmap) bitmap.recycle() }
        } else {
            bitmap
        }

        return ByteArrayOutputStream().use { output ->
            rotated.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
            rotated.recycle()
            output.toByteArray()
        }
    }

    private fun decodeScaled(jpeg: ByteArray): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size, bounds)
        val largestSide = maxOf(bounds.outWidth, bounds.outHeight)
        if (largestSide <= 0) return null

        var sampleSize = 1
        while (largestSide / sampleSize > MAX_SIDE * 2) sampleSize *= 2

        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val decoded = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size, options) ?: return null

        val side = maxOf(decoded.width, decoded.height)
        if (side <= MAX_SIDE) return decoded

        val ratio = MAX_SIDE.toFloat() / side
        val scaled = Bitmap.createScaledBitmap(
            decoded,
            (decoded.width * ratio).toInt().coerceAtLeast(1),
            (decoded.height * ratio).toInt().coerceAtLeast(1),
            true,
        )
        if (scaled != decoded) decoded.recycle()
        return scaled
    }
}
