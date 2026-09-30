package io.github.sdsd08013.viewmarkerkit

/**
 * マーカーを一意に識別するキー。
 *
 * エンジンは本型を Map のキーおよび等値比較にのみ使う。実装は `equals` / `hashCode` を
 * 値ベースで定義すること（`data class` / `data object` を推奨）。
 */
interface MarkerIdentity
