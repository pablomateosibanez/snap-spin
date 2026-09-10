package com.example.snapspin.data.vision.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AnnotateImagesRequestDto(val requests: List<AnnotateImageRequestDto>)

@Serializable
data class AnnotateImageRequestDto(
    val image: ImageDto,
    val features: List<FeatureDto>,
)

@Serializable
data class ImageDto(val content: String)

@Serializable
data class FeatureDto(val type: String, val maxResults: Int)

@Serializable
data class AnnotateImagesResponseDto(
    val responses: List<AnnotateImageResponseDto> = emptyList(),
)

@Serializable
data class AnnotateImageResponseDto(
    val webDetection: WebDetectionDto? = null,
    val error: StatusDto? = null,
)

@Serializable
data class WebDetectionDto(
    val webEntities: List<WebEntityDto> = emptyList(),
    val bestGuessLabels: List<BestGuessLabelDto> = emptyList(),
    val pagesWithMatchingImages: List<WebPageDto> = emptyList(),
)

@Serializable
data class WebEntityDto(
    val entityId: String? = null,
    val score: Float = 0f,
    val description: String = "",
)

@Serializable
data class BestGuessLabelDto(val label: String = "")

@Serializable
data class WebPageDto(
    val url: String = "",
    val pageTitle: String? = null,
    val score: Float = 0f,
)

@Serializable
data class StatusDto(val code: Int = 0, val message: String = "")

@Serializable
data class AccessTokenResponseDto(
    @SerialName("access_token") val accessToken: String = "",
    @SerialName("expires_in") val expiresInSeconds: Long = 0,
)
