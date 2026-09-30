package io.github.sdsd08013.viewmarkerkit

import com.google.android.gms.maps.Projection
import com.google.android.gms.maps.model.LatLng

/**
 * 座標計算に必要なカメラ状態。main thread で取得し、計算スレッドへ渡す。
 */
internal data class MarkerCameraState(
    val projection: Projection,
    val zoom: Float,
    val bearing: Float,
    val center: LatLng,
)
