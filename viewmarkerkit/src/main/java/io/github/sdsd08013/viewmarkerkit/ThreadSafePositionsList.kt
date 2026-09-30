package io.github.sdsd08013.viewmarkerkit

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Thread-safe wrapper for marker position list.
 * Encapsulates mutex operations to prevent race conditions.
 * Uses snapshot caching for lock-free reads.
 */
internal class ThreadSafePositionsList {
    private val list: MutableList<MarkerPositionDescriptor> = mutableListOf()
    private val mutex = Mutex()

    // 読み取り用スナップショット（ロックなしでアクセス可能）
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

    /**
     * 指定されたIDのdescriptorを更新、存在しなければ追加
     */
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

    /**
     * ロックなしでスナップショットを取得（毎フレーム呼び出し用）
     */
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
