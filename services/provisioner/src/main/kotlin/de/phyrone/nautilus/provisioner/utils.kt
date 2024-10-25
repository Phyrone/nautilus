package de.phyrone.nautilus.provisioner

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref
import java.net.URI

private val NON_ALPHANUMERIC = Regex("[^a-zA-Z0-9-]")

fun URI.destructForGit(): Pair<URI, String?> =
    Pair(
        URI(
            this.scheme,
            this.userInfo,
            this.host,
            this.port,
            this.path,
            this.query,
            null,
        ),
        this.fragment,
    )

fun URI.withoutCredentials(): URI =
    URI(
        this.scheme,
        null,
        this.host,
        this.port,
        this.path,
        this.query,
        this.fragment,
    )

fun URI.bestEffortName(): String? {
    val pathname = this.path.split("/").lastOrNull() ?: return null
    // remote .git ending
    val pathnameWithoutGit = pathname.removeSuffix(".git")
    // strip non alphanumeric characters
    val name = pathnameWithoutGit.replace(NON_ALPHANUMERIC, "")
    return name
}

fun URI.findRemoteName(names: MutableList<String>): String {
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
