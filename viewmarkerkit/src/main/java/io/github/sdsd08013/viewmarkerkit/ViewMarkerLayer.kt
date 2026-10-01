package io.github.sdsd08013.viewmarkerkit

import android.view.Choreographer
import android.view.View
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import androidx.annotation.MainThread
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLngBounds
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Renders [ViewMarker]s as Android views on top of a [GoogleMap] and keeps them in sync
 * with the camera.
 *
 * The layer attaches and detaches views and follows the camera. Deciding which markers to
 * show is up to the caller; call [show] / [hide] accordingly. To animate a marker, update
 * [ViewMarker.location] on each animation frame and call [updatePosition].
 *
 * Forward the map's camera callbacks to [onCameraMove] / [onCameraIdle], or call
 * [attachCameraListeners]. Unless noted otherwise, methods must be called on the main thread.
 *
 * An overlay serves one layer at a time: creating a layer on an overlay that already has one
 * closes the previous layer. Call [close] when the layer is no longer needed.
 *
 * @param lifecycleOwner scopes the background position calculation; use the view lifecycle of the screen
 * @param overlay the container for marker views, placed over the map with [MarkerOverlayView.viewFactory] set
 * @param visibleBoundsMarginDp how far [visibleBounds] extends beyond the overlay; use at least the largest marker size
 */
class ViewMarkerLayer<M : ViewMarker>(
    private val lifecycleOwner: LifecycleOwner,
    private val googleMap: GoogleMap,
    private val overlay: MarkerOverlayView,
    val edgeMode: EdgeMode = EdgeMode.None,
    visibleBoundsMarginDp: Int = DEFAULT_VISIBLE_BOUNDS_MARGIN_DP,
    private val listener: Listener<M>? = null,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    interface Listener<M : ViewMarker> {
        /** The view of [marker] has been attached and its position registered. */
        fun onMarkerAttached(marker: M, view: View, descriptor: MarkerPositionDescriptor) {}

        /**
         * The position of an attached view has been applied. Called for every attached view
         * on each camera move. [marker] is null if it has been hidden in the meantime.
         */
        fun onPositionApplied(marker: M?, view: View, descriptor: MarkerPositionDescriptor) {}
    }

    private val density: Float = overlay.resources.displayMetrics.density

    private val engine = MarkerPositionEngine<M>(
        density = density,
        viewport = {
            val metrics = overlay.resources.displayMetrics
            // Before the first layout the overlay has no size; fall back to the screen
            val width = if (overlay.width > 0) overlay.width else metrics.widthPixels
            val height = if (overlay.height > 0) overlay.height else metrics.heightPixels
            Viewport(width, height)
        },
        edgeMode = edgeMode,
        visibleBoundsMarginDp = visibleBoundsMarginDp,
    )

    // Conflated: only the latest camera state matters
    private val cameraUpdateChannel = Channel<CameraSnapshot>(Channel.CONFLATED)

    // Markers passed to show(), including those whose view is still being created
    private val shown = ConcurrentHashMap<MarkerIdentity, M>()

    private val cameraUpdateJob: Job

    /** True after [close]. A closed layer ignores [show] and camera callbacks. */
    @Volatile
    var isClosed: Boolean = false
        private set

    init {
        overlay.bind(this)

        cameraUpdateJob = lifecycleOwner.lifecycleScope.launch(ioDispatcher) {
            for (camera in cameraUpdateChannel) {
                engine.recalculate(camera)
            }
        }
    }

    /**
     * Detaches every view, stops following the camera and releases the overlay.
     * Idempotent.
     */
    @MainThread
    fun close() {
        if (isClosed) return
        isClosed = true
        cameraUpdateJob.cancel()
        cameraUpdateChannel.close()
        overlay.unbind(this)
        for (identity in overlay.annotationKeys().toList()) {
            overlay.removeAnnotation(identity)?.let { overlay.removeView(it.view) }
        }
        shown.clear()
        engine.clear()
    }

    // ---- Showing ----

    /**
     * Shows [marker]. If its view is already attached, only the position is updated.
     *
     * View creation may be asynchronous; the view is not attached if [shouldAttach] returns
     * false or [hide] was called in the meantime.
     *
     * @param initialScale scale applied to the view when attached, or null
     */
    @MainThread
    fun show(marker: M, initialScale: Float? = null, shouldAttach: () -> Boolean = { true }) {
        if (isClosed) return
        val identity = marker.identity
        shown[identity] = marker

        if (overlay.containsAnnotation(identity)) {
            updatePosition(marker)
        } else {
            attach(marker, initialScale, shouldAttach)
        }
    }

    /** Hides [marker]. Safe to call from any thread; the view is removed on the main thread. */
    fun hide(marker: M) {
        val identity = marker.identity

        // Unregister synchronously so a pending show() cannot attach after this
        val annotation = overlay.removeAnnotation(identity)
        shown.remove(identity)
        engine.detach(identity)

        if (annotation != null) {
            lifecycleOwner.lifecycleScope.launch {
                overlay.removeView(annotation.view)
            }
        }
    }

    private fun attach(marker: M, initialScale: Float?, shouldAttach: () -> Boolean) {
        val descriptor = engine.descriptorFor(marker, googleMap.projection.asScreenProjection())

        overlay.render(
            marker = marker,
            descriptor = descriptor,
            scale = initialScale,
            isRenderable = { !isClosed && shouldAttach() && shown.containsKey(marker.identity) },
        )
    }

    /** Called by the overlay once a view is attached. */
    internal fun onMarkerRendered(event: MarkerRendered) {
        // render() is only called with this layer's M
        @Suppress("UNCHECKED_CAST")
        val marker = event.marker as M
        engine.attach(marker, event.descriptor)

        listener?.onMarkerAttached(marker, event.annotation.view, event.descriptor)

        if (edgeMode is EdgeMode.Clamp) {
            // Align once the view has been measured
            val annotation = event.annotation
            val descriptor = event.descriptor
            annotation.view.viewTreeObserver.addOnGlobalLayoutListener(
                object : ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        applyTranslation(annotation.view, descriptor)
                        annotation.alignable?.align(descriptor)
                        annotation.view.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    }
                }
            )
        }
    }

    /** Re-applies the layout size ([ViewMarker.sizeInDp]) and position of an attached view. */
    @MainThread
    fun relayout(marker: M) {
        val view = overlay.getAnnotation(marker.identity)?.view ?: return
        val size = marker.sizeInPx(density)
        val descriptor = engine.descriptorFor(marker, googleMap.projection.asScreenProjection())

        applyTranslation(view, descriptor)
        view.layoutParams = FrameLayout.LayoutParams(size, size)
    }

    // ---- Positions ----

    /** Applies a changed [ViewMarker.location] to the attached view, if any. */
    @MainThread
    fun updatePosition(marker: M) {
        val identity = marker.identity
        val annotation = overlay.getAnnotation(identity) ?: return
        val descriptor = engine.updateLocation(marker, googleMap.projection.asScreenProjection(), googleMap.cameraPosition.target)

        applyTranslation(annotation.view, descriptor)
    }

    /**
     * Call on every camera move. Recalculates positions and applies the latest result to the views.
     *
     * @param applyToViews when false, only recalculates
     */
    @MainThread
    fun onCameraMove(applyToViews: Boolean = true) {
        if (isClosed) return
        cameraUpdateChannel.trySend(googleMap.cameraSnapshot())

        if (applyToViews) {
            updateMarkersScreenPosition(engine.positions)
        }
    }

    /**
     * Call when the camera stops. Recalculates positions, applies them and resets the reference frame.
     *
     * @param onRecalculated called after recalculation and before positions are applied.
     *                       Return false to skip applying them.
     */
    @MainThread
    fun onCameraIdle(onRecalculated: () -> Boolean = { true }) {
        if (isClosed) return
        engine.recalculate(googleMap.cameraSnapshot())

        if (onRecalculated()) {
            updateMarkersScreenPosition(engine.positions)
        }

        // Start from a full calculation on the next camera move
        engine.resetReferenceFrame()
    }

    /**
     * Sets this layer as the map's camera move / idle listener.
     * If you need your own listeners, forward them to [onCameraMove] / [onCameraIdle] instead.
     */
    @MainThread
    fun attachCameraListeners() {
        googleMap.setOnCameraMoveListener { onCameraMove() }
        googleMap.setOnCameraIdleListener { onCameraIdle() }
    }

    // ---- Queries ----

    /** Markers whose view is attached. Safe to read from any thread. */
    val attachedMarkers: List<M> get() = engine.attachedMarkers

    /** Number of attached views. */
    val attachedCount: Int get() = overlay.annotations.size

    fun isAttached(identity: MarkerIdentity): Boolean = overlay.containsAnnotation(identity)

    /** The attached view of [identity], or null. */
    fun viewOf(identity: MarkerIdentity): View? = overlay.getAnnotation(identity)?.view

    /** The latest position of the attached marker [identity], or null. */
    fun descriptorOf(identity: MarkerIdentity): MarkerPositionDescriptor? = engine.descriptorOf(identity)

    /** Current screen positions of attached markers, keyed by [ViewMarker.childIds]. */
    fun currentScreenPoints(): Map<Long, ScreenPoint> {
        val screenPoints = mutableMapOf<Long, ScreenPoint>()
        engine.positions.forEach { descriptor ->
            descriptor.childIds.forEach {
                screenPoints[it] = ScreenPoint(descriptor.origin.x, descriptor.origin.y)
            }
        }
        return screenPoints
    }

    /** The overlay area extended by `visibleBoundsMarginDp`. */
    @MainThread
    fun visibleBounds(): LatLngBounds = engine.visibleBounds(googleMap.projection.asScreenProjection())

    /** Whether [marker] is inside [visibleBounds]. Always true with [EdgeMode.Clamp]. */
    @MainThread
    fun isInVisibleBounds(marker: M): Boolean = engine.isInVisibleBounds(marker, googleMap.projection.asScreenProjection())

    /** [isInVisibleBounds] with precomputed [bounds]. Safe to call from any thread. */
    fun isInVisibleBounds(marker: M, bounds: LatLngBounds): Boolean = engine.isInVisibleBounds(marker, bounds)

    // ---- Internal ----

    @MainThread
    private fun updateMarkersScreenPosition(descriptors: List<MarkerPositionDescriptor>) {
        Choreographer.getInstance().postFrameCallback {
            descriptors.forEach {
                updateMarkerScreenPosition(it)
            }
        }
    }

    private fun updateMarkerScreenPosition(descriptor: MarkerPositionDescriptor) {
        val annotation = overlay.getAnnotation(descriptor.identifier) ?: return

        applyTranslation(annotation.view, descriptor)

        if (edgeMode is EdgeMode.Clamp) {
            annotation.alignable?.align(descriptor)
        }

        listener?.onPositionApplied(engine.attachedMarker(descriptor.identifier), annotation.view, descriptor)
    }

    private fun applyTranslation(view: View, descriptor: MarkerPositionDescriptor) {
        view.translationX = descriptor.screenPosition.x.toFloat()
        view.translationY = descriptor.screenPosition.y.toFloat()
    }

    companion object {
        /** Default `visibleBoundsMarginDp`. */
        const val DEFAULT_VISIBLE_BOUNDS_MARGIN_DP = MarkerPositionEngine.DEFAULT_VISIBLE_BOUNDS_MARGIN_DP
    }
}
