package heartk.eda

import heartk.utils.diff
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
import org.apache.commons.math3.stat.descriptive.rank.Median
import uk.me.berndporr.iirj.Butterworth

object EdaPhasicTonicFeatures {
    fun getFeatures(
        edaSignal: DoubleArray,
        samplingFrequency: Double,
        features: EDAFeatures = EDAFeatures()
    ): EDAFeatures {
        val cleanEDA = cleanSignal(edaSignal, samplingFrequency)
        val phasicEDA = phasicSignal(cleanEDA, samplingFrequency)
        val tonicEDA = tonicSignal(cleanEDA, samplingFrequency)
        val peaks = filterPeaks(findPeaks(phasicEDA), samplingFrequency)

        features.peaksEda = peaks.peaks.size.toDouble()
        features.meanPhasicEda = phasicEDA.average()
        val phasicEdaStats = DescriptiveStatistics(phasicEDA.toDoubleArray())
        features.kurtosisPhasicEda = phasicEdaStats.kurtosis
        features.skewnessPhasicEda = phasicEdaStats.skewness
        features.medianPhasicEda = Median().evaluate(phasicEDA.toDoubleArray())
        features.stdPhasicEda = phasicEdaStats.standardDeviation
        features.varPhasicEda = phasicEdaStats.variance
        features.maxPhasicEda = phasicEdaStats.max
        features.minPhasicEda = phasicEdaStats.min
        features.rangePhasicEda = phasicEdaStats.max - phasicEdaStats.min

        features.meanTonicEda = tonicEDA.average()
        val tonicEdaStats = DescriptiveStatistics(tonicEDA.toDoubleArray())
        features.kurtosisTonicEda = tonicEdaStats.kurtosis
        features.skewnessTonicEda = tonicEdaStats.skewness
        features.medianTonicEda = Median().evaluate(tonicEDA.toDoubleArray())
        features.stdTonicEda = tonicEdaStats.standardDeviation
        features.varTonicEda = tonicEdaStats.variance
        features.maxTonicEda = tonicEdaStats.max
        features.minTonicEda = tonicEdaStats.min
        features.rangeTonicEda = tonicEdaStats.max - tonicEdaStats.min
        
        features.meanPeaksHeightEda = peaks.peaksHeight.average()
        val instantaneousPeaks = instantaneousPeaks(peaks, samplingFrequency)
        features.meanInstantaneousPeaksEda = instantaneousPeaks.average()

        val peaksHeightStats = DescriptiveStatistics(peaks.peaksHeight.toDoubleArray())
        features.peaksHeight95Eda = peaksHeightStats.getPercentile(95.0)
        features.peaksHeight85Eda = peaksHeightStats.getPercentile(85.0)
        features.peaksHeight75Eda = peaksHeightStats.getPercentile(75.0)
        features.peaksHeight65Eda = peaksHeightStats.getPercentile(65.0)
        features.peaksHeight50Eda = peaksHeightStats.getPercentile(50.0)
        features.peaksHeight25Eda = peaksHeightStats.getPercentile(25.0)

        val instantaneousPeaksStats = DescriptiveStatistics(instantaneousPeaks.toDoubleArray())
        features.instantaneousPeaks95Eda = instantaneousPeaksStats.getPercentile(95.0)
        features.instantaneousPeaks85Eda = instantaneousPeaksStats.getPercentile(85.0)
        features.instantaneousPeaks75Eda = instantaneousPeaksStats.getPercentile(75.0)
        features.instantaneousPeaks65Eda = instantaneousPeaksStats.getPercentile(65.0)
        features.instantaneousPeaks50Eda = instantaneousPeaksStats.getPercentile(50.0)
        features.instantaneousPeaks25Eda = instantaneousPeaksStats.getPercentile(25.0)

        return features
    }

    fun tonicSignal(cleanEDA: List<Double>, samplingFrequency: Double): List<Double> {
        val butterworthLow = Butterworth()
        butterworthLow.lowPass(1, samplingFrequency, 0.05)
        return cleanEDA.map { butterworthLow.filter(it) }.let { filtered ->
            butterworthLow.reset()
            butterworthLow.lowPass(1, samplingFrequency, 0.05)
            filtered.asReversed().map {
                butterworthLow.filter(it)
            }
        }.asReversed()
    }

    fun phasicSignal(cleanEDA: List<Double>, samplingFrequency: Double): List<Double> {
        // We compute the phasic component of the signal. Using BioPacks recommendation we apply
        // a filter with a highcut of 0.05 Hz.
        val butterworthHigh = Butterworth()
        butterworthHigh.highPass(1, samplingFrequency, 0.05)
        return cleanEDA.map { butterworthHigh.filter(it) }.let { filtered ->
            butterworthHigh.reset()
            butterworthHigh.highPass(1, samplingFrequency, 0.05)
            filtered.asReversed().map {
                butterworthHigh.filter(it)
            }
        }.asReversed()
    }

    fun cleanSignal(edaSignal: DoubleArray, samplingFrequency: Double): List<Double> {
        // To remove noise we apply a lowpasss frequency filter with a cutoff frequency of 1 Hz
        val butterworthClear = Butterworth()
        butterworthClear.lowPass(2,samplingFrequency,1.0)
        return edaSignal.map { butterworthClear.filter(it) }.let { filtered ->
            butterworthClear.reset()
            butterworthClear.lowPass(2,samplingFrequency,1.0)
            filtered.asReversed().map {
                butterworthClear.filter(it)
            }
        }.asReversed()
    }

    data class EDAPeaksInformation (
        val signal: MutableList<Double> = mutableListOf(),
        val peaks: MutableList<Int> = mutableListOf(),
        val peaksStart: MutableList<Int> = mutableListOf(),
        val peaksEnd: MutableList<Int> = mutableListOf(),
        val peaksHeight: MutableList<Double> = mutableListOf()
    )

    fun instantaneousPeaks(peaks: EDAPeaksInformation, samplingFrequency: Double): List<Double> {
        return peaks.peaks.toIntArray().diff().map { it * (1 / samplingFrequency) }
    }

    /**
     * Returns the indices where local maxima are and where
     * the peaks start and end.
     * If the local maxima is a plateau than the index where
     * the plateau begins used.
     *
     * @param signal The signals to find the peaks at.
     * @return The indices of the peaks.
     */
    fun findPeaks(signal: List<Double>): EDAPeaksInformation {
        if (signal.size <= 2) return EDAPeaksInformation()
        val peaks = mutableListOf<Int>()
        val peaksStart = mutableListOf<Int>()
        val peaksEnd = mutableListOf<Int>()
        var lastRisingIndex = 0
        var lastFallingIndex = 0
        var isRising = false
        for (index in 1 until signal.size - 1) {
            if (signal[index-1] < signal[index] || signal[lastRisingIndex] == signal[index]) {
                if (!isRising) {
                    peaksStart.add(index - 1)
                    if (peaksStart.size > 1)
                        peaksEnd.add(lastFallingIndex)
                }
                if (signal[lastRisingIndex] < signal[index] || !isRising) {
                    lastRisingIndex = index
                }
                isRising = true
                if (signal[index + 1] < signal[index]) {
                    peaks.add(lastRisingIndex)
                }
            } else {
                if (signal[index] < signal[index - 1] ) {
                    lastFallingIndex = index
                }
                isRising = false
            }
        }
        if (isRising) {
            // The signal is now falling
            if (signal[signal.size - 1] < signal[signal.size - 2]) {
                peaks.add(signal.size - 2)
                peaksEnd.add(signal.size - 1)
                // Start of peak has already been set
            } else {
                peaksStart.removeAt(peaksStart.size - 1)
            }
        } else {
            if (signal[signal.size - 1] > signal[signal.size - 2]) {
                peaksEnd.add(signal.size - 2)
            }
            else {
                peaksEnd.add(signal.size - 1)
            }
        }
        require(peaks.size == peaksStart.size && peaksStart.size == peaksEnd.size)
        return EDAPeaksInformation(
            signal = signal.toMutableList(),
            peaks = peaks,
            peaksStart = peaksStart,
            peaksEnd = peaksEnd,
            peaksHeight = peaks.indices.map {
                signal[peaks[it]] - signal[peaksStart[it]]
            }.toMutableList()
        )
    }

    /**
     * Filter peaks signal to only keep relevant SCR and remove noise induced by
     * movement or other disturbing factors.
     *
     * Peaks must have a minimum amplitude of 0.05 μS.
     * Literature:
     * - (2012), Publication standards for EDA. Psychophysiol, 49: 1017-1034. https://doi.org/10.1111/j.1469-8986.2012.01384.x
     *
     *
     * @param peaks
     * @return
     */
    fun filterPeaks(peaks: EDAPeaksInformation, samplingFrequency: Double): EDAPeaksInformation {
        var drops = 0
        for (index in peaks.peaks.indices) {
            if (peaks.peaksHeight[index - drops] < 0.05) {
                peaks.peaks.removeAt(index - drops)
                peaks.peaksStart.removeAt(index - drops)
                peaks.peaksEnd.removeAt(index - drops)
                peaks.peaksHeight.removeAt(index - drops)
                drops++
            }
        }

        return peaks
    }
}