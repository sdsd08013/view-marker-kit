package io.github.sdsd08013.viewmarkerkit

/**
 * 幾何計算に必要な画面情報。Context を計算層に持ち込まないための純データ。
 */
internal data class ScreenMetrics(
    val density: Float,
    val widthPixels: Int,
    val heightPixels: Int,
)

internal fun Int.dpToPx(density: Float): Int = (this * density + 0.5).toInt()
