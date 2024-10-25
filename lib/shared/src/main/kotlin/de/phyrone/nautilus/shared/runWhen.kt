package de.phyrone.nautilus.shared

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * Run the block while the conditionFlow is true if not suspend until the conditionFlow is true.
 * When the conditionFlow is false the block will be canceled and re-executed later.
 * When the block is finished the function returns.
 */
suspend fun <T> runWhen(
    conditionFlow: Flow<Boolean>,
    block: suspend () -> T,
): T {
    return coroutineScope {
        var collector: Job? = null
        try {
            val completion = MutableSharedFlow<Result<T>>(1, 0)
            collector =
                launch {
                    var job: Job? = null
                    conditionFlow.cancellable()
                        .collect { condition ->
                            if (condition && job?.isActive != true) {
                                job =
                                    launch {
                                        try {
                                            val result = block()
                                            completion.emit(Result.success(result))
                                        } catch (e: CancellationException) {
                                            throw e
                                        } catch (e: InterruptedException) {
                                            throw e
                                        } catch (e: Throwable) {
                                            completion.emit(Result.failure(e))
                                        }
                                    }
                            } else if (!condition) {
                                job?.cancelAndJoin()
                                job = null
                            }
                        }
                }
            return@coroutineScope completion
                .first()
                .getOrThrow()
        } finally {
            collector?.cancelAndJoin()
        }
    }
}
