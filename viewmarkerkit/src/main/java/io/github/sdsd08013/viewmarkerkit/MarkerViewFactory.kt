package io.github.sdsd08013.viewmarkerkit

import android.view.View

/**
 * Creates the view for a marker. Set it on [MarkerOverlayView.viewFactory].
 *
 * [createAsync] may inflate on a background thread, but [onCreated] must be called on
 * the main thread. Use [sync] when the view can be created synchronously.
 */
fun interface MarkerViewFactory {
    fun createAsync(
        marker: ViewMarker,
        edge: MarkerEdge,
        onCreated: (View) -> Unit,
    )

    companion object {
        /** A factory that creates views synchronously on the main thread. */
        fun sync(create: (marker: ViewMarker, edge: MarkerEdge) -> View): MarkerViewFactory =
            MarkerViewFactory { marker, edge, onCreated -> onCreated(create(marker, edge)) }
    }
}
