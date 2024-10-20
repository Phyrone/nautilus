rootProject.name = "nautilus-cloud"

include(
    "agent",
    "agent:shared",
    "agent:common",
    "agent:paper",
    "agent:velocity",
    "lib:grpc",
    "lib:crds",
    "lib:shared",
    "lib:common",
    "provisioner",
    "operator",
)

project(":agent").projectDir = rootProject.projectDir.resolve("services/agent")
project(":agent:shared").projectDir = rootProject.projectDir.resolve("services/agent/shared")
project(":agent:paper").projectDir = rootProject.projectDir.resolve("services/agent/paper")
project(":agent:velocity").projectDir = rootProject.projectDir.resolve("services/agent/velocity")

project(":lib:grpc").projectDir = rootProject.projectDir.resolve("lib/grpc")
project(":lib:crds").projectDir = rootProject.projectDir.resolve("lib/crds")
project(":lib:shared").projectDir = rootProject.projectDir.resolve("lib/shared")

project(":provisioner").projectDir = rootProject.projectDir.resolve("services/provisioner")
project(":operator").projectDir = rootProject.projectDir.resolve("services/operator")
// project(":template-controller").projectDir = rootProject.projectDir.resolve("services/template-controller")
