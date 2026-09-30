package io.github.sdsd08013.viewmarkerkit

import android.view.View

/**
 * [ViewMarker] から描画用の concrete View を非同期生成する factory。
 *
 * inflate する layout / ViewBinding / concrete な View の生成は全て利用側に閉じる。
 * 呼び出し側（[MarkerOverlayView]）は layout / binding / inflater を一切知らず、本 interface に
 * 「marker に対応する View を作って渡して」と頼むだけにする。実装は利用側が overlay へ注入する
 * （GoogleMap の setInfoWindowAdapter と同じく、描画サーフェスが利用側注入の View factory を持つ）。
 *
 * View 生成に要る context / lifecycleOwner / listener は実装が構築時に閉じ込め、呼び出し側は marker と
 * edge だけ渡す。[onCreated] は main thread で呼ぶ。
 *
 * 非同期 inflate が要らなければ [sync] で作る。
 */
fun interface MarkerViewFactory {
    fun createAsync(
        marker: ViewMarker,
        edge: MarkerEdge,
        onCreated: (View) -> Unit,
    )

    companion object {
        /** View を同期で作る factory。[create] は main thread で呼ばれる。 */
        fun sync(create: (marker: ViewMarker, edge: MarkerEdge) -> View): MarkerViewFactory =
            MarkerViewFactory { marker, edge, onCreated -> onCreated(create(marker, edge)) }
    }
}
