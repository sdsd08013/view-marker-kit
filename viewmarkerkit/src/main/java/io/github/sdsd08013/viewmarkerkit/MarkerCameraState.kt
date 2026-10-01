package io.github.sdsd08013.viewmarkerkit

import com.google.android.gms.maps.model.LatLng

/** Camera state captured on the main thread and handed to the position calculation. */
internal data class MarkerCameraState(
    val projection: ScreenProjection,
    val zoom: Float,
    val bearing: Float,
    val center: LatLng,
)
