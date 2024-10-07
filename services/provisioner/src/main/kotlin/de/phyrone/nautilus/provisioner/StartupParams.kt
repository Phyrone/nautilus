package de.phyrone.nautilus.provisioner

import picocli.CommandLine.Command
import picocli.CommandLine.Option
import picocli.CommandLine.Parameters
import java.util.concurrent.Callable

@Command(
    name = "provisioner",
    mixinStandardHelpOptions = true,
)
class StartupParams : Callable<Int> {

    /**
     * The URL of the repository to clone.
     */
    @Parameters(
        index = "0",
        paramLabel = "repo",
    )
    lateinit var repo: String

    override fun call(): Int = ProvisionerMain.runApplication(this)
}