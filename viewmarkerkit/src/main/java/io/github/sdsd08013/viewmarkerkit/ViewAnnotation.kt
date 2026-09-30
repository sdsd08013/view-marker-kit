package io.github.sdsd08013.viewmarkerkit

import android.view.View
import android.widget.FrameLayout

/** An attached marker view. Capabilities are resolved once here instead of on every dispatch. */
internal data class ViewAnnotation(
    val view: View,
    val viewLayoutParams: FrameLayout.LayoutParams
) {
    val alignable: Alignable? = view as? Alignable
    val eventReceiver: MarkerEventReceiver? = view as? MarkerEventReceiver
}
