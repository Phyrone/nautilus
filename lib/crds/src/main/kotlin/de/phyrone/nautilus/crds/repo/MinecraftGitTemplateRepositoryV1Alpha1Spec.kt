package de.phyrone.nautilus.crds.repo

import com.fasterxml.jackson.annotation.JsonTypeInfo
import de.phyrone.nautilus.crds.NoArgs

@NoArgs
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
data class MinecraftGitTemplateRepositoryV1Alpha1Spec(
    var repository: String,
    var updateIntervall: String
)