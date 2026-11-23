package com.example.oraclepredictanything.util

import com.example.oraclepredictanything.engine.EnsembleResult
import com.example.oraclepredictanything.engine.PredictionOutput
import org.json.JSONArray
import org.json.JSONObject

/**
 * Lightweight deterministic JSON serializer for prediction snapshots.
 */
object JsonUtils {

    fun serializeForecast(ensemble: EnsembleResult): String {
        val root = JSONObject()

        val predArray = JSONArray(ensemble.predictions)
        val l80Array = JSONArray(ensemble.lowerBound80)
        val u80Array = JSONArray(ensemble.upperBound80)
        val l95Array = JSONArray(ensemble.lowerBound95)
        val u95Array = JSONArray(ensemble.upperBound95)

        root.put("predictions", predArray)
        root.put("lowerBound80", l80Array)
        root.put("upperBound80", u80Array)
        root.put("lowerBound95", l95Array)
        root.put("upperBound95", u95Array)
        root.put("bestModel", ensemble.bestModel)

        val weightsObj = JSONObject()
        for ((k, v) in ensemble.weights) {
            weightsObj.put(k, v)
        }
        root.put("weights", weightsObj)

        return root.toString()
    }

    fun deserializeForecast(jsonStr: String): EnsembleResult? {
        return try {
            val root = JSONObject(jsonStr)
            val predictions = jsonArrayToDoubleList(root.optJSONArray("predictions"))
            val lowerBound80 = jsonArrayToDoubleList(root.optJSONArray("lowerBound80"))
            val upperBound80 = jsonArrayToDoubleList(root.optJSONArray("upperBound80"))
            val lowerBound95 = jsonArrayToDoubleList(root.optJSONArray("lowerBound95"))
            val upperBound95 = jsonArrayToDoubleList(root.optJSONArray("upperBound95"))
            val bestModel = root.optString("bestModel", "Ensemble")

            val weightsObj = root.optJSONObject("weights")
            val weights = mutableMapOf<String, Double>()
            if (weightsObj != null) {
                val keys = weightsObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    weights[k] = weightsObj.optDouble(k, 0.0)
                }
            }

            EnsembleResult(
                predictions = predictions,
                lowerBound80 = lowerBound80,
                upperBound80 = upperBound80,
                lowerBound95 = lowerBound95,
                upperBound95 = upperBound95,
                modelResults = emptyMap(),
                weights = weights,
                bestModel = bestModel
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun jsonArrayToDoubleList(arr: JSONArray?): List<Double> {
        if (arr == null) return emptyList()
        val list = mutableListOf<Double>()
        for (i in 0 until arr.length()) {
            list.add(arr.optDouble(i, 0.0))
        }
        return list
    }
}
