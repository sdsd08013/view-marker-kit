package io.github.sdsd08013.viewmarkerkit

import android.graphics.Point
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.Projection
import com.google.android.gms.maps.model.LatLng

/**
 * Converts between map coordinates and overlay pixels.
 *
 * Wraps a map [Projection] via [asScreenProjection]; tests can supply their own implementation.
 */
interface ScreenProjection {
    fun toScreen(location: LatLng): ScreenPoint

    fun fromScreen(point: ScreenPoint): LatLng
}

fun Projection.asScreenProjection(): ScreenProjection = object : ScreenProjection {
    override fun toScreen(location: LatLng): ScreenPoint {
        val point = toScreenLocation(location)
        return ScreenPoint(point.x, point.y)
    }

    override fun fromScreen(point: ScreenPoint): LatLng = fromScreenLocation(Point(point.x, point.y))
}

/** Size of the area markers are placed in, in pixels. */
data class Viewport(val width: Int, val height: Int)

/**
 * Camera state at one moment. Capture it on the main thread with [GoogleMap.cameraSnapshot];
 * the snapshot itself can be used from any thread.
 */
data class CameraSnapshot(
    val projection: ScreenProjection,
    val zoom: Float,
    val bearing: Float,
    val center: LatLng,
)

/** Captures the current camera. Main thread only. */
fun GoogleMap.cameraSnapshot(): CameraSnapshot = CameraSnapshot(
    projection = projection.asScreenProjection(),
    zoom = cameraPosition.zoom,
    bearing = cameraPosition.bearing,
    center = cameraPosition.target,
)
