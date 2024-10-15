package de.phyrone.nautilus.builder

import com.google.cloud.tools.jib.api.Containerizer
import com.google.cloud.tools.jib.api.Jib
import com.google.cloud.tools.jib.api.RegistryImage
import com.google.cloud.tools.jib.api.TarImage
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.merge.ContentMergeStrategy
import picocli.CommandLine.Command
import java.io.File

@Command(
    name = "nautilus-builder",
    mixinStandardHelpOptions = true,
)
class BuilderMain {


    companion object {


        @JvmStatic
        fun main(args: Array<String>) {


            val repo = Git.open(File(""))
            repo.pull()
                .setContentMergeStrategy(ContentMergeStrategy.OURS)
                .call()
            val containerizer = Containerizer.to(RegistryImage.named(""))
            containerizer.setAllowInsecureRegistries(true)
            containerizer.setAlwaysCacheBaseImage(true)



            val container = Jib.from("azul/zulu-openjdk:11")
                .addLayer(listOf(), "/")
                .setUser("1000:1000")
                .containerize(containerizer)




        }
    }

}