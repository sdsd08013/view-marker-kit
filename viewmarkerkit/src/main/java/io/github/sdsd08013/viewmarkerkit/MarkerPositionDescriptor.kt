package io.github.sdsd08013.viewmarkerkit

/**
 * Where a marker view is placed on screen.
 *
 * @property origin screen position of [ViewMarker.location] after edge clamping
 * @property screenPosition top-left of the view, i.e. [origin] minus the marker's offset
 * @property rotation angle from the screen center in degrees when clamped to an edge, otherwise 0
 */
data class MarkerPositionDescriptor(
    val identifier: MarkerIdentity,
    val childIds: List<Long>,
    val origin: ScreenPoint,
    val screenPosition: ScreenPoint,
    val rotation: Float = 0f,
    val currentEdge: MarkerEdge = MarkerEdge.NONE,
    val previousEdge: MarkerEdge = MarkerEdge.NONE
) {
    /** The marker just moved from inside the screen to an edge. */
    val transitionToEdge: Boolean
        get() = previousEdge == MarkerEdge.NONE && currentEdge != MarkerEdge.NONE

    /** The marker just moved from an edge back inside the screen. */
    val transitionFromEdge: Boolean
        get() = previousEdge != MarkerEdge.NONE && currentEdge == MarkerEdge.NONE

    val isEdgeChanged: Boolean
        get() = previousEdge != currentEdge

    val isAtEdge: Boolean
        get() = currentEdge != MarkerEdge.NONE
}
