package com.example.snapspin.data.discogs

import android.content.Context

/**
 * Guarda el año de salida de cada obra.
 *
 * Averiguarlo cuesta una petición por álbum y el dato no cambia nunca, así que se conserva entre
 * arranques: la espera sólo se paga la primera vez.
 */
interface MasterYearStore {
    fun year(masterId: Long): Int?
    fun save(masterId: Long, year: Int)
}

class SharedPreferencesMasterYearStore(context: Context) : MasterYearStore {

    private val preferences = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    override fun year(masterId: Long): Int? =
        preferences.getInt(masterId.toString(), 0).takeIf { it > 0 }

    override fun save(masterId: Long, year: Int) {
        if (year > 0) preferences.edit().putInt(masterId.toString(), year).apply()
    }

    private companion object {
        const val NAME = "master_years"
    }
}
