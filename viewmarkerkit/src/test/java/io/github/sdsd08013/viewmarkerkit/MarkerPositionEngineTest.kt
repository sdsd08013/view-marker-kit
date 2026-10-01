package io.github.sdsd08013.viewmarkerkit

import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkerPositionEngineTest {
    private val projection = FakeProjection(center = LatLng(35.0, 139.0))
    private val engine = MarkerPositionEngine<TestMarker>(
        density = 2f,
        viewport = { Viewport(projection.width, projection.height) },
    )

    private fun camera() = CameraSnapshot(projection, 15f, 0f, projection.center)

    @Test
    fun `attach registers the marker and its position`() {
        val marker = TestMarker(1, LatLng(35.0, 139.0))

        val descriptor = engine.descriptorFor(marker, projection)
        engine.attach(marker, descriptor)

        assertTrue(engine.isAttached(marker.identity))
        assertEquals(listOf(marker), engine.attachedMarkers)
        assertEquals(ScreenPoint(540, 960), engine.descriptorOf(marker.identity)?.origin)
        assertEquals(ScreenPoint(492, 912), descriptor.screenPosition)
    }

    @Test
    fun `recalculate follows the camera for attached markers only`() {
        val attached = TestMarker(1, LatLng(35.0, 139.0))
        val detached = TestMarker(2, LatLng(35.0, 139.0))
        engine.attach(attached, engine.descriptorFor(attached, projection))
        engine.attach(detached, engine.descriptorFor(detached, projection))
        engine.detach(detached.identity)

        projection.center = LatLng(35.0, 139.1)
        val positions = engine.recalculate(camera())

        assertEquals(listOf(attached.identity), positions.map { it.identifier })
        assertEquals(ScreenPoint(440, 960), positions.single().origin)
        assertEquals(positions, engine.positions)
    }

    @Test
    fun `updateLocation moves one marker and keeps it in sync while panning`() {
        val marker = TestMarker(1, LatLng(35.0, 139.0))
        engine.attach(marker, engine.descriptorFor(marker, projection))
        engine.recalculate(camera())

        marker.location = LatLng(35.05, 139.0)
        val moved = engine.updateLocation(marker, projection, projection.center)
        assertEquals(ScreenPoint(540, 910), moved.origin)
        assertEquals(ScreenPoint(540, 910), engine.descriptorOf(marker.identity)?.origin)

        projection.center = LatLng(35.0, 139.1)
        val panned = engine.recalculate(camera()).single()
        assertEquals(ScreenPoint(440, 910), panned.origin)
    }

    @Test
    fun `resetReferenceFrame makes the next recalculate project every marker`() {
        val first = TestMarker(1, LatLng(35.0, 139.0))
        engine.attach(first, engine.descriptorFor(first, projection))
        engine.recalculate(camera())

        val second = TestMarker(2, LatLng(35.0, 139.0))
        engine.attach(second, engine.descriptorFor(second, projection))
        engine.resetReferenceFrame()

        assertEquals(2, engine.recalculate(camera()).size)
    }

    @Test
    fun `clear removes markers and positions`() {
        val marker = TestMarker(1, LatLng(35.0, 139.0))
        engine.attach(marker, engine.descriptorFor(marker, projection))

        engine.clear()

        assertFalse(engine.isAttached(marker.identity))
        assertNull(engine.descriptorOf(marker.identity))
        assertTrue(engine.positions.isEmpty())
    }

    @Test
    fun `visible bounds honor the edge mode`() {
        val offScreen = TestMarker(1, projection.fromScreen(ScreenPoint(-2000, 960)))
        val clamped = MarkerPositionEngine<TestMarker>(2f, { Viewport(projection.width, projection.height) }, EdgeMode.Clamp(60))

        assertFalse(engine.isInVisibleBounds(offScreen, projection))
        assertTrue(clamped.isInVisibleBounds(offScreen, projection))
    }
}
