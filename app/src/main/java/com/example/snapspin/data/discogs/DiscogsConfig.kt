package com.example.snapspin.data.discogs

/**
 * Configuración del cliente de Discogs.
 *
 * El token y el usuario se inyectan desde `BuildConfig` (leídos de `local.properties`), nunca
 * se escriben en el código.
 */
data class DiscogsConfig(
    val personalAccessToken: String,
    val username: String,
    /** Discogs exige un User-Agent propio y descriptivo en todas las peticiones. */
    val userAgent: String,
    /** Moneda en la que se piden los precios de mercado. */
    val currency: String = "EUR",
    val baseUrl: String = "https://api.discogs.com",
) {
    val isConfigured: Boolean
        get() = personalAccessToken.isNotBlank() && username.isNotBlank()
}
