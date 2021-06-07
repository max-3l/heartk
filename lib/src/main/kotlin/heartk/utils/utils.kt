package heartk.utils

import com.opengamma.strata.market.curve.interpolator.CurveExtrapolators
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators
import org.apache.commons.math3.stat.descriptive.moment.Variance
import kotlin.math.sign
import kotlin.math.sqrt

fun DoubleArray.consecutives(): MutableList<MutableList<Double>> {
    return this.fold(mutableListOf<MutableList<Double>>()) { current, element ->
        if (current.size == 0) {
            current.add(mutableListOf(element))
        } else {
            if (current.last().last() == element - 1) {
                current.last().add(element)
            } else {
                current.add(mutableListOf(element))
            }
        }
        return@fold current
    }
}

fun IntArray.consecutives(): MutableList<MutableList<Int>> {
    return this.fold(mutableListOf<MutableList<Int>>()) { current, element ->
        if (current.size == 0) {
            current.add(mutableListOf(element))
        } else {
            if (current.last().last() == element - 1) {
                current.last().add(element)
            } else {
                current.add(mutableListOf(element))
            }
        }
        return@fold current
    }
}

/**
 * Returns the indices where the condition is true.
 *
 * @param condition The condition which needs to be true.
 * @receiver The array which is checked.
 * @return The indices where the condition is true.
 */
fun DoubleArray.whereIndexed(condition: (index: Int, element: Double) -> Boolean): IntArray =
    this.foldIndexed(mutableListOf<Int>()) { index, current, value ->
        if (condition(index, value)) current.add(index)
        current
    }.toIntArray()

/**
 * Returns the indices where the condition is true.
 *
 * @param condition The condition which needs to be true.
 * @receiver The array which is checked.
 * @return The indices where the condition is true.
 */
fun DoubleArray.where(condition: (element: Double) -> Boolean): IntArray =
    this.foldIndexed(mutableListOf<Int>()) { index, current, value ->
        if (condition(value)) current.add(index)
        current
    }.toIntArray()

/**
 * Returns the indices where the condition is true.
 *
 * @param condition The condition which needs to be true.
 * @receiver The array which is checked.
 * @return The indices where the condition is true.
 */
fun BooleanArray.where(condition: (index: Int, element: Boolean) -> Boolean): IntArray =
    this.foldIndexed(mutableListOf<Int>()) { index, current, value ->
        if (condition(index, value)) current.add(index)
        current
    }.toIntArray()

/**
 * Returns the indices where the condition is true.
 *
 * @param condition The condition which needs to be true.
 * @receiver The array which is checked.
 * @return The indices where the condition is true.
 */
fun BooleanArray.where(condition: (element: Boolean) -> Boolean): IntArray =
    this.foldIndexed(mutableListOf<Int>()) { index, current, value ->
        if (condition(value)) current.add(index)
        current
    }.toIntArray()

fun DoubleArray.zeroCrossings(direction: String = "both"): IntArray {
    val signDifferences = this.map { it.sign }.toDoubleArray().diff()
    return when (direction) {
        "up" -> signDifferences.foldIndexed(mutableListOf<Int>()) { index, current, element ->
            if (element > 0) current.add(index)
            current
        }.toIntArray()
        "down" -> signDifferences.foldIndexed(mutableListOf<Int>()) { index, current, element ->
            if (element < 0) current.add(index)
            current
        }.toIntArray()
        else -> signDifferences.foldIndexed(mutableListOf<Int>()) { index, current, element ->
            if (element != 0.0) current.add(index)
            current
        }.toIntArray()
    }
}

fun std(array: FloatArray, degreesOfFreedom: Int = 0): Float {
    return std(array.toDoubleArray(), degreesOfFreedom).toFloat()
}

fun std(array: DoubleArray, degreesOfFreedom: Int = 0): Double {
    if (degreesOfFreedom > array.size) throw IllegalArgumentException("Degrees of freedom larger than sample size.")
    return sqrt(Variance().evaluate(array).div(array.size - degreesOfFreedom))
}

fun FloatArray.toDoubleArray(): DoubleArray =
    this.map { it.toDouble() }.toDoubleArray()

fun DoubleArray.toFloatArray(): FloatArray =
    this.map { it.toFloat() }.toFloatArray()

fun DoubleArray.diff(): DoubleArray =
    this.foldIndexed(mutableListOf<Double>()) { index, current, element ->
        if (index != this.size) current.add(this[index + 1] - element)
        current
    }.toDoubleArray()

fun FloatArray.diff(): FloatArray =
    this.foldIndexed(mutableListOf<Float>()) { index, current, element ->
        if (index != this.size) current.add(this[index + 1] - element)
        current
    }.toFloatArray()

fun IntArray.diff(): IntArray =
    this.foldIndexed(mutableListOf<Int>()) { index, current, element ->
        if (index != this.size - 1) current.add(this[index + 1] - element)
        current
    }.toIntArray()

fun LongArray.diff(): LongArray =
    this.foldIndexed(mutableListOf<Long>()) { index, current, element ->
        if (index != this.size) current.add(this[index + 1] - element)
        current
    }.toLongArray()

/**
 * Interpolate signal
 * Creates a monotone cubic spline interpolation using the given x indices and y values as
 * control points. Samples an array of the given length from the interpolation where the x
 * values correspond to the index.
 *
 * @param x The x indices of the control points
 * @param y The y values of the control points
 * @param length The length of the signal that should be interpolated
 * @return An interpolated signal of the required length. The signal gets
 */
fun interpolateSignal(x: DoubleArray, y: DoubleArray, length: Int): DoubleArray {
    val pchip = CurveInterpolators.PCHIP
    val curveExtrapolator = CurveExtrapolators.LINEAR
    val interoplator = pchip.bind(
        com.opengamma.strata.collect.array.DoubleArray.copyOf(x),
        com.opengamma.strata.collect.array.DoubleArray.copyOf(y),
        curveExtrapolator,
        curveExtrapolator
    )
    return DoubleArray(length) { index -> interoplator.interpolate(index.toDouble()) }
}