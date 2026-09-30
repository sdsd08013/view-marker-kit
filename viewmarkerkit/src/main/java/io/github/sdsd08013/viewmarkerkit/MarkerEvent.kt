package io.github.sdsd08013.viewmarkerkit

/**
 * 描画済みマーカーへの一過性イベント（state ではなく event）。
 *
 * 分類基準: 「今この view が破棄→再生成されたら復元すべき物があるか？」が **無い** もの = event。
 * 継続的な状態の描画経路には載せず、out-of-band な単一 channel
 * （[MarkerOverlayView.dispatchEvent] → [MarkerEventReceiver.receive]）で渡す。
 *
 * イベントの種類は利用側が定義する。per-event メソッドを生やさず、sealed な階層で
 * 直和として表現することを推奨する。
 */
interface MarkerEvent
