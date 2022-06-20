package playground

import javax.inject.Inject
import kotlin.math.*

class SignalAnalyzer @Inject constructor() {

    private val windows = HashMap<Int, FloatArray>()
    private val butters = HashMap<Int, IntArray>()

    fun butter(size: Int) = butters.getOrPut(size) {
        IntArray(size) { it.revBits(size.lenBits - 1) }
    }

    fun window(size: Int) = windows.getOrPut(size) {
        FloatArray(size) { 0.5f - 0.5f * cos(it * 2 * PI.toFloat() / size) }
    }

    fun spectrum(x: FloatArray, nfft: Int, from: Int): FloatArray {
        return rfft(x, nfft, from, window(nfft)).let {
            FloatArray(it.size) { i ->
                ln(it[i].abs + 1)
            }
        }
    }

    fun diff(x1: FloatArray, x2: FloatArray): Float {
        return x1.reduceIndexed { i, sum, _ ->
            sum + max(0f, x2[i] - x1[i])
        }
    }

    fun correlate(
        xref: FloatArray, iref: Int,
        xtrg: FloatArray, itrg: Int,
        lmin: Int, lmax: Int, win: Int
    ): FloatArray {
        return FloatArray(lmax - lmin) { i ->
            val l = lmin + i
            var c = 0f
            for (j in 0 until win) {
                c += xref[iref + j] * xtrg[itrg + j + l]
            }
            c
        }
    }

    fun corr(
        xref: FloatArray, iref: Int,
        xtrg: FloatArray, itrg: Int,
        lmin: Int, lmax: Int, win: Int
    ): Int {
        return correlate(
            xref, iref,
            xtrg, itrg,
            lmin, lmax, win
        ).withIndex().maxByOrNull { it.value }!!.index + lmin
    }

    fun rfft(x: FloatArray, nfft: Int, from: Int = 0, window: FloatArray? = null): ComplexArray {
        val res = cfft(x, nfft, from, window)
        return ComplexArray.from(
            res.re.copyOf(nfft / 2 + 1),
            res.im.copyOf(nfft / 2 + 1)
        )
    }

    fun cfft(x: FloatArray, nfft: Int, from: Int = 0, window: FloatArray? = null): ComplexArray {
        val res = ComplexArray.fromReal(x.copyOfRange(from, from + nfft))
        if (window != null) {
            for (i in 0 until nfft) {
                res[i] = res[i] * Complex(window[i], 0f)
            }
        }
        val butterfly = butter(nfft)
        for (i in 0 until nfft) {
            if (i < butterfly[i]) {
                val tmp = res[i]
                res[i] = res[butterfly[i]]
                res[butterfly[i]] = tmp
            }
        }
        var l = 2
        while (l <= nfft) {
            val e = Complex(cos(-PI * 2 / l).toFloat(), sin(-PI * 2 / l).toFloat())
            for (i in 0 until nfft step l) {
                var w = Complex(1F, 0F)
                for (j in 0 until l / 2) {
                    val u = res[i + j]
                    val v = res[i + j + l / 2] * w
                    res[i + j] = u + v
                    res[i + j + l / 2] = u - v
                    w *= e
                }
            }
            l *= 2
        }
        return res
    }

    private fun Int.revBits(len: Int): Int {
        var n = this
        var r = 0
        repeat(len) {
            r = r * 2 + n % 2
            n /= 2
        }
        return r
    }

    private val Int.lenBits: Int
        get() {
            var n = this
            var c = 0
            while (n > 0) {
                c += 1
                n /= 2
            }
            return c
        }
}
