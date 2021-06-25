package heartk.hrv

import heartk.fft.FFT
import heartk.utils.where
import org.apache.commons.math3.complex.Complex
import org.apache.commons.math3.util.MathUtils.TWO_PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.pow

/**
 * Computes frequency HRV features.
 * The implementation is an adaptation of neurokit2.
 * [https://github.com/neuropsychology/NeuroKit]
 */
object HrvFrequency {
    /**
     * Computes frequency HRV features.
     * The implementation is an adaptation of neurokit2.
     * [https://github.com/neuropsychology/NeuroKit]
     *
     * @param rrIntervals The rr intervals of a heart rate signal. This must be a signal of
     * rr Intervals at the given sampling rate. This can be archieved by interpolating the
     * rr Intervals using the indices of the corresponding peaks as positional values.
     * @param samplingRate The sampling rate of the signal.
     * @param featuresObject The object to store the computed features in. If none is specified a new one
     * is created.
     * @return The feature object with the new features.
     */
    fun getFeatures(rrIntervals: DoubleArray, samplingRate: Double, featuresObject: HRVFeatures = HRVFeatures()): HRVFeatures {
        println("Computing frequency features")
        val frequencyBands = mapOf(
            "ulf" to Pair(0.0, 0.0033),
            "vlf" to Pair(0.0033, 0.04),
            "lf" to Pair(0.04, 0.15),
            "hf" to Pair(0.15, 0.4),
            "vhf" to Pair(0.4, 0.5)
        )
        val (frequencies, power) = psd(rrIntervals, frequencyBands.values, samplingRate)

        val ulfIndices =
            frequencies.where { frequencyBands["ulf"]!!.first <= it && frequencyBands["ulf"]!!.second > it }.toList()
        val ulfPower = trapezoidal(frequencies.slice(ulfIndices), power.slice(ulfIndices))

        val vlfIndices =
            frequencies.where { frequencyBands["vlf"]!!.first <= it && frequencyBands["vlf"]!!.second > it }.toList()
        val vlfPower = trapezoidal(frequencies.slice(vlfIndices), power.slice(vlfIndices))

        val lfIndices =
            frequencies.where { frequencyBands["lf"]!!.first <= it && frequencyBands["lf"]!!.second > it }.toList()
        val lfPower = trapezoidal(frequencies.slice(lfIndices), power.slice(lfIndices))

        val hfIndices =
            frequencies.where { frequencyBands["hf"]!!.first <= it && frequencyBands["hf"]!!.second > it }.toList()
        val hfPower = trapezoidal(frequencies.slice(hfIndices), power.slice(hfIndices))

        val vhfIndices =
            frequencies.where { frequencyBands["vhf"]!!.first <= it && frequencyBands["vhf"]!!.second > it }.toList()
        val vhfPower = trapezoidal(frequencies.slice(vhfIndices), power.slice(vhfIndices))

        val totalPower = ulfPower + vlfPower + lfPower + hfPower + vhfPower
        featuresObject.LF = lfPower
        featuresObject.HF = hfPower
        featuresObject.ULF = ulfPower
        featuresObject.VLF = vlfPower
        featuresObject.VHF = vhfPower
        featuresObject.LFHF = lfPower / hfPower
        featuresObject.LFn = lfPower / totalPower
        featuresObject.HFn = hfPower / totalPower
        featuresObject.LnHF = ln(hfPower)
        return featuresObject
    }

    /**
     * Computes the psd of a given signal using the specified method.
     * Currently only the welch method is supported.
     *
     * This is an close adaptation of neurokit2 and scipy's methods to
     * compute the psd of a given signal using given frequency bands.
     * [neurokit2](https://github.com/neuropsychology/NeuroKit)
     * [scipy](https://github.com/scipy/scipy)
     *
     * @param signal The signal which is used to estimate the psd.
     * @param frequencyBands The frequency bands to be analyzed.
     * @param samplingRate The sampling rate of the signal.
     * @param method The method to estimate the psd.
     * @param maxFrequency The maximum frequency which should be returned.
     * @param normalize Specifies whether the estimated powers should be normalized by
     * dividing by the maximum power, so that the maximum power is 1.
     * @return The frequencies and power estimations of those frequencies as a pair.
     * Only those lying between the minimum frequency and maximum frequency of the specified
     * frequency bands are returned.
     */
    fun psd(
        signal: DoubleArray,
        frequencyBands: Collection<Pair<Double, Double>>,
        samplingRate: Double,
        method: String = "welch",
        maxFrequency: Double = 0.5,
        normalize: Boolean = true
    ): Pair<DoubleArray, DoubleArray> {
        if (frequencyBands.isEmpty()) return Pair(DoubleArray(0), DoubleArray(0))
        var minFrequency: Double = 0.001
        for (freq in frequencyBands) {
            minFrequency = max(freq.first, 0.001)
            if (2.div(minFrequency).times(samplingRate).toInt() <= signal.size.div(2))
                break
        }

        // Normalization of the signal
        val average = signal.average()
        val normalizedSignal = signal.map { it - average }.toDoubleArray()

        // Calculate window size
        var windowSize = 2.div(minFrequency).times(samplingRate).toInt()
        // We want to apply the window at least two times
        if (windowSize > signal.size.div(2)) windowSize = signal.size.div(2)

        val (frequencies, psd) = when (method.toLowerCase()) {
            "welch" -> psdWelch(
                signal = normalizedSignal,
                samplingRate = samplingRate,
                windowSize = windowSize,
                scaling = "density",
                windowType = "hann",
                normalize = normalize,
                returnOneSided = true
            )
            else -> throw NotImplementedError()
        }
        val frequencyIndices = frequencies.where { it in minFrequency..maxFrequency }.toList()

        return Pair(frequencies.slice(frequencyIndices).toDoubleArray(), psd.slice(frequencyIndices).toDoubleArray())
    }

    /**
     * Computes the power spectrum density using welch's method.
     *
     * @param signal The signals to compute the psd of.
     * @param samplingRate The sampling rate of the signal.
     * @param windowSize The number of samples per segment (width of the sliding window).
     * @param fftSize The length of the fft signal if a zero padded fft is required. This defaults to `2 * windowSize`.
     * The FFT is currently only supported on signals which have a length of a power of two. The windows will automatically
     * zero-padded to the next power of two to fulfill this requirement. If fftSize is set the padding will be generated
     * to the next power of two greater than the fftSize.
     * @param scaling Can be one of density and spectrum.
     * @param overlap The overlap of the sliding window. Defaults to `windowSize / 2` (floored division). The step size
     * of the sliding window is calculated as `windowSize - overlap`.
     * @param windowType The type of the window. Currently only the hann (also called hanning) window is supported.
     * @param normalize Whether the psd should be normalized by the maximum power.
     * @return A pair consisting of (computed psd, frequencies).
     */
    fun psdWelch(
        signal: DoubleArray,
        samplingRate: Double,
        windowSize: Int = 256,
        fftSize: Int = 2 * windowSize,
        scaling: String = "density",
        overlap: Int = windowSize.div(2),
        windowType: String = "hann",
        normalize: Boolean = true,
        returnOneSided: Boolean = true,
        constantBoundary: Boolean = false
    ): Pair<DoubleArray, DoubleArray> {
        val mutableSignal = signal.toMutableList()

        // Create window of correct size
        val transformWindow = when (windowType) {
            "hann" -> {
                hannWindow(windowSize)
            }
            "hannCSV" -> {
                val window = mutableListOf<Double>()
                HrvFrequency::class.java.classLoader.getResourceAsStream("window10000.csv")!!.bufferedReader()
                    .forEachLine {
                        window.add(it.toDouble())
                    }
                window.toDoubleArray()
            }
            else -> {
                throw NotImplementedError("The specified window type is not implemented.")
            }
        }

        // Compute step size of window
        val stepSize = windowSize - overlap

        // Extend signal to remove boundary effects
        if (constantBoundary) {
            val firstElement = signal.first()
            val lastElement = signal.last()
            for (i in 0 until windowSize.div(2)) {
                mutableSignal.add(0, firstElement)
            }
            for (i in 0 until windowSize.div(2)) {
                mutableSignal.add(0, lastElement)
            }
        }

        /**
         * Set scaling factor. When computing the spectral power it equals 1/sum(window)². When computing
         * the power spectral density it equals 1/sum(window²).
         */
        val scale = when (scaling) {
            "density" -> 1.0 / (samplingRate * (transformWindow.map { it.pow(2) }.sum()))
            "spectral" -> 1.0 / transformWindow.sum().pow(2)
            else -> throw IllegalArgumentException("Argument scaling must be one of 'density' or 'spectral'.")
        }

        // Create windows of the signal.
        val appliedWindow = mutableSignal.windowed(windowSize, stepSize, partialWindows = false) { window ->
            window.mapIndexed { index, value ->
                // Apply the window function to the signal
                transformWindow[index] * value
            }
        }

        val paddedWindows = appliedWindow.map {
            // Pad to a power of 2. The algorithm requires an input with a length of the power of two.
            //padZerosToPowerOfTwo(
            // Pad with zeros if min fftSize is not reached
                it.toMutableList().padEnd(fftSize - windowSize)
            //)
        }

        // Compute the fft of the window
        var ffts = paddedWindows.map { window ->
            //FastFourierTransformer(DftNormalization.STANDARD)
             //    .transform(window, TransformType.FORWARD)
                 // Compute the squared magnitude and scale. We only take the real part as we have a real valued signal.
             //    .map { it.conjugate().multiply(it).multiply(scale).real }
            val realArray = window.toDoubleArray().copyOf()
            val imagArray = DoubleArray(paddedWindows.firstOrNull()?.size ?: 0)
            FFT.transform(realArray, imagArray)
            realArray.mapIndexed { index, element ->
                 val comp = Complex(element, imagArray[index])
                 comp.conjugate().multiply(comp).multiply(scale).real
            }
        }

        // If one sided discard the second side and double the result
        if (returnOneSided) {
            /**
             * As we can only compute FFTs of a power of two the FFT window has a even size and the interesting part
             * of the FFT return value has a length of (n/2)+1
             */
            ffts = ffts.map { element ->
                val slice = element.slice(IntRange(0, element.size.div(2)))
                // Multiply by two as we omitted the negative power of the signal.
                return@map slice.mapIndexed sliceMapIndexed@{ sliceIndex, sliceElement ->
                    // Do not double the first value as it is the DC power.
                    // Do not double the last value when the size is odd as it is the unmatched nyquist frequency point.
                    if (sliceIndex == 0 || ((sliceIndex == element.size - 1) && (ffts.first().size % 2 == 0))) return@sliceMapIndexed sliceElement
                    return@sliceMapIndexed sliceElement.times(2)
                }

            }
        }

        // Calculate the frequency bins from the length of the computed signal.
        val frequencyBands = if (returnOneSided) rfftFrequencies(ffts.first().size, samplingRate)
        else fftFrequencies(ffts.first().size, samplingRate)

        // Compute the average over the windows for each bin
        var psd = ffts.fold(DoubleArray(ffts.first().size)) { current, element ->
            element.forEachIndexed { index, bin ->
                current[index] = current[index] + bin
            }
            return@fold current
        }
        val numberOfWindows = ffts.size
        psd.indices.forEach { index ->
            psd[index] = psd[index] / numberOfWindows
        }

        // Normalize by the maximum power
        if (normalize) {
            val maxPower = psd.max() ?: 1.0
            psd.indices.forEach { index ->
                psd[index] = psd[index] / maxPower
            }
        }
        return Pair(frequencyBands, psd)
    }

    /**
     * The frequency buckets of an FFT array.
     *
     * Note
     * ===============================================================
     * This is a close implementation to numpy's fftFreqs function.
     *
     * @param windowSize The size of the input signal.
     * @param samplingRate The sampling rate of the original signal in Hz.
     * @return The frequency buckets of a FFT result.
     */
    fun fftFrequencies(windowSize: Int, samplingRate: Double): DoubleArray {
        val value = 1.0.div(windowSize.div(samplingRate))
        val half = windowSize.minus(1).div(2).plus(1)
        return DoubleArray(half) {
            it.times(value)
        } + DoubleArray((windowSize).div(2)) {
            ((-windowSize).div(2).plus(it)) * value
        }
    }

    /**
     * The frequency buckets of an FFT array, without the negative frequencies.
     *
     * Note
     * ===============================================================
     * This is a close implementation to scipy's rfftFreqs function.
     *
     * @param windowSize The size of the window which has been used to compute the FFT.
     * @param samplingRate The sampling rate of the original signal.
     * @return The frequencies associated with the returned buckets.
     */
    fun rfftFrequencies(windowSize: Int, samplingRate: Double): DoubleArray {
        val factor = samplingRate / ((windowSize - 1)*2)
        return DoubleArray(windowSize) {
            it * factor
        }
    }

    private fun padZerosToPowerOfTwo(signal: MutableCollection<Double>): DoubleArray {
        val length = signal.size
        val requiredLength = nextPowerOfZero(length)
        (0 until (requiredLength - length).toInt()).forEach { _ ->
            signal.add(0.0)
        }
        return signal.toDoubleArray()
    }

    private fun MutableCollection<Double>.padEnd(length: Int, number: Double = 0.0): MutableCollection<Double> {
        for (i in 0 until length) this.add(number)
        return this
    }

    private fun nextPowerOfZero(n: Number): Double {
        var power = 2
        while (power < n.toDouble()) power = power shl 1
        return power.toDouble()
    }

    /**
     * Returns the magnitude sqrt(r²+i²) of the complex value.
     * This is an alias for [Complex.abs].
     * @see Complex.abs
     *
     * @return The magnitude of the complex number.
     */
    private fun Complex.magnitude(): Double {
        return this.abs()
    }

    /**
     * Calculates the area under the curve using the trapezoidal rule. The x values must be sorted.
     *
     * [https://ece.uwaterloo.ca/~dwharder/NumericalAnalysis/13Integration/comptrap/complete.html]
     *
     * @param x The x values
     * @param y The y values
     */
    private fun trapezoidal(x: List<Double>, y: List<Double>): Double {
        return x.indices.fold(0.0) { current, index ->
            if (index != x.size - 1) {
                return@fold current + ((x[index + 1] - x[index]) * ((y[index] + y[index + 1]) / 2.0))
            }
            return@fold current
        }
    }

    /**
     * Returns the values of an non symmetric hann window of the
     * given size.
     *
     * @param length The size of the window to be returned.
     * @return A hann window of the given size.
     */
    fun hannWindow(length: Int): DoubleArray {
        return DoubleArray(length) {
            0.5 - 0.5 * cos((TWO_PI * it) / (length))
        }
    }
}