package heartk.hrv

import heartk.utils.diff
import heartk.utils.interpolateSignal
import heartk.utils.where
import kotlin.math.roundToInt

public object HRV {
    /**
     * Process HRV features computed in a over a sliding window.
     *
     * @param peaks The peaks of a heart signal. This can be obtained from e.g. a
     * PPG signal using [heartk.ppg.PPG.processSignal].
     * @param samplingRate The sampling rate of the peaks signal.
     * @param windowSize The size of the window which is used to calculate the HRV features in
     * seconds. When this parameter is set to `-1` the whole signal is used to compute the HRV features.
     * In this case the features will be constant. When the window size is set the resulting signals will be reduced by
     * window/2 at the beginning and end of the signal (to also get valid features here).
     *
     * @return A map which contains the feature name and the computed feature signal.
     */
    fun processFeatures(
        peaks: BooleanArray,
        samplingRate: Double,
        windowSize: Double = 300.0
    ): Map<String, DoubleArray> {
        val output = mutableMapOf<String, DoubleArray>()
        output["HR"] = HrvHr.computeHR(peaks, samplingRate)
        if (windowSize == -1.0) {
            val features = HrvNonlinear.getFeatures(getRRIntervals(peaks, samplingRate, false))
            val windowFeatures = mutableMapOf<String, DoubleArray>()
            features.entries.forEach { entry -> windowFeatures[entry.key] = DoubleArray(peaks.size) { entry.value } }
            return output + windowFeatures
        }
        var windowSamples = (samplingRate * windowSize).roundToInt()
        windowSamples += windowSamples % 2 + 1 // make it odd so that window computation is easier
        val allFeatures = peaks
            .toList()
            .windowed(size = windowSamples, step = 1, partialWindows = false)
            .fold(mutableMapOf<String, DoubleArray>()) { current, element ->
                val features =
                    HrvNonlinear.getFeatures(getRRIntervals(element.toBooleanArray(), samplingRate, false))
                val windowFeatures = mutableMapOf<String, DoubleArray>()
                features.entries.forEach { entry ->
                    windowFeatures[entry.key] = DoubleArray(windowSamples) { entry.value }
                }
                if (current.isEmpty()) return@fold windowFeatures
                windowFeatures.keys.forEach {
                    current[it] =
                        current[it]?.plus(windowFeatures[it] ?: DoubleArray(0))
                            ?: windowFeatures[it]
                                    ?: DoubleArray(0)
                }
                return@fold current
            }
        output["HR"]?.let {
            output["HR"] = it.slice(IntRange(windowSamples / 2, (it.size - (windowSamples / 2)) - 1)).toDoubleArray()
        }
        return output + allFeatures
    }

    fun getRRIntervals(peaks: BooleanArray, samplingRate: Double = 1000.0, interpolate: Boolean = false, interpolationMethod: String = "monotonCubic"): DoubleArray {
        val peaksIndices = peaks.map { if (it) 1.0 else 0.0 }.toDoubleArray().where { it == 1.0 }
        val rri = peaksIndices.diff().map { it / (samplingRate / 1000) }.toDoubleArray()
        if (!interpolate) return rri
        return interpolateSignal(peaksIndices.slice(IntRange(1, peaksIndices.size - 1)).map { it.toDouble() }
            .toDoubleArray(), rri, peaksIndices.last(), interpolationMethod)
    }
}
