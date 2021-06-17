package heartk.eda

import uk.me.berndporr.iirj.Butterworth
import kotlin.math.sign

object EdaPhasicTonicFeatures {
    fun getFeatures(
        edaSignal: DoubleArray,
        samplingFrequency: Double,
        features: EDAFeatures = EDAFeatures()
    ): EDAFeatures {
        // To remove noise we apply a lowpasss frequency filter with a cutoff frequency of 1 Hz
        val butterworthClear = Butterworth()
        butterworthClear.lowPass(2,samplingFrequency,1.0)
        val cleanEDA = edaSignal.map { butterworthClear.filter(it) }.let { filtered ->
            butterworthClear.reset()
            butterworthClear.lowPass(2,samplingFrequency,1.0)
            filtered.asReversed().map {
                butterworthClear.filter(it)
            }
        }.asReversed()

        // First we compute the phasic component of the signal. Using BioPacks recommendation we apply
        // a filter with a highcut of 0.05 Hz.
        val butterworthHigh = Butterworth()
        butterworthHigh.highPass(1, samplingFrequency, 0.05)
        val phasicEDA = cleanEDA.map { butterworthHigh.filter(it) }.let { filtered ->
            butterworthHigh.reset()
            butterworthHigh.highPass(1, samplingFrequency, 0.05)
            filtered.asReversed().map {
                butterworthHigh.filter(it)
            }
        }.asReversed()

        val butterworthLow = Butterworth()
        butterworthLow.lowPass(1, samplingFrequency, 0.05)
        val tonicEDA = cleanEDA.map { butterworthLow.filter(it) }.let { filtered ->
            butterworthLow.reset()
            butterworthLow.lowPass(1, samplingFrequency, 0.05)
            filtered.asReversed().map {
                butterworthLow.filter(it)
            }
        }.asReversed()
        Butterworth()

        return features
    }

    /**
     * Returns the indices where local maxima are.
     *
     * @param signal
     * @return
     */
    fun findPeaks(signal: DoubleArray): DoubleArray {
        val startIndex = 1 // First index cannot be maximum
        val stopIndex = signal.size - 1 // Last index cannot be maximum
        val peaks = mutableListOf<Int>()
        val peakStart = mutableListOf<Int>()
        val peakEnd = mutableListOf<Int>()

    }
}