package de.phyrone.nautilus.shared

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import java.io.Closeable
import java.util.concurrent.atomic.AtomicLong

/**
 * Represents a lock with shared and exclusive locks.
 * It is almost fair, exlusive locks are guaranteed to be acquired before shared locks that are requested after them.
 * Shared locks dont get aquired if an exclusive lock is waiting in front of them.
 * The queue is FIFO and updates to it are optimistic.
 */
class RwLock {
    private val atomicCounter = AtomicLong(0)

    private data class LockEntry(
        val nr: Long,
        val owner: Any?,
        val exclusive: Boolean,
    )

    private data class LockState(
        val entries: List<LockEntry> = emptyList(),
    ) {
        fun add(entry: LockEntry): LockState {
            return copy(entries = entries + entry)
        }

        fun remove(entry: LockEntry): LockState {
            return copy(entries = entries - entry)
        }

        fun canAcquire(entry: LockEntry): Boolean {
            if (entry !in entries) {
                error("lock entry is not placed in the lock state")
            }

            return if (entry.exclusive) {
                // if the entry is an exclusive lock it can be acquired if it is in the first position
                entries.indexOf(entry) == 0
            } else {
                // in the entry is a shared lock it can be acquired if there is no exclusive lock in front of it
                val exclusiveIndex = entries.indexOfFirst { it.exclusive }
                exclusiveIndex == -1 || exclusiveIndex > entries.indexOf(entry)
            }
        }
    }

    private val lockState = MutableStateFlow(LockState())

    private suspend fun lock(
        owner: Any?,
        exclusive: Boolean,
    ): LockGuard {
        val entry = LockEntry(atomicCounter.incrementAndGet(), owner, exclusive)
        val newState = lockState.updateAndGet { state -> state.add(entry) }
        if (!newState.canAcquire(entry)) {
            lockState.first { it.canAcquire(entry) }
        }

        return this.LockGuardImpl(entry)
    }

    interface LockGuard : Closeable {
        val isExclusive: Boolean

        fun downgrade()
    }

    private inner class LockGuardImpl constructor(
        private val entry: LockEntry,
    ) : LockGuard {
        override val isExclusive: Boolean
            get() = entry.exclusive

        @Synchronized
        override fun downgrade() {
            if (!entry.exclusive) {
                return
            }

            val downgraded = entry.copy(exclusive = false)
            lockState.update { state ->
                // replace the old entry with the new one at the same position
                val index = state.entries.indexOf(entry)
                state.copy(
                    entries =
                        state.entries.toMutableList()
                            .apply { set(index, downgraded) },
                )
            }
        }

        override fun close() {
            lockState.update { state -> state.remove(entry) }
        }
    }

    @JvmOverloads
    suspend fun readLock(owner: Any? = null) = lock(owner, false)

    @JvmOverloads
    suspend fun writeLock(owner: Any? = null) = lock(owner, true)
}
