package de.phyrone.nautilus.provisioner

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.transport.URIish
import java.net.URI
import java.net.URL

private val NON_ALPHANUMERIC = Regex("[^a-zA-Z0-9-]")

fun URI.withoutCredentials() =
    URI(
        this.scheme,
        null,
        this.host,
        this.port,
        this.path,
        this.query,
        this.fragment,
    )

fun URI.containsCredentials() = this.userInfo != null

fun URL.containsCredentials() = this.userInfo != null

fun URI.bestEffortName(): String? {
    val pathname = this.path.split("/").lastOrNull() ?: return null
    // remote .git ending
    val pathnameWithoutGit = pathname.removeSuffix(".git")
    // strip non alphanumeric characters
    val name = pathnameWithoutGit.replace(NON_ALPHANUMERIC, "")
    return name
}

fun URI.findRemoteName(names: MutableSet<String>): String {
    val name = bestEffortName() ?: "remote"
    if (name !in names) {
        return name
    }
    var i = 1
    while (true) {
        val newName = "$name-$i"
        if (newName !in names) {
            return newName
        }
        i++
    }
}

fun Git.getOrCreateBranch(
    name: String,
    start: String? = null,
): Ref =
    repository.findRef(name) ?: branchCreate()
        .setName(name)
        .setStartPoint(start)
        .call()

fun Git.upsertRemotes(remotes: List<URI>): Map<URI, String> {
    val uniqueRemotes = remotes.distinct()
    val usedNames = mutableSetOf<String>()
    val urlToName =
        uniqueRemotes
            .associateWith { it.findRemoteName(usedNames) }
    for ((url, remoteName) in urlToName) {
        remoteAdd()
            .setName(remoteName)
            .setUri(URIish(url.toURL()))
            .call()
    }
    return urlToName
}
