package de.phyrone.nautilus.builder

import picocli.CommandLine
import picocli.CommandLine.Command
import kotlin.system.exitProcess

@Command(
    name = "builder",
    description = ["Builds the docker image for the given service"],
)
class BuilderMain {
    class RefreshRepo()

    companion object Main {
        @JvmStatic
        fun main(args: Array<String>) {
            exitProcess(CommandLine(BuilderMain::class.java).execute(*args))
        }
    }
}
