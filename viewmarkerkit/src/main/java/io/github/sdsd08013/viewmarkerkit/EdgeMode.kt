package io.github.sdsd08013.viewmarkerkit

/** A screen position after [EdgeMode] is applied. [angleInDegrees] is 0 unless the point was clamped. */
internal data class MarkerPositionResult(
    val adjustedPoint: ScreenPoint,
    val angleInDegrees: Float,
)

/** How markers outside the screen are handled. */
sealed class EdgeMode {
    internal abstract fun resolve(screenPoint: ScreenPoint, screenWidth: Int, screenHeight: Int): MarkerPositionResult

    internal abstract fun edgeAt(adjustedPoint: ScreenPoint, screenWidth: Int, screenHeight: Int): MarkerEdge

    /** Markers keep their real screen position, even when it is off screen. */
    data object None : EdgeMode() {
        override fun resolve(screenPoint: ScreenPoint, screenWidth: Int, screenHeight: Int): MarkerPositionResult =
            MarkerPositionResult(adjustedPoint = screenPoint, angleInDegrees = 0f)

        override fun edgeAt(adjustedPoint: ScreenPoint, screenWidth: Int, screenHeight: Int): MarkerEdge = MarkerEdge.NONE
    }

    /**
     * Off-screen markers are pulled to the screen edge, [marginPx] away from it.
     *
     * @param bottomInsetPx area excluded from the bottom, e.g. an input bar
     */
    data class Clamp(
        val marginPx: Int,
        val bottomInsetPx: Int = 0,
    ) : EdgeMode() {
        override fun resolve(screenPoint: ScreenPoint, screenWidth: Int, screenHeight: Int): MarkerPositionResult {
            val centerX = screenWidth / 2
            val centerY = screenHeight / 2
            val (adjustedPoint, isAtEdge) = screenPoint.toScreenEdgeIfOutside(marginPx, screenWidth, screenHeight, bottomInsetPx)
            val angleInDegrees = if (isAtEdge) adjustedPoint.calculateAngleFromCenter(centerX, centerY).toFloat() else 0f

            return MarkerPositionResult(
                adjustedPoint = adjustedPoint,
                angleInDegrees = angleInDegrees,
            )
        }

        override fun edgeAt(adjustedPoint: ScreenPoint, screenWidth: Int, screenHeight: Int): MarkerEdge {
            val left = marginPx
            val right = screenWidth - marginPx
            val top = marginPx
            val bottom = screenHeight - marginPx - bottomInsetPx

            val isLeft = adjustedPoint.x <= left
            val isRight = adjustedPoint.x >= right
            val isTop = adjustedPoint.y <= top
            val isBottom = adjustedPoint.y >= bottom

            return when {
                isLeft && isTop -> MarkerEdge.TOP_LEFT
                isRight && isTop -> MarkerEdge.TOP_RIGHT
                isLeft && isBottom -> MarkerEdge.BOTTOM_LEFT
                isRight && isBottom -> MarkerEdge.BOTTOM_RIGHT
                isLeft -> MarkerEdge.LEFT
                isRight -> MarkerEdge.RIGHT
                isTop -> MarkerEdge.TOP
                isBottom -> MarkerEdge.BOTTOM
                else -> MarkerEdge.NONE
            }
        }
    }
}
