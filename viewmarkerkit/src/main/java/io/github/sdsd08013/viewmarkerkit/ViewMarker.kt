package io.github.sdsd08013.viewmarkerkit

import com.google.android.gms.maps.model.LatLng

/**
 * A marker rendered as an Android view.
 *
 * The layer only needs where to put the view ([location]), how large it is ([sizeInDp])
 * and how to identify it ([identity]). Everything else belongs to your implementation.
 */
interface ViewMarker {
    val id: Long

    /**
     * Key used to match the marker with its view and position.
     * Override when [id] alone is not unique, e.g. when ids of different marker types overlap.
     */
    val identity: MarkerIdentity get() = IdMarkerIdentity(id)

    var location: LatLng

    /** Side length of the square area the view occupies, in dp. */
    val sizeInDp: Int

    fun sizeInPx(density: Float): Int = sizeInDp.dpToPx(density)

    /** Horizontal distance in px from the view's top-left corner to the point placed at [location]. Defaults to the center. */
    fun offsetX(density: Float): Int = sizeInPx(density) / 2

    /** Vertical distance in px from the view's top-left corner to the point placed at [location]. Defaults to the center. */
    fun offsetY(density: Float): Int = sizeInPx(density) / 2

    /** Ids represented by this marker. A marker that groups several items returns their ids. */
    val childIds: List<Long> get() = listOf(id)
}
