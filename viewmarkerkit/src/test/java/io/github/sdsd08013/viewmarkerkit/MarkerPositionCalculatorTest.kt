package io.github.sdsd08013.viewmarkerkit

import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class MarkerPositionCalculatorTest {
    private val density = 2f
    private val projection = FakeProjection(center = LatLng(35.0, 139.0))
    private val viewport = Viewport(projection.width, projection.height)

    private fun calculator(edgeMode: EdgeMode = EdgeMode.None) =
        MarkerPositionCalculator(density, { viewport }, edgeMode)

    private fun camera(zoom: Float = 15f, bearing: Float = 0f) =
        MarkerCameraState(projection, zoom, bearing, projection.center)

    private val marker = TestMarker(1, LatLng(35.0, 139.0))
    private val pool = mapOf(marker.identity to marker)

    @Test
    fun `full calculation projects every attached marker`() {
        val result = calculator().calculate(camera(), emptyList(), setOf(marker.identity), pool)

        val descriptor = result.single()
        assertEquals(marker.identity, descriptor.identifier)
        assertEquals(ScreenPoint(540, 960), descriptor.origin)
        // 48dp at density 2 is 96px, anchored at the center of the view
        assertEquals(ScreenPoint(492, 912), descriptor.screenPosition)
        assertEquals(MarkerEdge.NONE, descriptor.currentEdge)
    }

    @Test
    fun `markers that are attached but unknown are skipped`() {
        val result = calculator().calculate(camera(), emptyList(), setOf(IdMarkerIdentity(99)), pool)

        assertEquals(emptyList<MarkerPositionDescriptor>(), result)
    }

    @Test
    fun `panning applies the camera delta to the reference positions`() {
        val calculator = calculator()
        val first = calculator.calculate(camera(), emptyList(), setOf(marker.identity), pool)

        // Camera moves 0.1 degree east: the marker moves 100px to the left
        projection.center = LatLng(35.0, 139.1)
        val second = calculator.calculate(camera(), first, setOf(marker.identity), pool)

        assertEquals(ScreenPoint(440, 960), second.single().origin)
        assertEquals(ScreenPoint(392, 912), second.single().screenPosition)
    }

    @Test
    fun `panning keeps descriptors that have no reference position`() {
        val calculator = calculator()
        calculator.calculate(camera(), emptyList(), setOf(marker.identity), pool)
        val stale = MarkerPositionDescriptor(IdMarkerIdentity(99), listOf(99), ScreenPoint(1, 1), ScreenPoint(0, 0))

        projection.center = LatLng(35.0, 139.1)
        val result = calculator.calculate(camera(), listOf(stale), setOf(marker.identity), pool)

        assertSame(stale, result.single())
    }

    @Test
    fun `zoom or bearing change triggers a full calculation`() {
        val calculator = calculator()
        val first = calculator.calculate(camera(zoom = 15f), emptyList(), setOf(marker.identity), pool)
        val newcomer = TestMarker(2, LatLng(35.0, 139.0))
        val pool = pool + (newcomer.identity to newcomer)

        // A newly attached marker only shows up in a full calculation
        val zoomed = calculator.calculate(camera(zoom = 16f), first, setOf(marker.identity, newcomer.identity), pool)
        assertEquals(setOf(marker.identity, newcomer.identity), zoomed.map { it.identifier }.toSet())

        val rotated = calculator.calculate(camera(zoom = 16f, bearing = 90f), zoomed, setOf(marker.identity), pool)
        assertEquals(listOf(marker.identity), rotated.map { it.identifier })
    }

    @Test
    fun `resetReferenceFrame forces a full calculation`() {
        val calculator = calculator()
        val first = calculator.calculate(camera(), emptyList(), setOf(marker.identity), pool)
        val newcomer = TestMarker(2, LatLng(35.0, 139.0))

        calculator.resetReferenceFrame()
        val result = calculator.calculate(
            camera(), first, setOf(marker.identity, newcomer.identity), pool + (newcomer.identity to newcomer),
        )

        assertEquals(2, result.size)
    }

    @Test
    fun `updateMarkerBase moves the reference position of one marker`() {
        val calculator = calculator()
        val first = calculator.calculate(camera(), emptyList(), setOf(marker.identity), pool)

        // The marker moves 0.05 degree north while the camera is at the reference center
        marker.location = LatLng(35.05, 139.0)
        calculator.updateMarkerBase(marker.identity, projection.toScreen(marker.location), marker, projection, projection.center)

        projection.center = LatLng(35.0, 139.1)
        val result = calculator.calculate(camera(), first, setOf(marker.identity), pool)

        assertEquals(ScreenPoint(440, 910), result.single().origin)
    }

    @Test
    fun `updateMarkerBase without a reference frame is ignored`() {
        val calculator = calculator()

        calculator.updateMarkerBase(marker.identity, ScreenPoint(0, 0), marker, projection, projection.center)
        val result = calculator.calculate(camera(), emptyList(), setOf(marker.identity), pool)

        assertEquals(ScreenPoint(540, 960), result.single().origin)
    }

    @Test
    fun `clamp mode reports the edge and rotation of off-screen markers`() {
        val calculator = calculator(EdgeMode.Clamp(marginPx = 60))
        val offScreen = TestMarker(3, LatLng(35.0, 138.0))

        val result = calculator.calculate(camera(), emptyList(), setOf(offScreen.identity), mapOf(offScreen.identity to offScreen))

        val descriptor = result.single()
        assertEquals(ScreenPoint(60, 960), descriptor.origin)
        assertEquals(MarkerEdge.LEFT, descriptor.currentEdge)
        assertEquals(90f, descriptor.rotation, 0.001f)
    }

    @Test
    fun `panning records the previous edge`() {
        val calculator = calculator(EdgeMode.Clamp(marginPx = 60))
        val first = calculator.calculate(camera(), emptyList(), setOf(marker.identity), pool)

        // Pan far enough to push the marker off the left edge
        projection.center = LatLng(35.0, 140.0)
        val second = calculator.calculate(camera(), first, setOf(marker.identity), pool)

        assertEquals(MarkerEdge.NONE, second.single().previousEdge)
        assertEquals(MarkerEdge.LEFT, second.single().currentEdge)
    }
}
