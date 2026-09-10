package com.example.snapspin.data.discogs

import okhttp3.Interceptor
import okhttp3.Response

/** Añade a cada petición el token personal y el User-Agent obligatorio de Discogs. */
class DiscogsAuthInterceptor(private val config: DiscogsConfig) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header("Authorization", "Discogs token=${config.personalAccessToken}")
            .header("User-Agent", config.userAgent)
            .header("Accept", "application/json")
            .build()
        return chain.proceed(request)
    }
}
