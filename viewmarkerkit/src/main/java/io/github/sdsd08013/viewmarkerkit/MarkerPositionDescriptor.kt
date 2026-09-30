package io.github.sdsd08013.viewmarkerkit

import io.github.sdsd08013.viewmarkerkit.MarkerEdge
import io.github.sdsd08013.viewmarkerkit.ScreenPoint


data class MarkerPositionDescriptor(
    val identifier: MarkerIdentity,
    val childIds: List<Long>,
    val origin: ScreenPoint,
    val screenPosition: ScreenPoint,
    val rotation: Float = 0f,
    val currentEdge: MarkerEdge = MarkerEdge.NONE,
    val previousEdge: MarkerEdge = MarkerEdge.NONE
) {
    /**
     * 非edge → edgeの遷移が起こったかどうかを判定
     */
    val transitionToEdge: Boolean
        get() = previousEdge == MarkerEdge.NONE && currentEdge != MarkerEdge.NONE

    /**
     * edge → 非edgeの遷移が起こったかどうかを判定
     */
    val transitionFromEdge: Boolean
        get() = previousEdge != MarkerEdge.NONE && currentEdge == MarkerEdge.NONE

    val isEdgeChanged: Boolean
        get() = previousEdge != currentEdge

    val isAtEdge: Boolean
        get() = currentEdge != MarkerEdge.NONE
}