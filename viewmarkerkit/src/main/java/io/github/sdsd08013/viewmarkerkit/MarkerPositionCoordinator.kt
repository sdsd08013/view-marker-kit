package io.github.sdsd08013.viewmarkerkit

import android.widget.FrameLayout
import androidx.annotation.WorkerThread
import com.google.android.gms.maps.Projection
import com.google.android.gms.maps.model.LatLng

/** Keeps the position calculator and the stored positions in sync. */
internal class MarkerPositionCoordinator(
    density: Float,
    mapOverlay: FrameLayout,
    edgeMode: EdgeMode,
) {
    private val calculator = MarkerPositionCalculator(density, mapOverlay, edgeMode)
    private val positions = ThreadSafePositionsList()

    /** Recomputes all positions for [cameraState] and stores them. */
    @WorkerThread
    suspend fun updateAllPositions(
        cameraState: MarkerCameraState,
        viewAnnotationMap: Map<MarkerIdentity, ViewAnnotation>,
        markersPool: Map<MarkerIdentity, ViewMarker>,
    ): List<MarkerPositionDescriptor> {
        val descriptors = calculator.calculate(
            cameraState = cameraState,
            currentDescriptors = positions.getSnapshot(),
            viewAnnotationMap = viewAnnotationMap,
            markersPool = markersPool
        )
        positions.replaceAll(descriptors)
        return descriptors
    }

    /** Stores a new position for one marker whose location changed. */
    suspend fun updateSingleMarkerPosition(
        markerId: MarkerIdentity,
        marker: ViewMarker,
        screenPoint: ScreenPoint,
        projection: Projection,
        currentCenter: LatLng,
        descriptor: MarkerPositionDescriptor
    ) {
        calculator.updateMarkerBase(markerId, screenPoint, marker, projection, currentCenter)
        positions.updateOrAdd(descriptor)
    }

    /** Registers a newly attached marker. The next calculation projects every marker again. */
    suspend fun addPosition(descriptor: MarkerPositionDescriptor) {
        positions.add(descriptor)
        calculator.resetReferencePoint()
    }

    /** Latest positions without taking the lock, for per-frame reads. */
    fun getSnapshot(): List<MarkerPositionDescriptor> = positions.getSnapshot()

    suspend fun toList(): List<MarkerPositionDescriptor> = positions.toList()

    suspend fun find(predicate: (MarkerPositionDescriptor) -> Boolean): MarkerPositionDescriptor? {
        return positions.find(predicate)
    }

    suspend fun forEach(action: (MarkerPositionDescriptor) -> Unit) {
        positions.forEach(action)
    }

    fun resetReferencePoint() {
        calculator.resetReferencePoint()
    }
}
