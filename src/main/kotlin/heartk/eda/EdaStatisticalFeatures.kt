package heartk.eda

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics

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
        return features
    }
}