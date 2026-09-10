package com.example.snapspin.data.gemini.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Respuesta que se le pide al modelo: JSON con el grupo y el título, o una negativa explícita. */
@Serializable
data class CoverAnswerDto(
    @SerialName("is_album_cover") val isAlbumCover: Boolean = false,
    val artist: String = "",
    val album: String = "",
)
