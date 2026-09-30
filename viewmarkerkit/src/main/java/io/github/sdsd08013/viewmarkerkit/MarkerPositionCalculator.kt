package io.github.sdsd08013.viewmarkerkit

import android.widget.FrameLayout
import androidx.annotation.WorkerThread
import com.google.android.gms.maps.Projection
import com.google.android.gms.maps.model.LatLng

/**
 * Computes screen positions of attached markers.
 *
 * While the camera only pans, positions are derived from a reference frame by applying the
 * camera's screen-space delta (cheap, no accumulated error). On zoom or bearing changes, and
 * whenever the reference frame is missing, every marker is projected again.
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

    private var baseCenterLatLng: LatLng? = null
    private var baseDescriptors: Map<MarkerIdentity, BaseMarkerInfo>? = null
    private var previousZoom: Float? = null
    private var previousBearing: Float? = null

    private fun canUseDeltaCalculation(currentZoom: Float, currentBearing: Float): Boolean {
        baseCenterLatLng ?: return false
        baseDescriptors ?: return false
        val prevZoom = previousZoom ?: return false
        val prevBearing = previousBearing ?: return false
        return currentZoom == prevZoom && currentBearing == prevBearing
    }

    @WorkerThread
    fun calculate(
        cameraState: MarkerCameraState,
        currentDescriptors: List<MarkerPositionDescriptor>,
        viewAnnotationMap: Map<MarkerIdentity, ViewAnnotation>,
        markersPool: Map<MarkerIdentity, ViewMarker>,
    ): List<MarkerPositionDescriptor> {
        val result = if (canUseDeltaCalculation(cameraState.zoom, cameraState.bearing)) {
            calculateWithDelta(cameraState, currentDescriptors)
        } else {
            calculateFull(cameraState, viewAnnotationMap, markersPool)
        }

        previousZoom = cameraState.zoom
        previousBearing = cameraState.bearing

        return result
    }

    @WorkerThread
    private fun calculateWithDelta(
        cameraState: MarkerCameraState,
        currentDescriptors: List<MarkerPositionDescriptor>,
    ): List<MarkerPositionDescriptor> {
        val baseCenter = baseCenterLatLng ?: return emptyList()
        val baseMap = baseDescriptors ?: return emptyList()
        val projection = cameraState.projection

        val baseCenterScreen = projection.toScreenLocation(baseCenter)
        val currentCenterScreen = projection.toScreenLocation(cameraState.center)
        val deltaX = currentCenterScreen.x - baseCenterScreen.x
        val deltaY = currentCenterScreen.y - baseCenterScreen.y

        return currentDescriptors.map { descriptor ->
            applyDeltaToDescriptor(descriptor, deltaX, deltaY, baseMap)
        }
    }

    private fun applyDeltaToDescriptor(
        descriptor: MarkerPositionDescriptor,
        deltaX: Int,
        deltaY: Int,
        baseMap: Map<MarkerIdentity, BaseMarkerInfo>,
    ): MarkerPositionDescriptor {
        val baseInfo = baseMap[descriptor.identifier] ?: return descriptor

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

    @WorkerThread
    private fun calculateFull(
        cameraState: MarkerCameraState,
        viewAnnotationMap: Map<MarkerIdentity, ViewAnnotation>,
        markersPool: Map<MarkerIdentity, ViewMarker>,
    ): List<MarkerPositionDescriptor> {
        val projection = cameraState.projection
        val newBaseDescriptors = mutableMapOf<MarkerIdentity, BaseMarkerInfo>()

        val result = viewAnnotationMap.mapNotNull { (id, _) ->
            val marker = markersPool[id] ?: return@mapNotNull null
            val point = projection.toScreenLocation(marker.location)
            val screenPoint = ScreenPoint(point.x, point.y)
            val positionResult = calculatePositionResult(screenPoint)

            newBaseDescriptors[id] = BaseMarkerInfo(
                origin = screenPoint,
                offsetX = marker.offsetX(density),
                offsetY = marker.offsetY(density)
            )

            createDescriptor(id, marker, positionResult)
        }

        baseCenterLatLng = cameraState.center
        baseDescriptors = newBaseDescriptors

        return result
    }

    /** Drops the reference frame so that the next calculation projects every marker again. */
    fun resetReferencePoint() {
        baseCenterLatLng = null
        baseDescriptors = null
    }

    /**
     * Updates the reference frame for one marker whose location changed while the camera
     * is moving, so delta calculation keeps producing correct positions for it.
     */
    @Synchronized
    fun updateMarkerBase(
        markerId: MarkerIdentity,
        currentScreenPoint: ScreenPoint,
        marker: ViewMarker,
        projection: Projection,
        currentCenter: LatLng
    ) {
        val currentBase = baseDescriptors?.toMutableMap() ?: return
        val baseCenter = baseCenterLatLng ?: return

        // Translate the current screen point back into the reference frame
        val baseCenterScreen = projection.toScreenLocation(baseCenter)
        val currentCenterScreen = projection.toScreenLocation(currentCenter)
        val deltaX = currentCenterScreen.x - baseCenterScreen.x
        val deltaY = currentCenterScreen.y - baseCenterScreen.y

        val baseScreenPoint = ScreenPoint(
            currentScreenPoint.x + deltaX,
            currentScreenPoint.y + deltaY
        )

        currentBase[markerId] = BaseMarkerInfo(
            origin = baseScreenPoint,
            offsetX = marker.offsetX(density),
            offsetY = marker.offsetY(density)
        )
        baseDescriptors = currentBase
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
