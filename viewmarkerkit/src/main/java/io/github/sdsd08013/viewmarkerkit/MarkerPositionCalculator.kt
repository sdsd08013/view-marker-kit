package io.github.sdsd08013.viewmarkerkit

import android.widget.FrameLayout
import com.google.android.gms.maps.Projection
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
    private val mapOverlay: FrameLayout,
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
        cameraState: MarkerCameraState,
        currentDescriptors: List<MarkerPositionDescriptor>,
        viewAnnotationMap: Map<MarkerIdentity, ViewAnnotation>,
        markersPool: Map<MarkerIdentity, ViewMarker>,
    ): List<MarkerPositionDescriptor> {
        val frame = reference
        return if (frame != null && frame.zoom == cameraState.zoom && frame.bearing == cameraState.bearing) {
            calculateWithDelta(frame, cameraState, currentDescriptors)
        } else {
            calculateFull(cameraState, viewAnnotationMap, markersPool)
        }
    }

    private fun calculateWithDelta(
        frame: ReferenceFrame,
        cameraState: MarkerCameraState,
        currentDescriptors: List<MarkerPositionDescriptor>,
    ): List<MarkerPositionDescriptor> {
        val projection = cameraState.projection

        val baseCenterScreen = projection.toScreenLocation(frame.center)
        val currentCenterScreen = projection.toScreenLocation(cameraState.center)
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
        cameraState: MarkerCameraState,
        viewAnnotationMap: Map<MarkerIdentity, ViewAnnotation>,
        markersPool: Map<MarkerIdentity, ViewMarker>,
    ): List<MarkerPositionDescriptor> {
        val projection = cameraState.projection
        val bases = mutableMapOf<MarkerIdentity, BaseMarkerInfo>()

        val result = viewAnnotationMap.mapNotNull { (id, _) ->
            val marker = markersPool[id] ?: return@mapNotNull null
            val point = projection.toScreenLocation(marker.location)
            val screenPoint = ScreenPoint(point.x, point.y)
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
        projection: Projection,
        currentCenter: LatLng
    ) {
        val frame = reference ?: return

        // Translate the current screen point back into the reference frame
        val baseCenterScreen = projection.toScreenLocation(frame.center)
        val currentCenterScreen = projection.toScreenLocation(currentCenter)
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
        return edgeMode.resolve(
            screenPoint,
            mapOverlay.width,
            mapOverlay.height
        )
    }

    private fun getEdgePosition(point: ScreenPoint): MarkerEdge {
        return edgeMode.edgeAt(
            point,
            mapOverlay.width,
            mapOverlay.height
        )
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
