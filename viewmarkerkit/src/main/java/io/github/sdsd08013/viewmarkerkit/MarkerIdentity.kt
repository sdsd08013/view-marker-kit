package io.github.sdsd08013.viewmarkerkit

/**
 * Key that identifies a marker. Implementations must have value-based `equals` / `hashCode`.
 *
 * When [ViewMarker.id] alone is unique, the default [IdMarkerIdentity] is used and
 * no implementation is needed.
 */
interface MarkerIdentity

/** [MarkerIdentity] backed by [ViewMarker.id]. The default for [ViewMarker.identity]. */
data class IdMarkerIdentity(val id: Long) : MarkerIdentity
