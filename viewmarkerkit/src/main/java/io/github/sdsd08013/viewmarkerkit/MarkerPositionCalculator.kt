package io.github.sdsd08013.viewmarkerkit

import com.google.android.gms.maps.model.LatLng

/**
 * Computes screen positions of attached markers.
 *
 * While the camera only pans, positions are derived from a reference frame by applying the
 * camera's screen-space delta (cheap, no accumulated error). On zoom or bearing changes, and
 * whenever the reference frame is missing, every marker is projected again.
 *
 * The reference frame is an immutable value replaced atomically, so [calculate] may run on a
 * background thread while [updateMarkerBase] / [resetReferenceFrame] are called from the main thread.
 */
internal class MarkerPositionCalculator(
    private val density: Float,
    private val viewport: () -> Viewport,
    private val edgeMode: EdgeMode,
) {
    private data class BaseMarkerInfo(
        val origin: ScreenPoint,
        val offsetX: Int,
        val offsetY: Int
    )

    private data class ReferenceFrame(
        val center: LatLng,
        val zoom: Float,
        val bearing: Float,
        val bases: Map<MarkerIdentity, BaseMarkerInfo>,
    )

    @Volatile
    private var reference: ReferenceFrame? = null

    fun calculate(
        cameraState: CameraSnapshot,
        currentDescriptors: List<MarkerPositionDescriptor>,
        attached: Set<MarkerIdentity>,
        markersPool: Map<MarkerIdentity, ViewMarker>,
    ): List<MarkerPositionDescriptor> {
        val frame = reference
        return if (frame != null && frame.zoom == cameraState.zoom && frame.bearing == cameraState.bearing) {
            calculateWithDelta(frame, cameraState, currentDescriptors)
        } else {
            calculateFull(cameraState, attached, markersPool)
        }
    }

    private fun calculateWithDelta(
        frame: ReferenceFrame,
        cameraState: CameraSnapshot,
        currentDescriptors: List<MarkerPositionDescriptor>,
    ): List<MarkerPositionDescriptor> {
        val projection = cameraState.projection

        val baseCenterScreen = projection.toScreen(frame.center)
        val currentCenterScreen = projection.toScreen(cameraState.center)
        val deltaX = currentCenterScreen.x - baseCenterScreen.x
        val deltaY = currentCenterScreen.y - baseCenterScreen.y

        return currentDescriptors.map { descriptor ->
            applyDeltaToDescriptor(descriptor, deltaX, deltaY, frame.bases)
        }
    }

    private fun applyDeltaToDescriptor(
        descriptor: MarkerPositionDescriptor,
        deltaX: Int,
        deltaY: Int,
        bases: Map<MarkerIdentity, BaseMarkerInfo>,
    ): MarkerPositionDescriptor {
        val baseInfo = bases[descriptor.identifier] ?: return descriptor

        val newOrigin = ScreenPoint(
            baseInfo.origin.x - deltaX,
            baseInfo.origin.y - deltaY
        )
        val positionResult = calculatePositionResult(newOrigin)

        return descriptor.copy(
            origin = newOrigin,
            screenPosition = ScreenPoint(
                positionResult.adjustedPoint.x - baseInfo.offsetX,
                positionResult.adjustedPoint.y - baseInfo.offsetY
            ),
            rotation = positionResult.angleInDegrees,
            currentEdge = getEdgePosition(positionResult.adjustedPoint),
            previousEdge = descriptor.currentEdge
        )
    }

    private fun calculateFull(
        cameraState: CameraSnapshot,
        attached: Set<MarkerIdentity>,
        markersPool: Map<MarkerIdentity, ViewMarker>,
    ): List<MarkerPositionDescriptor> {
        val projection = cameraState.projection
        val bases = mutableMapOf<MarkerIdentity, BaseMarkerInfo>()

        val result = attached.mapNotNull { id ->
            val marker = markersPool[id] ?: return@mapNotNull null
            val screenPoint = projection.toScreen(marker.location)
            val positionResult = calculatePositionResult(screenPoint)

            bases[id] = BaseMarkerInfo(
                origin = screenPoint,
                offsetX = marker.offsetX(density),
                offsetY = marker.offsetY(density)
            )

            createDescriptor(id, marker, positionResult)
        }

        reference = ReferenceFrame(
            center = cameraState.center,
            zoom = cameraState.zoom,
            bearing = cameraState.bearing,
            bases = bases,
        )

        return result
    }

    /** Drops the reference frame so that the next calculation projects every marker again. */
    fun resetReferenceFrame() {
        reference = null
    }

    /**
     * Updates the reference frame for one marker whose location changed while the camera
     * is moving, so delta calculation keeps producing correct positions for it.
     */
    fun updateMarkerBase(
        markerId: MarkerIdentity,
        currentScreenPoint: ScreenPoint,
        marker: ViewMarker,
        projection: ScreenProjection,
        currentCenter: LatLng
    ) {
        val frame = reference ?: return

        // Translate the current screen point back into the reference frame
        val baseCenterScreen = projection.toScreen(frame.center)
        val currentCenterScreen = projection.toScreen(currentCenter)
        val deltaX = currentCenterScreen.x - baseCenterScreen.x
        val deltaY = currentCenterScreen.y - baseCenterScreen.y

        val baseInfo = BaseMarkerInfo(
            origin = ScreenPoint(currentScreenPoint.x + deltaX, currentScreenPoint.y + deltaY),
            offsetX = marker.offsetX(density),
            offsetY = marker.offsetY(density)
        )
        reference = frame.copy(bases = frame.bases + (markerId to baseInfo))
    }

    private fun calculatePositionResult(screenPoint: ScreenPoint): MarkerPositionResult {
        val (width, height) = viewport()
        return edgeMode.resolve(screenPoint, width, height)
    }

    private fun getEdgePosition(point: ScreenPoint): MarkerEdge {
        val (width, height) = viewport()
        return edgeMode.edgeAt(point, width, height)
    }

    private fun createDescriptor(
        id: MarkerIdentity,
        marker: ViewMarker,
        result: MarkerPositionResult,
    ): MarkerPositionDescriptor {
        return MarkerPositionDescriptor(
            identifier = id,
            childIds = marker.childIds,
            origin = ScreenPoint(result.adjustedPoint.x, result.adjustedPoint.y),
            screenPosition = ScreenPoint(
                result.adjustedPoint.x - marker.offsetX(density),
                result.adjustedPoint.y - marker.offsetY(density)
            ),
            rotation = result.angleInDegrees,
            currentEdge = getEdgePosition(result.adjustedPoint),
            previousEdge = MarkerEdge.NONE
        )
    }
}
