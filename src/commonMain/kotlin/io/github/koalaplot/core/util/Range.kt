package io.github.koalaplot.core.util

@Suppress("UNCHECKED_CAST")
private fun <Z : Number> doubleToTypeOf(
    value: Double,
    example: Z,
): Z = when (example) {
    is Double -> {
        value as Z
    }

    is Float -> {
        value.toFloat() as Z
    }

    is Int -> {
        kotlin.math.floor(value).toInt() as Z
    }

    is Long -> {
        kotlin.math.floor(value).toLong() as Z
    }

    is Short -> {
        kotlin.math
            .floor(value)
            .toInt()
            .toShort() as Z
    }

    is Byte -> {
        kotlin.math
            .floor(value)
            .toInt()
            .toByte() as Z
    }

    else -> {
        throw UnsupportedOperationException("Unsupported numeric type: ${example::class}")
    }
}

/**
 * Linearly normalizes the value within the range between 0.0 and 1.0.
 * Values outside the range are extrapolated.
 * When extremes of the range are equal, always returns zero.
 * This is the inverse of operation [lerp].
 */
public fun <T> ClosedRange<T>.normalize(value: T): Double
    where T : Number, T : Comparable<T> {
    val r0 = start.toDouble()
    val r1 = endInclusive.toDouble()
    val size = r1 - r0
    if (size == 0.0) return 0.0
    return (value.toDouble() - r0) / size
}

/**
 * Linearly interpolates within the range by the factor t.
 * For t values beyond 0.0..1.0, linear extrapolation is done.
 * This is the inverse of operation [normalize].
 */
public fun <T> ClosedRange<T>.lerp(t: Double): T
    where T : Number, T : Comparable<T> {
    val r0 = start.toDouble()
    val r1 = endInclusive.toDouble()
    val size = r1 - r0
    return doubleToTypeOf(t * size + r0, start)
}
