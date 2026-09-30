package io.github.sdsd08013.viewmarkerkit

/**
 * Implemented by marker views that want to know their placement when clamped to a screen edge.
 * Called on every position update while [EdgeMode.Clamp] is active.
 */
interface Alignable {
    fun align(descriptor: MarkerPositionDescriptor)
}

/** Implemented by marker views that receive [MarkerEvent]s via [MarkerOverlayView.dispatchEvent]. */
interface MarkerEventReceiver {
    fun receive(event: MarkerEvent)
}
