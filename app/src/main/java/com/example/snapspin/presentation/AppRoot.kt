package com.example.snapspin.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.snapspin.di.ViewModelFactory
import com.example.snapspin.presentation.collection.CollectionScreen
import com.example.snapspin.presentation.collection.CollectionViewModel
import com.example.snapspin.presentation.scan.ScanScreen
import com.example.snapspin.presentation.scan.ScanViewModel

/** Las dos pantallas de la app. */
private enum class AppScreen { SCAN, COLLECTION }

/**
 * Raíz de la interfaz.
 *
 * Con dos destinos no compensa una librería de navegación: un estado recordado basta y mantiene
 * vivos los ViewModels al ir y volver, así que la colección no se vuelve a descargar.
 */
@Composable
fun AppRoot() {
    var screen by rememberSaveable { mutableStateOf(AppScreen.SCAN) }

    when (screen) {
        AppScreen.SCAN -> ScanScreen(
            viewModel = viewModel<ScanViewModel>(factory = ViewModelFactory.scan),
            onOpenCollection = { screen = AppScreen.COLLECTION },
        )

        AppScreen.COLLECTION -> CollectionScreen(
            viewModel = viewModel<CollectionViewModel>(factory = ViewModelFactory.collection),
            onBack = { screen = AppScreen.SCAN },
        )
    }
}
