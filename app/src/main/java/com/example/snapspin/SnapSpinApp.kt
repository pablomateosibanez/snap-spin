package com.example.snapspin

import android.app.Application
import com.example.snapspin.di.AppContainer

/** Punto de entrada de la app; expone el contenedor de dependencias. */
class SnapSpinApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
