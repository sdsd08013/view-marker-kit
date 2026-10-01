package io.github.sdsd08013.viewmarkerkit.sample.views

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.TextView
import io.github.sdsd08013.viewmarkerkit.Alignable
import io.github.sdsd08013.viewmarkerkit.MarkerEdge
import io.github.sdsd08013.viewmarkerkit.MarkerEvent
import io.github.sdsd08013.viewmarkerkit.MarkerEventReceiver
import io.github.sdsd08013.viewmarkerkit.MarkerPositionDescriptor

/**
 * The view rendered for a [Pin]: a colored circle with the label.
 *
 * [MarkerEventReceiver] lets the layer deliver [Bounce]; [Alignable] tells the view
 * which screen edge it is clamped to.
 */
class PinView(context: Context, pin: Pin, onClick: (Pin) -> Unit) : FrameLayout(context), MarkerEventReceiver, Alignable {
    private val circle = TextView(context).apply {
        text = pin.label
        setTextColor(Color.WHITE)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
        maxLines = 1
        gravity = Gravity.CENTER
        background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(pin.color)
            setStroke(dp(3), Color.WHITE)
        }
        elevation = dp(4).toFloat()
    }

    init {
        val circleSize = dp(CIRCLE_DP)
        addView(circle, LayoutParams(circleSize, circleSize, Gravity.CENTER))
        setOnClickListener { onClick(pin) }
    }

    override fun receive(event: MarkerEvent) {
        if (event === Bounce) {
            animate().scaleX(1.4f).scaleY(1.4f).setDuration(120)
                .withEndAction { animate().scaleX(1f).scaleY(1f).setDuration(200).start() }
                .start()
        }
    }

    override fun align(descriptor: MarkerPositionDescriptor) {
        // Fade the pin while it sits on a screen edge
        alpha = if (descriptor.currentEdge == MarkerEdge.NONE) 1f else 0.6f
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density + 0.5f).toInt()

    companion object {
        /** Side of the square area the layer reserves for the view. */
        const val SIZE_DP = 56
        private const val CIRCLE_DP = 44
    }
}
