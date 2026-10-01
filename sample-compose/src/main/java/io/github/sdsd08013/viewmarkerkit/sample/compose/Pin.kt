package io.github.sdsd08013.viewmarkerkit.sample.compose

import androidx.compose.ui.graphics.Color
import com.google.android.gms.maps.model.LatLng
import io.github.sdsd08013.viewmarkerkit.MarkerEvent
import io.github.sdsd08013.viewmarkerkit.ViewMarker

/** A marker with a label. Only id, location and size are required by the library. */
class Pin(
    override val id: Long,
    override var location: LatLng,
    val label: String,
    val color: Color,
) : ViewMarker {
    override val sizeInDp: Int = PIN_SIZE_DP
}

/** Side of the square area the layer reserves for each pin view. */
const val PIN_SIZE_DP = 56

/** Sent to a pin's view when it is tapped. */
object Bounce : MarkerEvent
