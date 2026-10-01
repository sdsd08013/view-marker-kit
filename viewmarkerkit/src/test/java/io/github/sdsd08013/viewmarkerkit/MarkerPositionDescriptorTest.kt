package io.github.sdsd08013.viewmarkerkit

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkerPositionDescriptorTest {
    private fun descriptor(previous: MarkerEdge, current: MarkerEdge) = MarkerPositionDescriptor(
        identifier = IdMarkerIdentity(1),
        childIds = listOf(1),
        origin = ScreenPoint(0, 0),
        screenPosition = ScreenPoint(0, 0),
        currentEdge = current,
        previousEdge = previous,
    )

    @Test
    fun `moving onto an edge`() {
        val d = descriptor(MarkerEdge.NONE, MarkerEdge.LEFT)

        assertTrue(d.transitionToEdge)
        assertFalse(d.transitionFromEdge)
        assertTrue(d.isEdgeChanged)
        assertTrue(d.isAtEdge)
    }

    @Test
    fun `moving off an edge`() {
        val d = descriptor(MarkerEdge.TOP, MarkerEdge.NONE)

        assertFalse(d.transitionToEdge)
        assertTrue(d.transitionFromEdge)
        assertTrue(d.isEdgeChanged)
        assertFalse(d.isAtEdge)
    }

    @Test
    fun `moving between edges`() {
        val d = descriptor(MarkerEdge.TOP, MarkerEdge.TOP_LEFT)

        assertFalse(d.transitionToEdge)
        assertFalse(d.transitionFromEdge)
        assertTrue(d.isEdgeChanged)
        assertTrue(d.isAtEdge)
    }
}
