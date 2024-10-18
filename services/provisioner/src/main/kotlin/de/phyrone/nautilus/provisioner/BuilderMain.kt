package de.phyrone.nautilus.provisioner

import org.eclipse.jgit.api.Git
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import java.io.File
import java.util.concurrent.Callable
import kotlin.system.exitProcess

@Command(
    name = "builder",
    mixinStandardHelpOptions = true,
)
class BuilderMain : Callable<Int> {

    @Option(
        names = ["-d", "--data-dir"],
        description = ["The directory where the data is stored"],
        required = false
    )
    var dataDir: File? = null

    @Option(
        names = ["--on-merge"],
        required = false,
    )
    var mergeStrategy: MergeStrategy = MergeStrategy.KEEP

    enum class MergeStrategy {
        /**
         * Keep the existing file
         */
        KEEP,

        /**
         * Overwrite the existing file
         */
        OVERWRITE,

        /**
         * Clear the local repository and clone the remote repository again
         */
        FRESH
    }


    override fun call(): Int {


        return 0
    }


    companion object Main {
        @JvmStatic
        fun main(args: Array<String>) {
            val commandLine = CommandLine(BuilderMain::class.java)

            exitProcess(commandLine.execute(*args))
        }
    }


}