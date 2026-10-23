package app.kiddos.autoclicker.data

import android.content.Context
import android.content.SharedPreferences
import app.kiddos.autoclicker.model.ClickPoint
import app.kiddos.autoclicker.model.ClickerMode
import app.kiddos.autoclicker.model.GridClickOrder
import app.kiddos.autoclicker.model.GridConfig
import app.kiddos.autoclicker.model.MultiClickConfig
import app.kiddos.autoclicker.model.SingleClickConfig
import app.kiddos.autoclicker.model.StopCondition
import org.json.JSONArray
import org.json.JSONObject

class ClickerPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("auto_clicker_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_MODE = "clicker_mode"

        // Single click keys
        private const val KEY_SINGLE_INTERVAL = "single_interval"
        private const val KEY_SINGLE_DURATION = "single_duration"
        private const val KEY_SINGLE_STOP_CONDITION = "single_stop_condition"
        private const val KEY_SINGLE_COUNT_LIMIT = "single_count_limit"
        private const val KEY_SINGLE_DURATION_MINUTES = "single_duration_minutes"
        private const val KEY_SINGLE_RANDOM_JITTER = "single_random_jitter"
        private const val KEY_SINGLE_RANDOM_RADIUS = "single_random_radius"

        // Multi click keys
        private const val KEY_MULTI_INTERVAL = "multi_interval"
        private const val KEY_MULTI_DURATION = "multi_duration"
        private const val KEY_MULTI_LOOP_COUNT = "multi_loop_count"
        private const val KEY_MULTI_POINTS = "multi_points"
        private const val KEY_MULTI_RANDOM_JITTER = "multi_random_jitter"
        private const val KEY_MULTI_RANDOM_RADIUS = "multi_random_radius"

        // Grid keys
        private const val KEY_GRID_COLUMNS = "grid_columns"
        private const val KEY_GRID_ROWS = "grid_rows"
        private const val KEY_GRID_BOTTOM_RATIO = "grid_bottom_ratio"
        private const val KEY_GRID_INTERVAL = "grid_interval"
        private const val KEY_GRID_DURATION = "grid_duration"
        private const val KEY_GRID_LOOP_COUNT = "grid_loop_count"
        private const val KEY_GRID_ORDER = "grid_order"
        private const val KEY_GRID_RANDOM_JITTER = "grid_random_jitter"
        private const val KEY_GRID_RANDOM_RADIUS = "grid_random_radius"
        private const val KEY_GRID_SELECTED_CELLS = "grid_selected_cells"
    }

    fun getMode(): ClickerMode {
        val modeStr = prefs.getString(KEY_MODE, ClickerMode.SINGLE.name)
        return try {
            ClickerMode.valueOf(modeStr ?: ClickerMode.SINGLE.name)
        } catch (_: Exception) {
            ClickerMode.SINGLE
        }
    }

    fun setMode(mode: ClickerMode) {
        prefs.edit().putString(KEY_MODE, mode.name).apply()
    }

    fun getSingleClickConfig(): SingleClickConfig {
        val stopConditionStr = prefs.getString(KEY_SINGLE_STOP_CONDITION, StopCondition.INFINITE.name)
        val stopCondition = try {
            StopCondition.valueOf(stopConditionStr ?: StopCondition.INFINITE.name)
        } catch (_: Exception) {
            StopCondition.INFINITE
        }

        return SingleClickConfig(
            intervalMs = prefs.getLong(KEY_SINGLE_INTERVAL, 500L),
            pressDurationMs = prefs.getLong(KEY_SINGLE_DURATION, 50L),
            stopCondition = stopCondition,
            countLimit = prefs.getInt(KEY_SINGLE_COUNT_LIMIT, 100),
            durationMinutes = prefs.getInt(KEY_SINGLE_DURATION_MINUTES, 5),
            randomJitterMs = prefs.getLong(KEY_SINGLE_RANDOM_JITTER, 0L),
            randomRadiusPx = prefs.getInt(KEY_SINGLE_RANDOM_RADIUS, 0)
        )
    }

    fun saveSingleClickConfig(config: SingleClickConfig) {
        prefs.edit()
            .putLong(KEY_SINGLE_INTERVAL, config.intervalMs)
            .putLong(KEY_SINGLE_DURATION, config.pressDurationMs)
            .putString(KEY_SINGLE_STOP_CONDITION, config.stopCondition.name)
            .putInt(KEY_SINGLE_COUNT_LIMIT, config.countLimit)
            .putInt(KEY_SINGLE_DURATION_MINUTES, config.durationMinutes)
            .putLong(KEY_SINGLE_RANDOM_JITTER, config.randomJitterMs)
            .putInt(KEY_SINGLE_RANDOM_RADIUS, config.randomRadiusPx)
            .apply()
    }

    fun getMultiClickConfig(): MultiClickConfig {
        val pointsJson = prefs.getString(KEY_MULTI_POINTS, "[]") ?: "[]"
        val points = mutableListOf<ClickPoint>()
        try {
            val jsonArray = JSONArray(pointsJson)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                points.add(
                    ClickPoint(
                        id = obj.optInt("id", i + 1),
                        x = obj.optDouble("x", 0.0).toFloat(),
                        y = obj.optDouble("y", 0.0).toFloat(),
                        delayMs = obj.optLong("delayMs", 500L),
                        durationMs = obj.optLong("durationMs", 50L),
                        label = obj.optString("label", "P${i + 1}")
                    )
                )
            }
        } catch (_: Exception) {
            // fallback
        }

        return MultiClickConfig(
            intervalMs = prefs.getLong(KEY_MULTI_INTERVAL, 500L),
            pressDurationMs = prefs.getLong(KEY_MULTI_DURATION, 50L),
            loopCount = prefs.getInt(KEY_MULTI_LOOP_COUNT, 0),
            points = points,
            randomJitterMs = prefs.getLong(KEY_MULTI_RANDOM_JITTER, 0L),
            randomRadiusPx = prefs.getInt(KEY_MULTI_RANDOM_RADIUS, 0)
        )
    }

    fun saveMultiClickConfig(config: MultiClickConfig) {
        val jsonArray = JSONArray()
        for (point in config.points) {
            val obj = JSONObject().apply {
                put("id", point.id)
                put("x", point.x.toDouble())
                put("y", point.y.toDouble())
                put("delayMs", point.delayMs)
                put("durationMs", point.durationMs)
                put("label", point.label)
            }
            jsonArray.put(obj)
        }

        prefs.edit()
            .putLong(KEY_MULTI_INTERVAL, config.intervalMs)
            .putLong(KEY_MULTI_DURATION, config.pressDurationMs)
            .putInt(KEY_MULTI_LOOP_COUNT, config.loopCount)
            .putString(KEY_MULTI_POINTS, jsonArray.toString())
            .putLong(KEY_MULTI_RANDOM_JITTER, config.randomJitterMs)
            .putInt(KEY_MULTI_RANDOM_RADIUS, config.randomRadiusPx)
            .apply()
    }

    fun getGridConfig(): GridConfig {
        val orderStr = prefs.getString(KEY_GRID_ORDER, GridClickOrder.BOTTOM_ROW_FIRST.name)
        val order = try {
            GridClickOrder.valueOf(orderStr ?: GridClickOrder.BOTTOM_ROW_FIRST.name)
        } catch (_: Exception) {
            GridClickOrder.BOTTOM_ROW_FIRST
        }

        val selectedCellsJson = prefs.getString(KEY_GRID_SELECTED_CELLS, "[]") ?: "[]"
        val selectedCells = mutableListOf<Pair<Int, Int>>()
        try {
            val arr = JSONArray(selectedCellsJson)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                selectedCells.add(Pair(o.getInt("r"), o.getInt("c")))
            }
        } catch (_: Exception) {}

        return GridConfig(
            columns = prefs.getInt(KEY_GRID_COLUMNS, 16),
            rows = prefs.getInt(KEY_GRID_ROWS, 2),
            bottomHeightRatio = prefs.getFloat(KEY_GRID_BOTTOM_RATIO, 0.28f),
            intervalMs = prefs.getLong(KEY_GRID_INTERVAL, 50L),
            pressDurationMs = prefs.getLong(KEY_GRID_DURATION, 20L),
            loopCount = prefs.getInt(KEY_GRID_LOOP_COUNT, 0),
            order = order,
            randomJitterMs = prefs.getLong(KEY_GRID_RANDOM_JITTER, 0L),
            randomRadiusPx = prefs.getInt(KEY_GRID_RANDOM_RADIUS, 0),
            selectedCells = selectedCells
        )
    }

    fun saveGridConfig(config: GridConfig) {
        val arr = JSONArray()
        for ((r, c) in config.selectedCells) {
            val o = JSONObject().apply {
                put("r", r)
                put("c", c)
            }
            arr.put(o)
        }

        prefs.edit()
            .putInt(KEY_GRID_COLUMNS, config.columns)
            .putInt(KEY_GRID_ROWS, config.rows)
            .putFloat(KEY_GRID_BOTTOM_RATIO, config.bottomHeightRatio)
            .putLong(KEY_GRID_INTERVAL, config.intervalMs)
            .putLong(KEY_GRID_DURATION, config.pressDurationMs)
            .putInt(KEY_GRID_LOOP_COUNT, config.loopCount)
            .putString(KEY_GRID_ORDER, config.order.name)
            .putLong(KEY_GRID_RANDOM_JITTER, config.randomJitterMs)
            .putInt(KEY_GRID_RANDOM_RADIUS, config.randomRadiusPx)
            .putString(KEY_GRID_SELECTED_CELLS, arr.toString())
            .apply()
    }
}
