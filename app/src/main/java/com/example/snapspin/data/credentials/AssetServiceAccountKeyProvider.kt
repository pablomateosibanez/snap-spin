package com.example.snapspin.data.credentials

import android.content.Context
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Lee el JSON de la cuenta de servicio desde `assets/`.
 *
 * El fichero está excluido del control de versiones; si no existe, la app seguirá funcionando
 * con código de barras y avisará al intentar usar el reconocimiento de portadas.
 */
class AssetServiceAccountKeyProvider(
    private val context: Context,
    private val assetName: String,
    private val json: Json,
) : ServiceAccountKeyProvider {

    private val mutex = Mutex()
    private var cached: ServiceAccountKey? = null
    private var loaded = false

    override suspend fun key(): ServiceAccountKey? = mutex.withLock {
        if (!loaded) {
            cached = withContext(Dispatchers.IO) { readAsset() }
            loaded = true
        }
        cached
    }

    private fun readAsset(): ServiceAccountKey? = try {
        val raw = context.assets.open(assetName).bufferedReader().use { it.readText() }
        val dto = json.decodeFromString<ServiceAccountDto>(raw)
        if (dto.clientEmail.isBlank() || dto.privateKey.isBlank()) {
            null
        } else {
            ServiceAccountKey(
                clientEmail = dto.clientEmail,
                privateKeyPem = dto.privateKey,
                tokenUri = dto.tokenUri,
            )
        }
    } catch (e: IOException) {
        null
    } catch (e: IllegalArgumentException) {
        null
    }

    @Serializable
    private data class ServiceAccountDto(
        @SerialName("client_email") val clientEmail: String = "",
        @SerialName("private_key") val privateKey: String = "",
        @SerialName("token_uri") val tokenUri: String = "https://oauth2.googleapis.com/token",
    )
}
