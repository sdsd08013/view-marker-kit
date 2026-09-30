package io.github.sdsd08013.viewmarkerkit

/**
 * 画面端へ寄せたときの配置（[MarkerPositionDescriptor]）を受け取れる View の capability。
 * [EdgeMode.Clamp] のとき、位置が更新されるたびに呼ばれる。
 */
interface Alignable {
    fun align(descriptor: MarkerPositionDescriptor)
}

/**
 * 描画済み View へ一過性の [MarkerEvent] を渡す capability。
 *
 * 位置(align) と違い「トリガーされたら終わり」の event を **単一 channel** で受ける
 * （per-event メソッドを生やさない）。利用側は concrete View 型を知らず、
 * [MarkerOverlayView.dispatchEvent] 経由で叩く。
 */
interface MarkerEventReceiver {
    fun receive(event: MarkerEvent)
}
