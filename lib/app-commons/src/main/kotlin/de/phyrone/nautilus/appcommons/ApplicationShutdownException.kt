package de.phyrone.nautilus.appcommons

import kotlinx.coroutines.CancellationException

class ApplicationShutdownException : CancellationException("Application is shutting down")