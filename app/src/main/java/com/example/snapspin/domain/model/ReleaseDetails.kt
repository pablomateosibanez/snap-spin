package com.example.snapspin.domain.model

/** Detalle de una edición concreta, con su precio de mercado. */
data class ReleaseDetails(
    val id: Long,
    val artist: String,
    val title: String,
    val year: Int?,
    val country: String?,
    val formats: List<String>,
    val labels: List<String>,
    val catalogNumber: String?,
    val coverImageUrl: String?,
    val lowestPrice: Double?,
    val currency: String?,
    val copiesForSale: Int,
)
