package de.phyrone.nautilus.appcommons

import kotlinx.coroutines.*
import org.koin.core.Koin
import org.koin.dsl.module
import org.slf4j.LoggerFactory
import java.io.File
import java.util.UUID
import kotlin.system.exitProcess

private val logger = LoggerFactory.getLogger("Subsystems")
fun launchSubsystems(koin: Koin) {

    val coroutineScope = koin.get<CoroutineScope>()
    coroutineScope.launch {
        try {
            val subsystems = koin.getAll<Subsystem>()
            logger.info("Starting ${subsystems.size} subsystems...")
            val runningSubsystems = subsystems.map { subsystem -> launch { subsystem.runSubsystem() } }
            logger.info("All subsystems started")
            runningSubsystems.joinAll()
            logger.info("All subsystems completed")
        } catch (e: Throwable) {
            logger.error("Error in subsystem", e)
            exitProcess(1)
        }
        exitProcess(0)
    }
}

val appCommonsModule = module {
    single {
        CoroutineScope(Dispatchers.Main).also {
            ShutdownHandler.addShutdownTask {
                it.cancel(ApplicationShutdownException())
            }
        }
    }
    single { UUID.randomUUID() }
}