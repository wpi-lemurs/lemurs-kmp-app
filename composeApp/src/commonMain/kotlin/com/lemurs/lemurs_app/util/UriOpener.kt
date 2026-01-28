package com.lemurs.lemurs_app.util


// Local Uri opener is abstracted in commonMain to be Kotlin Multiplatform compatible.
// This interface was made because the android-specific API  local uriHandler is not KMM compatible.
// other platforms such as iOS will have their own implementation of this interface.
// commonMain uses this interface to open URIs in a platform-agnostic way.

interface UriOpener {
    fun openUri(uri: String)
}
