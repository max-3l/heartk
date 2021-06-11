package heartk.FFT

/*
 * Free FFT and convolution (Java)
 *
 * Copyright (c) 2020 Project Nayuki. (MIT License)
 * https://www.nayuki.io/page/free-small-fft-in-multiple-languages
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 * - The above copyright notice and this permission notice shall be included in
 *   all copies or substantial portions of the Software.
 * - The Software is provided "as is", without warranty of any kind, express or
 *   implied, including but not limited to the warranties of merchantability,
 *   fitness for a particular purpose and noninfringement. In no event shall the
 *   authors or copyright holders be liable for any claim, damages or other
 *   liability, whether in an action of contract, tort or otherwise, arising from,
 *   out of or in connection with the Software or the use or other dealings in the
 *   Software.
 */

/**
 * This file has been automatically converted to kotlin using IntelliJ from Jetbrains.
 */

object FFT {
    /*
     * Computes the discrete Fourier transform (DFT) of the given complex vector, storing the result back into the vector.
     * The vector can have any length. This is a wrapper function.
     */
    fun transform(real: DoubleArray, imag: DoubleArray) {
        val n = real.size
        require(n == imag.size) { "Mismatched lengths" }
        if (n == 0) return else if (n and n - 1 == 0) // Is power of 2
            transformRadix2(real, imag) else  // More complicated algorithm for arbitrary sizes
            transformBluestein(real, imag)
    }

    /*
     * Computes the inverse discrete Fourier transform (IDFT) of the given complex vector, storing the result back into the vector.
     * The vector can have any length. This is a wrapper function. This transform does not perform scaling, so the inverse is not a true inverse.
     */
    fun inverseTransform(real: DoubleArray, imag: DoubleArray) {
        transform(imag, real)
    }

    /*
     * Computes the discrete Fourier transform (DFT) of the given complex vector, storing the result back into the vector.
     * The vector's length must be a power of 2. Uses the Cooley-Tukey decimation-in-time radix-2 algorithm.
     */
    fun transformRadix2(real: DoubleArray, imag: DoubleArray) {
        // Length variables
        val n = real.size
        require(n == imag.size) { "Mismatched lengths" }
        val levels = 31 - Integer.numberOfLeadingZeros(n) // Equal to floor(log2(n))
        require(1 shl levels == n) { "Length is not a power of 2" }

        // Trigonometric tables
        val cosTable = DoubleArray(n / 2)
        val sinTable = DoubleArray(n / 2)
        for (i in 0 until n / 2) {
            cosTable[i] = Math.cos(2 * Math.PI * i / n)
            sinTable[i] = Math.sin(2 * Math.PI * i / n)
        }

        // Bit-reversed addressing permutation
        for (i in 0 until n) {
            val j = Integer.reverse(i) ushr 32 - levels
            if (j > i) {
                var temp = real[i]
                real[i] = real[j]
                real[j] = temp
                temp = imag[i]
                imag[i] = imag[j]
                imag[j] = temp
            }
        }

        // Cooley-Tukey decimation-in-time radix-2 FFT
        var size = 2
        while (size <= n) {
            val halfsize = size / 2
            val tablestep = n / size
            var i = 0
            while (i < n) {
                var j = i
                var k = 0
                while (j < i + halfsize) {
                    val l = j + halfsize
                    val tpre = real[l] * cosTable[k] + imag[l] * sinTable[k]
                    val tpim = -real[l] * sinTable[k] + imag[l] * cosTable[k]
                    real[l] = real[j] - tpre
                    imag[l] = imag[j] - tpim
                    real[j] += tpre
                    imag[j] += tpim
                    j++
                    k += tablestep
                }
                i += size
            }
            if (size == n) // Prevent overflow in 'size *= 2'
                break
            size *= 2
        }
    }

    /*
     * Computes the discrete Fourier transform (DFT) of the given complex vector, storing the result back into the vector.
     * The vector can have any length. This requires the convolution function, which in turn requires the radix-2 FFT function.
     * Uses Bluestein's chirp z-transform algorithm.
     */
    fun transformBluestein(real: DoubleArray, imag: DoubleArray) {
        // Find a power-of-2 convolution length m such that m >= n * 2 + 1
        val n = real.size
        require(n == imag.size) { "Mismatched lengths" }
        require(n < 0x20000000) { "Array too large" }
        val m = Integer.highestOneBit(n) * 4

        // Trigonometric tables
        val cosTable = DoubleArray(n)
        val sinTable = DoubleArray(n)
        for (i in 0 until n) {
            val j = (i.toLong() * i % (n * 2)).toInt() // This is more accurate than j = i * i
            cosTable[i] = Math.cos(Math.PI * j / n)
            sinTable[i] = Math.sin(Math.PI * j / n)
        }

        // Temporary vectors and preprocessing
        val areal = DoubleArray(m)
        val aimag = DoubleArray(m)
        for (i in 0 until n) {
            areal[i] = real[i] * cosTable[i] + imag[i] * sinTable[i]
            aimag[i] = -real[i] * sinTable[i] + imag[i] * cosTable[i]
        }
        val breal = DoubleArray(m)
        val bimag = DoubleArray(m)
        breal[0] = cosTable[0]
        bimag[0] = sinTable[0]
        for (i in 1 until n) {
            breal[m - i] = cosTable[i]
            breal[i] = breal[m - i]
            bimag[m - i] = sinTable[i]
            bimag[i] = bimag[m - i]
        }

        // Convolution
        val creal = DoubleArray(m)
        val cimag = DoubleArray(m)
        convolve(areal, aimag, breal, bimag, creal, cimag)

        // Postprocessing
        for (i in 0 until n) {
            real[i] = creal[i] * cosTable[i] + cimag[i] * sinTable[i]
            imag[i] = -creal[i] * sinTable[i] + cimag[i] * cosTable[i]
        }
    }

    /*
     * Computes the circular convolution of the given real vectors. Each vector's length must be the same.
     */
    fun convolve(xvec: DoubleArray, yvec: DoubleArray, outvec: DoubleArray) {
        val n = xvec.size
        require(!(n != yvec.size || n != outvec.size)) { "Mismatched lengths" }
        convolve(xvec, DoubleArray(n), yvec, DoubleArray(n), outvec, DoubleArray(n))
    }

    /*
     * Computes the circular convolution of the given complex vectors. Each vector's length must be the same.
     */
    fun convolve(
        xreal: DoubleArray, ximag: DoubleArray,
        yreal: DoubleArray, yimag: DoubleArray, outreal: DoubleArray, outimag: DoubleArray
    ) {
        var xreal = xreal
        var ximag = ximag
        var yreal = yreal
        var yimag = yimag
        val n = xreal.size
        require(!(n != ximag.size || n != yreal.size || n != yimag.size || n != outreal.size || n != outimag.size)) { "Mismatched lengths" }
        xreal = xreal.clone()
        ximag = ximag.clone()
        yreal = yreal.clone()
        yimag = yimag.clone()
        transform(xreal, ximag)
        transform(yreal, yimag)
        for (i in 0 until n) {
            val temp = xreal[i] * yreal[i] - ximag[i] * yimag[i]
            ximag[i] = ximag[i] * yreal[i] + xreal[i] * yimag[i]
            xreal[i] = temp
        }
        inverseTransform(xreal, ximag)
        for (i in 0 until n) {  // Scaling (because this FFT implementation omits it)
            outreal[i] = xreal[i] / n
            outimag[i] = ximag[i] / n
        }
    }
}