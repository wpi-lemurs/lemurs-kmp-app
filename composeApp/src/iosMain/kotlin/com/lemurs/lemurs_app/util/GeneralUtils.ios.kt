package com.lemurs.lemurs_app.util

import platform.Foundation.NSString
import platform.Foundation.stringWithFormat

actual fun formatTwoDecimals(value: Double): String =
    NSString.stringWithFormat("%.2f", value) as String