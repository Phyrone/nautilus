# Terminologie

## Kubernetes

### Minecraft Service and Pod

Each minecraft service runs in a pod. A pod is the smallest deployable unit in Kubernetes.
For more look at [Kubernetes Pods](https://kubernetes.io/docs/concepts/workloads/pods/pod/)

### Agent

The agent is a service that runs alongside the minecraft service typically in the same process in form of a plugin, mod
or other kind of extension. The agent is responsible for the communication between the minecraft service and the cloud.

### Minecraft Service
A minecraft service is the abstract representation of any kind of operating software that runs inside the cloud related to
letting players play minecraft. This includes servers, proxies and similar.
Examples are Spigot, BungeeCord, Velocity, Waterfall, Paper, etc.

