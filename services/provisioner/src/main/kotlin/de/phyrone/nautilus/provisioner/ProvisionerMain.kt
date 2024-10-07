package de.phyrone.nautilus.provisioner

import picocli.CommandLine
import kotlin.system.exitProcess

object ProvisionerMain{

    @JvmStatic
    fun main(args: Array<String>) {
        val cli = CommandLine(StartupParams::class.java)

        exitProcess(cli.execute(*args))
    }

    internal fun runApplication(params:StartupParams) : Int{


        return 0
    }

}