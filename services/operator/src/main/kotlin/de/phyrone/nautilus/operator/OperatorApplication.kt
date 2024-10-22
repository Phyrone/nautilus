package de.phyrone.nautilus.operator

import de.phyrone.nautilus.appcommons.MainLoop
import de.phyrone.nautilus.appcommons.Subsystem
import de.phyrone.nautilus.appcommons.appCommonsModule
import de.phyrone.nautilus.appcommons.launchSubsystems
import de.phyrone.nautilus.lib.k8s.k8sModule
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.logger.slf4jLogger
import picocli.CommandLine
import picocli.CommandLine.Command
import java.util.concurrent.Callable
import kotlin.system.exitProcess

@Command()
class OperatorApplication : Callable<Int> {
    companion object Main {
        private val applicationModule =
            module {
                includes(k8sModule, appCommonsModule)

                /* Operator Subsystems */
                single { MinecraftServiceReconcileSubSystem() } bind Subsystem::class
                single { LeaderElectionSubsystem() } bind Subsystem::class
            }

        @JvmStatic
        fun main(args: Array<String>) {
            exitProcess(CommandLine(OperatorApplication::class.java).execute(*args))
        }
    }

    override fun call(): Int {
        val koinApplication =
            startKoin {
                slf4jLogger(Level.DEBUG)
                modules(applicationModule)
            }
        launchSubsystems(koinApplication.koin)

        MainLoop.runLoop()
    }
}
