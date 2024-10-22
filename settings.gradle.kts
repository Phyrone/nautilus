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
    "operator",
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
// project(":template-controller").projectDir = rootProject.projectDir.resolve("services/template-controller")
