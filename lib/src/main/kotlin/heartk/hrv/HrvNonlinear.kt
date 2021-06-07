package heartk.hrv

import heartk.utils.consecutives
import heartk.utils.diff
import heartk.utils.std
import heartk.utils.where
import heartk.utils.zeroCrossings
import org.apache.commons.math3.analysis.function.Atan
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Computes nonlinear HRV features.
 * The implementation is very similiar to neurokit2.
 * [https://github.com/neuropsychology/NeuroKit]
 */
object HrvNonlinear {
    fun getFeatures(rrIntervals: DoubleArray): Map<String, Double> {
        val output = mutableMapOf<String, Double>()

        val N = rrIntervals.size - 1
        val sqrt2 = sqrt(2.0)

        val x = rrIntervals.slice(IntRange(0, rrIntervals.size - 2))
        val y = rrIntervals.slice(IntRange(1, N))
        val diff = y.mapIndexed { index, el -> el - x[index] }.toDoubleArray()
        val sum = y.mapIndexed { index, el -> el + x[index] }.toDoubleArray()
        val decelerateIndices = diff.where { it > 0 }
        val accelerateIndices = diff.where { it < 0 }
        val noChangeIndices = diff.where { it == 0.0 }

        val centroidX = x.average()
        val centroidY = y.average()
        val centerDistX = x.map { it - centroidX }
        val centerDistY = y.map { it - centroidY }
        val distL2All = (centerDistX.mapIndexed { index, el -> abs(el + centerDistY[index]) / sqrt2 })
        val distAll = diff.map { abs(it) / sqrt2 } // Distances to LI
        val thetaAll = x.mapIndexed { index, element ->
            abs(Atan().value(1.0) - Atan().value((y[index] / element)))
        }
        val r = x.mapIndexed { index, element ->
            sqrt(element.pow(2) + y[index].pow(2))
        }
        val SAll = thetaAll.mapIndexed { index, element ->
            0.5 * element * r[index].pow(2)
        }

        // GI (Guzik's Index)
        val denominatorGI = distAll.sum()
        val nominatorGI = distAll.slice(decelerateIndices.toList()).sum()
        output["GI"] = (nominatorGI / denominatorGI) * 100

        // SI (Slope Index)
        val denominatorSI = SAll.sum()
        val nominatorSI = SAll.slice(decelerateIndices.toList()).sum()
        output["SI"] = (nominatorSI / denominatorSI) * 100

        // PI (Porta's Index)
        val m = N - noChangeIndices.size
        val b = accelerateIndices.size
        output["PI"] = (b.toDouble() / m.toDouble()) * 100

        // SD1  (Short-term asymmetry)
        val sd1d = sqrt(distAll.slice(decelerateIndices.toList()).map { it.pow(2) / (N - 1) }.sum())
        val sd1a = sqrt(distAll.slice(accelerateIndices.toList()).map { it.pow(2) / (N - 1) }.sum())
        val sd1I = sqrt(sd1d.pow(2) + sd1a.pow(2))
        output["C1d"] = (sd1d / sd1I).pow(2)
        output["C1a"] = (sd1a / sd1I).pow(2)
        output["SD1d"] = sd1d
        output["SD1a"] = sd1a

        // SD2 (Long-term asymmetry)
        val longTermDec = distL2All.slice(decelerateIndices.toList()).map { it.pow(2) / (N - 1) }.sum()
        val longTermAcc = distL2All.slice(accelerateIndices.toList()).map { it.pow(2) / (N - 1) }.sum()
        val longTermNoDiff = distL2All.slice(noChangeIndices.toList()).map { it.pow(2) / (N - 1) }.sum()
        val sd2d = sqrt(longTermDec + 0.5 * longTermNoDiff)
        val sd2a = sqrt(longTermAcc + 0.5 * longTermNoDiff)
        val sd2I = sqrt(sd2d.pow(2) + sd2a.pow(2))

        output["C2d"] = (sd2d / sd2I).pow(2)
        output["C2a"] = (sd2a / sd2I).pow(2)
        output["SD2d"] = sd2d
        output["SD2a"] = sd2a

        // Total asymmetry
        val sdnnd = sqrt(0.5 * (sd1d.pow(2) + sd2d.pow(2)))
        val sdnna = sqrt(0.5 * (sd1a.pow(2) + sd2d.pow(2)))
        val sdnn = sqrt(sdnnd.pow(2) + sdnna.pow(2))
        output["Cd"] = (sdnnd / sdnn).pow(2)
        output["Ca"] = (sdnna / sdnn).pow(2)
        output["SDNNd"] = sdnnd
        output["SDNDDa"] = sdnna

        val x1 = diff.map { it / sqrt2 }
        val x2 = sum.map { it / sqrt2 }
        val sd1 = std(x1.toDoubleArray(), 1)
        val sd2 = std(x2.toDoubleArray(), 1)

        output["SD1"] = sd1
        output["SD2"] = sd2
        output["SD1SD2"] = sd1 / sd2
        output["S"] = PI * sd1 * sd2

        val T = 4 * sd1
        val L = 4 * sd2
        output["CSI"] = L / T
        output["CVI"] = log10(L * T)
        output["CSI_Modified"] = L.pow(2) / T

        // Heart Rate fragmentation indices
        val diffRRIntervals = rrIntervals.diff()
        val zeroCrossings = diffRRIntervals.zeroCrossings()
        output["PIP"] = zeroCrossings.size.toDouble().div(rrIntervals.size)
        val accelerations = diffRRIntervals.where { it > 0 }
        val decelerations = diffRRIntervals.where { it < 0 }
        val consecutives = accelerations.consecutives() + decelerations.consecutives()
        val consecutivesLengths = consecutives.map { it.size }
        output["IALS"] = 1 / consecutivesLengths.average()
        output["PSS"] = consecutivesLengths.filter { it < 3 }.size.toDouble().div(consecutivesLengths.size)

        val alternations = zeroCrossings.consecutives()
        val alternationsLengths = alternations.map { it.size }
        output["PAS"] = alternationsLengths.filter { it >= 4 }.size.toDouble().div(alternationsLengths.size)

        return output
    }
}
