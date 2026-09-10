package com.example.snapspin.presentation.scan

import com.example.snapspin.domain.model.RegistrationResult

/**
 * Textos que produce el ViewModel.
 *
 * Se mantienen aquí (y no en `strings.xml`) porque se componen con datos del dominio; el resto de
 * literales de la interfaz sí están en recursos.
 */
internal object ScanMessages {

    const val DISCARDED = "Registro descartado"
    const val GENERIC_ERROR = "Ha ocurrido un error inesperado."

    fun registered(result: RegistrationResult): String {
        val album = "${result.release.artist} – ${result.release.title}"
        return if (result.replacedPreviousCopies > 0) {
            "Actualizado en tu colección: $album"
        } else {
            "Añadido a tu colección: $album"
        }
    }
}
