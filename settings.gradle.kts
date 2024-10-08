rootProject.name = "nautilus-cloud"

include(
    "agent",
    "agent:shared",
    "agent:common",
    "agent:paper",
    "agent:velocity",
    "provisioner",
    "lib:grpc"
)

project(":agent").projectDir = rootProject.projectDir.resolve("services/agent")
project(":agent:shared").projectDir = rootProject.projectDir.resolve("services/agent/shared")
project(":agent:paper").projectDir = rootProject.projectDir.resolve("services/agent/paper")
project(":agent:velocity").projectDir = rootProject.projectDir.resolve("services/agent/velocity")

project(":lib:grpc").projectDir = rootProject.projectDir.resolve("lib/grpc")