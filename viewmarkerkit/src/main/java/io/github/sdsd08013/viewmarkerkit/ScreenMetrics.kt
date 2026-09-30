package io.github.sdsd08013.viewmarkerkit

/** Screen size and density needed by the geometry code, so it never touches a `Context`. */
internal data class ScreenMetrics(
    val density: Float,
    val widthPixels: Int,
    val heightPixels: Int,
)

internal fun Int.dpToPx(density: Float): Int = (this * density + 0.5).toInt()
