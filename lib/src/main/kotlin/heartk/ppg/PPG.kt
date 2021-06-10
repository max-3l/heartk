package heartk.ppg

import heartk.configuration.Configurator
import uk.me.berndporr.iirj.Butterworth
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.round

object PPG {
    /**
     * Cleans signal and detects RR-Peaks on a PPG signal.
     * Uses double Butterworth filter (normal + reversed) to account for introduced delay.
     *
     * @param signal The PPG signal as DoubleArray
     * @param sampling_rate The sampling rate of the PPG signal as Double
     *
     * @return a DoubleArray containing the Peaks
     */
    fun processSignal(signal: FloatArray, sampling_rate: Double): BooleanArray {
        val filteredSignal = filterSignal(filterSignal(signal, sampling_rate).reversedArray(), sampling_rate).reversedArray()
        return detectPeaks(filteredSignal, sampling_rate)
    }

    /**
     * Filters a given signal using a butterworth filter.
     *
     * @param signal The signal to be filtered
     * @param sampling_rate The sampling rate of the signal
     *
     * @return The filtered signal
     */
    fun filterSignal(signal: DoubleArray, sampling_rate: Double): DoubleArray {
        val order = 3
        val filter = Butterworth()
        val (freqs, type) = Configurator.signalFilterSanitize(
            lowcut = 0.5,
            highcut = 8.0,
            sampling_rate = sampling_rate
        )
        when (type) {
            ("lowpass") -> {
                filter.lowPass(order, sampling_rate, freqs.first())
            }
            ("highpass") -> {
                filter.highPass(order, sampling_rate, freqs.first())
            }
            ("bandpass") -> {
                filter.bandPass(
                    order,
                    sampling_rate,
                    (freqs[0] + freqs[1]) / 2,
                    abs(freqs[0] - freqs[1])
                )
            }
            ("bandstop") -> {
                filter.bandStop(
                    order,
                    sampling_rate,
                    (freqs[0] + freqs[1]) / 2,
                    abs(freqs[0] - freqs[1])
                )
            }
        }
        return signal.map { filter.filter(it) }.toDoubleArray()
    }

    /**
     * Implementation of Elgendi M, Norton I, Brearley M, Abbott D, Schuurmans D (2013) Systolic Peak Detection in
     * Acceleration Photoplethysmograms Measured from Emergency Responders in Tropical Conditions. PLoS ONE 8(10): e76585.
     * doi:10.1371/journal.pone.0076585.
     *
     * All tune-able parameters are specified as keyword arguments. `signal` must be the bandpass-filtered raw PPG
     * with a lowcut of 0.5 Hz, a highcut of 8 Hz.
     *
     * @param signal The PPG signal to find peaks from
     * @param peakWindow Window size of the first moving average (ms)
     * @param beatWindow Window size of the second moving average (ms)
     * @param beta Offset of the beat
     */
    fun detectPeaks(
        signal: DoubleArray,
        sampling_rate: Double,
        peakWindow: Double = 0.111,
        beatWindow: Double = 0.667,
        beta: Double = 0.02
    ): BooleanArray {
        var clippedSquaredSignal = signal.map { el -> max(el, 0.0F).pow(2) }.toFloatArray()

        var peakAverages = movingAverage(clippedSquaredSignal, round(sampling_rate * peakWindow).toInt())
        var beatAverages = movingAverage(clippedSquaredSignal, round(sampling_rate * beatWindow).toInt())

        val offsetLevel: Double = clippedSquaredSignal.average() * beta
        val blocksOfInterest = peakAverages.mapIndexed { index: Int, value: Double ->
            if (value > beatAverages[index] + offsetLevel) return@mapIndexed 0.1F
            return@mapIndexed 0
        }

        // array of systolic peaks
        val systolicPeaks = BooleanArray(clippedSquaredSignal.size) { false }

        // computes the maximum in blocks of interests wider than the peaksWindow threshold
        val threshold = peakWindow * sampling_rate
        var interestStart = 0
        for (index in (0 until (blocksOfInterest.size))) {
            if (blocksOfInterest[index] == 0) {
                if (((index - 1) - interestStart + 1) > threshold) {
                    systolicPeaks[clippedSquaredSignal.sliceMax(interestStart, index)] = true
                }
                interestStart = index
            }
        }

        return systolicPeaks
    }

    /**
     * Computes a moving average over a fixed window size using the online
     * approximation. This runs faster than the standard moving average.
     *
     * Discards (windowSize/2) - 1 (floored) at the beginning and the end of the signal.
     *
     * If the window size is even the window size gets subtracted by one.
     *
     * @param signal The signal to compute the moving average on.
     * @param windowSize The window size (samples) to compute the average of.
     *
     * @return The signal with the moving average
     */
    fun movingAverageOnline(signal: DoubleArray, windowSize: Int): DoubleArray {
        val oddWindowSize = windowSize + ((windowSize + 1) % 2)
        val discardSize = floor((oddWindowSize / 2.0)).toInt() - 1
        val averaged = DoubleArray(signal.size - (2 * discardSize))

        // Compute first average
        averaged[0] = signal.sliceAverage(discardSize - discardSize, discardSize + discardSize)

        // Use online algorithm
        for (index in (discardSize + 1) until (signal.size - discardSize)) {
            averaged[index] = averaged[index - 1] +
                    (signal[index] / oddWindowSize) +
                    (signal[index - oddWindowSize] / oddWindowSize)
        }
        return averaged
    }

    /**
     * Computes a moving average over a fixed window size.
     *
     * Discards (windowSize/2) - 1 (floored) at the beginning and the end of the signal.
     *
     * If the window size is even the window size gets subtracted by one.
     *
     * @param signal The signal to compute the moving average on.
     * @param windowSize The window size (samples) to compute the average of.
     *
     * @return The signal with the moving average
     */
    fun movingAverageDiscard(signal: DoubleArray, windowSize: Int): DoubleArray {
        val oddWindowSize = windowSize + ((windowSize + 1) % 2)
        val discardSize = floor((oddWindowSize / 2.0)).toInt() - 1
        val averaged = DoubleArray(signal.size - (2 * discardSize))

        // Use slice average for every step
        for (index in (discardSize until (signal.size - discardSize))) {
            averaged[index - discardSize] = signal.sliceAverage(index - discardSize, index + discardSize)
        }
        return averaged
    }

    /**
     * Computing an average over a signal using a moving window.
     * Reduces the window size on the start and end.
     *
     * When the windowSize is even the next odd number is taken
     * as windowSize.
     *
     * @param signal The signal to process the average of.
     * @param windowSize The window size to be computed.
     */
    fun movingAverage(signal: DoubleArray, windowSize: Int): DoubleArray {
        val oddWindowSize = windowSize + ((windowSize + 1) % 2)
        var firstIndex = 0
        var lastIndex = (oddWindowSize / 2) - 1
        var currentSum = 0.0

        for (index in (0 until (oddWindowSize / 2) - 1)) {
            currentSum += signal[index]
        }

        val averaged = DoubleArray(signal.size)
        for (index in (signal.indices)) {
            currentSum += signal[lastIndex]
            if (firstIndex > 0) currentSum -= signal[firstIndex - 1]
            averaged[index] = currentSum / (lastIndex - firstIndex + 1)
            if (lastIndex < (signal.size - 1)) lastIndex++
            if ((lastIndex - firstIndex) > oddWindowSize) firstIndex++
        }
        return averaged
    }

    /**
     * Computing an average over a signal using a moving window.
     * Extends the array to the left and right by windowSize/2 elements
     * when computing the average to reduce side effects.
     *
     * @param signal The signal to process the average of.
     * @param windowSize The window size to be computed.
     */
    fun movingAverageExtend(signal: DoubleArray, windowSize: Int): DoubleArray {
        val oddWindowSize = windowSize + ((windowSize + 1) % 2)
        val halfLength = ceil(oddWindowSize / 2.0).toInt()
        val extensionLeft = DoubleArray(halfLength) { signal[0] }
        val extensionRight = DoubleArray(halfLength) { signal[1] }
        val extendedSignal = extensionLeft + signal + extensionRight
        var lastIndex = oddWindowSize - 1
        var currentSum = 0.0

        for (index in (0 until oddWindowSize - 1)) {
            currentSum += signal[index]
        }

        val averaged = DoubleArray(signal.size)
        for ((firstIndex, index) in (halfLength until signal.size + halfLength).withIndex()) {
            currentSum += extendedSignal[lastIndex]
            if (firstIndex > 0) currentSum -= extendedSignal[firstIndex - 1]
            averaged[index] = currentSum / (lastIndex - firstIndex + 1)
            lastIndex++
        }
        return averaged.sliceArray(halfLength until signal.size + halfLength)
    }

    /**
     * Computes the average over a slice of an Double array.
     *
     * The end index is **included**.
     *
     * @param start The index to begin the slice.
     * @param end The index to end the slice.
     *
     * @return The average of the slice.
     */
    fun DoubleArray.sliceAverage(start: Int, end: Int): Double {
        var sum = 0.0
        for (index in start until (end + 1)) {
            sum += this[index]
        }
        return sum / (end + 1 - start)
    }

    /**
     * Returns the index of the maximum of a slice.
     *
     * @param start The start index of the slice.
     * @param end The end index of the slice (inclusive).
     * @return The index of the maximum element between the start index and the end index.
     */
    fun DoubleArray.sliceMax(start: Int, end: Int): Int {
        var maxIndex = start
        for (index in (start..end)) {
            if (this[index] > this[maxIndex]) maxIndex = index
        }
        return maxIndex
    }
}