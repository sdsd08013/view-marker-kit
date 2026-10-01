package io.github.sdsd08013.viewmarkerkit

/**
 * Positions of attached markers. Reads are lock-free (per-frame); writes are serialized.
 */
internal class PositionStore {
    @Volatile
    private var positions: List<MarkerPositionDescriptor> = emptyList()

    val snapshot: List<MarkerPositionDescriptor> get() = positions

    @Synchronized
    fun add(descriptor: MarkerPositionDescriptor) {
        positions = positions + descriptor
    }

    @Synchronized
    fun replaceAll(descriptors: List<MarkerPositionDescriptor>) {
        positions = descriptors.toList()
    }

    @Synchronized
    fun updateOrAdd(descriptor: MarkerPositionDescriptor) {
        val current = positions
        val index = current.indexOfFirst { it.identifier == descriptor.identifier }
        positions = if (index >= 0) {
            current.toMutableList().also { it[index] = descriptor }
        } else {
            current + descriptor
        }
    }

    @Synchronized
    fun clear() {
        positions = emptyList()
    }

    fun find(identity: MarkerIdentity): MarkerPositionDescriptor? = positions.find { it.identifier == identity }
}
