// app/src/main/java/com/maisha/game/util/ComposableToImage.kt (new)
package com.maisha.game.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.FileProvider
import com.maisha.game.BuildConfig
import java.io.File
import java.io.FileOutputStream

/**
 * Captures a Composable by rendering it in an off-screen [ComposeView].
 *
 * Method: measure → layout → draw to [Canvas] backed by [Bitmap].
 * Reliable from minSdk 26+ without requiring the view to be attached to a
 * window (unlike [View.drawToBitmap] extensions that need hardware rendering).
 */
object ComposableToImage {

    fun captureComposableAsBitmap(
        context: Context,
        widthPx: Int,
        heightPx: Int,
        content: @Composable () -> Unit
    ): Bitmap {
        val composeView = ComposeView(context)
        composeView.setContent(content)
        val widthSpec = View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(heightPx, View.MeasureSpec.EXACTLY)
        composeView.measure(widthSpec, heightSpec)
        composeView.layout(0, 0, widthPx, heightPx)
        return captureComposableAsBitmap(composeView)
    }

    fun captureComposableAsBitmap(view: View): Bitmap {
        val bitmap = Bitmap.createBitmap(
            view.measuredWidth.coerceAtLeast(1),
            view.measuredHeight.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }

    fun saveBitmapToCache(context: Context, bitmap: Bitmap): Uri {
        val shareDir = File(context.cacheDir, "share").apply { mkdirs() }
        val file = File(shareDir, "life_card_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { output ->
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                throw IllegalStateException("Bitmap compression failed")
            }
        }
        return FileProvider.getUriForFile(
            context,
            "${BuildConfig.APPLICATION_ID}.fileprovider",
            file
        )
    }
}
