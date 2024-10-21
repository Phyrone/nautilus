package de.phyrone.nautilus.provisioner

import org.slf4j.LoggerFactory
import picocli.CommandLine
import picocli.CommandLine.Command
import java.util.concurrent.Callable
import kotlin.system.exitProcess

@Command(
    name = "builder",
    mixinStandardHelpOptions = true,
)
class ProvisionerMain : Callable<Int> {
    override fun call(): Int {
        return 0
    }

    companion object Main {
        private val logger = LoggerFactory.getLogger(ProvisionerMain::class.java)

        @JvmStatic
        fun main(args: Array<String>) {
            exitProcess(CommandLine(ProvisionerMain::class.java).execute(*args))
        }
    }
}
