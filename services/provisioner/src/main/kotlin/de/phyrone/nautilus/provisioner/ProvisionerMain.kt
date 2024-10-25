package de.phyrone.nautilus.provisioner

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.errors.RepositoryNotFoundException
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.merge.MergeStrategy
import org.eclipse.jgit.transport.URIish
import org.slf4j.LoggerFactory
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import picocli.CommandLine.Parameters
import java.io.File
import java.net.URI
import java.util.concurrent.Callable
import kotlin.system.exitProcess

@Command(
    name = "builder",
    mixinStandardHelpOptions = true,
)
class ProvisionerMain : Callable<Int> {
    @Option(
        names = ["-o", "--output"],
        required = true,
    )
    lateinit var output: File

    @Parameters(
        index = "0..*",
    )
    var urls: List<URI> = emptyList()

    override fun call(): Int {
        logger.debug("Output: {}", output.absolutePath)

        val git =
            try {
                Git.open(output)
            } catch (e: RepositoryNotFoundException) {
                Git.init()
                    .setDirectory(output)
                    .call()
            }

        val names = mutableListOf<String>()

        val templateRepos =
            urls
                .map {
                    val (url, branch) = it.destructForGit()
                    url to (branch ?: "HEAD")
                }

        val urlToName = mutableMapOf<URI, String>()
        val remotesGrouped =
            urls
                .groupBy { it.destructForGit().first }
                .mapValues { (_, v) -> v.map { it.destructForGit().second ?: "HEAD" } }

        for ((url, branches) in remotesGrouped) {
            val remoteName = url.findRemoteName(names)
            urlToName[url] = remoteName
            logger.info("Mapped $url -> $remoteName")
            val remote =
                git.remoteAdd()
                    .setName(remoteName)
                    .setUri(URIish(url.toURL()))
                    .call()
            git.pull().setRemote(remoteName)

            git.fetch()
                .setRemote(remoteName)
                .setRemoveDeletedRefs(true)
                .setRefSpecs(
                    *branches
                        .map {
                            if (it != "HEAD") {
                                "+refs/heads/$it:refs/remotes/$remoteName/$it"
                            } else {
                                // fetch main branch
                                "+HEAD:refs/remotes/$remoteName/HEAD"
                            }
                        }.toTypedArray(),
                )
                .setProgressMonitor(ProgressbarMon("Fetch [$remoteName]"))
                .call()
        }

        var templateBranchN: Ref? = null
        templateRepos.forEach { (url, branchName) ->
            val remoteName = urlToName[url] ?: error("no remote was defined for $url")
            val remoteRef = git.repository.findRef("refs/remotes/$remoteName/$branchName")
            val templateBranch =
                templateBranchN ?: git.getOrCreateBranch("template", remoteRef.name).also {
                    templateBranchN = it
                    git.checkout().setName("template")
                        .setForced(true)
                        .setForceRefUpdate(true)
                        .setStartPoint(remoteRef.name)
                        .call()
                }

            git.merge()
                .include(remoteRef)
                .setStrategy(MergeStrategy.RESOLVE)
                .setSquash(true)
                .setMessage("Merge [$remoteName/$branchName] -> [template]")
                .setProgressMonitor(ProgressbarMon("Merge [$remoteName/$branchName] -> [template]"))
                .call()
        }

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
