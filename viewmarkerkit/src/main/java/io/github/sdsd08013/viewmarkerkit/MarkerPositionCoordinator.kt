package io.github.sdsd08013.viewmarkerkit

import com.google.android.gms.maps.model.LatLng

/** Keeps the position calculator and the stored positions in sync. */
internal class MarkerPositionCoordinator(
    density: Float,
    viewport: () -> Viewport,
    edgeMode: EdgeMode,
) {
    private val calculator = MarkerPositionCalculator(density, viewport, edgeMode)
    private val positions = PositionStore()

    /** Recomputes all positions for [cameraState] and stores them. May be called from any thread. */
    fun updateAllPositions(
        cameraState: MarkerCameraState,
        attached: Set<MarkerIdentity>,
        markersPool: Map<MarkerIdentity, ViewMarker>,
    ): List<MarkerPositionDescriptor> {
        val descriptors = calculator.calculate(
            cameraState = cameraState,
            currentDescriptors = positions.snapshot,
            attached = attached,
            markersPool = markersPool
        )
        positions.replaceAll(descriptors)
        return descriptors
    }

    /** Stores a new position for one marker whose location changed. */
    fun updateSingleMarkerPosition(
        markerId: MarkerIdentity,
        marker: ViewMarker,
        screenPoint: ScreenPoint,
        projection: ScreenProjection,
        currentCenter: LatLng,
        descriptor: MarkerPositionDescriptor
    ) {
        calculator.updateMarkerBase(markerId, screenPoint, marker, projection, currentCenter)
        positions.updateOrAdd(descriptor)
    }

    /** Registers a newly attached marker. The next calculation projects every marker again. */
    fun addPosition(descriptor: MarkerPositionDescriptor) {
        positions.add(descriptor)
        calculator.resetReferenceFrame()
    }

    /** Latest positions without taking a lock, for per-frame reads. */
    val snapshot: List<MarkerPositionDescriptor> get() = positions.snapshot

    fun find(identity: MarkerIdentity): MarkerPositionDescriptor? = positions.find(identity)

    fun resetReferenceFrame() {
        calculator.resetReferenceFrame()
    }

    fun clear() {
        positions.clear()
        calculator.resetReferenceFrame()
    }
}
