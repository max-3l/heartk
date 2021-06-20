package heartk.eda

object EDA {
    fun processFeatures(signal: DoubleArray, samplingRate: Double, features: EDAFeatures = EDAFeatures()): EDAFeatures {
        EdaStatisticalFeatures.getFeatures(signal, features)
        EdaPhasicTonicFeatures.getFeatures(signal, samplingRate, features)
        return features
    }
}