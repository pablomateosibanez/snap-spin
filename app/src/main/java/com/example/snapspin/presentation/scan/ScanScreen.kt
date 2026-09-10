package com.example.snapspin.presentation.scan

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.snapspin.R
import com.example.snapspin.domain.model.RegistrationStep
import com.example.snapspin.domain.model.ScanMode
import com.example.snapspin.presentation.camera.CameraCapture

/**
 * Pantalla única de la aplicación.
 *
 * El recorrido completo es: elegir modo -> apuntar con la cámara -> confirmar. Todo lo demás
 * (buscar, elegir edición, detectar duplicados y dar de alta) ocurre sin intervención.
 */
@Composable
fun ScanScreen(viewModel: ScanViewModel, onOpenCollection: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var cameraGranted by remember { mutableStateOf(context.hasCameraPermission()) }
    var pendingMode by remember { mutableStateOf<ScanMode?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        cameraGranted = granted
        if (granted) pendingMode?.let(viewModel::startScan)
        pendingMode = null
    }

    fun requestScan(mode: ScanMode) {
        if (cameraGranted) {
            viewModel.startScan(mode)
        } else {
            pendingMode = mode
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Los avisos de éxito caben en un snackbar; los errores no: el mensaje de una API puede ser
    // largo y es justo lo que el usuario necesita leer entero para saber qué hacer.
    state.message?.takeIf { !it.isError }?.let { message ->
        LaunchedEffect(message) {
            snackbarHostState.showSnackbar(message.text)
            viewModel.dismissMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.fillMaxSize(),
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (state.phase == ScanPhase.CAMERA) {
                if (cameraGranted) {
                    CameraCapture(
                        mode = state.mode ?: ScanMode.BARCODE,
                        onBarcode = viewModel::onBarcodeDetected,
                        onCover = viewModel::onCoverCaptured,
                        onCancel = viewModel::cancelScan,
                        onFailure = viewModel::onCameraFailure,
                    )
                } else {
                    PermissionRequest(
                        onGrant = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            } else {
                HomeContent(
                    discogsConfigured = state.discogsConfigured,
                    onSelect = ::requestScan,
                    onOpenCollection = onOpenCollection,
                    modifier = Modifier.padding(innerPadding),
                )
            }

            if (state.phase == ScanPhase.WORKING || state.phase == ScanPhase.REGISTERING) {
                WorkingOverlay(step = state.step, registering = state.phase == ScanPhase.REGISTERING)
            }
        }

        state.message?.takeIf { it.isError }?.let { failure ->
            ErrorDialog(message = failure.text, onDismiss = viewModel::dismissMessage)
        }

        val proposal = state.proposal
        if (state.phase == ScanPhase.CONFIRMING && proposal != null) {
            ConfirmationDialog(
                proposal = proposal,
                onConfirm = { viewModel.confirm(replaceExisting = false) },
                onOverwrite = { viewModel.confirm(replaceExisting = true) },
                onDiscard = viewModel::discard,
                onDismiss = viewModel::cancelScan,
            )
        }
    }
}

/** Muestra íntegro el motivo del fallo, incluido el mensaje literal que devuelva la API. */
@Composable
private fun ErrorDialog(message: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.error_dialog_title)) },
        text = { Text(message, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_understood)) }
        },
    )
}

@Composable
private fun HomeContent(
    discogsConfigured: Boolean,
    onSelect: (ScanMode) -> Unit,
    onOpenCollection: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text(
            text = stringResource(R.string.home_title),
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = stringResource(R.string.home_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (!discogsConfigured) {
            Text(
                text = stringResource(R.string.missing_credentials),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }

        ModeCard(
            title = stringResource(R.string.action_barcode),
            description = stringResource(R.string.action_barcode_description),
            enabled = discogsConfigured,
            onClick = { onSelect(ScanMode.BARCODE) },
        )
        ModeCard(
            title = stringResource(R.string.action_cover),
            description = stringResource(R.string.action_cover_description),
            enabled = discogsConfigured,
            onClick = { onSelect(ScanMode.COVER) },
        )
        ModeCard(
            title = stringResource(R.string.collection_open),
            description = stringResource(R.string.collection_open_description),
            enabled = discogsConfigured,
            onClick = onOpenCollection,
            container = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

@Composable
private fun ModeCard(
    title: String,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
    container: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.secondaryContainer,
) {
    Card(
        onClick = onClick,
        enabled = enabled,
        colors = CardDefaults.cardColors(containerColor = container),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleLarge)
            Text(text = description, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun PermissionRequest(onGrant: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.camera_permission_required),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onGrant) {
            Text(stringResource(R.string.camera_permission_grant))
        }
    }
}

@Composable
private fun WorkingOverlay(step: RegistrationStep?, registering: Boolean) {
    Surface(color = Color.Black.copy(alpha = 0.6f), modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator(color = Color.White)
            Text(
                text = stringResource(if (registering) R.string.step_registering else step.label()),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

private fun RegistrationStep?.label(): Int = when (this) {
    RegistrationStep.RECOGNISING_COVER -> R.string.step_recognising
    RegistrationStep.SEARCHING_CATALOG -> R.string.step_searching
    RegistrationStep.CHOOSING_EDITION -> R.string.step_choosing
    RegistrationStep.CHECKING_COLLECTION -> R.string.step_checking
    null -> R.string.step_searching
}

private fun Context.hasCameraPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED
