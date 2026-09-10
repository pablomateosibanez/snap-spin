package com.example.snapspin.data.network

import android.util.Log
import com.example.snapspin.BuildConfig

/**
 * Traza de las llamadas a APIs externas.
 *
 * Todo sale bajo la misma etiqueta para poder filtrar en Logcat con `adb logcat -s LPCollection`.
 * Las peticiones correctas sólo se registran en depuración; los fallos se registran siempre y
 * **con el mensaje íntegro del servicio**, que es lo único que permite distinguir un "sin saldo"
 * de un "demasiadas peticiones" o de una clave mal copiada.
 */
internal object ApiLog {

    const val TAG = "LPCollection"

    fun request(service: String, method: String, url: String) {
        if (BuildConfig.DEBUG) Log.d(TAG, "$service → $method ${url.redacted()}")
    }

    fun response(service: String, status: Int, url: String, millis: Long) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "$service ← $status ${url.redacted()} (${millis} ms)")
        }
    }

    fun httpError(service: String, status: Int, url: String, body: String) {
        Log.w(TAG, "$service ← $status ${url.redacted()}\n${body.trim().take(MAX_BODY)}")
    }

    fun failure(service: String, description: String, cause: Throwable? = null) {
        Log.w(TAG, "$service ✗ $description", cause)
    }

    /** Nunca deben acabar en el log una API key ni un token. */
    private fun String.redacted(): String = SECRET_PARAMETER.replace(this) { match ->
        "${match.groupValues[1]}=***"
    }

    private const val MAX_BODY = 2_000
    private val SECRET_PARAMETER = """\b(key|token|access_token)=[^&\s]+""".toRegex()
}
