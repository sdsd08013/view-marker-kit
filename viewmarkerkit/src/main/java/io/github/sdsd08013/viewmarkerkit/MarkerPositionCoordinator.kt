package io.github.sdsd08013.viewmarkerkit

import android.widget.FrameLayout
import androidx.annotation.WorkerThread
import com.google.android.gms.maps.Projection
import com.google.android.gms.maps.model.LatLng
import io.github.sdsd08013.viewmarkerkit.ScreenPoint

/**
 * マーカー座標の計算と管理を統合するファサード
 *
 * 責務:
 * - 座標計算の実行（MarkerPositionCalculatorに委譲）
 * - 座標データの保管（ThreadSafePositionsListに委譲）
 * - 2つのコンポーネントの同期を保証
 */
internal class MarkerPositionCoordinator(
    density: Float,
    mapOverlay: FrameLayout,
    edgeMode: EdgeMode,
) {
    private val calculator = MarkerPositionCalculator(density, mapOverlay, edgeMode)
    private val positions = ThreadSafePositionsList()

    /**
     * カメラ移動時の座標一括更新
     * calculate + replaceAllを一括で行う
     */
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

    /**
     * 単一マーカーの座標更新（moveMarkerSmoothly等で使用）
     * updateMarkerBase + updateOrAddを一括で行う
     */
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

    /**
     * 新しいマーカー追加時
     * 基準点をリセットし、次のonCameraMoveでフル計算が行われるようにする
     */
    suspend fun addPosition(descriptor: MarkerPositionDescriptor) {
        positions.add(descriptor)
        calculator.resetReferencePoint()
    }

    /**
     * ロックなしでスナップショット取得（毎フレーム呼び出し用）
     */
    fun getSnapshot(): List<MarkerPositionDescriptor> = positions.getSnapshot()

    /**
     * ロック付きでリスト取得
     */
    suspend fun toList(): List<MarkerPositionDescriptor> = positions.toList()

    /**
     * 特定のdescriptorを検索
     */
    suspend fun find(predicate: (MarkerPositionDescriptor) -> Boolean): MarkerPositionDescriptor? {
        return positions.find(predicate)
    }

    /**
     * 各descriptorに対して処理を実行
     */
    suspend fun forEach(action: (MarkerPositionDescriptor) -> Unit) {
        positions.forEach(action)
    }

    /**
     * カメラ停止時に基準点をリセット
     */
    fun resetReferencePoint() {
        calculator.resetReferencePoint()
    }
}
