package com.example.snapspin.di

import android.content.Context
import com.example.snapspin.BuildConfig
import com.example.snapspin.data.discogs.DiscogsApi
import com.example.snapspin.data.discogs.DiscogsAuthInterceptor
import com.example.snapspin.data.discogs.DiscogsCatalogRepository
import com.example.snapspin.data.discogs.DiscogsCollectionRepository
import com.example.snapspin.data.discogs.DiscogsConfig
import com.example.snapspin.data.discogs.SharedPreferencesMasterYearStore
import com.example.snapspin.data.gemini.GeminiConfig
import com.example.snapspin.data.gemini.GeminiCoverRecognitionService
import com.example.snapspin.data.scanner.BarcodeDetector
import com.example.snapspin.data.scanner.MlKitBarcodeDetector
import com.example.snapspin.domain.repository.CollectionRepository
import com.example.snapspin.domain.repository.MusicCatalogRepository
import com.example.snapspin.domain.service.CoverRecognitionService
import com.example.snapspin.domain.usecase.ConfirmRegistrationUseCase
import com.example.snapspin.domain.usecase.FilterCollectionUseCase
import com.example.snapspin.domain.usecase.LoadCollectionUseCase
import com.example.snapspin.domain.usecase.PrepareRegistrationUseCase
import com.example.snapspin.domain.usecase.ResolveAlbumYearsUseCase
import com.example.snapspin.domain.usecase.RemoveFromCollectionUseCase
import com.example.snapspin.domain.usecase.SelectStandardEditionUseCase
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient

/**
 * Contenedor de dependencias de la aplicación.
 *
 * Se construye a mano (en lugar de con Hilt) porque el grafo es pequeño y así el proyecto no
 * depende de procesadores de anotaciones. Lo relevante para la arquitectura es que aquí, y sólo
 * aquí, se decide qué implementación concreta cumple cada interfaz del dominio.
 */
class AppContainer(context: Context) {

    /** Necesario para la caché en disco del año de salida de los álbumes. */
    private val appContext = context.applicationContext

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // --- Discogs -------------------------------------------------------------------------

    private val discogsConfig = DiscogsConfig(
        personalAccessToken = BuildConfig.DISCOGS_TOKEN,
        username = BuildConfig.DISCOGS_USERNAME,
        userAgent = BuildConfig.DISCOGS_USER_AGENT,
    )

    private val discogsApi = DiscogsApi(
        client = httpClient.newBuilder()
            .addInterceptor(DiscogsAuthInterceptor(discogsConfig))
            .build(),
        json = json,
        config = discogsConfig,
    )

    val catalogRepository: MusicCatalogRepository =
        DiscogsCatalogRepository(discogsApi, discogsConfig)

    val collectionRepository: CollectionRepository = DiscogsCollectionRepository(
        api = discogsApi,
        masterYears = SharedPreferencesMasterYearStore(appContext),
    )

    // --- Reconocimiento de portadas --------------------------------------------------------

    private val geminiConfig = GeminiConfig(
        apiKey = BuildConfig.GEMINI_API_KEY,
        model = BuildConfig.GEMINI_MODEL,
    )

    /**
     * Gemini reconoce la portada mirándola, así que acierta también con carátulas que no están
     * indexadas en internet.
     *
     * Se instancia siempre: si faltase la clave, el propio servicio avisa de que no está
     * configurado. Antes había un repliegue silencioso a Cloud Vision y eso sólo servía para no
     * saber nunca con qué motor se estaba reconociendo. La implementación con Vision sigue en
     * `data/vision` por si hiciera falta volver a ella, pero ya no se enchufa sola.
     */
    val coverRecognitionService: CoverRecognitionService =
        GeminiCoverRecognitionService(geminiConfig, json)

    // --- Casos de uso --------------------------------------------------------------------

    val prepareRegistration = PrepareRegistrationUseCase(
        catalog = catalogRepository,
        collection = collectionRepository,
        coverRecognition = coverRecognitionService,
        selectStandardEdition = SelectStandardEditionUseCase(catalogRepository),
    )

    val confirmRegistration = ConfirmRegistrationUseCase(collectionRepository)

    val loadCollection = LoadCollectionUseCase(collectionRepository)

    val filterCollection = FilterCollectionUseCase()

    val removeFromCollection = RemoveFromCollectionUseCase(collectionRepository)

    val resolveAlbumYears = ResolveAlbumYearsUseCase(collectionRepository)

    /** Cada pantalla de cámara crea (y cierra) su propio detector. */
    fun createBarcodeDetector(): BarcodeDetector = MlKitBarcodeDetector()

    val isDiscogsConfigured: Boolean get() = discogsConfig.isConfigured
}
