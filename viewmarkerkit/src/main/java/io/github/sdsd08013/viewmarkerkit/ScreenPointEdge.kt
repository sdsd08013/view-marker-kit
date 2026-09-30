package io.github.sdsd08013.viewmarkerkit


internal fun ScreenPoint.toScreenEdgeIfOutside(
    marginPx: Int,
    screenWidth: Int,
    screenHeight: Int,
    bottomOffsetPx: Int = 0, // EditTextの高さなど、画面下端から除外する領域のサイズ
): Pair<ScreenPoint, Boolean> {
    // Handle edge cases where screen dimensions are invalid
    if (screenWidth <= 0 || screenHeight <= 0) {
        return Pair(ScreenPoint(screenWidth / 2, screenHeight / 2), true)
    }
    
    // Handle case where margin is too large (would create invalid range for coerceIn)
    val maxValidX = screenWidth - marginPx
    val maxValidY = screenHeight - marginPx - bottomOffsetPx // 下端のオフセットを考慮
    
    if (marginPx >= maxValidX || marginPx >= maxValidY) {
        // Margin is too large, fall back to center point
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

    // If point is at center, just clamp it
    if (dx == 0.0 && dy == 0.0) {
        return Pair(ScreenPoint(centerX, centerY), true)
    }

    val edgeX: Int
    val edgeY: Int

    // Handle special cases
    when {
        dx == 0.0 -> {
            // Vertical line from center
            edgeX = centerX
            edgeY = if (dy > 0) maxValidY else marginPx
        }
        dy == 0.0 -> {
            // Horizontal line from center
            edgeX = if (dx > 0) maxValidX else marginPx
            edgeY = centerY
        }
        else -> {
            // Calculate which edge the ray from center through point will hit
            val slope = dy / dx
            
            // Calculate potential intersection points with each edge
            val leftX = marginPx.toDouble()
            val leftY = centerY + slope * (leftX - centerX)
            
            val rightX = maxValidX.toDouble()
            val rightY = centerY + slope * (rightX - centerX)
            
            val topY = marginPx.toDouble()
            val topX = centerX + (topY - centerY) / slope
            
            val bottomY = maxValidY.toDouble()
            val bottomX = centerX + (bottomY - centerY) / slope
            
            // Determine which edge we'll hit first
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
                    // Fallback to safe clamping (we already validated the range is valid)
                    edgeX = x.coerceIn(marginPx, maxValidX)
                    edgeY = y.coerceIn(marginPx, maxValidY)
                }
            }
        }
    }

    return Pair(ScreenPoint(edgeX, edgeY), true)
}