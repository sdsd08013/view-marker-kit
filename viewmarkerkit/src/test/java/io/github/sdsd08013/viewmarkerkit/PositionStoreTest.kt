package io.github.sdsd08013.viewmarkerkit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class PositionStoreTest {
    private fun descriptor(id: Long, x: Int = 0) = MarkerPositionDescriptor(
        identifier = IdMarkerIdentity(id),
        childIds = listOf(id),
        origin = ScreenPoint(x, 0),
        screenPosition = ScreenPoint(x, 0),
    )

    @Test
    fun `updateOrAdd replaces an existing entry and appends a new one`() {
        val store = PositionStore()
        store.add(descriptor(1))
        store.add(descriptor(2))

        store.updateOrAdd(descriptor(1, x = 10))
        store.updateOrAdd(descriptor(3))

        assertEquals(listOf(10, 0, 0), store.snapshot.map { it.origin.x })
        assertEquals(10, store.find(IdMarkerIdentity(1))?.origin?.x)
        assertNull(store.find(IdMarkerIdentity(4)))
    }

    @Test
    fun `snapshot is immutable until the next write`() {
        val store = PositionStore()
        store.add(descriptor(1))
        val before = store.snapshot

        assertSame(before, store.snapshot)
        store.replaceAll(listOf(descriptor(2)))
        assertEquals(listOf(1L), before.map { it.childIds.single() })
        assertEquals(listOf(2L), store.snapshot.map { it.childIds.single() })
    }
}
