package com.example.snapspin.data.gemini

/**
 * Configuración del reconocedor de portadas con Gemini.
 *
 * El modelo es configurable desde `local.properties` porque Google retira modelos con cierta
 * frecuencia; por defecto se usa el alias `gemini-flash-latest`, que siempre apunta al modelo
 * flash vigente y evita que la app deje de funcionar cuando jubilan una versión concreta.
 */
data class GeminiConfig(
    val apiKey: String,
    val model: String,
    val timeoutMillis: Long = 45_000L,
) {
    val isConfigured: Boolean get() = apiKey.isNotBlank()
}
