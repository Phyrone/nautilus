package de.phyrone.nautilus.shared

import java.io.ByteArrayOutputStream

class StringLineOutputStream(private val consumer:(String) -> Unit):ByteArrayOutputStream() {

    private var buffer = ""
    override fun flush() {
        buffer += toString(Charsets.UTF_8)
        super.reset()
        val lines = buffer.split("\n")
        buffer = lines.last()
        lines.dropLast(1).forEach(consumer)
    }
}