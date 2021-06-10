package heartk.hrv

import heartk.utils.diff
import heartk.utils.std
import heartk.utils.where
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
import org.apache.commons.math3.stat.descriptive.rank.Median
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

object HrvTime {

    /**
     * Computes time domain features from an rr-Interval signal (in ms).
     *
     * @param rrIntervals The rr-Intervals (in ms) from which the features are calculated. This must not be an
     * interpolated signal.
     * @return A map containing the computed features.
     */
    public fun getFeatures(rrIntervals: DoubleArray): Map<String, Double> {
        val diff = rrIntervals.diff()
        val output = mutableMapOf<String, Double>()

        output["RMSSD"] = sqrt(diff.map { it.pow(2) }.average())
        output["MeanNN"] = rrIntervals.average()
        output["SDNN"] = std(rrIntervals, 1)
        output["SDSD"] = std(diff, 1)

        output["CVNN"] = output["SDNN"]!! / output["MeanNN"]!!
        output["CVSD"] = output["RMSSD"]!! / output["MeanNN"]!!

        output["MedianNN"] = Median().evaluate(rrIntervals)
        output["MadNN"] = Median().evaluate(rrIntervals.map { abs(it - output["MedianNN"]!!) }.toDoubleArray()) * 1.4826
        output["MCVNN"] = output["MadNN"]!! / output["MedianNN"]!!

        val ds = DescriptiveStatistics(rrIntervals)
        output["IQR"] = ds.getPercentile(75.0) - ds.getPercentile(25.0)

        val nn50 = diff.map { abs(it) }.toDoubleArray().where { it > 50 }.size
        val nn20 = diff.map { abs(it) }.toDoubleArray().where { it > 20 }.size

        output["pNN50"] = nn50.toDouble() / rrIntervals.size * 100
        output["pNN20"] = nn20.toDouble() / rrIntervals.size * 100

        // TODO: TINN and HTI
        return output
    }
}