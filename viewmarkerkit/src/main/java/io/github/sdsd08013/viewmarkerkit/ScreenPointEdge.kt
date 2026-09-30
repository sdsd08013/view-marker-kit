package io.github.sdsd08013.viewmarkerkit

/**
 * Clamps this point to the screen edge along the ray from the screen center.
 *
 * Returns the point unchanged (with `false`) when it is already inside the margin,
 * otherwise the clamped point with `true`.
 *
 * @param bottomOffsetPx extra area excluded from the bottom, e.g. an input bar
 */
internal fun ScreenPoint.toScreenEdgeIfOutside(
    marginPx: Int,
    screenWidth: Int,
    screenHeight: Int,
    bottomOffsetPx: Int = 0,
): Pair<ScreenPoint, Boolean> {
    if (screenWidth <= 0 || screenHeight <= 0) {
        return Pair(ScreenPoint(screenWidth / 2, screenHeight / 2), true)
    }

    val maxValidX = screenWidth - marginPx
    val maxValidY = screenHeight - marginPx - bottomOffsetPx

    if (marginPx >= maxValidX || marginPx >= maxValidY) {
        // Margin too large for this screen: fall back to the center
        val centerX = screenWidth / 2
        val centerY = screenHeight / 2
        return Pair(ScreenPoint(centerX, centerY), true)
    }

    val isInsideScreen = x in marginPx..maxValidX && y in marginPx..maxValidY

    if (isInsideScreen) {
        return Pair(this, false)
    }

    val centerX = screenWidth / 2
    val centerY = screenHeight / 2
    val dx = (x - centerX).toDouble()
    val dy = (y - centerY).toDouble()

    if (dx == 0.0 && dy == 0.0) {
        return Pair(ScreenPoint(centerX, centerY), true)
    }

    val edgeX: Int
    val edgeY: Int

    when {
        dx == 0.0 -> {
            edgeX = centerX
            edgeY = if (dy > 0) maxValidY else marginPx
        }
        dy == 0.0 -> {
            edgeX = if (dx > 0) maxValidX else marginPx
            edgeY = centerY
        }
        else -> {
            // Intersect the ray with each edge and pick the one it hits first
            val slope = dy / dx

            val leftX = marginPx.toDouble()
            val leftY = centerY + slope * (leftX - centerX)

            val rightX = maxValidX.toDouble()
            val rightY = centerY + slope * (rightX - centerX)

            val topY = marginPx.toDouble()
            val topX = centerX + (topY - centerY) / slope

            val bottomY = maxValidY.toDouble()
            val bottomX = centerX + (bottomY - centerY) / slope

            when {
                dx < 0 && leftY in marginPx.toDouble()..maxValidY.toDouble() -> {
                    edgeX = leftX.toInt()
                    edgeY = leftY.toInt()
                }
                dx > 0 && rightY in marginPx.toDouble()..maxValidY.toDouble() -> {
                    edgeX = rightX.toInt()
                    edgeY = rightY.toInt()
                }
                dy < 0 && topX in marginPx.toDouble()..maxValidX.toDouble() -> {
                    edgeX = topX.toInt()
                    edgeY = topY.toInt()
                }
                dy > 0 && bottomX in marginPx.toDouble()..maxValidX.toDouble() -> {
                    edgeX = bottomX.toInt()
                    edgeY = bottomY.toInt()
                }
                else -> {
                    edgeX = x.coerceIn(marginPx, maxValidX)
                    edgeY = y.coerceIn(marginPx, maxValidY)
                }
            }
        }
    }

    return Pair(ScreenPoint(edgeX, edgeY), true)
}
