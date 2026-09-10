package com.example.snapspin.data.vision

import android.util.Base64
import com.example.snapspin.data.network.fetchText
import com.example.snapspin.data.vision.dto.AnnotateImageRequestDto
import com.example.snapspin.data.vision.dto.AnnotateImageResponseDto
import com.example.snapspin.data.vision.dto.AnnotateImagesRequestDto
import com.example.snapspin.data.vision.dto.AnnotateImagesResponseDto
import com.example.snapspin.data.vision.dto.FeatureDto
import com.example.snapspin.data.vision.dto.ImageDto
import com.example.snapspin.domain.error.AppError
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Cliente de `images:annotate` de Google Cloud Vision.
 *
 * Sólo se pide `WEB_DETECTION`: es el analizador que de verdad reconoce la portada, buscando la
 * imagen en la web. El OCR del texto impreso se descartó porque muchas carátulas no llevan
 * título legible o usan tipografías que no lee bien.
 */
class GoogleVisionApi(
    private val client: OkHttpClient,
    private val json: Json,
    private val authenticator: VisionAuthenticator,
    private val endpoint: String = DEFAULT_ENDPOINT,
) {

    suspend fun annotate(jpeg: ByteArray): AnnotateImageResponseDto {
        val payload = AnnotateImagesRequestDto(
            requests = listOf(
                AnnotateImageRequestDto(
                    image = ImageDto(content = Base64.encodeToString(jpeg, Base64.NO_WRAP)),
                    features = listOf(
                        FeatureDto(type = "WEB_DETECTION", maxResults = MAX_WEB_RESULTS),
                    ),
                )
            )
        )

        val credential = authenticator.credential()
        val url = endpoint.toHttpUrl().newBuilder()
            .apply {
                if (credential is VisionCredential.ApiKey) addQueryParameter("key", credential.value)
            }
            .build()

        val request = Request.Builder()
            .url(url)
            .post(json.encodeToString(payload).toRequestBody(JSON_MEDIA_TYPE))
            .apply {
                if (credential is VisionCredential.BearerToken) {
                    header("Authorization", "Bearer ${credential.value}")
                }
            }
            .build()

        val raw = client.fetchText(request, SERVICE).orEmpty()
        val response = try {
            json.decodeFromString<AnnotateImagesResponseDto>(raw)
        } catch (e: SerializationException) {
            throw AppError.Parsing(SERVICE, e)
        }

        val first = response.responses.firstOrNull() ?: throw AppError.CoverNotRecognised()
        first.error?.takeIf { it.code != 0 }?.let {
            throw AppError.Remote(SERVICE, it.code, it.message)
        }
        return first
    }

    companion object {
        const val SERVICE = "Google Cloud Vision"
        private const val DEFAULT_ENDPOINT = "https://vision.googleapis.com/v1/images:annotate"
        private const val MAX_WEB_RESULTS = 10
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()
    }
}
