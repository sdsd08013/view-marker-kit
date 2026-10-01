package io.github.sdsd08013.viewmarkerkit

import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BoundaryTest {
    private val projection = FakeProjection(center = LatLng(35.0, 139.0))

    // 10dp at density 2 is a 20px margin around the 1080x1920 viewport
    private val boundary = Boundary(marginDp = 10) { ScreenMetrics(2f, projection.width, projection.height) }

    private fun markerAt(x: Int, y: Int) = TestMarker(1, projection.fromScreen(ScreenPoint(x, y)))

    @Test
    fun `markers inside the viewport are visible`() {
        assertTrue(boundary.isInVisibleBounds(markerAt(540, 960), projection))
    }

    @Test
    fun `the margin extends every side of the viewport`() {
        assertTrue(boundary.isInVisibleBounds(markerAt(-15, 960), projection))
        assertTrue(boundary.isInVisibleBounds(markerAt(1095, 960), projection))
        assertTrue(boundary.isInVisibleBounds(markerAt(540, -15), projection))
        assertTrue(boundary.isInVisibleBounds(markerAt(540, 1935), projection))
    }

    @Test
    fun `the margin extends every corner of the viewport`() {
        assertTrue(boundary.isInVisibleBounds(markerAt(-15, -15), projection))
        assertTrue(boundary.isInVisibleBounds(markerAt(1095, -15), projection))
        assertTrue(boundary.isInVisibleBounds(markerAt(-15, 1935), projection))
        assertTrue(boundary.isInVisibleBounds(markerAt(1095, 1935), projection))
    }

    @Test
    fun `markers beyond the margin are not visible`() {
        assertFalse(boundary.isInVisibleBounds(markerAt(-25, 960), projection))
        assertFalse(boundary.isInVisibleBounds(markerAt(540, 1945), projection))
    }

    @Test
    fun `precomputed bounds give the same answer`() {
        val bounds = boundary.boundsWithMargin(projection)

        assertTrue(boundary.isInVisibleBounds(markerAt(1095, 1935), bounds))
        assertFalse(boundary.isInVisibleBounds(markerAt(1105, 1935), bounds))
    }
}
