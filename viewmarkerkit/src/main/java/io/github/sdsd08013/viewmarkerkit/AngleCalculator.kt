package io.github.sdsd08013.viewmarkerkit

import kotlin.math.atan2

internal fun calculateAngleFromCenter(
    edgeX: Double,
    edgeY: Double,
    centerX: Double,
    centerY: Double,
): Double {
    val dx = edgeX - centerX
    val dy = edgeY - centerY
    
    val angleInDegrees = Math.toDegrees(atan2(-dx, dy))
    
    return if (angleInDegrees < 0) angleInDegrees + 360 else angleInDegrees
}

internal fun ScreenPoint.calculateAngleFromCenter(centerX: Int, centerY: Int): Double {
    return calculateAngleFromCenter(
        edgeX = x.toDouble(),
        edgeY = y.toDouble(),
        centerX = centerX.toDouble(),
        centerY = centerY.toDouble()
    )
}