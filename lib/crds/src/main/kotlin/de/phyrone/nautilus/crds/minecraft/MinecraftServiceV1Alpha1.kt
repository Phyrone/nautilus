package de.phyrone.nautilus.crds.minecraft

import de.phyrone.nautilus.crds.NAUTILUS_GROUP
import de.phyrone.nautilus.crds.NAUTILUS_VERSION_1ALPHA1
import io.fabric8.kubernetes.api.model.Namespaced
import io.fabric8.kubernetes.client.CustomResource
import io.fabric8.kubernetes.model.annotation.Group
import io.fabric8.kubernetes.model.annotation.Kind
import io.fabric8.kubernetes.model.annotation.Version

@Group(NAUTILUS_GROUP)
@Version(NAUTILUS_VERSION_1ALPHA1)
@Kind(SERVICE_KIND)
class MinecraftServiceV1Alpha1 : CustomResource<MinecraftServiceV1Alpha1Spec, MinecraftServiceV1Alpha1Status>(),
    Namespaced