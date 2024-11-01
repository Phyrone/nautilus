rootProject.name = "nautilus-cloud"

include(
    "agent",
    "agent:shared",
    "agent:common",
    "agent:paper",
    "agent:velocity",
    "agent:bungee",
    "lib:grpc",
    "lib:crds",
    "lib:k8s",
    "lib:app-commons",
    "lib:shared",
    "lib:common",
    "provisioner",
    "builder",
    "builder:shared",
    "operator",
    "lib:api-client",
    "lib:api-client:common",
    "lib:api-client:paper",
    "lib:api-client:spigot",
    "lib:api-client:purpur",
    "lib:api-client:modrinth",
)

project(":agent").projectDir = rootProject.projectDir.resolve("services/agent")
project(":agent:shared").projectDir = rootProject.projectDir.resolve("services/agent/shared")
project(":agent:paper").projectDir = rootProject.projectDir.resolve("services/agent/paper")
project(":agent:velocity").projectDir = rootProject.projectDir.resolve("services/agent/velocity")
project(":agent:bungee").projectDir = rootProject.projectDir.resolve("services/agent/bungee")

project(":lib:grpc").projectDir = rootProject.projectDir.resolve("lib/grpc")
project(":lib:crds").projectDir = rootProject.projectDir.resolve("lib/crds")
project(":lib:k8s").projectDir = rootProject.projectDir.resolve("lib/k8s")
project(":lib:app-commons").projectDir = rootProject.projectDir.resolve("lib/app-commons")
project(":lib:shared").projectDir = rootProject.projectDir.resolve("lib/shared")

project(":provisioner").projectDir = rootProject.projectDir.resolve("services/provisioner")
project(":operator").projectDir = rootProject.projectDir.resolve("services/operator")
project(":builder").projectDir = rootProject.projectDir.resolve("services/builder")
project(":builder:shared").projectDir = rootProject.projectDir.resolve("services/builder/shared")
// project(":template-controller").projectDir = rootProject.projectDir.resolve("services/template-controller")

// API Client
project(":lib:api-client").projectDir = rootProject.projectDir.resolve("lib/api-client")
project(":lib:api-client:common").projectDir = rootProject.projectDir.resolve("lib/api-client/common")
project(":lib:api-client:paper").projectDir = rootProject.projectDir.resolve("lib/api-client/paper")
project(":lib:api-client:spigot").projectDir = rootProject.projectDir.resolve("lib/api-client/spigot")
project(":lib:api-client:purpur").projectDir = rootProject.projectDir.resolve("lib/api-client/purpur")
project(":lib:api-client:modrinth").projectDir = rootProject.projectDir.resolve("lib/api-client/modrinth")
