package de.phyrone.nautilus.shared

import kotlinx.coroutines.flow.*
import java.io.Closeable
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Not to be confused with memory management, this class is used to keep track of how often a shared resource is used.
 */
class RefCounter {
    private val counter = MutableStateFlow<Int>(0)
    val count = counter.asStateFlow()

    /**
     * Equivalent to creating a [Ref] class on the [RefCounter] instance.
     */
    fun ref(): Ref = Ref()

    /**
     * Represents a reference to a shared resource.
     * When closed, the reference is removed from the counter.
     * Since its a [Closeable], it can be used in a `use` block and might be auto-closed by the GC.
     */
    inner class Ref : Closeable {
        init {
            counter.update { it + 1 }
        }

        private var closed = AtomicBoolean(false)
        override fun close() {
            val alreadyClosed = closed.getAndSet(true)
            if (alreadyClosed) return

            counter.update { it - 1 }
        }
    }


}
