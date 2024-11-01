package de.phyrone.nautilus.provisioner

import org.eclipse.jgit.api.ArchiveCommand
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.archive.TarFormat
import org.eclipse.jgit.archive.TgzFormat
import org.eclipse.jgit.archive.ZipFormat
import org.eclipse.jgit.errors.RepositoryNotFoundException
import org.slf4j.LoggerFactory
import picocli.CommandLine
import picocli.CommandLine.*
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
        defaultValue = "\${env:NAUTILUS_PROVISIONER_OUTPUT:-./out}",
    )
    var output: File = File("./out")

    @Option(
        names = ["-w", "--work-dir"],
        required = true,
        defaultValue = "\${env:NAUTILUS_PROVISIONER_WORK_DIR:-./}",
    )
    var workDir: File = File(".")

    @ArgGroup(exclusive = false, multiplicity = "0..", heading = "Repositories:")
    var repos: List<Repo>? = null

    class Repo {
        @Parameters(index = "0", arity = "1", description = ["Name of the repository"])
        lateinit var name: String

        @Option(names = ["-r", "--from", "--uri", "--url", "--repo"], required = true)
        lateinit var url: URI

        @Option(names = ["-b", "--branch"])
        var branch: String? = null

        @Option(names = ["-p", "--path"])
        var paths: List<File>? = null

        // TODO credentials
    }

    override fun call(): Int {
        ArchiveCommand.registerFormat("zip", ZipFormat())
        ArchiveCommand.registerFormat("tar", TarFormat())
        ArchiveCommand.registerFormat("tgz", TgzFormat())
        repos?.forEach {
            it.name = it.name.replace(NO_SPECIAL_CHARACTERS, "")
        }
        logger.debug("Output: {}", output.absolutePath)
        logger.debug("WorkDir: {}", workDir.absolutePath)

        val git =
            try {
                Git.open(workDir)
            } catch (e: RepositoryNotFoundException) {
                Git.init()
                    .setGitDir(workDir)
                    .setBare(true)
                    .call()
            }
        // TODO maybe do something more
        val repos = repos ?: return 0
        output.mkdirs()
        val remotes = git.upsertRemotes(repos.map { it.url })
        for (repo in repos) {
            logger.info("Provisioning {}", repo.name)
            val remote = remotes[repo.url] ?: error("no remote was defined for ${repo.url} but it should be")
            val remoteRef = repo.branch?.let { "refs/heads/$it" } ?: "HEAD"
            val localRef = "refs/heads/${repo.name}"
            git.fetch()
                .setRemoveDeletedRefs(true)
                .setRemote(remote)
                .setRefSpecs("+$remoteRef:$localRef")
                .setProgressMonitor(ProgressbarMon("Fetch [${repo.name}]"))
                .call()
            val tree = git.repository.resolve(localRef)
            val archiveFile = output.resolve(File("${repo.name}.zip"))
            archiveFile.outputStream().use { fileStream ->
                git.archive()
                    .setFormatOptions(
                        mapOf(
                            // dont need compression here
                            "compression-level" to 0,
                        ),
                    )
                    .setTree(tree)
                    .setOutputStream(fileStream)
                    .setFilename(archiveFile.name)
                    .also {
                        val paths =
                            repo.paths
                                ?.takeUnless { it.isEmpty() }
                                ?.map { it.path }?.toTypedArray()
                        if (paths != null) {
                            it.setPaths(*paths)
                        }
                    }
                    .call()
            }
        }

        return 0
    }

    companion object Main {
        private val NO_SPECIAL_CHARACTERS = Regex("[^a-zA-Z0-9-]")
        private val logger = LoggerFactory.getLogger(ProvisionerMain::class.java)

        @JvmStatic
        fun main(args: Array<String>) {
            exitProcess(CommandLine(ProvisionerMain::class.java).execute(*args))
        }
    }
}
