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
    fun `identity の既定値は id だけで決まる`() {
        assertEquals(Pin(1).identity, Pin(1, LatLng(1.0, 1.0)).identity)
        assertNotEquals(Pin(1).identity, Pin(2).identity)
        assertEquals(IdMarkerIdentity(1), Pin(1).identity)
    }

    @Test
    fun `sizeInPx と offset は sizeInDp と density から決まる`() {
        val pin = Pin(1)

        assertEquals(96, pin.sizeInPx(2f))
        assertEquals(48, pin.offsetX(2f))
        assertEquals(48, pin.offsetY(2f))
        assertEquals(listOf(1L), pin.childIds)
    }
}
