package io.github.sdsd08013.viewmarkerkit

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import java.util.concurrent.ConcurrentHashMap

/** Emitted after [MarkerOverlayView.render] has attached a view. */
internal data class MarkerRendered(
    val marker: ViewMarker,
    val annotation: ViewAnnotation,
    val descriptor: MarkerPositionDescriptor,
)

/**
 * The container that holds marker views. Place it over the map, covering the same area,
 * and set [viewFactory] before markers are shown.
 *
 * Marker views are added and removed by [ViewMarkerLayer]; the only direct operation
 * is [dispatchEvent].
 */
class MarkerOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private val viewAnnotationMap = ConcurrentHashMap<MarkerIdentity, ViewAnnotation>()

    /** Creates the view for each marker. Required. */
    var viewFactory: MarkerViewFactory? = null

    /** The layer currently using this overlay. An overlay serves one layer at a time. */
    internal var layer: ViewMarkerLayer<*>? = null
        private set

    /** Binds [newLayer]; a previously bound layer is closed first. */
    internal fun bind(newLayer: ViewMarkerLayer<*>) {
        val previous = layer
        if (previous === newLayer) return
        previous?.close()
        layer = newLayer
    }

    internal fun unbind(boundLayer: ViewMarkerLayer<*>) {
        if (layer === boundLayer) layer = null
    }

    /** Attached views by identity. */
    internal val annotations: MutableMap<MarkerIdentity, ViewAnnotation> get() = viewAnnotationMap

    internal fun putAnnotation(identity: MarkerIdentity, annotation: ViewAnnotation) {
        viewAnnotationMap[identity] = annotation
        addView(annotation.view, annotation.viewLayoutParams)
    }

    internal fun removeAnnotation(identity: MarkerIdentity): ViewAnnotation? {
        return viewAnnotationMap.remove(identity)
    }

    /**
     * Creates the view for [marker] via [viewFactory] and attaches it at [descriptor].
     *
     * View creation may be asynchronous. [isRenderable] is checked once the view is ready,
     * so a marker hidden in the meantime is not attached.
     */
    internal fun render(
        marker: ViewMarker,
        descriptor: MarkerPositionDescriptor,
        scale: Float?,
        isRenderable: () -> Boolean,
    ) {
        val factory = viewFactory ?: run {
            Log.w(TAG, "viewFactory is not set; skipping ${marker.identity}")
            return
        }
        factory.createAsync(marker, descriptor.currentEdge) { view ->
            if (!isRenderable()) return@createAsync
            if (containsAnnotation(marker.identity)) return@createAsync

            val size = marker.sizeInPx(resources.displayMetrics.density)
            val annotation = ViewAnnotation(
                view = view,
                viewLayoutParams = FrameLayout.LayoutParams(size, size),
            )

            view.applyPlacement(descriptor, scale)

            putAnnotation(marker.identity, annotation)

            layer?.onMarkerRendered(MarkerRendered(marker, annotation, descriptor))
        }
    }

    private fun View.applyPlacement(descriptor: MarkerPositionDescriptor, scale: Float?) {
        translationX = descriptor.screenPosition.x.toFloat()
        translationY = descriptor.screenPosition.y.toFloat()
        scale?.let {
            scaleX = it
            scaleY = it
        }
    }

    internal fun containsAnnotation(identity: MarkerIdentity): Boolean = viewAnnotationMap.containsKey(identity)

    internal fun isAnnotationEmpty(): Boolean = viewAnnotationMap.isEmpty()

    internal fun getAnnotation(identity: MarkerIdentity): ViewAnnotation? = viewAnnotationMap[identity]

    internal fun annotationKeys(): Set<MarkerIdentity> = viewAnnotationMap.keys

    /** Delivers [event] to the attached view of [identity] if it implements [MarkerEventReceiver]. */
    fun dispatchEvent(identity: MarkerIdentity, event: MarkerEvent) {
        viewAnnotationMap[identity]?.eventReceiver?.receive(event)
    }

    private companion object {
        const val TAG = "MarkerOverlayView"
    }
}
