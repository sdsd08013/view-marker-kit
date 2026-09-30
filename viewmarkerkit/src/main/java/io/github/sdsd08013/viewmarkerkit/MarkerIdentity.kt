package io.github.sdsd08013.viewmarkerkit

/**
 * マーカーを一意に識別するキー。
 *
 * エンジンは本型を Map のキーおよび等値比較にのみ使う。実装は `equals` / `hashCode` を
 * 値ベースで定義すること（`data class` / `data object` を推奨）。
 *
 * [ViewMarker.id] だけで一意になるなら実装は不要で、[ViewMarker.identity] の既定値（[IdMarkerIdentity]）が使われる。
 */
interface MarkerIdentity

/** [ViewMarker.id] をそのままキーにする [MarkerIdentity]。[ViewMarker.identity] の既定値。 */
data class IdMarkerIdentity(val id: Long) : MarkerIdentity
