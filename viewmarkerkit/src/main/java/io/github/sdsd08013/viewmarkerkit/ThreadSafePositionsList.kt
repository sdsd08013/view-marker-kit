package io.github.sdsd08013.viewmarkerkit

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Mutex-guarded list of positions with a lock-free snapshot for per-frame reads. */
internal class ThreadSafePositionsList {
    private val list: MutableList<MarkerPositionDescriptor> = mutableListOf()
    private val mutex = Mutex()

    @Volatile
    private var cachedSnapshot: List<MarkerPositionDescriptor> = emptyList()

    suspend fun add(descriptor: MarkerPositionDescriptor) {
        mutex.withLock {
            list.add(descriptor)
            cachedSnapshot = list.toList()
        }
    }

    suspend fun clear() {
        mutex.withLock {
            list.clear()
            cachedSnapshot = emptyList()
        }
    }

    suspend fun replaceAll(descriptors: List<MarkerPositionDescriptor>) {
        mutex.withLock {
            list.clear()
            list.addAll(descriptors)
            cachedSnapshot = descriptors.toList()
        }
    }

    suspend fun updateOrAdd(descriptor: MarkerPositionDescriptor) {
        mutex.withLock {
            val index = list.indexOfFirst { it.identifier == descriptor.identifier }
            if (index >= 0) {
                list[index] = descriptor
            } else {
                list.add(descriptor)
            }
            cachedSnapshot = list.toList()
        }
    }

    /** Latest snapshot, readable without taking the lock. */
    fun getSnapshot(): List<MarkerPositionDescriptor> = cachedSnapshot

    suspend fun toList(): List<MarkerPositionDescriptor> {
        return mutex.withLock {
            list.toList()
        }
    }

    suspend fun toMutableList(): MutableList<MarkerPositionDescriptor> {
        return mutex.withLock {
            list.toMutableList()
        }
    }

    suspend fun find(predicate: (MarkerPositionDescriptor) -> Boolean): MarkerPositionDescriptor? {
        return mutex.withLock {
            list.find(predicate)
        }
    }

    suspend fun forEach(action: (MarkerPositionDescriptor) -> Unit) {
        mutex.withLock {
            list.forEach(action)
        }
    }
}
