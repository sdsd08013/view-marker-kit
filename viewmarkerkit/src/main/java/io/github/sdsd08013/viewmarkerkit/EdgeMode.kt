package io.github.sdsd08013.viewmarkerkit

/**
 * スクリーン座標の補正結果。
 *
 * @param adjustedPoint 補正後のアンカー座標
 * @param angleInDegrees 画面端へ寄せた場合の、画面中央から見た角度。寄せていなければ 0
 */
internal data class MarkerPositionResult(
    val adjustedPoint: ScreenPoint,
    val angleInDegrees: Float,
)

/**
 * 画面外にあるマーカーの扱い。
 */
sealed class EdgeMode {
    /** スクリーン座標をモードに応じて補正する。 */
    internal abstract fun resolve(screenPoint: ScreenPoint, screenWidth: Int, screenHeight: Int): MarkerPositionResult

    /** 補正後の座標が画面のどの端にあるかを返す。 */
    internal abstract fun edgeAt(adjustedPoint: ScreenPoint, screenWidth: Int, screenHeight: Int): MarkerEdge

    /** 画面外のマーカーは画面外のまま置く（端への寄せ・角度計算をしない）。 */
    data object None : EdgeMode() {
        override fun resolve(screenPoint: ScreenPoint, screenWidth: Int, screenHeight: Int): MarkerPositionResult =
            MarkerPositionResult(adjustedPoint = screenPoint, angleInDegrees = 0f)

        override fun edgeAt(adjustedPoint: ScreenPoint, screenWidth: Int, screenHeight: Int): MarkerEdge = MarkerEdge.NONE
    }

    /**
     * 画面外のマーカーを画面端へ寄せて表示する。
     *
     * @param marginPx 画面端からの余白
     * @param bottomInsetPx 画面下端から除外する領域の高さ（入力欄など）
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
            val bottom = screenHeight - marginPx

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
