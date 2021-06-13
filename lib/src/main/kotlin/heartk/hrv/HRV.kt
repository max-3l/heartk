package heartk.hrv

import heartk.utils.diff
import heartk.utils.interpolateSignal
import heartk.utils.where
import kotlin.math.roundToInt

object HRV {
    /**
     * Compute HRV features using a RR-Peaks signal which has been sampled at the given
     * sampling rate. To reduce the computational
     *
     * @param peaks The peaks of a heart signal. This can be obtained from e.g. a
     * PPG signal using [heartk.ppg.PPG.processSignal].
     * @param samplingRate The sampling rate of the peaks signal.
     * @param hr Specify whether the hr should be computed or not.
     * @param nonlinearFeatures Specify whether the nonlinear features should be computed or not.
     * @param timeFeatures Specify whether the time features should be computed or not.
     * @param frequencyFeatures Specify whether the frequency features should be computed or not.
     *
     * @return A map which contains the feature name and the computed feature signal.
     */
    fun processFeatures(
        peaks: BooleanArray,
        samplingRate: Double,
        hr: Boolean = true,
        nonlinearFeatures: Boolean = true,
        timeFeatures: Boolean = true,
        frequencyFeatures: Boolean = true,
        featuresObject: HRVFeatures = HRVFeatures()
    ): HRVFeatures {
        val rrIntervals = getRRIntervals(peaks, samplingRate, false)
        if (hr)
            featuresObject.HR = HrvHr.computeHR(peaks, samplingRate)
        if (nonlinearFeatures)
            HrvNonlinear.getFeatures(rrIntervals, featuresObject)
        if (timeFeatures)
            HrvTime.getFeatures(rrIntervals, featuresObject)
        if (frequencyFeatures) {
            val interpolatedRRIntervals = getRRIntervals(peaks, samplingRate, true, "quadratic")
            HrvFrequency.getFeatures(interpolatedRRIntervals, samplingRate, featuresObject)
        }
        return featuresObject

    }

    fun getRRIntervals(peaks: BooleanArray, samplingRate: Double = 1000.0, interpolate: Boolean = false, interpolationMethod: String = "monotonicCubic"): DoubleArray {
        val peaksIndices = peaks.map { if (it) 1.0 else 0.0 }.toDoubleArray().where { it == 1.0 }
        val rri = peaksIndices.diff().map { it / (samplingRate / 1000) }.toDoubleArray()
        if (!interpolate) return rri
        return interpolateSignal(peaksIndices.slice(IntRange(1, peaksIndices.size - 1)).map { it.toDouble() }
            .toDoubleArray(), rri, peaksIndices.last(), interpolationMethod)
    }
}
