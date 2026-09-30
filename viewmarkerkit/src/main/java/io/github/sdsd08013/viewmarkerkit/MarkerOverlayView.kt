package io.github.sdsd08013.viewmarkerkit

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import java.util.concurrent.ConcurrentHashMap

/**
 * マーカー描画完了イベント。[MarkerOverlayView.render] が View を attach した後に発火する。
 * View が存在して初めて意味を持つ後処理（position 登録や利用側への通知）を
 * 呼び出し側が本イベントに反応して行うためのペイロード。
 */
internal data class MarkerRendered(
    val marker: ViewMarker,
    val annotation: ViewAnnotation,
    val descriptor: MarkerPositionDescriptor,
)

/**
 * マーカーのViewツリー操作とViewAnnotation管理を担うカスタムFrameLayout。
 *
 * 責務:
 * - viewAnnotationMap の保持
 * - addView / removeView のラップ
 * - ViewAnnotationのルックアップ
 * - 描画済みViewへの一過性イベントの発火（アニメーション、スケール、方向更新等）
 *
 * ## 設計ルール: 「状態は ViewMarker、イベントは MarkerOverlayView」
 *
 * - **[ViewMarker] のプロパティとして持つべきもの** = 継続的な状態（cluster()で再評価される前提）
 * - **MarkerOverlayViewで直接操作すべきもの** = 一過性のイベント（トリガーされたら終わり）
 *   例: dispatchEvent([MarkerEvent])
 */
class MarkerOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    // レンダリング済みのView
    private val viewAnnotationMap = ConcurrentHashMap<MarkerIdentity, ViewAnnotation>()

    /**
     * View 生成（inflate 込み）の seam。GoogleMap の InfoWindowAdapter に相当し、描画サーフェスである
     * 本 overlay が保持する。concrete な View と layout を知る利用側の実装を注入する。
     */
    var viewFactory: MarkerViewFactory? = null

    /**
     * マーカー描画完了の通知先（単一 sink）。[putAnnotation] 後に発火する。
     * position 登録や利用側への通知など、View が存在して初めて意味を持つ後処理は
     * 呼び出し側が本 sink に反応して行う。
     */
    internal var onMarkerRendered: ((MarkerRendered) -> Unit)? = null

    /**
     * レンダリング済みViewAnnotationのスナップショット。
     * MarkerPositionCoordinator, MarkerScaleContext等の外部コンポーネントに渡す用途。
     */
    internal val annotations: MutableMap<MarkerIdentity, ViewAnnotation> get() = viewAnnotationMap

    // ---- ViewAnnotation管理 ----

    internal fun putAnnotation(identity: MarkerIdentity, annotation: ViewAnnotation) {
        viewAnnotationMap[identity] = annotation
        addView(annotation.view, annotation.viewLayoutParams)
    }

    internal fun removeAnnotation(identity: MarkerIdentity): ViewAnnotation? {
        return viewAnnotationMap.remove(identity)
    }

    // ---- 描画 ----

    /**
     * [marker] を（[viewFactory] 経由で非同期 inflate して）描画し overlay に attach する。
     *
     * inflate の完了タイミングは本 overlay が所有し、呼び出し側は「描画して」と頼むだけ（fire-and-forget）。
     * 完了後の View 依存処理は [onMarkerRendered] で返す。
     *
     * @param descriptor 配置情報（translation / edge）。View 非依存に呼び出し側が事前計算して渡す。
     * @param scale groupable マーカーの初期スケール。不要なら null。
     * @param isRenderable inflate 完了時点で描画を続行して良いか（削除・グルーピング変化の遅延ガード）。
     */
    internal fun render(
        marker: ViewMarker,
        descriptor: MarkerPositionDescriptor,
        scale: Float?,
        isRenderable: () -> Boolean,
    ) {
        val factory = viewFactory ?: run {
            Log.w(TAG, "viewFactory 未注入のため描画をスキップ: ${marker.identity}")
            return
        }
        factory.createAsync(marker, descriptor.currentEdge) { view ->
            if (!isRenderable()) return@createAsync
            if (containsAnnotation(marker.identity)) return@createAsync

            val size = marker.sizeInPx(resources.displayMetrics.density)
            val annotation = ViewAnnotation(
                view = view,
                viewLayoutParams = FrameLayout.LayoutParams(size, size),
            )

            view.applyPlacement(descriptor, scale)

            putAnnotation(marker.identity, annotation)

            onMarkerRendered?.invoke(MarkerRendered(marker, annotation, descriptor))
        }
    }

    private fun View.applyPlacement(descriptor: MarkerPositionDescriptor, scale: Float?) {
        translationX = descriptor.screenPosition.x.toFloat()
        translationY = descriptor.screenPosition.y.toFloat()
        scale?.let {
            scaleX = it
            scaleY = it
        }
    }

    // ---- ルックアップ ----

    internal fun containsAnnotation(identity: MarkerIdentity): Boolean = viewAnnotationMap.containsKey(identity)

    internal fun isAnnotationEmpty(): Boolean = viewAnnotationMap.isEmpty()

    internal fun getAnnotation(identity: MarkerIdentity): ViewAnnotation? = viewAnnotationMap[identity]

    internal fun annotationKeys(): Set<MarkerIdentity> = viewAnnotationMap.keys

    // ---- 描画済みViewへの操作 ----

    /**
     * 描画済み View へ一過性イベントを渡す。
     * concrete View 型を知らずに [MarkerEventReceiver] capability cast で叩く（単一 channel）。
     */
    fun dispatchEvent(identity: MarkerIdentity, event: MarkerEvent) {
        viewAnnotationMap[identity]?.eventReceiver?.receive(event)
    }

    private companion object {
        const val TAG = "MarkerOverlayView"
    }
}
