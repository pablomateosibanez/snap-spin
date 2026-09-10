package com.example.snapspin.presentation.scan

import com.example.snapspin.domain.model.RegistrationProposal
import com.example.snapspin.domain.model.RegistrationStep
import com.example.snapspin.domain.model.ScanMode

/** Fase en la que se encuentra el registro. */
enum class ScanPhase { HOME, CAMERA, WORKING, CONFIRMING, REGISTERING }

/** Aviso puntual que se muestra al usuario (snackbar). */
data class UiMessage(val text: String, val isError: Boolean = false)

data class ScanUiState(
    val phase: ScanPhase = ScanPhase.HOME,
    val mode: ScanMode? = null,
    val step: RegistrationStep? = null,
    val proposal: RegistrationProposal? = null,
    val message: UiMessage? = null,
    val discogsConfigured: Boolean = true,
)
