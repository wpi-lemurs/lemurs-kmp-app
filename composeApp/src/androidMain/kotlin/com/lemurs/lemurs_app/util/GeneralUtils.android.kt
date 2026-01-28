package com.lemurs.lemurs_app.util

import java.util.Locale

actual fun formatTwoDecimals(value: Double): String {
    return String.format(Locale.US, "%.2f", value)
}