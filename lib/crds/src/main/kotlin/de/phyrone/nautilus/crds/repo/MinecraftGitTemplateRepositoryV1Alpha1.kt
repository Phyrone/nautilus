package de.phyrone.nautilus.crds.repo

import de.phyrone.nautilus.crds.NAUTILUS_GROUP
import de.phyrone.nautilus.crds.NAUTILUS_VERSION_1ALPHA1
import io.fabric8.kubernetes.api.model.Namespaced
import io.fabric8.kubernetes.client.CustomResource
import io.fabric8.kubernetes.model.annotation.Group
import io.fabric8.kubernetes.model.annotation.Kind
import io.fabric8.kubernetes.model.annotation.Version

@Group(NAUTILUS_GROUP)
@Kind(REPO_KIND)
@Version(NAUTILUS_VERSION_1ALPHA1)
class MinecraftGitTemplateRepositoryV1Alpha1 :
    CustomResource<MinecraftGitTemplateRepositoryV1Alpha1Spec, MinecraftGitTemplateRepositoryV1Alpha1Status>(),
    Namespaced {

}
