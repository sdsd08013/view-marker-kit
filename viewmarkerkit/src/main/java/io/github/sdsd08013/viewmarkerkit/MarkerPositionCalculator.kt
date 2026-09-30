package io.github.sdsd08013.viewmarkerkit

import android.widget.FrameLayout
import androidx.annotation.WorkerThread
import com.google.android.gms.maps.Projection
import com.google.android.gms.maps.model.LatLng
import io.github.sdsd08013.viewmarkerkit.MarkerEdge
import io.github.sdsd08013.viewmarkerkit.ScreenPoint

/**
 * マーカーのスクリーン座標を計算するクラス
 *
 * 2つの計算方式をサポート:
 * - デルタ計算: スクロールのみの場合、移動量から座標を算出（高速）
 * - フル計算: ズーム変更時や初回、LatLng→スクリーン座標を全て再計算
 */
internal class MarkerPositionCalculator(
    private val density: Float,
    private val mapOverlay: FrameLayout,
    private val edgeMode: EdgeMode,
) {
    /**
     * 基準点のマーカー情報
     */
    private data class BaseMarkerInfo(
        val origin: ScreenPoint,
        val offsetX: Int,
        val offsetY: Int
    )

    // 基準点方式: 累積誤差を防ぐため、基準点からの差分で計算
    private var baseCenterLatLng: LatLng? = null
    private var baseDescriptors: Map<MarkerIdentity, BaseMarkerInfo>? = null
    private var previousZoom: Float? = null
    private var previousBearing: Float? = null

    /**
     * デルタ計算が可能かどうかを判定
     *
     * 条件:
     * - 基準点が存在する
     * - ズームレベルが変更されていない
     * - 回転角度（bearing）が変更されていない
     */
    private fun canUseDeltaCalculation(currentZoom: Float, currentBearing: Float): Boolean {
        baseCenterLatLng ?: return false
        baseDescriptors ?: return false
        val prevZoom = previousZoom ?: return false
        val prevBearing = previousBearing ?: return false
        return currentZoom == prevZoom && currentBearing == prevBearing
    }

    /**
     * マーカー座標を計算
     *
     * @param cameraState 現在のカメラ状態
     * @param currentDescriptors 現在の座標リスト（デルタ計算時に使用）
     * @param viewAnnotationMap レンダリング済みのマーカーView
     * @return 更新された座標リスト
     */
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

        // ズームレベルと回転角度を更新
        previousZoom = cameraState.zoom
        previousBearing = cameraState.bearing

        return result
    }

    /**
     * デルタ計算: 基準点からの移動量で座標を算出（高速・累積誤差なし）
     *
     * 仕組み:
     * 1. 基準カメラ中心と現在のカメラ中心のスクリーン座標差分を計算
     * 2. 各マーカーの基準originに差分を適用
     *
     * 累積ではなく常に基準点からの差分を計算するため、誤差が蓄積しない
     */
    @WorkerThread
    private fun calculateWithDelta(
        cameraState: MarkerCameraState,
        currentDescriptors: List<MarkerPositionDescriptor>,
    ): List<MarkerPositionDescriptor> {
        val baseCenter = baseCenterLatLng ?: return emptyList()
        val baseMap = baseDescriptors ?: return emptyList()
        val projection = cameraState.projection

        // 基準点からの移動量を計算（累積ではない）
        val baseCenterScreen = projection.toScreenLocation(baseCenter)
        val currentCenterScreen = projection.toScreenLocation(cameraState.center)
        val deltaX = currentCenterScreen.x - baseCenterScreen.x
        val deltaY = currentCenterScreen.y - baseCenterScreen.y

        return currentDescriptors.map { descriptor ->
            applyDeltaToDescriptor(descriptor, deltaX, deltaY, baseMap)
        }
    }

    /**
     * 単一のDescriptorにデルタを適用（基準originを使用）
     */
    private fun applyDeltaToDescriptor(
        descriptor: MarkerPositionDescriptor,
        deltaX: Int,
        deltaY: Int,
        baseMap: Map<MarkerIdentity, BaseMarkerInfo>,
    ): MarkerPositionDescriptor {
        // 基準情報を取得（なければスキップ）
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

    /**
     * フル計算: 全マーカーのLatLng→スクリーン座標を再計算
     *
     * 以下の場合に使用:
     * - ズームレベルが変更された
     * - 初回計算（前回の座標がない）
     *
     * フル計算後、基準点を設定してデルタ計算を有効化する
     */
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

            // 基準点情報を保存
            newBaseDescriptors[id] = BaseMarkerInfo(
                origin = screenPoint,
                offsetX = marker.offsetX(density),
                offsetY = marker.offsetY(density)
            )

            createDescriptor(id, marker, positionResult)
        }

        // 基準点を設定
        baseCenterLatLng = cameraState.center
        baseDescriptors = newBaseDescriptors

        return result
    }

    /**
     * カメラ停止時に基準点をリセット
     *
     * onCameraIdle後に呼び出すことで、次のonCameraMoveで
     * フル計算から開始される
     */
    fun resetReferencePoint() {
        baseCenterLatLng = null
        baseDescriptors = null
    }

    /**
     * 特定のマーカーの基準座標を更新
     *
     * moveMarkerSmoothly等でマーカー位置を動的に更新する際に使用
     * これにより、デルタ計算でも正しい座標が計算される
     *
     * @param markerId 更新対象のマーカーID
     * @param currentScreenPoint 現在のカメラ位置でのスクリーン座標
     * @param marker オフセット計算用のマーカー
     * @param projection 座標変換用のProjection
     * @param currentCenter 現在のカメラ中心座標
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

        // 現在のカメラ位置からbaseCenterLatLng時点への変換
        // デルタ計算と整合性を保つため、基準点時点での座標として保存
        val baseCenterScreen = projection.toScreenLocation(baseCenter)
        val currentCenterScreen = projection.toScreenLocation(currentCenter)
        val deltaX = currentCenterScreen.x - baseCenterScreen.x
        val deltaY = currentCenterScreen.y - baseCenterScreen.y

        // baseCenterLatLng時点での座標に変換
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

    /**
     * スクリーン座標から位置情報を計算
     */
    private fun calculatePositionResult(screenPoint: ScreenPoint): MarkerPositionResult {
        return edgeMode.resolve(
            screenPoint,
            mapOverlay.width,
            mapOverlay.height
        )
    }

    /**
     * エッジ位置を判定
     */
    private fun getEdgePosition(point: ScreenPoint): MarkerEdge {
        return edgeMode.edgeAt(
            point,
            mapOverlay.width,
            mapOverlay.height
        )
    }

    /**
     * MarkerPositionDescriptorを生成
     */
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
