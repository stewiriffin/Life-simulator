package com.maisha.game.util

import android.util.Log
import kotlinx.serialization.json.Json

/**
 * Safe JSON deserialization for Room blob columns.
 *
 * If a persisted blob no longer matches the current data class shape, returns [default]
 * for that field only so the rest of the save can still load.
 */
object SerializationUtils {

    const val TAG = "SerializationUtils"

    val json: Json = Json { ignoreUnknownKeys = true }

    inline fun <reified T> safeDeserialize(
        raw: String,
        fieldName: String,
        default: T,
        slotId: Int? = null
    ): T {
        return try {
            json.decodeFromString<T>(raw)
        } catch (e: Exception) {
            val slotSuffix = slotId?.let { " (slot $it)" }.orEmpty()
            Log.e(TAG, "Failed to deserialize $fieldName$slotSuffix; using default", e)
            default
        }
    }
}
