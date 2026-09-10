package com.example.snapspin.data.vision

import android.util.Base64
import com.example.snapspin.data.credentials.ServiceAccountKey
import com.example.snapspin.data.credentials.ServiceAccountKeyProvider
import com.example.snapspin.data.network.fetchText
import com.example.snapspin.data.vision.dto.AccessTokenResponseDto
import com.example.snapspin.domain.error.AppError
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Obtiene un token OAuth2 a partir de la cuenta de servicio de Google.
 *
 * Implementa el flujo estándar de *JWT bearer*: se firma localmente un JWT con la clave privada
 * de la cuenta y se canjea por un access token. Se hace a mano (con `java.security`) para no
 * arrastrar la librería completa de Google Auth por un único caso de uso.
 *
 * El token se cachea hasta poco antes de su caducidad.
 */
class ServiceAccountVisionAuthenticator(
    private val keyProvider: ServiceAccountKeyProvider,
    private val client: OkHttpClient,
    private val json: Json,
    private val clock: () -> Long = System::currentTimeMillis,
) : VisionAuthenticator {

    private val mutex = Mutex()
    private var cachedToken: String? = null
    private var expiresAtMillis: Long = 0

    override suspend fun credential(): VisionCredential = mutex.withLock {
        val now = clock()
        cachedToken?.takeIf { now < expiresAtMillis }?.let {
            return@withLock VisionCredential.BearerToken(it)
        }

        val key = keyProvider.key() ?: throw AppError.Unauthorized(SERVICE)
        val response = requestAccessToken(key, now / 1000)
        if (response.accessToken.isBlank()) throw AppError.Unauthorized(SERVICE)

        cachedToken = response.accessToken
        expiresAtMillis = now + (response.expiresInSeconds.coerceAtLeast(60) - EXPIRY_MARGIN_SECONDS) * 1000
        VisionCredential.BearerToken(response.accessToken)
    }

    private suspend fun requestAccessToken(
        key: ServiceAccountKey,
        nowSeconds: Long,
    ): AccessTokenResponseDto {
        val body = FormBody.Builder()
            .add("grant_type", GRANT_TYPE)
            .add("assertion", buildSignedAssertion(key, nowSeconds))
            .build()
        val request = Request.Builder().url(key.tokenUri).post(body).build()
        val raw = client.fetchText(request, SERVICE).orEmpty()
        return try {
            json.decodeFromString(raw)
        } catch (e: SerializationException) {
            throw AppError.Parsing(SERVICE, e)
        }
    }

    private fun buildSignedAssertion(key: ServiceAccountKey, nowSeconds: Long): String {
        val header = """{"alg":"RS256","typ":"JWT"}"""
        val claims = """
            {"iss":"${key.clientEmail}","scope":"$SCOPE","aud":"${key.tokenUri}",
            "iat":$nowSeconds,"exp":${nowSeconds + TOKEN_LIFETIME_SECONDS}}
        """.trimIndent().replace("\n", "")
        val signingInput = "${header.base64Url()}.${claims.base64Url()}"
        return "$signingInput.${sign(signingInput, key.privateKeyPem).base64Url()}"
    }

    private fun sign(input: String, privateKeyPem: String): ByteArray = try {
        val der = Base64.decode(
            privateKeyPem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("\\s".toRegex(), ""),
            Base64.DEFAULT,
        )
        val privateKey = KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(der))
        Signature.getInstance("SHA256withRSA").run {
            initSign(privateKey)
            update(input.toByteArray(Charsets.UTF_8))
            sign()
        }
    } catch (e: Exception) {
        throw AppError.Unauthorized(SERVICE)
    }

    private fun String.base64Url(): String = toByteArray(Charsets.UTF_8).base64Url()

    private fun ByteArray.base64Url(): String =
        Base64.encodeToString(this, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)

    private companion object {
        const val SERVICE = "Google Cloud Vision"
        const val SCOPE = "https://www.googleapis.com/auth/cloud-platform"
        const val GRANT_TYPE = "urn:ietf:params:oauth:grant-type:jwt-bearer"
        const val TOKEN_LIFETIME_SECONDS = 3600L
        const val EXPIRY_MARGIN_SECONDS = 120L
    }
}
