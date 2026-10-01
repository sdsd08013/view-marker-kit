package io.github.sdsd08013.viewmarkerkit.sample.views

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.FrameMetrics
import android.view.Window

/**
 * Logs frame timing percentiles for stress tests: `adb shell am start ... --ez metrics true`,
 * then `adb logcat -s FrameStats`. Every 120 frames it prints one line.
 */
class FrameMetricsLogger(activity: Activity) {
    private val samples = mutableListOf<LongArray>()
    private val handler = Handler(Looper.getMainLooper())

    private val listener = Window.OnFrameMetricsAvailableListener { _, metrics, _ ->
        if (metrics.getMetric(FrameMetrics.FIRST_DRAW_FRAME) == 1L) return@OnFrameMetricsAvailableListener
        samples += LongArray(IDS.size) { metrics.getMetric(IDS[it]) }
        if (samples.size >= 120) flush()
    }

    init {
        activity.window.addOnFrameMetricsAvailableListener(listener, handler)
    }

    private fun flush() {
        val n = samples.size
        fun pct(id: Int, p: Double): Double {
            val sorted = samples.map { it[id] }.sorted()
            return sorted[(n * p).toInt().coerceAtMost(n - 1)] / 1e6
        }
        val line = NAMES.indices.joinToString(" ") { i -> "%s=%.1f/%.1f".format(NAMES[i], pct(i, 0.5), pct(i, 0.9)) }
        Log.i("FrameStats", "n=$n $line")
        samples.clear()
    }

    private companion object {
        val IDS = intArrayOf(
            FrameMetrics.TOTAL_DURATION,
            FrameMetrics.INPUT_HANDLING_DURATION,
            FrameMetrics.ANIMATION_DURATION,
            FrameMetrics.LAYOUT_MEASURE_DURATION,
            FrameMetrics.DRAW_DURATION,
            FrameMetrics.SYNC_DURATION,
            FrameMetrics.COMMAND_ISSUE_DURATION,
            FrameMetrics.SWAP_BUFFERS_DURATION,
        )
        val NAMES = listOf("total", "input", "anim", "layout", "draw", "sync", "cmd", "swap")
    }
}
