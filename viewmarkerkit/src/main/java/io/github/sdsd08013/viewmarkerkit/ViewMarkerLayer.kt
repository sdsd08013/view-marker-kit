package io.github.sdsd08013.viewmarkerkit

import android.animation.ValueAnimator
import android.app.Activity
import android.view.Choreographer
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import androidx.core.animation.doOnEnd
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.sign

/**
 * GoogleMap の上に Android View をマーカーとして重ね、カメラに同期させる描画層。
 *
 * 持つのは「View を載せる / 外す」「スクリーン座標をカメラに追従させる」「位置を滑らかに動かす」だけで、
 * どのマーカーを載せるか（クラスタリング・間引き・focus など）は利用側が決めて [show] / [hide] を呼ぶ。
 *
 * カメラの `OnCameraMoveListener` / `OnCameraIdleListener` から [onCameraMove] / [onCameraIdle] を
 * 呼ぶこと（自前のリスナーが無ければ [attachCameraListeners] で足りる）。特記のない操作は main thread から行う。
 *
 * @param lifecycleOwner 内部の coroutine の寿命。View を持つ画面なら view の lifecycle を渡す
 * @param overlay マーカー View の載せ先。GoogleMap と同じ領域に重ねて配置し、
 *                [MarkerOverlayView.viewFactory] を設定しておく
 * @param visibleBoundsMarginDp [visibleBounds] が画面の外側へ広げる余白。
 *                              画面端で View が見切れて消えないよう、最大マーカーサイズ以上を渡す
 */
class ViewMarkerLayer<M : ViewMarker>(
    private val activity: Activity,
    private val lifecycleOwner: LifecycleOwner,
    private val googleMap: GoogleMap,
    private val overlay: MarkerOverlayView,
    private val edgeMode: EdgeMode = EdgeMode.None,
    visibleBoundsMarginDp: Int = DEFAULT_VISIBLE_BOUNDS_MARGIN_DP,
    private val listener: Listener<M>? = null,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    interface Listener<M : ViewMarker> {
        /** [marker] の View が overlay に載り、位置が登録された直後に呼ばれる。 */
        fun onMarkerAttached(marker: M, view: View, descriptor: MarkerPositionDescriptor) {}

        /**
         * カメラ追従で View の位置を反映した直後に、載っている View ごとに呼ばれる（カメラ移動中は毎フレーム）。
         * [marker] は [show] された登録が既に無ければ null。
         */
        fun onPositionApplied(marker: M?, view: View, descriptor: MarkerPositionDescriptor) {}
    }

    private val density: Float = activity.resources.displayMetrics.density

    private val boundary = Boundary(marginDp = visibleBoundsMarginDp) {
        activity.resources.displayMetrics.let {
            ScreenMetrics(it.density, it.widthPixels, it.heightPixels)
        }
    }

    // Conflated Channel for projection updates (drops old values, keeps only latest)
    private val cameraUpdateChannel = Channel<MarkerCameraState>(Channel.CONFLATED)

    // 座標計算と座標データを統合管理
    private val positionCoordinator = MarkerPositionCoordinator(density, overlay, edgeMode)

    // show されたマーカー（inflate 待ちを含む）
    private val markersPool = ConcurrentHashMap<MarkerIdentity, M>()

    init {
        // 描画完了イベントに反応して View 依存の後処理を行う（inflate の完了タイミングは overlay が所有）
        overlay.onMarkerRendered = ::handleMarkerRendered

        // Single coroutine consuming projection updates on IO dispatcher
        lifecycleOwner.lifecycleScope.launch(ioDispatcher) {
            for (request in cameraUpdateChannel) {
                updatePositionDescriptors(request)
            }
        }
    }

    // ---- 表示 ----

    /**
     * [marker] を載せる。既に View が載っていれば位置だけ更新する。
     *
     * View の生成（inflate）は非同期で、完了時に [shouldAttach] が false を返すか、
     * それまでに [hide] されていれば載せない。
     *
     * @param initialScale 載せるときの View のスケール。不要なら null
     */
    fun show(marker: M, initialScale: Float? = null, shouldAttach: () -> Boolean = { true }) {
        val identity = marker.identity
        markersPool[identity] = marker

        if (overlay.containsAnnotation(identity)) {
            updatePosition(marker)
        } else {
            attach(marker, initialScale, shouldAttach)
        }
    }

    /**
     * [marker] を外す。main thread 以外から呼んでもよい（View の取り外しは main thread で行う）。
     */
    fun hide(marker: M) {
        val identity = marker.identity

        // 登録の削除は同期的に実行する
        // inflate完了後のガード（markersPool.containsKey）が確実に機能するために必要
        val annotation = overlay.removeAnnotation(identity)
        markersPool.remove(identity)

        // View操作はMain threadで実行
        lifecycleOwner.lifecycleScope.launch {
            if (annotation != null) {
                overlay.removeView(annotation.view)
            }
        }
    }

    private fun attach(marker: M, initialScale: Float?, shouldAttach: () -> Boolean) {
        val point = googleMap.projection.toScreenLocation(marker.location)
        val screenPoint = ScreenPoint(point.x, point.y)
        val positionResult = generatePositionResult(screenPoint)

        lifecycleOwner.lifecycleScope.launch {
            val descriptor = generateMarkerPositionDescriptor(marker, positionResult)

            // 描画（inflate 込み）のタイミングは overlay が所有する。layer は配置情報を計算して依頼するだけ。
            // 描画続行判定（削除・表示条件の変化の遅延ガード）は overlay が inflate 完了時に評価する。
            overlay.render(
                marker = marker,
                descriptor = descriptor,
                scale = initialScale,
                isRenderable = { shouldAttach() && markersPool.containsKey(marker.identity) },
            )
        }
    }

    /**
     * overlay からの描画完了通知に反応する。View が attach された後に初めて意味を持つ後処理を行う。
     *
     * position 登録は「描画済みマーカーの集合」に対してのみ意味を持つため attach 内では行えず、
     * 描画完了イベントで行う。overlay が putAnnotation → 本イベント発火 の順を保証するため、
     * putAnnotation が addPosition に先行する（＝計算基準リセット後の calculateFull で descriptor が脱落しない）。
     *
     * 本処理は新規 coroutine で走るため putAnnotation とは非アトミック。dispatch の隙間に
     * hide が割り込むと削除済みマーカーの descriptor を addPosition しうるが、
     * calculateFull が viewAnnotationMap 基準で positions を replaceAll するため一過性で自己修復する。
     */
    private fun handleMarkerRendered(event: MarkerRendered) {
        lifecycleOwner.lifecycleScope.launch {
            positionCoordinator.addPosition(event.descriptor)

            // overlay.render に渡すのは本 layer の M だけ
            @Suppress("UNCHECKED_CAST")
            listener?.onMarkerAttached(event.marker as M, event.annotation.view, event.descriptor)

            if (edgeMode is EdgeMode.Clamp) {
                // 端に寄せた表示は View のサイズが確定してから揃え直す
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
    }

    /**
     * 載っている [marker] の View に、レイアウトサイズ（[ViewMarker.sizeInDp]）と位置を適用し直す。
     */
    suspend fun relayout(marker: M) {
        val view = overlay.getAnnotation(marker.identity)?.view ?: return
        val point = googleMap.projection.toScreenLocation(marker.location)
        val screenPoint = ScreenPoint(point.x, point.y)
        val positionResult = generatePositionResult(screenPoint)

        val size = marker.sizeInPx(density)
        val descriptor = generateMarkerPositionDescriptor(marker, positionResult)

        applyTranslation(view, descriptor)
        view.layoutParams = FrameLayout.LayoutParams(size, size)
    }

    // ---- 位置 ----

    /**
     * [marker] の [ViewMarker.location] の変更を View に反映する。View が載っていなければ何もしない。
     */
    fun updatePosition(marker: M) {
        val identity = marker.identity
        val annotation = overlay.getAnnotation(identity) ?: return
        val projection = googleMap.projection
        val point = projection.toScreenLocation(marker.location)
        val screenPoint = ScreenPoint(point.x, point.y)
        val positionResult = generatePositionResult(screenPoint)

        lifecycleOwner.lifecycleScope.launch {
            val descriptor = generateMarkerPositionDescriptor(marker, positionResult)

            applyTranslation(annotation.view, descriptor)

            // 座標計算基準とpositionsを一括更新
            positionCoordinator.updateSingleMarkerPosition(
                markerId = identity,
                marker = marker,
                screenPoint = screenPoint,
                projection = projection,
                currentCenter = googleMap.cameraPosition.target,
                descriptor = descriptor
            )
        }
    }

    /**
     * [marker] を現在位置から [to] へ等速で動かす。
     *
     * @param onEnd 移動完了時（[ViewMarker.location] が [to] になった後）に呼ばれる
     */
    fun moveSmoothly(marker: M, to: LatLng, durationMs: Long = DEFAULT_MOVE_DURATION_MS, onEnd: () -> Unit = {}) {
        val startPosition = marker.location
        val moveAnimator = ValueAnimator.ofFloat(0F, 1F)
        moveAnimator.apply {
            duration = durationMs
            interpolator = linearInterpolator
            addUpdateListener { animation ->
                try {
                    val fraction = animation.animatedFraction
                    marker.location = interpolate(fraction, startPosition, to)
                    Choreographer.getInstance().postFrameCallback {
                        updatePosition(marker)
                    }
                } catch (ex: Exception) {
                    marker.location = to
                    cancel()
                    removeAllUpdateListeners()
                }
            }
            start()
            doOnEnd {
                marker.location = to
                onEnd()
                removeAllUpdateListeners()
            }
        }
    }

    /**
     * カメラ移動中に毎フレーム呼ぶ。座標を再計算し、直近の計算結果を View に反映する。
     *
     * @param applyToViews false なら再計算だけを行い、View には反映しない
     */
    @MainThread
    fun onCameraMove(applyToViews: Boolean = true) {
        // 座標更新は毎フレーム実行
        // Capture camera state on main thread before sending to IO
        cameraUpdateChannel.trySend(captureCameraState())

        if (applyToViews) {
            // スナップショットキャッシュを使用（ロックなし、コルーチン不要）
            val positions = positionCoordinator.getSnapshot()
            updateMarkersScreenPosition(positions)
        }
    }

    /**
     * カメラ停止時に呼ぶ。座標を再計算して View に反映し、計算の基準点をリセットする。
     *
     * @param onRecalculated 再計算の直後・View への反映の前に main thread で呼ばれる。
     *                       載せるマーカーの見直しなど、カメラ停止時の処理をここで行う。
     *                       false を返すと View への反映を行わない
     */
    @MainThread
    fun onCameraIdle(onRecalculated: () -> Boolean = { true }) {
        // Capture camera state on main thread
        val cameraState = captureCameraState()

        lifecycleOwner.lifecycleScope.launch {
            // 座標更新
            updatePositionDescriptors(cameraState)

            if (onRecalculated()) {
                val positions = positionCoordinator.toList()
                updateMarkersScreenPosition(positions)
            }

            // onCameraIdle後は必ず参照点をリセット
            // これにより次のonCameraMoveで正しくデルタ計算が開始される
            positionCoordinator.resetReferencePoint()
        }
    }

    /**
     * GoogleMap のカメラリスナーを本 layer に繋ぐ。
     *
     * `setOnCameraMoveListener` / `setOnCameraIdleListener` を上書きするため、自前のリスナーを
     * 持つ場合は使わず、そのリスナーから [onCameraMove] / [onCameraIdle] を呼ぶ。
     */
    @MainThread
    fun attachCameraListeners() {
        googleMap.setOnCameraMoveListener { onCameraMove() }
        googleMap.setOnCameraIdleListener { onCameraIdle() }
    }

    // ---- 参照 ----

    /** View が overlay に載っているマーカー。main thread 以外から読んでもよい。 */
    val attachedMarkers: List<M>
        get() = overlay.annotationKeys().mapNotNull { markersPool[it] }

    /** overlay に載っている View の数。 */
    val attachedCount: Int get() = overlay.annotations.size

    fun isAttached(identity: MarkerIdentity): Boolean = overlay.containsAnnotation(identity)

    /** [identity] のマーカーの View。載っていなければ null。 */
    fun viewOf(identity: MarkerIdentity): View? = overlay.getAnnotation(identity)?.view

    /** [identity] のマーカーの直近の配置情報。載っていなければ null。 */
    suspend fun descriptorOf(identity: MarkerIdentity): MarkerPositionDescriptor? =
        positionCoordinator.find { it.identifier == identity }

    /** 載っているマーカーの現在のスクリーン座標。キーは [ViewMarker.childIds] の各 id。 */
    suspend fun currentScreenPoints(): Map<Long, ScreenPoint> {
        val screenPoints = mutableMapOf<Long, ScreenPoint>()
        positionCoordinator.forEach { descriptor ->
            descriptor.childIds.forEach {
                val screenPoint = ScreenPoint(
                    descriptor.origin.x,
                    descriptor.origin.y
                )
                screenPoints[it] = screenPoint
            }
        }
        return screenPoints
    }

    /** 画面に余白（`visibleBoundsMarginDp`）を加えた範囲。 */
    @MainThread
    fun visibleBounds(): LatLngBounds = boundary.boundsWithMargin(googleMap)

    /**
     * [marker] が [visibleBounds] の内側にあるか。
     * [EdgeMode.Clamp] では画面外のマーカーも端に寄せて表示するため、常に true。
     */
    @MainThread
    fun isInVisibleBounds(marker: M): Boolean = when (edgeMode) {
        is EdgeMode.Clamp -> true
        EdgeMode.None -> boundary.isInVisibleBounds(marker, googleMap)
    }

    /** [bounds] を事前に取得済みのときの [isInVisibleBounds]。main thread 以外から呼んでもよい。 */
    fun isInVisibleBounds(marker: M, bounds: LatLngBounds): Boolean = when (edgeMode) {
        is EdgeMode.Clamp -> true
        EdgeMode.None -> boundary.isInVisibleBounds(marker, bounds)
    }

    // ---- 内部 ----

    @MainThread
    private fun captureCameraState() = MarkerCameraState(
        projection = googleMap.projection,
        zoom = googleMap.cameraPosition.zoom,
        bearing = googleMap.cameraPosition.bearing,
        center = googleMap.cameraPosition.target
    )

    /**
     * マーカーの座標更新（毎フレーム実行）
     * 座標計算はMarkerPositionCoordinatorに委譲
     */
    @WorkerThread
    private suspend fun updatePositionDescriptors(cameraState: MarkerCameraState) {
        positionCoordinator.updateAllPositions(
            cameraState = cameraState,
            viewAnnotationMap = overlay.annotations,
            markersPool = markersPool,
        )
    }

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

        listener?.onPositionApplied(markersPool[descriptor.identifier], annotation.view, descriptor)
    }

    private fun applyTranslation(view: View, descriptor: MarkerPositionDescriptor) {
        view.translationX = descriptor.screenPosition.x.toFloat()
        view.translationY = descriptor.screenPosition.y.toFloat()
    }

    private suspend fun generateMarkerPositionDescriptor(
        marker: ViewMarker,
        result: MarkerPositionResult,
    ): MarkerPositionDescriptor {
        val identity = marker.identity
        return MarkerPositionDescriptor(
            identifier = identity,
            childIds = marker.childIds,
            origin = ScreenPoint(result.adjustedPoint.x, result.adjustedPoint.y),
            screenPosition = ScreenPoint(result.adjustedPoint.x - marker.offsetX(density), result.adjustedPoint.y - marker.offsetY(density)),
            rotation = result.angleInDegrees,
            currentEdge = edgeMode.edgeAt(result.adjustedPoint, overlay.width, overlay.height),
            previousEdge = positionCoordinator.find { it.identifier == identity }?.currentEdge ?: MarkerEdge.NONE
        )
    }

    private fun generatePositionResult(screenPoint: ScreenPoint): MarkerPositionResult =
        edgeMode.resolve(screenPoint, overlay.width, overlay.height)

    companion object {
        /** [moveSmoothly] の既定の移動時間。 */
        const val DEFAULT_MOVE_DURATION_MS = 2000L

        /** `visibleBoundsMarginDp` の既定値。 */
        const val DEFAULT_VISIBLE_BOUNDS_MARGIN_DP = 240

        private val linearInterpolator = LinearInterpolator()

        /** 経度 180 度線を跨ぐときは短い側を通る線形補間。 */
        private fun interpolate(fraction: Float, from: LatLng, to: LatLng): LatLng {
            var delta = to.longitude - from.longitude
            if (abs(delta) > 180) {
                delta -= sign(delta) * 360
            }
            val lat = (to.latitude - from.latitude) * fraction + from.latitude
            val lng = delta * fraction + from.longitude
            return LatLng(lat, lng)
        }
    }
}
