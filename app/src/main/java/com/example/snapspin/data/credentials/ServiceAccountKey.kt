package com.example.snapspin.data.credentials

/** Datos mínimos de una cuenta de servicio de Google necesarios para firmar un JWT. */
data class ServiceAccountKey(
    val clientEmail: String,
    val privateKeyPem: String,
    val tokenUri: String,
)

/** Origen de la cuenta de servicio (assets, almacenamiento seguro, tests...). */
interface ServiceAccountKeyProvider {
    /** Devuelve la clave o `null` si la app no está configurada con cuenta de servicio. */
    suspend fun key(): ServiceAccountKey?
}
