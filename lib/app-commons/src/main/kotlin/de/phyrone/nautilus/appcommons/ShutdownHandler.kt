package de.phyrone.nautilus.appcommons

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.concurrent.thread

private typealias ShutdownTask = suspend () -> Unit;

object ShutdownHandler {
    private val shutdownTasks = mutableListOf<ShutdownTask>()

    init {
        Runtime.getRuntime().addShutdownHook(thread(
            start = false,
            name = "ShutdownHook"
        ) {
            runBlocking {
                shutdownTasks.forEach { shutdownTask -> launch{ shutdownTask() } }
            }
        })
    }

    fun addShutdownTask(task: Runnable) {
        shutdownTasks.add(task::run)
    }
    fun addShutdownTask(task: ShutdownTask) {
        shutdownTasks.add(task)
    }

}