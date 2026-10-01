package io.github.sdsd08013.viewmarkerkit

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import java.util.concurrent.ConcurrentHashMap

/**
 * Computes where marker views go on screen and keeps the result in sync with the camera.
 *
 * The engine knows nothing about views: it tracks which markers are attached, projects their
 * locations into pixels and exposes the result as [positions]. [ViewMarkerLayer] drives it for
 * Android views; a Compose host can drive it the same way.
 *
 * Call [attach] / [detach] as markers come and go, [recalculate] on every camera move and when
 * the camera stops (followed by [resetReferenceFrame]), and [updateLocation] when a single
 * marker moves. [recalculate] may run on a background thread; everything else is expected on
 * the main thread.
 *
 * @param density screen density, used to convert [ViewMarker.sizeInDp] and offsets to pixels
 * @param viewport size of the area markers are placed in; read on every calculation
 * @param visibleBoundsMarginDp how far [visibleBounds] extends beyond the viewport; use at least the largest marker size
 */
class MarkerPositionEngine<M : ViewMarker>(
    private val density: Float,
    private val viewport: () -> Viewport,
    val edgeMode: EdgeMode = EdgeMode.None,
    visibleBoundsMarginDp: Int = DEFAULT_VISIBLE_BOUNDS_MARGIN_DP,
) {
    private val coordinator = MarkerPositionCoordinator(density, viewport, edgeMode)

    private val boundary = Boundary(marginDp = visibleBoundsMarginDp) {
        val (width, height) = viewport()
        ScreenMetrics(density, width, height)
    }

    private val attached = ConcurrentHashMap<MarkerIdentity, M>()

    // ---- Attaching ----

    /** Computes where [marker] goes for the current camera, without attaching it. */
    fun descriptorFor(marker: M, projection: ScreenProjection): MarkerPositionDescriptor =
        descriptorFor(marker, projection.toScreen(marker.location))

    /** Registers [marker] as attached at [descriptor] (usually from [descriptorFor]). */
    fun attach(marker: M, descriptor: MarkerPositionDescriptor) {
        attached[marker.identity] = marker
        coordinator.addPosition(descriptor)
    }

    fun detach(identity: MarkerIdentity) {
        attached.remove(identity)
    }

    fun isAttached(identity: MarkerIdentity): Boolean = attached.containsKey(identity)

    fun attachedMarker(identity: MarkerIdentity): M? = attached[identity]

    /** Attached markers. Safe to read from any thread. */
    val attachedMarkers: List<M> get() = attached.values.toList()

    /** Removes every marker and all positions. */
    fun clear() {
        attached.clear()
        coordinator.clear()
    }

    // ---- Positions ----

    /** Latest positions of attached markers. Safe to read from any thread. */
    val positions: List<MarkerPositionDescriptor> get() = coordinator.snapshot

    fun descriptorOf(identity: MarkerIdentity): MarkerPositionDescriptor? = coordinator.find(identity)

    /**
     * Recomputes the positions of all attached markers for [camera] and returns them.
     * May be called from a background thread.
     */
    fun recalculate(camera: CameraSnapshot): List<MarkerPositionDescriptor> =
        coordinator.updateAllPositions(camera, attached.keys, attached)

    /**
     * Drops the pan reference so the next [recalculate] projects every marker again.
     * Call it when the camera stops.
     */
    fun resetReferenceFrame() {
        coordinator.resetReferenceFrame()
    }

    /**
     * Applies a changed [ViewMarker.location] of an attached [marker] and returns its new position.
     * [cameraCenter] is the current camera target.
     */
    fun updateLocation(marker: M, projection: ScreenProjection, cameraCenter: LatLng): MarkerPositionDescriptor {
        val screenPoint = projection.toScreen(marker.location)
        val descriptor = descriptorFor(marker, screenPoint)
        coordinator.updateSingleMarkerPosition(marker.identity, marker, screenPoint, projection, cameraCenter, descriptor)
        return descriptor
    }

    // ---- Bounds ----

    /** The viewport extended by `visibleBoundsMarginDp`. */
    fun visibleBounds(projection: ScreenProjection): LatLngBounds = boundary.boundsWithMargin(projection)

    /** Whether [marker] is inside [visibleBounds]. Always true with [EdgeMode.Clamp]. */
    fun isInVisibleBounds(marker: M, projection: ScreenProjection): Boolean = when (edgeMode) {
        is EdgeMode.Clamp -> true
        EdgeMode.None -> boundary.isInVisibleBounds(marker, projection)
    }

    /** [isInVisibleBounds] with precomputed [bounds]. Safe to call from any thread. */
    fun isInVisibleBounds(marker: M, bounds: LatLngBounds): Boolean = when (edgeMode) {
        is EdgeMode.Clamp -> true
        EdgeMode.None -> boundary.isInVisibleBounds(marker, bounds)
    }

    // ---- Internal ----

    private fun descriptorFor(marker: ViewMarker, screenPoint: ScreenPoint): MarkerPositionDescriptor {
        val identity = marker.identity
        val (width, height) = viewport()
        val result = edgeMode.resolve(screenPoint, width, height)
        return MarkerPositionDescriptor(
            identifier = identity,
            childIds = marker.childIds,
            origin = ScreenPoint(result.adjustedPoint.x, result.adjustedPoint.y),
            screenPosition = ScreenPoint(result.adjustedPoint.x - marker.offsetX(density), result.adjustedPoint.y - marker.offsetY(density)),
            rotation = result.angleInDegrees,
            currentEdge = edgeMode.edgeAt(result.adjustedPoint, width, height),
            previousEdge = coordinator.find(identity)?.currentEdge ?: MarkerEdge.NONE
        )
    }

    companion object {
        /** Default `visibleBoundsMarginDp`. */
        const val DEFAULT_VISIBLE_BOUNDS_MARGIN_DP = 240
    }
}
