package io.github.sdsd08013.viewmarkerkit

import org.junit.Assert.assertEquals
import org.junit.Test

class EdgeModeTest {
    private val width = 1080
    private val height = 1920

    @Test
    fun `None keeps off-screen points and reports NONE`() {
        val point = ScreenPoint(-100, 3000)

        val result = EdgeMode.None.resolve(point, width, height)

        assertEquals(point, result.adjustedPoint)
        assertEquals(0f, result.angleInDegrees, 0.001f)
        assertEquals(MarkerEdge.NONE, EdgeMode.None.edgeAt(result.adjustedPoint, width, height))
    }

    @Test
    fun `Clamp keeps on-screen points and reports NONE`() {
        val mode = EdgeMode.Clamp(marginPx = 60)
        val point = ScreenPoint(540, 960)

        val result = mode.resolve(point, width, height)

        assertEquals(point, result.adjustedPoint)
        assertEquals(0f, result.angleInDegrees, 0.001f)
        assertEquals(MarkerEdge.NONE, mode.edgeAt(result.adjustedPoint, width, height))
    }

    @Test
    fun `Clamp pulls a point beyond the left edge to LEFT with the angle from center`() {
        val mode = EdgeMode.Clamp(marginPx = 60)

        val result = mode.resolve(ScreenPoint(-100, 960), width, height)

        assertEquals(ScreenPoint(60, 960), result.adjustedPoint)
        assertEquals(90f, result.angleInDegrees, 0.001f)
        assertEquals(MarkerEdge.LEFT, mode.edgeAt(result.adjustedPoint, width, height))
    }

    @Test
    fun `Clamp pulls a point beyond the bottom edge to BOTTOM`() {
        val mode = EdgeMode.Clamp(marginPx = 60)

        val result = mode.resolve(ScreenPoint(540, 3000), width, height)

        assertEquals(ScreenPoint(540, 1860), result.adjustedPoint)
        assertEquals(MarkerEdge.BOTTOM, mode.edgeAt(result.adjustedPoint, width, height))
    }

    @Test
    fun `bottomInsetPx affects the clamped position but not the edge`() {
        val mode = EdgeMode.Clamp(marginPx = 60, bottomInsetPx = 200)

        val result = mode.resolve(ScreenPoint(540, 3000), width, height)

        // The point is raised by the inset, but edge detection still uses the bottom without the inset
        assertEquals(ScreenPoint(540, 1660), result.adjustedPoint)
        assertEquals(MarkerEdge.NONE, mode.edgeAt(result.adjustedPoint, width, height))
    }
}
