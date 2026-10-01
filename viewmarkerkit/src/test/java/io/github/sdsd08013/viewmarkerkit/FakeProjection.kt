package io.github.sdsd08013.viewmarkerkit

import com.google.android.gms.maps.model.LatLng
import kotlin.math.roundToInt

/** Linear projection: [center] maps to the middle of the viewport, one degree is [pxPerDegree] px. */
internal class FakeProjection(
    var center: LatLng,
    val width: Int = 1080,
    val height: Int = 1920,
    val pxPerDegree: Double = 1000.0,
) : ScreenProjection {
    override fun toScreen(location: LatLng): ScreenPoint = ScreenPoint(
        (width / 2 + (location.longitude - center.longitude) * pxPerDegree).roundToInt(),
        (height / 2 - (location.latitude - center.latitude) * pxPerDegree).roundToInt(),
    )

    override fun fromScreen(point: ScreenPoint): LatLng = LatLng(
        center.latitude - (point.y - height / 2) / pxPerDegree,
        center.longitude + (point.x - width / 2) / pxPerDegree,
    )
}

internal class TestMarker(
    override val id: Long,
    override var location: LatLng,
    override val sizeInDp: Int = 48,
) : ViewMarker
