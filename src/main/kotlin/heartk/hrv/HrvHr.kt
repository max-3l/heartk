package heartk.hrv

import heartk.utils.interpolateSignal
import heartk.utils.where
import java.security.InvalidParameterException
import kotlin.math.floor
import kotlin.math.roundToInt

object HrvHr {
    /**
     * Computes the **heart rate** by a given peaks signal. The peaks signal
     * must be at least 1 minutes of data.
     *
     * @param peaks the peaks
     */
    fun computeHR(peaks: BooleanArray, samplingRate: Double = 1000.0): DoubleArray {
        val oneMinuteSamples = floor(samplingRate * 60).toInt()
        if (peaks.size < oneMinuteSamples) {
            throw InvalidParameterException("The peaks array must contain samples of one minute.")
        }
        val peaksIndices = peaks.where { el -> el }.map { it.toDouble() }.toDoubleArray()
        val peaksTimeDifferences = peaksIndices.foldIndexed(mutableListOf(0.0)) { index, current, element ->
            if (index == 0) return@foldIndexed current
            current.add(60 / ((element - peaksIndices[index - 1]) / samplingRate))
            return@foldIndexed current
        }.toDoubleArray()
        peaksTimeDifferences[0] = peaksTimeDifferences[1]
        val interpolated = interpolateSignal(peaksIndices, peaksTimeDifferences, peaks.size)
        val firstIndex = peaksIndices.first().roundToInt()
        val lastIndex = peaksIndices.last().roundToInt()
        for (index in 0 until firstIndex) {
            interpolated[index] = interpolated[firstIndex]
        }
        for (index in lastIndex + 1 until peaks.size) {
            interpolated[index] = interpolated[lastIndex]
        }
        return interpolated
    }
}
