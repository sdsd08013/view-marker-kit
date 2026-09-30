package io.github.sdsd08013.viewmarkerkit

import com.google.android.gms.maps.model.LatLng

/**
 * 地図上に Android View として描画するマーカーの契約。
 *
 * 描画層が必要とするのは「どこに（[location]）」「どの大きさで（[sizeInDp]）」「何として
 * （[identity]）」置くかだけで、見た目や種別ごとの状態は利用側の実装が持つ。
 */
interface ViewMarker {
    val id: Long

    /**
     * 描画済み View と座標の対応付けに使うキー。
     * 既定は [id] をそのまま使う。種別ごとに id の空間が分かれているなど、id だけでは
     * 一意にならない場合に override する。
     */
    val identity: MarkerIdentity get() = IdMarkerIdentity(id)

    var location: LatLng

    /** View の一辺の長さ（dp）。View は正方形の領域として overlay に置かれる。 */
    val sizeInDp: Int

    fun sizeInPx(density: Float): Int = sizeInDp.dpToPx(density)

    /** View の左上から [location] に合わせるアンカーまでの水平距離（px）。既定は中央。 */
    fun offsetX(density: Float): Int = sizeInPx(density) / 2

    /** View の左上から [location] に合わせるアンカーまでの垂直距離（px）。既定は中央。 */
    fun offsetY(density: Float): Int = sizeInPx(density) / 2

    /** このマーカーが代表する要素の id。複数要素を束ねるマーカーは構成要素の id を返す。 */
    val childIds: List<Long> get() = listOf(id)
}
