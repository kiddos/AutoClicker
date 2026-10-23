package app.kiddos.autoclicker.model

enum class ClickerMode {
    SINGLE,
    MULTI,
    GRID
}

enum class StopCondition {
    INFINITE,
    COUNT,
    DURATION
}

enum class GridClickOrder {
    BOTTOM_ROW_FIRST, // Bottom row (row 2) left to right, then upper row (row 1) left to right
    TOP_ROW_FIRST,    // Upper row (row 1) left to right, then bottom row (row 2) left to right
    LEFT_TO_RIGHT,    // Row-by-row left to right
    COLUMN_BY_COLUMN, // Col 0 (r0, r1), Col 1 (r0, r1), ...
    ALTERNATING,      // Row 0 left-to-right, Row 1 right-to-left
    CUSTOM,           // User-specified custom cell click order
    RANDOM
}

data class ClickPoint(
    val id: Int,
    var x: Float,
    var y: Float,
    var delayMs: Long = 500L,
    var durationMs: Long = 50L,
    var label: String = "P$id"
)

data class SingleClickConfig(
    val intervalMs: Long = 500L,
    val pressDurationMs: Long = 50L,
    val stopCondition: StopCondition = StopCondition.INFINITE,
    val countLimit: Int = 100,
    val durationMinutes: Int = 5,
    val randomJitterMs: Long = 0L,
    val randomRadiusPx: Int = 0
)

data class MultiClickConfig(
    val intervalMs: Long = 500L,
    val pressDurationMs: Long = 50L,
    val loopCount: Int = 0, // 0 = infinite
    val points: List<ClickPoint> = emptyList(),
    val randomJitterMs: Long = 0L,
    val randomRadiusPx: Int = 0
)

data class GridConfig(
    val columns: Int = 16,
    val rows: Int = 2,
    val bottomHeightRatio: Float = 0.28f, // Occupies the bottom 28% of the screen height
    val intervalMs: Long = 50L,           // Rapid tap interval (50ms default)
    val pressDurationMs: Long = 20L,
    val loopCount: Int = 0,               // 0 = infinite
    val order: GridClickOrder = GridClickOrder.BOTTOM_ROW_FIRST,
    val randomJitterMs: Long = 0L,
    val randomRadiusPx: Int = 0,
    val marginHorizontalPx: Float = 0f,
    val marginVerticalPx: Float = 0f,
    val selectedCells: List<Pair<Int, Int>> = emptyList() // (row, col) pairs. If empty, all cells are active. If order is CUSTOM, defines execution sequence.
)

object GridGenerator {
    /**
     * Generates a configurable grid (e.g. 16x2, 10x2, 5x3) of ClickPoints positioned
     * at the bottom rows of the screen for horizontal/landscape games.
     * Allows selecting specific cells to click and custom ordering.
     */
    fun generateBottomGrid(
        columns: Int = 16,
        rows: Int = 2,
        screenWidth: Float,
        screenHeight: Float,
        bottomHeightRatio: Float = 0.28f,
        marginHorizontalPx: Float = 0f,
        marginVerticalPx: Float = 0f,
        intervalMs: Long = 50L,
        durationMs: Long = 20L,
        order: GridClickOrder = GridClickOrder.BOTTOM_ROW_FIRST,
        selectedCells: List<Pair<Int, Int>> = emptyList()
    ): List<ClickPoint> {
        val safeColumns = columns.coerceAtLeast(1)
        val safeRows = rows.coerceAtLeast(1)
        val gridHeight = screenHeight * bottomHeightRatio.coerceIn(0.05f, 0.95f)
        val gridTop = screenHeight - gridHeight - marginVerticalPx
        val availableWidth = (screenWidth - (marginHorizontalPx * 2)).coerceAtLeast(100f)

        val cellWidth = availableWidth / safeColumns
        val cellHeight = gridHeight / safeRows

        fun cellCenter(r: Int, c: Int): Pair<Float, Float> {
            val cx = marginHorizontalPx + (c + 0.5f) * cellWidth
            val cy = gridTop + (r + 0.5f) * cellHeight
            return Pair(cx, cy)
        }

        // Determine which cells to include
        val rawCells: List<Triple<Int, Int, Pair<Float, Float>>> = if (selectedCells.isNotEmpty()) {
            selectedCells
                .filter { (r, c) -> r in 0 until safeRows && c in 0 until safeColumns }
                .map { (r, c) -> Triple(r, c, cellCenter(r, c)) }
        } else {
            val all = mutableListOf<Triple<Int, Int, Pair<Float, Float>>>()
            for (r in 0 until safeRows) {
                for (c in 0 until safeColumns) {
                    all.add(Triple(r, c, cellCenter(r, c)))
                }
            }
            all
        }

        // Order the cells based on selected strategy
        val orderedCells = when (order) {
            GridClickOrder.CUSTOM -> {
                // If CUSTOM, preserve the exact user sequence defined in selectedCells
                rawCells
            }
            GridClickOrder.BOTTOM_ROW_FIRST -> {
                // Higher row index is closer to screen bottom
                rawCells.sortedWith(
                    compareByDescending<Triple<Int, Int, Pair<Float, Float>>> { it.first }
                        .thenBy { it.second }
                )
            }
            GridClickOrder.TOP_ROW_FIRST, GridClickOrder.LEFT_TO_RIGHT -> {
                rawCells.sortedWith(
                    compareBy<Triple<Int, Int, Pair<Float, Float>>> { it.first }
                        .thenBy { it.second }
                )
            }
            GridClickOrder.COLUMN_BY_COLUMN -> {
                rawCells.sortedWith(
                    compareBy<Triple<Int, Int, Pair<Float, Float>>> { it.second }
                        .thenBy { it.first }
                )
            }
            GridClickOrder.ALTERNATING -> {
                val result = mutableListOf<Triple<Int, Int, Pair<Float, Float>>>()
                for (r in 0 until safeRows) {
                    val rowCells = rawCells.filter { it.first == r }
                    if (r % 2 == 0) {
                        result.addAll(rowCells.sortedBy { it.second })
                    } else {
                        result.addAll(rowCells.sortedByDescending { it.second })
                    }
                }
                result
            }
            GridClickOrder.RANDOM -> rawCells.shuffled()
        }

        return orderedCells.mapIndexed { index, (r, c, coords) ->
            ClickPoint(
                id = index + 1,
                x = coords.first,
                y = coords.second,
                delayMs = intervalMs,
                durationMs = durationMs,
                label = "R${r + 1}C${c + 1}"
            )
        }
    }
}

data class ClickLogEntry(
    val id: Long = System.currentTimeMillis(),
    val targetIndex: Int,
    val x: Float,
    val y: Float,
    val timestamp: Long = System.currentTimeMillis(),
    val intervalFromLastMs: Long = 0L
)
