// app/src/main/java/com/maisha/game/util/ShareIntentHelper.kt (new)
package com.maisha.game.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import com.maisha.game.R

object ShareIntentHelper {

    fun shareImage(context: Context, imageUri: Uri, caption: String) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, imageUri)
            putExtra(Intent.EXTRA_TEXT, caption)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(
            sendIntent,
            context.getString(R.string.share_chooser_title)
        ).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (context !is android.app.Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        if (context.packageManager.resolveActivity(
                chooser,
                PackageManager.MATCH_DEFAULT_ONLY
            ) == null
        ) {
            throw IllegalStateException("No app available to handle share intent")
        }
        context.startActivity(chooser)
    }
}
