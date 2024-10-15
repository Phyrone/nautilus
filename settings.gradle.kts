rootProject.name = "nautilus-cloud"

include(
    "agent",
    "agent:shared",
    "agent:common",
    "agent:paper",
    "agent:velocity",
    "lib:grpc",
    "builder",
    "sub:mc-image-helper"
)

project(":agent").projectDir = rootProject.projectDir.resolve("services/agent")
project(":agent:shared").projectDir = rootProject.projectDir.resolve("services/agent/shared")
project(":agent:paper").projectDir = rootProject.projectDir.resolve("services/agent/paper")
project(":agent:velocity").projectDir = rootProject.projectDir.resolve("services/agent/velocity")

project(":lib:grpc").projectDir = rootProject.projectDir.resolve("lib/grpc")

project(":builder").projectDir = rootProject.projectDir.resolve("services/builder")

project(":sub:mc-image-helper").projectDir = rootProject.projectDir.resolve("ref/mc-image-helper")