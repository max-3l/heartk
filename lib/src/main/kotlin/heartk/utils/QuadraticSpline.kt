package heartk.utils

import org.apache.commons.math3.linear.LUDecomposition
import org.apache.commons.math3.linear.MatrixUtils
import java.lang.IllegalArgumentException
import kotlin.math.pow

/**
 * This class computes a quadratic spline interpolation using the given control points.
 *
 * TODO: It is currently not working.
 *
 */
internal class QuadraticSplineInterpolation {
    private var coefficients: DoubleArray
    private lateinit var points: List<Pair<Double, Double>>

    constructor(x: DoubleArray, y: DoubleArray) {
        this.coefficients = build(x, y)
        this.points = x.zip(y)
    }

    private fun build(x: DoubleArray, y: DoubleArray): DoubleArray {
        val n = x.size
        if (n < 2 || x.size != y.size) throw IllegalArgumentException("x must have equal size to y and have more than 2 elements.")
        val mat = MatrixUtils.createRealMatrix((x.size - 1) * 3, (x.size - 1) * 3)
        val rightSide = MatrixUtils.createRealVector(DoubleArray((x.size - 1) * 3))
        for (i in x.indices) {
            if (i != n - 1) {
                // set equations where this point is at start
                mat.setEntry(i * 2, i * 3, x[i].pow(2))
                mat.setEntry(i * 2, i * 3 + 1, x[i])
                mat.setEntry(i * 2, i * 3 + 2, 1.0)
                rightSide.setEntry(i * 2, y[i])
            }
            if (i != 0) {
                // set equations where this point end the curve
                mat.setEntry(i*2 - 1, (i - 1) * 3, x[i].pow(2))
                mat.setEntry(i*2 - 1, (i - 1) * 3 + 1, x[i])
                mat.setEntry(i*2 - 1, (i - 1) * 3 + 2, 1.0)
                rightSide.setEntry(i*2 - 1, y[i])
            }

            // Differential equation points - the left and right side differential value must be equal
            // 2 * a1 * x[i] + b1 - 2 * a1 x[i+1] - b1 = 0 where a1, b1 are coefficients of a quadratic equation
            // we don't need to set the right side to 0 as it is initialized with 0

            // Set the right side differential equations
            if (i < n - 2) {
                mat.setEntry(((n - 1) * 2) + (i - 2), i * 3, 2 * x[i])
                mat.setEntry(((n - 1) * 2) + (i - 2), i * 3 + 1, 1.0)
            }

            // Set the left side differential equations
            if (i > 1) {
                mat.setEntry(((n - 1) * 2)  + (i - 2) - 1, (i - 1)*3, -2*x[i] )
                mat.setEntry(((n - 1) * 2)  + (i - 2) - 1, (i - 1)*3 + 1, -1.0 )
            }
        }
        return LUDecomposition(mat).solver.solve(rightSide).toArray()
    }

    /**
     * Interpolates the point x on the curve using quadratic interpolation.
     *
     * @param x The x value of the point from which the y value is interpolated.
     * @return The y value of the interpolated point.
     */
    public fun interpolate(x: Double): Double {
        // Find the index of the coefficients
        val index = this.points.indexOfFirst { it.first - x >= 0 }
        if (index == 0) return this.points.first().second
        if (index == -1) return this.points.last().second
        if (this.points[index].first == x) return this.points[index].second
        // Calculate f(x) = a x^2 + bx + c
        val a = this.coefficients[(index - 1) * 3]
        val b = this.coefficients[(index - 1) * 3 + 1]
        val c = this.coefficients[(index - 1) * 3 + 2]
        return a * (x.pow(2)) + b * x + c
    }
}