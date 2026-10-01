package io.github.sdsd08013.viewmarkerkit

import com.google.android.gms.maps.model.LatLngBounds

/**
 * The visible area of the map extended by [marginDp] on every side.
 *
 * The margin keeps markers near the edge attached while part of their view is still on screen.
 */
internal class Boundary(
    private val marginDp: Int,
    private val screenMetrics: () -> ScreenMetrics,
) {
    fun isInVisibleBounds(marker: ViewMarker, projection: ScreenProjection): Boolean =
        boundsWithMargin(projection).contains(marker.location)

    fun isInVisibleBounds(marker: ViewMarker, bounds: LatLngBounds): Boolean {
        return bounds.contains(marker.location)
    }

    fun boundsWithMargin(projection: ScreenProjection): LatLngBounds {
        val metrics = screenMetrics()
        val margin = marginDp.dpToPx(metrics.density)
        val w = metrics.widthPixels
        val h = metrics.heightPixels

        return LatLngBounds.Builder().apply {
            include(projection.fromScreen(ScreenPoint(-margin, -margin)))
            include(projection.fromScreen(ScreenPoint(w + margin, -margin)))
            include(projection.fromScreen(ScreenPoint(-margin, h + margin)))
            include(projection.fromScreen(ScreenPoint(w + margin, h + margin)))
        }.build()
    }
}
