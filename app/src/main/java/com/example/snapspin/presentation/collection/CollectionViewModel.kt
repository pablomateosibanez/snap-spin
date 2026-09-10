package com.example.snapspin.presentation.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.snapspin.domain.error.AppError
import com.example.snapspin.domain.model.CollectionItem
import com.example.snapspin.domain.model.CollectionOrder
import com.example.snapspin.domain.usecase.FilterCollectionUseCase
import com.example.snapspin.domain.usecase.LoadCollectionUseCase
import com.example.snapspin.domain.usecase.RemoveFromCollectionUseCase
import com.example.snapspin.domain.usecase.ResolveAlbumYearsUseCase
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Listado de la colección.
 *
 * La lista completa se descarga una vez y se guarda aquí; buscar y reordenar son operaciones
 * locales, así que la pantalla responde mientras se escribe sin castigar la API de Discogs.
 */
class CollectionViewModel(
    private val loadCollection: LoadCollectionUseCase,
    private val filterCollection: FilterCollectionUseCase,
    private val removeFromCollection: RemoveFromCollectionUseCase,
    private val resolveAlbumYears: ResolveAlbumYearsUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(CollectionUiState())
    val state: StateFlow<CollectionUiState> = _state.asStateFlow()

    private var allItems: List<CollectionItem> = emptyList()
    private var loadJob: Job? = null
    private var albumYearsJob: Job? = null

    init {
        load(forceRefresh = false)
    }

    fun refresh() = load(forceRefresh = true)

    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query) }
        applyFilters()
    }

    fun onOrderChange(order: CollectionOrder) {
        _state.update { it.copy(order = order) }
        applyFilters()
    }

    fun openDetail(item: CollectionItem) = _state.update { it.copy(selected = item) }

    fun closeDetail() = _state.update { it.copy(selected = null) }

    /** Pide confirmación antes de borrar: es la única acción destructiva de la app. */
    fun askToDelete(item: CollectionItem) = _state.update { it.copy(pendingDeletion = item) }

    fun cancelDeletion() = _state.update { it.copy(pendingDeletion = null) }

    fun confirmDeletion() {
        val item = _state.value.pendingDeletion ?: return
        viewModelScope.launch {
            _state.update { it.copy(deleting = true) }
            try {
                removeFromCollection(item)
                allItems = allItems.filterNot { it.instanceId == item.instanceId }
                _state.update {
                    it.copy(
                        deleting = false,
                        pendingDeletion = null,
                        selected = null,
                        total = allItems.size,
                        message = "Eliminado de tu colección: ${item.artist} – ${item.title}",
                    )
                }
                applyFilters()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Throwable) {
                _state.update {
                    it.copy(
                        deleting = false,
                        pendingDeletion = null,
                        message = (error as? AppError)?.message
                            ?: "No se ha podido eliminar el disco.",
                    )
                }
            }
        }
    }

    fun dismissMessage() = _state.update { it.copy(message = null) }

    private fun load(forceRefresh: Boolean) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                allItems = loadCollection(forceRefresh)
                _state.update { it.copy(loading = false, total = allItems.size) }
                applyFilters()
                resolvePendingAlbumYears()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Throwable) {
                _state.update {
                    it.copy(
                        loading = false,
                        error = (error as? AppError)?.message
                            ?: "No se ha podido cargar tu colección.",
                    )
                }
            }
        }
    }

    /**
     * Rellena en segundo plano el año de salida de los álbumes que aún no se conocen. La lista ya
     * está en pantalla con el año de la edición y se va corrigiendo sola según llegan los datos.
     */
    private fun resolvePendingAlbumYears() {
        albumYearsJob?.cancel()
        albumYearsJob = viewModelScope.launch {
            resolveAlbumYears(allItems).collect { (masterId, year) ->
                allItems = allItems.map { item ->
                    if (item.masterId == masterId) item.copy(albumYear = year) else item
                }
                applyFilters()
            }
        }
    }

    private fun applyFilters() = _state.update { current ->
        current.copy(
            visible = filterCollection(allItems, current.query, current.order),
            // El detalle abierto se reengancha a la copia viva del disco para que también él
            // refleje el año en cuanto se resuelve.
            selected = current.selected?.let { open ->
                allItems.firstOrNull { it.instanceId == open.instanceId }
            },
        )
    }
}
