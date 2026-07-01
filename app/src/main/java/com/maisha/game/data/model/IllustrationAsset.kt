// app/src/main/java/com/maisha/game/data/model/IllustrationAsset.kt (new)
package com.maisha.game.data.model

/**
 * Indirection for bundled illustrations — swap [resourceName] when real art is imported
 * (e.g. Storyset SVG → Vector Asset in Android Studio) without touching UI code.
 */
enum class ResourceType {
    VECTOR_DRAWABLE,
    RASTER
}

data class IllustrationRef(
    val id: String,
    val resourceType: ResourceType,
    val resourceName: String
)
