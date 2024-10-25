package de.phyrone.nautilus.shared

import java.util.Locale
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.primaryConstructor

// https://stackoverflow.com/a/44570679
inline infix fun <reified T : Any> T.merge(other: T): T {
    val propertiesByName = T::class.declaredMemberProperties.associateBy { it.name }
    val primaryConstructor =
        T::class.primaryConstructor
            ?: throw IllegalArgumentException("merge type must have a primary constructor")
    val args =
        primaryConstructor.parameters.associateWith { parameter ->
            val property =
                propertiesByName[parameter.name]
                    ?: throw IllegalStateException("no declared member property found with name '${parameter.name}'")
            (property.get(this) ?: property.get(other))
        }
    return primaryConstructor.callBy(args)
}

fun unreachable(): Nothing = throw IllegalStateException("This should never be reached")

fun unsupported(): Nothing = throw UnsupportedOperationException()

/**
 * Parses a memory size string to a long.
 * Supported units are:
 * - B, KB, MB, GB, TB, PB, EB (case-insensitive)
 * - KiB, MiB, GiB, TiB, PiB, EiB (case-insensitive)
 * - K, M, G, T, P, E, Z, Y (case-insensitive)
 * - If no unit is specified, the size is interpreted using the fallback unit.
 * - If [relativeTo] is specified, relative units in percent (e.g. "50%") are supported.
 * Floating point numbers are supported.
 * Examples:
 * @return The parsed memory size or null if the input is invalid
 */
fun parseMemorySize(
    size: String,
    fallbackUnit: String = "B",
    relativeTo: Long? = null,
): Long? {
    val units =
        mapOf(
            "b" to 1L,
            "kb" to 1_000L, "k" to 1_000L,
            "mb" to 1_000_000L, "m" to 1_000_000L,
            "gb" to 1_000_000_000L, "g" to 1_000_000_000L,
            "tb" to 1_000_000_000_000L, "t" to 1_000_000_000_000L,
            "pb" to 1_000_000_000_000_000L, "p" to 1_000_000_000_000_000L,
            "eb" to 1_000_000_000_000_000_000L, "e" to 1_000_000_000_000_000_000L,
            "kib" to 1_024L,
            "mib" to 1_048_576L,
            "gib" to 1_073_741_824L,
            "tib" to 1_099_511_627_776L,
            "pib" to 1_125_899_906_842_624L,
            "eib" to 1_152_921_504_606_846_976L,
        )

    val trimmed = size.trim()

    val unit = trimmed.dropWhile { it.isDigit() || it == '.' }.lowercase()
    val value = trimmed.substring(0, trimmed.length - unit.length).trim().toDoubleOrNull() ?: return null
    if (relativeTo != null && unit == "%") {
        return (relativeTo * value / 100).toLong()
    } else {
        val multiplier = units[unit] ?: units[fallbackUnit.lowercase()] ?: return null
        return (value * multiplier).toLong()
    }
}

/**
 * This function converts a memory size in bytes to a human-readable string.
 * The output is rounded to the nearest integer.
 * If [binaryUnit] is true, binary units (KiB, MiB, ...) are used instead of decimal units (KB, MB, ...).
 * If [singleCharUnit] is true, single-character units are used (B, K, M, ...) instead of the full name.
 * This has no effect on the calculation of the size.
 *
 * Examples:
 * - 1000 -> "1 KB" (decimal)
 * - 1024 -> "1 KiB" (binary)
 * - 1000 -> "1.0 K" (decimal, single char unit)
 * - 1024 -> "1.0 K" (binary, single char unit)
 *
 * Decimal points can be added using [decimalPlaces].
 * Examples:
 * - 1000 -> "1.0 KB" (decimal, 1 decimal place)
 * - 1000 -> "1.00 KB" (decimal, 2 decimal places, fixed width)
 *
 */
fun stringifyMemorySize(
    size: Long,
    binaryUnit: Boolean = false,
    singleCharUnit: Boolean = false,
    decimalPlaces: Int = 0,
    noSpace: Boolean = false,
    locale: Locale? = null,
): String {
    val units =
        when {
            singleCharUnit -> arrayOf("B", "K", "M", "G", "T", "P", "E", "Z", "Y")
            binaryUnit -> arrayOf("B", "KiB", "MiB", "GiB", "TiB", "PiB", "EiB")
            else -> arrayOf("B", "KB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB")
        }

    if (size < 0) throw IllegalArgumentException("Size must be non-negative")

    var value = size.toDouble()
    var unitIndex = 0
    val multiplier = if (binaryUnit) 1024 else 1000

    while (value >= (multiplier) && unitIndex < units.size - 1) {
        value /= multiplier
        unitIndex++
    }

    val formatString = "%.${decimalPlaces}f${if (noSpace) "%s" else " %s"}"

    return formatString.format(locale, value, units[unitIndex])
}

fun main(args: Array<String>) {
    println(
        stringifyMemorySize(
            1024 * 1024 * 1024 * 14L,
            binaryUnit = true,
            singleCharUnit = true,
            noSpace = true,
            decimalPlaces = 0,
        ),
    )
}
