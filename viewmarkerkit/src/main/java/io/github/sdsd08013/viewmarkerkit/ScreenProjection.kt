package io.github.sdsd08013.viewmarkerkit

import android.graphics.Point
import com.google.android.gms.maps.Projection
import com.google.android.gms.maps.model.LatLng

/** Converts between map coordinates and overlay pixels. Wraps [Projection] so geometry can be tested without a map. */
internal interface ScreenProjection {
    fun toScreen(location: LatLng): ScreenPoint

    fun fromScreen(point: ScreenPoint): LatLng
}

internal fun Projection.asScreenProjection(): ScreenProjection = object : ScreenProjection {
    override fun toScreen(location: LatLng): ScreenPoint {
        val point = toScreenLocation(location)
        return ScreenPoint(point.x, point.y)
    }

    override fun fromScreen(point: ScreenPoint): LatLng = fromScreenLocation(Point(point.x, point.y))
}

/** Size of the overlay in pixels. */
internal data class Viewport(val width: Int, val height: Int)
