package io.github.sdsd08013.viewmarkerkit

import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ViewMarkerTest {
    private class Pin(override val id: Long, override var location: LatLng = LatLng(0.0, 0.0)) : ViewMarker {
        override val sizeInDp: Int = 48
    }

    @Test
    fun `default identity is derived from id only`() {
        assertEquals(Pin(1).identity, Pin(1, LatLng(1.0, 1.0)).identity)
        assertNotEquals(Pin(1).identity, Pin(2).identity)
        assertEquals(IdMarkerIdentity(1), Pin(1).identity)
    }

    @Test
    fun `sizeInPx and offsets are derived from sizeInDp and density`() {
        val pin = Pin(1)

        assertEquals(96, pin.sizeInPx(2f))
        assertEquals(48, pin.offsetX(2f))
        assertEquals(48, pin.offsetY(2f))
        assertEquals(listOf(1L), pin.childIds)
    }
}
