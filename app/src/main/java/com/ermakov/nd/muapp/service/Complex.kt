package playground

import kotlin.math.pow
import kotlin.math.sqrt

class Complex(val re: Float, val im: Float) {
    operator fun times(other: Complex) = Complex(
        re * other.re - im * other.im,
        re * other.im + im * other.re
    )

    operator fun plus(other: Complex) = Complex(
        re + other.re,
        im + other.im
    )

    operator fun minus(other: Complex) = Complex(
        re - other.re,
        im - other.im
    )

    val abs
        get() = sqrt(re.pow(2) + im.pow(2))
}

class ComplexArray private constructor(
    val re: FloatArray,
    val im: FloatArray
) {
    val size: Int

    init {
        check(re.size == im.size)
        size = re.size
    }

    constructor(size: Int) : this(FloatArray(size), FloatArray(size))

    operator fun get(index: Int): Complex {
        return Complex(re[index], im[index])
    }

    operator fun set(index: Int, value: Complex) {
        re[index] = value.re
        im[index] = value.im
    }

    companion object {
        fun from(re: FloatArray, im: FloatArray) = ComplexArray(re, im)
        fun fromReal(re: FloatArray) = from(re, FloatArray(re.size))
    }
}

fun ComplexArray.map(mapper: (Complex) -> Float): FloatArray {
    val res = FloatArray(size)
    for (i in res.indices) {
        res[i] = mapper.invoke(this[i])
    }
    return res
}

fun FloatArray.map(mapper: (Int, Float) -> Float): FloatArray {
    val res = FloatArray(size)
    for (i in res.indices) {
        res[i] = mapper.invoke(i, this[i])
    }
    return res
}

fun FloatArray.map(mapper: (Float) -> Float): FloatArray {
    return map { _, v -> mapper.invoke(v) }
}

operator fun FloatArray.plus(other: FloatArray): FloatArray {
    check(size == other.size)
    return map { i, v -> v + other[i] }
}

operator fun FloatArray.minus(other: FloatArray): FloatArray {
    check(size == other.size)
    return map { i, v -> v - other[i] }
}
