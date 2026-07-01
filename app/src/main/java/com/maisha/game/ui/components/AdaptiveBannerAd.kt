// app/src/main/java/com/maisha/game/ui/components/AdaptiveBannerAd.kt
package com.maisha.game.ui.components

import android.app.Activity
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

@Composable
fun AdaptiveBannerAd(
    adUnitId: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val adWidth = remember(context) {
        val displayMetrics = context.resources.displayMetrics
        (displayMetrics.widthPixels / displayMetrics.density).toInt()
    }

    if (activity == null) return

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        factory = { ctx ->
            AdView(ctx).apply {
                setAdSize(
                    AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(ctx, adWidth)
                )
                this.adUnitId = adUnitId
                loadAd(AdRequest.Builder().build())
            }
        },
        update = { adView ->
            adView.adUnitId = adUnitId
        },
        onRelease = { adView ->
            adView.destroy()
        }
    )
}
