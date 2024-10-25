package de.phyrone.nautilus.appcommons

import org.koin.core.component.KoinComponent

interface Subsystem : KoinComponent {
    suspend fun runSubsystem()
}