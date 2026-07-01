// app/src/main/java/com/maisha/game/ui/components/IllustrationImage.kt (new)
package com.maisha.game.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.maisha.game.data.model.IllustrationRef
import com.maisha.game.data.model.ResourceType

/**
 * Single call site for object illustrations — vector or raster bundled resources.
 */
@Composable
fun IllustrationImage(
    ref: IllustrationRef,
    size: Dp,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    tint: Color? = null
) {
    val context = LocalContext.current
    val resolvedDescription = contentDescription ?: ref.resourceName
    val resId = remember(ref) {
        context.resources.getIdentifier(ref.resourceName, "drawable", context.packageName)
    }

    if (resId == 0) {
        Box(modifier = modifier.size(size)) {}
        return
    }

    when (ref.resourceType) {
        ResourceType.VECTOR_DRAWABLE -> {
            if (tint != null) {
                Icon(
                    painter = painterResource(id = resId),
                    contentDescription = resolvedDescription,
                    modifier = modifier.size(size),
                    tint = tint
                )
            } else {
                Image(
                    painter = painterResource(id = resId),
                    contentDescription = resolvedDescription,
                    modifier = modifier.size(size),
                    contentScale = ContentScale.Fit
                )
            }
        }
        ResourceType.RASTER -> {
            Image(
                painter = painterResource(id = resId),
                contentDescription = resolvedDescription,
                modifier = modifier.size(size),
                contentScale = ContentScale.Fit,
                colorFilter = tint?.let { ColorFilter.tint(it) }
            )
        }
    }
}

@Composable
fun IllustrationImage(
    ref: IllustrationRef,
    size: Dp,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    IllustrationImage(
        ref = ref,
        size = size,
        modifier = modifier,
        contentDescription = contentDescription,
        tint = null
    )
}
