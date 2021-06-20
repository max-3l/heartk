package heartk.eda

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
import org.apache.commons.math3.stat.descriptive.rank.Median

object EdaStatisticalFeatures {
    fun getFeatures(edaSignal: DoubleArray, features: EDAFeatures = EDAFeatures()): EDAFeatures {
        val statistics = DescriptiveStatistics(edaSignal)
        features.kurtosisEda = statistics.kurtosis
        features.skewnessEda = statistics.skewness
        features.meanEda = statistics.mean
        features.stdEda = statistics.standardDeviation
        features.varEda = statistics.variance
        features.minEda = statistics.min
        features.maxEda = statistics.max
        features.rangeEda = statistics.max - statistics.min
        features.medianEda = Median().evaluate(edaSignal)
        return features
    }
}