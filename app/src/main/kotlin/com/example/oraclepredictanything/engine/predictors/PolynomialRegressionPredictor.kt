package com.example.oraclepredictanything.engine.predictors

import com.example.oraclepredictanything.engine.BacktestResult
import com.example.oraclepredictanything.engine.BacktestUtils
import com.example.oraclepredictanything.engine.PredictionOutput
import com.example.oraclepredictanything.engine.Predictor
import kotlin.math.abs

/**
 * Second-degree polynomial regression: y = a + b*x + c*x^2
 * Solved analytically via normal equations with Gaussian elimination.
 * Includes damping to avoid explosive extrapolation.
 */
class PolynomialRegressionPredictor : Predictor {
    override val name: String = "Polynomial Trend (Quadratic)"

    override fun predict(data: List<Double>, horizon: Int): PredictionOutput {
        if (data.size < 4) {
            // Need at least 4 points to fit quadratic without exact interpolation
            return LinearRegressionPredictor().predict(data, horizon).copy(modelName = name)
        }

        val n = data.size
        // Normal equations: (X^T * X) * w = X^T * y
        // Matrix size 3x3 for degree 2:
        // [ sum(x^0)  sum(x^1)  sum(x^2) ] [ a ]   [ sum(y) ]
        // [ sum(x^1)  sum(x^2)  sum(x^3) ] [ b ] = [ sum(x*y) ]
        // [ sum(x^2)  sum(x^3)  sum(x^4) ] [ c ]   [ sum(x^2*y) ]

        val s = DoubleArray(5) // s[k] = sum(x^k) for k=0..4
        val v = DoubleArray(3) // v[0]=sum(y), v[1]=sum(x*y), v[2]=sum(x^2*y)

        for (i in 0 until n) {
            val x = i.toDouble()
            val y = data[i]
            var xPow = 1.0
            for (k in 0..4) {
                s[k] += xPow
                if (k < 3) {
                    v[k] += xPow * y
                }
                xPow *= x
            }
        }

        val aMatrix = arrayOf(
            doubleArrayOf(s[0], s[1], s[2]),
            doubleArrayOf(s[1], s[2], s[3]),
            doubleArrayOf(s[2], s[3], s[4])
        )

        val coeffs = solve3x3(aMatrix, v)
        if (coeffs == null) {
            // Degenerate matrix, fallback to linear
            return LinearRegressionPredictor().predict(data, horizon).copy(modelName = name)
        }

        val c0 = coeffs[0]
        val c1 = coeffs[1]
        val c2 = coeffs[2]

        val fitted = mutableListOf<Double>()
        for (i in 0 until n) {
            val x = i.toDouble()
            fitted.add(c0 + c1 * x + c2 * x * x)
        }

        val lastFitted = fitted.last()
        val predictions = mutableListOf<Double>()
        for (h in 0 until horizon) {
            val x = (n + h).toDouble()
            var rawPred = c0 + c1 * x + c2 * x * x

            // Damp quadratic term when extrapolating deep to prevent runaway acceleration
            val damping = 1.0 / (1.0 + 0.05 * (h + 1))
            val dampedPred = lastFitted + (rawPred - lastFitted) * damping
            predictions.add(dampedPred)
        }

        val stdDev = BacktestUtils.computeResidualStdDev(data, fitted)
        val bands = BacktestUtils.computeBands(predictions, stdDev)

        return PredictionOutput(
            predictions = predictions,
            lowerBound80 = bands.lower80,
            upperBound80 = bands.upper80,
            lowerBound95 = bands.lower95,
            upperBound95 = bands.upperBound95,
            modelName = name
        )
    }

    override fun backtest(data: List<Double>, holdout: Int): BacktestResult {
        val actualHoldout = holdout.coerceIn(1, (data.size - 3).coerceAtLeast(1))
        if (data.size <= actualHoldout + 2) {
            return BacktestResult(0.0, 0.0, 0.0, "Insufficient Data")
        }
        val train = data.dropLast(actualHoldout)
        val test = data.takeLast(actualHoldout)

        val out = predict(train, actualHoldout)
        val mae = BacktestUtils.computeMAE(test, out.predictions)
        val rmse = BacktestUtils.computeRMSE(test, out.predictions)
        val mape = BacktestUtils.computeMAPE(test, out.predictions)

        return BacktestResult(mae, rmse, mape, BacktestUtils.accuracyLabel(mape))
    }

    private fun solve3x3(a: Array<DoubleArray>, b: DoubleArray): DoubleArray? {
        val m = Array(3) { i -> DoubleArray(4) { j -> if (j < 3) a[i][j] else b[i] } }

        for (col in 0 until 3) {
            var maxRow = col
            for (row in col + 1 until 3) {
                if (abs(m[row][col]) > abs(m[maxRow][col])) maxRow = row
            }
            val temp = m[col]
            m[col] = m[maxRow]
            m[maxRow] = temp

            if (abs(m[col][col]) < 1e-12) return null // Singular matrix

            for (row in col + 1 until 3) {
                val factor = m[row][col] / m[col][col]
                for (j in col until 4) {
                    m[row][j] -= factor * m[col][j]
                }
            }
        }

        // Back substitution
        val x = DoubleArray(3)
        for (i in 2 downTo 0) {
            var sum = m[i][3]
            for (j in i + 1 until 3) {
                sum -= m[i][j] * x[j]
            }
            x[i] = sum / m[i][i]
        }
        return x
    }
}
