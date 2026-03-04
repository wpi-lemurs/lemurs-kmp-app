package com.lemurs.lemurs_app.ui.navigation

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

fun Modifier.swipeBack(
    enabled: Boolean,
    edgeOnly: Boolean = true,
    edgeWidthDp: Float = 24f,        // touch must start near left edge
    triggerDistanceDp: Float = 80f,  // how far to drag right to trigger back
    onBack: () -> Unit
): Modifier = composed {
    if (!enabled) return@composed this

    val density = LocalDensity.current
    val edgeWidthPx = with(density) { edgeWidthDp.dp.toPx() }
    val triggerDistancePx = with(density) { triggerDistanceDp.dp.toPx() }

    var startedFromEdge by remember { mutableStateOf(false) }
    var totalDx by remember { mutableStateOf(0f) }

    this.pointerInput(enabled) {
        detectHorizontalDragGestures(
            onDragStart = { offset ->
                totalDx = 0f
                startedFromEdge = !edgeOnly || offset.x <= edgeWidthPx
            },
            onHorizontalDrag = { _, dragAmount ->
                if (!startedFromEdge) return@detectHorizontalDragGestures
                totalDx += dragAmount
            },
            onDragEnd = {
                if (startedFromEdge && totalDx >= triggerDistancePx) {
                    onBack()
                }
                startedFromEdge = false
                totalDx = 0f
            },
            onDragCancel = {
                startedFromEdge = false
                totalDx = 0f
            }
        )
    }
}