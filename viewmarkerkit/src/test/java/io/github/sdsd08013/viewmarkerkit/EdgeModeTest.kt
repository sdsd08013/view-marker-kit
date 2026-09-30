package io.github.sdsd08013.viewmarkerkit

import org.junit.Assert.assertEquals
import org.junit.Test

class EdgeModeTest {
    private val width = 1080
    private val height = 1920

    @Test
    fun `None - 画面外の点も補正せず edge は NONE`() {
        val point = ScreenPoint(-100, 3000)

        val result = EdgeMode.None.resolve(point, width, height)

        assertEquals(point, result.adjustedPoint)
        assertEquals(0f, result.angleInDegrees, 0.001f)
        assertEquals(MarkerEdge.NONE, EdgeMode.None.edgeAt(result.adjustedPoint, width, height))
    }

    @Test
    fun `Clamp - 画面内の点はそのままで edge は NONE`() {
        val mode = EdgeMode.Clamp(marginPx = 60)
        val point = ScreenPoint(540, 960)

        val result = mode.resolve(point, width, height)

        assertEquals(point, result.adjustedPoint)
        assertEquals(0f, result.angleInDegrees, 0.001f)
        assertEquals(MarkerEdge.NONE, mode.edgeAt(result.adjustedPoint, width, height))
    }

    @Test
    fun `Clamp - 左に外れた点は左端へ寄せ 画面中央から見た角度を返す`() {
        val mode = EdgeMode.Clamp(marginPx = 60)

        val result = mode.resolve(ScreenPoint(-100, 960), width, height)

        assertEquals(ScreenPoint(60, 960), result.adjustedPoint)
        assertEquals(90f, result.angleInDegrees, 0.001f)
        assertEquals(MarkerEdge.LEFT, mode.edgeAt(result.adjustedPoint, width, height))
    }

    @Test
    fun `Clamp - 下に外れた点は下端へ寄せる`() {
        val mode = EdgeMode.Clamp(marginPx = 60)

        val result = mode.resolve(ScreenPoint(540, 3000), width, height)

        assertEquals(ScreenPoint(540, 1860), result.adjustedPoint)
        assertEquals(MarkerEdge.BOTTOM, mode.edgeAt(result.adjustedPoint, width, height))
    }

    @Test
    fun `Clamp - bottomInsetPx は寄せ先だけに効き edge 判定には使われない`() {
        val mode = EdgeMode.Clamp(marginPx = 60, bottomInsetPx = 200)

        val result = mode.resolve(ScreenPoint(540, 3000), width, height)

        // 寄せ先は inset の分だけ上がるが、edge 判定は inset を引かない下端を基準にするため NONE になる
        assertEquals(ScreenPoint(540, 1660), result.adjustedPoint)
        assertEquals(MarkerEdge.NONE, mode.edgeAt(result.adjustedPoint, width, height))
    }
}
