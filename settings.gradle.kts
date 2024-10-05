rootProject.name = "nautilus-cloud"

include(
    "agent",
    "agent:common",
    "agent:paper"
)

project(":agent").projectDir = rootProject.projectDir.resolve("services/agent")
project(":agent:common").projectDir = rootProject.projectDir.resolve("services/agent/common")
project(":agent:paper").projectDir = rootProject.projectDir.resolve("services/agent/paper")