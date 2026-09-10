package com.example.snapspin.di

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.snapspin.SnapSpinApp
import com.example.snapspin.presentation.collection.CollectionViewModel
import com.example.snapspin.presentation.scan.ScanViewModel

/** Fábrica de ViewModels que resuelve las dependencias contra el [AppContainer]. */
object ViewModelFactory {

    val scan: ViewModelProvider.Factory = viewModelFactory {
        initializer {
            val container = container()
            ScanViewModel(
                prepareRegistration = container.prepareRegistration,
                confirmRegistration = container.confirmRegistration,
                discogsConfigured = container.isDiscogsConfigured,
            )
        }
    }

    val collection: ViewModelProvider.Factory = viewModelFactory {
        initializer {
            val container = container()
            CollectionViewModel(
                loadCollection = container.loadCollection,
                filterCollection = container.filterCollection,
                removeFromCollection = container.removeFromCollection,
                resolveAlbumYears = container.resolveAlbumYears,
            )
        }
    }

    private fun CreationExtras.container(): AppContainer =
        (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as SnapSpinApp).container
}
