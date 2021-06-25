package heartk.hrv

import heartk.utils.diff
import heartk.utils.std
import heartk.utils.where
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
import org.apache.commons.math3.stat.descriptive.rank.Median
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Computes time HRV features.
 * The implementation is an adaptation of neurokit2.
 * [https://github.com/neuropsychology/NeuroKit]
 */
object HrvTime {

    /**
     * Computes time domain features from an rr-Interval signal (in ms).
     *
     * @param rrIntervals The rr-Intervals (in ms) from which the features are calculated. This must not be an
     * interpolated signal.
     * @return A map containing the computed features.
     */
    public fun getFeatures(rrIntervals: DoubleArray, featuresObject: HRVFeatures = HRVFeatures()): HRVFeatures {
        println("Computing time features.")
        val diff = rrIntervals.diff()

        featuresObject.RMSSD = sqrt(diff.map { it.pow(2) }.average())
        featuresObject.MeanNN = rrIntervals.average()
        featuresObject.SDNN = std(rrIntervals, 1)
        featuresObject.SDSD = std(diff, 1)

        featuresObject.CVNN = featuresObject.SDNN!! / featuresObject.MeanNN!!
        featuresObject.CVSD = featuresObject.RMSSD!! / featuresObject.MeanNN!!

        featuresObject.MedianNN = Median().evaluate(rrIntervals)
        featuresObject.MadNN = Median().evaluate(rrIntervals.map { abs(it - featuresObject.MedianNN!!) }.toDoubleArray()) * 1.4826
        featuresObject.MCVNN = featuresObject.MadNN!! / featuresObject.MedianNN!!

        val ds = DescriptiveStatistics(rrIntervals)
        featuresObject.IQR = ds.getPercentile(75.0) - ds.getPercentile(25.0)

        val nn50 = diff.map { abs(it) }.toDoubleArray().where { it > 50 }.size
        val nn20 = diff.map { abs(it) }.toDoubleArray().where { it > 20 }.size

        featuresObject.pNN50 = nn50.toDouble() / rrIntervals.size * 100
        featuresObject.pNN20 = nn20.toDouble() / rrIntervals.size * 100

        // TODO: TINN and HTI
        return featuresObject
    }
}