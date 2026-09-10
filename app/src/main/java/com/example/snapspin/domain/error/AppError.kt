package com.example.snapspin.domain.error

/**
 * Errores de negocio de la aplicación.
 *
 * Las implementaciones de los repositorios y servicios traducen cualquier fallo de
 * infraestructura (HTTP, parseo, IO...) a uno de estos tipos, de forma que ni los casos de uso
 * ni la capa de presentación conocen detalles de Retrofit/OkHttp/Google/Discogs.
 */
sealed class AppError(
    override val message: String,
    override val cause: Throwable? = null,
) : Exception(message, cause) {

    /** No hay red o el servidor no responde. */
    class Network(cause: Throwable? = null) :
        AppError("No se ha podido conectar. Revisa tu conexión a internet.", cause)

    /** Token inválido, caducado o sin permisos. */
    class Unauthorized(val service: String) :
        AppError("Las credenciales de $service no son válidas.")

    /** El servicio está saturado o caído temporalmente; reintentar suele bastar. */
    class ServiceBusy(val service: String) :
        AppError("$service está saturado ahora mismo. Vuelve a intentarlo en unos segundos.")

    /** Se ha superado el límite de peticiones del servicio. */
    class RateLimited(val service: String) :
        AppError("$service ha limitado las peticiones. Inténtalo de nuevo en unos segundos.")

    /** La búsqueda no ha devuelto ningún resultado. */
    class NotFound(val what: String) :
        AppError("No se ha encontrado $what.")

    /** La portada no se ha podido identificar con suficiente confianza. */
    class CoverNotRecognised :
        AppError("No se ha podido reconocer la portada. Prueba con más luz o encuadra sólo la carátula.")

    /** La respuesta del servicio no tiene el formato esperado. */
    class Parsing(val service: String, cause: Throwable? = null) :
        AppError("La respuesta de $service no se ha podido interpretar.", cause)

    /**
     * Respuesta de error del servicio remoto.
     *
     * [detail] es el mensaje literal del servicio: se conserva porque es lo único que distingue
     * un "sin saldo" de un "demasiadas peticiones", y cada uno se arregla de una forma.
     */
    class Remote(val service: String, val status: Int? = null, val detail: String? = null) :
        AppError(
            buildString {
                append(service).append(" ha devuelto un error")
                status?.let { append(" (").append(it).append(")") }
                if (detail.isNullOrBlank()) append(".") else append(": ").append(detail.trim())
            }
        )

    /** Cualquier otro fallo no contemplado. */
    class Unexpected(cause: Throwable? = null) :
        AppError("Ha ocurrido un error inesperado.", cause)
}
