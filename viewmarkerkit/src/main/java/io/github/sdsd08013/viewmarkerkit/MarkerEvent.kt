package io.github.sdsd08013.viewmarkerkit

/**
 * A one-shot event delivered to an attached marker view via [MarkerOverlayView.dispatchEvent].
 *
 * Use it for things that do not need to be restored when the view is recreated
 * (an animation trigger, for example). Define the concrete events in your app,
 * preferably as a sealed hierarchy.
 */
interface MarkerEvent
