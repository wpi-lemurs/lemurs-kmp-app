package com.lemurs.lemurs_app.util

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri

class AndroidUriOpener(
    private val context: Context
) : UriOpener {
    override fun openUri(uri: String) {
        val intent = Intent(Intent.ACTION_VIEW, uri.toUri())
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}