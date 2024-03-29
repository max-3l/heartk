package heartk.hrv

import heartk.utils.diff
import heartk.utils.interpolateSignal
import heartk.utils.where
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
import org.apache.commons.math3.stat.descriptive.rank.Median

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
        println("Processing HRV features of signal.")
        val rrIntervals = getRRIntervals(peaks, samplingRate, false)
        if (hr) {
            println("Comptuing heart rate")
            featuresObject.HR = HrvHr.computeHR(peaks, samplingRate)
            println("Computing mean hr")
            featuresObject.meanHR = featuresObject.HR?.average()
            println("Computing median hr")
            featuresObject.medianHR = featuresObject.HR?.let { Median().evaluate(it) }
            println("Computing max hr")
            featuresObject.maxHR = featuresObject.HR?.max()
            println("Computing min hr")
            featuresObject.minHR = featuresObject.HR?.min()
            val stats = featuresObject.HR?.let { DescriptiveStatistics(it) }
            featuresObject.varHR = stats?.variance
            featuresObject.rangeHR = featuresObject.minHR?.let { featuresObject.maxHR?.minus(it) }
        }
        if (nonlinearFeatures)
            HrvNonlinear.getFeatures(rrIntervals, featuresObject)
        if (timeFeatures)
            HrvTime.getFeatures(rrIntervals, featuresObject)
        if (frequencyFeatures) {
            val interpolatedRRIntervals = getRRIntervals(peaks, samplingRate, true, "b-spline-2")
            HrvFrequency.getFeatures(interpolatedRRIntervals, samplingRate, featuresObject)
        }
        return featuresObject

    }

    fun getRRIntervals(peaks: BooleanArray, samplingRate: Double = 1000.0, interpolate: Boolean = false, interpolationMethod: String = "monotonicCubic"): DoubleArray {
        println("Computing rr intervals.")
        val peaksIndices = peaks.map { if (it) 1.0 else 0.0 }.toDoubleArray().where { it == 1.0 }
        println("Found peak indices.")
        val rri = peaksIndices.diff().map { it / (samplingRate / 1000) }.toDoubleArray()
        println("Computed rr intervalls.")
        if (!interpolate) return rri
        println("Interpolating rr intervals.")
        return interpolateSignal(peaksIndices.slice(IntRange(1, peaksIndices.size - 1)).map { it.toDouble() }
            .toDoubleArray(), rri, peaksIndices.last(), interpolationMethod)
    }
}
