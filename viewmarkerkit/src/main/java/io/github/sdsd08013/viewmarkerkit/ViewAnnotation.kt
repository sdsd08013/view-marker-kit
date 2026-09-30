package io.github.sdsd08013.viewmarkerkit

import android.view.View
import android.widget.FrameLayout

internal data class ViewAnnotation(
    val view: View,
    val viewLayoutParams: FrameLayout.LayoutParams
) {
    // capability は View の実装クラスで固定なので、構築時に 1 度だけ判定して dispatch 毎の cast を不要にする
    val alignable: Alignable? = view as? Alignable
    val eventReceiver: MarkerEventReceiver? = view as? MarkerEventReceiver
}
