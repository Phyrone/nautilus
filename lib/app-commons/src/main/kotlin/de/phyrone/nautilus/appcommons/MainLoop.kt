package de.phyrone.nautilus.appcommons

import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.Runnable
import org.slf4j.LoggerFactory
import java.util.concurrent.LinkedBlockingDeque
import kotlin.coroutines.CoroutineContext

object MainLoop : MainCoroutineDispatcher() {
    private val logger = LoggerFactory.getLogger(MainLoop::class.java)
    private val tasks = LinkedBlockingDeque<Runnable>()

    object Immediate : MainCoroutineDispatcher() {
        override val immediate: MainCoroutineDispatcher = this

        override fun dispatch(context: CoroutineContext, block: Runnable) {
            tasks.putFirst(block)
        }

    }

    override val immediate: MainCoroutineDispatcher = Immediate

    override fun dispatch(context: CoroutineContext, block: Runnable) {

        tasks.add(block)
    }

    @Synchronized
    fun runLoop(): Nothing {
        while (true) {
            val runnable = tasks.take()
            try {
                runnable.run()
            } catch (e: InterruptedException) {
                throw e
            } catch (e: Exception) {
                logger.error("A task in the main loop failed", e)
            }
        }
    }

}