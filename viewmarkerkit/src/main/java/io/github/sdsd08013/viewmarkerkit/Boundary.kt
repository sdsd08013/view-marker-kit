package io.github.sdsd08013.viewmarkerkit

import android.graphics.Point
import androidx.annotation.MainThread
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import io.github.sdsd08013.viewmarkerkit.ScreenMetrics

/**
 * 描画対象とみなす範囲（画面 + 余白）の判定。
 *
 * @param marginDp 画面の外側に取る余白。画面端で View が見切れて消えないよう、最大マーカーサイズ以上を渡す
 */
internal class Boundary(
    private val marginDp: Int,
    private val screenMetrics: () -> ScreenMetrics,
) {
    fun isInVisibleBounds(marker: ViewMarker, map: GoogleMap): Boolean = isInBounds(marker.location, map)

    fun isInVisibleBounds(marker: ViewMarker, bounds: LatLngBounds): Boolean {
        return bounds.contains(marker.location)
    }

    private fun isInBounds(position: LatLng, map: GoogleMap): Boolean {
        return boundsWithMargin(map).contains(position)
    }

    @MainThread
    fun boundsWithMargin(googleMap: GoogleMap): LatLngBounds {
        val metrics = screenMetrics()
        val maxMarkerSize = marginDp.dpToPx(metrics.density)
        val w = metrics.widthPixels
        val h = metrics.heightPixels

        val topLeft = Point(-maxMarkerSize, -maxMarkerSize)
        val topRight = Point(w + maxMarkerSize, 0)
        val bottomLeft = Point(0, h + maxMarkerSize)
        val bottomRight = Point(w + maxMarkerSize, h + maxMarkerSize)

        val topLeftLatLng: LatLng = googleMap.projection.fromScreenLocation(topLeft)
        val topRightLatLng: LatLng = googleMap.projection.fromScreenLocation(topRight)
        val bottomLeftLatLng: LatLng = googleMap.projection.fromScreenLocation(bottomLeft)
        val bottomRightLatLng: LatLng = googleMap.projection.fromScreenLocation(bottomRight)

        return LatLngBounds.Builder().apply {
            include(topLeftLatLng)
            include(topRightLatLng)
            include(bottomLeftLatLng)
            include(bottomRightLatLng)
        }.build()
    }
}
