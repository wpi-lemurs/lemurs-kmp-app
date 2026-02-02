package com.lemurs.lemurs_app.util

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

class IosUriOpener : UriOpener {
    override fun openUri(uri: String) {
        val nsUrl = NSURL.URLWithString(uri)
        if (nsUrl != null) {
            UIApplication.sharedApplication.openURL(nsUrl, emptyMap<Any?, Any?>()) { success ->
                if (!success) {
                    println("⚠️ Failed to open URL: $uri")
                }
            }
        }
    }
}
