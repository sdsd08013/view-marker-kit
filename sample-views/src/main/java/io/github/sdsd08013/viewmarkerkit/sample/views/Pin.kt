package io.github.sdsd08013.viewmarkerkit.sample.views

import com.google.android.gms.maps.model.LatLng
import io.github.sdsd08013.viewmarkerkit.MarkerEvent
import io.github.sdsd08013.viewmarkerkit.ViewMarker

/** A marker with a label. Only id, location and size are required by the library. */
class Pin(
    override val id: Long,
    override var location: LatLng,
    val label: String,
    val color: Int,
) : ViewMarker {
    override val sizeInDp: Int = PinView.SIZE_DP
}

/** Sent to a pin's view when it is tapped. */
object Bounce : MarkerEvent
