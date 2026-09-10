package com.example.snapspin.presentation.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.snapspin.domain.error.AppError
import com.example.snapspin.domain.model.ScanInput
import com.example.snapspin.domain.model.ScanMode
import com.example.snapspin.domain.usecase.ConfirmRegistrationUseCase
import com.example.snapspin.domain.usecase.PrepareRegistrationUseCase
import com.example.snapspin.domain.usecase.RegistrationProgress
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado de la única pantalla de la app.
 *
 * Toda la lógica de negocio vive en los casos de uso; aquí sólo se traduce su avance a estado de
 * UI y se decide cuándo pedir confirmación al usuario.
 */
class ScanViewModel(
    private val prepareRegistration: PrepareRegistrationUseCase,
    private val confirmRegistration: ConfirmRegistrationUseCase,
    discogsConfigured: Boolean,
) : ViewModel() {

    private val _state = MutableStateFlow(ScanUiState(discogsConfigured = discogsConfigured))
    val state: StateFlow<ScanUiState> = _state.asStateFlow()

    private var work: Job? = null

    fun startScan(mode: ScanMode) {
        work?.cancel()
        _state.update {
            it.copy(
                phase = ScanPhase.CAMERA,
                mode = mode,
                step = null,
                proposal = null,
                message = null,
            )
        }
    }

    fun cancelScan() {
        work?.cancel()
        _state.update { it.copy(phase = ScanPhase.HOME, mode = null, step = null, proposal = null) }
    }

    /** El analizador puede emitir el mismo código varias veces: sólo cuenta el primero. */
    fun onBarcodeDetected(barcode: String) {
        if (_state.value.phase != ScanPhase.CAMERA) return
        prepare(ScanInput.Barcode(barcode))
    }

    fun onCoverCaptured(jpeg: ByteArray) {
        if (_state.value.phase != ScanPhase.CAMERA) return
        prepare(ScanInput.Cover(jpeg))
    }

    fun onCameraFailure(message: String) {
        _state.update {
            it.copy(phase = ScanPhase.HOME, mode = null, message = UiMessage(message, isError = true))
        }
    }

    /** Confirmación del modal: alta definitiva, sobrescribiendo si el disco ya estaba. */
    fun confirm(replaceExisting: Boolean) {
        val proposal = _state.value.proposal ?: return
        work?.cancel()
        work = viewModelScope.launch {
            _state.update { it.copy(phase = ScanPhase.REGISTERING) }
            try {
                val result = confirmRegistration(proposal, replaceExisting)
                _state.update {
                    it.copy(
                        phase = ScanPhase.HOME,
                        mode = null,
                        step = null,
                        proposal = null,
                        message = UiMessage(
                            text = ScanMessages.registered(result),
                            isError = false,
                        ),
                    )
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Throwable) {
                fail(error)
            }
        }
    }

    /** Descartar el registro cuando el disco ya estaba en la colección. */
    fun discard() {
        work?.cancel()
        _state.update {
            it.copy(
                phase = ScanPhase.HOME,
                mode = null,
                step = null,
                proposal = null,
                message = UiMessage(ScanMessages.DISCARDED),
            )
        }
    }

    fun dismissMessage() = _state.update { it.copy(message = null) }

    private fun prepare(input: ScanInput) {
        work?.cancel()
        work = viewModelScope.launch {
            _state.update { it.copy(phase = ScanPhase.WORKING, step = null) }
            prepareRegistration(input)
                .catch { error -> fail(error) }
                .collect { progress ->
                    when (progress) {
                        is RegistrationProgress.Working ->
                            _state.update { it.copy(phase = ScanPhase.WORKING, step = progress.step) }

                        is RegistrationProgress.Ready ->
                            _state.update {
                                it.copy(
                                    phase = ScanPhase.CONFIRMING,
                                    step = null,
                                    proposal = progress.proposal,
                                )
                            }
                    }
                }
        }
    }

    private fun fail(error: Throwable) {
        if (error is CancellationException) throw error
        val text = (error as? AppError)?.message ?: ScanMessages.GENERIC_ERROR
        _state.update {
            it.copy(
                phase = ScanPhase.HOME,
                mode = null,
                step = null,
                proposal = null,
                message = UiMessage(text, isError = true),
            )
        }
    }
}
