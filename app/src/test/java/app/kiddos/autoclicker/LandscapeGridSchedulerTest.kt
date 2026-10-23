package app.kiddos.autoclicker

import app.kiddos.autoclicker.controller.ClickerController
import app.kiddos.autoclicker.model.ClickerMode
import app.kiddos.autoclicker.model.GridClickOrder
import app.kiddos.autoclicker.model.GridConfig
import app.kiddos.autoclicker.model.GridGenerator
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LandscapeGridSchedulerTest {

    @Test
    fun test16x2LandscapeBottomGridGeneration() {
        val screenWidth = 2400f
        val screenHeight = 1080f
        val bottomRatio = 0.28f // bottom 28% of the screen

        val points = GridGenerator.generateBottomGrid(
            columns = 16,
            rows = 2,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            bottomHeightRatio = bottomRatio,
            intervalMs = 50L,
            durationMs = 20L,
            order = GridClickOrder.BOTTOM_ROW_FIRST
        )

        // 1. Must generate exactly 16 x 2 = 32 points
        assertEquals(32, points.size)

        // 2. All points must be placed strictly in the bottom portion of the screen
        val bottomAreaTop = screenHeight * (1f - bottomRatio) // 1080 * 0.72 = 777.6
        for (pt in points) {
            assertTrue(
                "Point ${pt.label} Y (${pt.y}) must be at or below bottom boundary ($bottomAreaTop)",
                pt.y >= bottomAreaTop
            )
            assertTrue(
                "Point ${pt.label} Y (${pt.y}) must not exceed screen height ($screenHeight)",
                pt.y <= screenHeight
            )
            assertTrue(
                "Point ${pt.label} X (${pt.x}) must be within screen width ($screenWidth)",
                pt.x in 0f..screenWidth
            )
        }

        // 3. Must have 16 distinct column coordinates spanning across the width
        val uniqueX = points.map { it.x }.distinct()
        assertEquals(16, uniqueX.size)

        // Column centers must be sorted left to right
        val sortedX = uniqueX.sorted()
        assertEquals(sortedX, uniqueX)

        // 4. Must have 2 distinct row Y coordinates
        val uniqueY = points.map { it.y }.distinct()
        assertEquals(2, uniqueY.size)
    }

    @Test
    fun testBottomRowFirstSchedulingOrder() {
        val screenWidth = 2400f
        val screenHeight = 1080f

        val points = GridGenerator.generateBottomGrid(
            columns = 16,
            rows = 2,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            bottomHeightRatio = 0.30f,
            order = GridClickOrder.BOTTOM_ROW_FIRST
        )

        assertEquals(32, points.size)

        // In BOTTOM_ROW_FIRST:
        // The first 16 points (index 0..15) are Row 2 (the bottommost row, higher Y)
        val firstRowY = points[0].y
        for (i in 0 until 16) {
            assertEquals("First 16 points must share row 2 Y", firstRowY, points[i].y, 0.01f)
            assertTrue("Label should indicate row 2", points[i].label.startsWith("R2"))
        }

        // The second 16 points (index 16..31) are Row 1 (upper row, lower Y)
        val secondRowY = points[16].y
        assertTrue("Bottom row Y must be greater than upper row Y", firstRowY > secondRowY)
        for (i in 16 until 32) {
            assertEquals("Second 16 points must share row 1 Y", secondRowY, points[i].y, 0.01f)
            assertTrue("Label should indicate row 1", points[i].label.startsWith("R1"))
        }
    }

    @Test
    fun testTopRowFirstSchedulingOrder() {
        val screenWidth = 1920f
        val screenHeight = 1080f

        val points = GridGenerator.generateBottomGrid(
            columns = 16,
            rows = 2,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            bottomHeightRatio = 0.25f,
            order = GridClickOrder.TOP_ROW_FIRST
        )

        assertEquals(32, points.size)

        // In TOP_ROW_FIRST:
        // First 16 points are Row 1 (upper row)
        for (i in 0 until 16) {
            assertTrue("First 16 points should be R1", points[i].label.startsWith("R1"))
        }

        // Next 16 points are Row 2 (bottom row)
        for (i in 16 until 32) {
            assertTrue("Second 16 points should be R2", points[i].label.startsWith("R2"))
        }
    }

    @Test
    fun testColumnByColumnSchedulingOrder() {
        val points = GridGenerator.generateBottomGrid(
            columns = 16,
            rows = 2,
            screenWidth = 2400f,
            screenHeight = 1080f,
            order = GridClickOrder.COLUMN_BY_COLUMN
        )

        assertEquals(32, points.size)

        // Points should advance column by column
        for (c in 0 until 16) {
            val ptA = points[c * 2]
            val ptB = points[c * 2 + 1]
            assertEquals("Points in same column pair must share X", ptA.x, ptB.x, 0.01f)
            assertTrue(ptA.label.contains("C${c + 1}"))
            assertTrue(ptB.label.contains("C${c + 1}"))
        }
    }

    @Test
    fun testRapidTapIntervalAndDuration() {
        val fastInterval = 20L // 50 CPS
        val pressDuration = 10L

        val points = GridGenerator.generateBottomGrid(
            columns = 16,
            rows = 2,
            screenWidth = 2400f,
            screenHeight = 1080f,
            intervalMs = fastInterval,
            durationMs = pressDuration
        )

        for (pt in points) {
            assertEquals(fastInterval, pt.delayMs)
            assertEquals(pressDuration, pt.durationMs)
        }
    }

    @Test
    fun testControllerGridModeIntegration() {
        ClickerController.setMode(ClickerMode.GRID)
        assertEquals(ClickerMode.GRID, ClickerController.currentMode.value)

        ClickerController.initializePointsForCurrentMode(2400, 1080)
        val activePoints = ClickerController.activePoints.value
        assertEquals(32, activePoints.size)

        // Change orientation / screen size to tablet landscape (2560 x 1600)
        ClickerController.handleScreenSizeChange(2560, 1600)
        val updatedPoints = ClickerController.activePoints.value
        assertEquals(32, updatedPoints.size)

        // All points must fit in the new bounds
        for (pt in updatedPoints) {
            assertTrue(pt.x in 0f..2560f)
            assertTrue(pt.y in 0f..1600f)
        }
    }

    @Test
    fun testApplyLandscapeGridHelper() {
        val applied = ClickerController.applyLandscapeGrid(
            columns = 16,
            rows = 2,
            screenWidth = 2400f,
            screenHeight = 1080f,
            bottomHeightRatio = 0.35f,
            intervalMs = 30L,
            durationMs = 15L,
            order = GridClickOrder.BOTTOM_ROW_FIRST
        )

        assertEquals(32, applied.size)
        assertEquals(32, ClickerController.activePoints.value.size)
        assertEquals(30L, ClickerController.gridConfig.value.intervalMs)
        assertEquals(15L, ClickerController.gridConfig.value.pressDurationMs)
    }

    @Test
    fun testScheduleGridExecutionFlow() = runBlocking {
        val screenWidth = 2400f
        val screenHeight = 1080f

        val points = GridGenerator.generateBottomGrid(
            columns = 16,
            rows = 2,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            intervalMs = 5L,
            durationMs = 5L,
            order = GridClickOrder.BOTTOM_ROW_FIRST
        )

        val executedCellIds = mutableListOf<Int>()
        val job = ClickerController.scheduleGridClicks(
            points = points,
            intervalMs = 5L,
            pressDurationMs = 5L,
            loops = 1,
            onCellExecuted = { pt, _ ->
                executedCellIds.add(pt.id)
            }
        )

        job.join()

        // All 32 cells should be executed in order
        assertEquals(32, executedCellIds.size)
        for (i in 1..32) {
            assertEquals(i, executedCellIds[i - 1])
        }
    }

    @Test
    fun test10x2ConfigurableGridGeneration() {
        val points = GridGenerator.generateBottomGrid(
            columns = 10,
            rows = 2,
            screenWidth = 2400f,
            screenHeight = 1080f,
            bottomHeightRatio = 0.25f,
            order = GridClickOrder.BOTTOM_ROW_FIRST
        )

        // 10 x 2 = 20 points
        assertEquals(20, points.size)
        assertEquals(10, points.map { it.x }.distinct().size)
        assertEquals(2, points.map { it.y }.distinct().size)
    }

    @Test
    fun test5x3ConfigurableGridGeneration() {
        val points = GridGenerator.generateBottomGrid(
            columns = 5,
            rows = 3,
            screenWidth = 2400f,
            screenHeight = 1080f,
            bottomHeightRatio = 0.35f,
            order = GridClickOrder.TOP_ROW_FIRST
        )

        // 5 x 3 = 15 points
        assertEquals(15, points.size)
        assertEquals(5, points.map { it.x }.distinct().size)
        assertEquals(3, points.map { it.y }.distinct().size)
    }

    @Test
    fun testSelectiveCellClicking() {
        // Only select 4 specific cells from a 16x2 grid
        val selected = listOf(
            Pair(1, 2), // Row 2, Col 3
            Pair(1, 5), // Row 2, Col 6
            Pair(0, 3), // Row 1, Col 4
            Pair(0, 8)  // Row 1, Col 9
        )

        val points = GridGenerator.generateBottomGrid(
            columns = 16,
            rows = 2,
            screenWidth = 2400f,
            screenHeight = 1080f,
            order = GridClickOrder.BOTTOM_ROW_FIRST,
            selectedCells = selected
        )

        // Must only return the 4 selected cells
        assertEquals(4, points.size)
        val labels = points.map { it.label }
        assertTrue(labels.contains("R2C3"))
        assertTrue(labels.contains("R2C6"))
        assertTrue(labels.contains("R1C4"))
        assertTrue(labels.contains("R1C9"))
    }

    @Test
    fun testCustomClickOrdering() {
        // User configures an arbitrary custom sequence of clicks:
        // R2C10 -> R1C4 -> R2C1 -> R1C8
        val customSequence = listOf(
            Pair(1, 9), // R2C10
            Pair(0, 3), // R1C4
            Pair(1, 0), // R2C1
            Pair(0, 7)  // R1C8
        )

        val points = GridGenerator.generateBottomGrid(
            columns = 16,
            rows = 2,
            screenWidth = 2400f,
            screenHeight = 1080f,
            order = GridClickOrder.CUSTOM,
            selectedCells = customSequence
        )

        assertEquals(4, points.size)
        assertEquals("R2C10", points[0].label)
        assertEquals("R1C4", points[1].label)
        assertEquals("R2C1", points[2].label)
        assertEquals("R1C8", points[3].label)
    }

    @Test
    fun testCustomOrderExecutionWithController() = runBlocking {
        val customSequence = listOf(
            Pair(1, 4), // R2C5
            Pair(0, 1), // R1C2
            Pair(1, 0)  // R2C1
        )

        val points = GridGenerator.generateBottomGrid(
            columns = 10,
            rows = 2,
            screenWidth = 2000f,
            screenHeight = 1000f,
            intervalMs = 5L,
            durationMs = 5L,
            order = GridClickOrder.CUSTOM,
            selectedCells = customSequence
        )

        val executedLabels = mutableListOf<String>()
        val job = ClickerController.scheduleGridClicks(
            points = points,
            intervalMs = 5L,
            pressDurationMs = 5L,
            loops = 1,
            onCellExecuted = { pt, _ ->
                executedLabels.add(pt.label)
            }
        )

        job.join()

        assertEquals(3, executedLabels.size)
        assertEquals("R2C5", executedLabels[0])
        assertEquals("R1C2", executedLabels[1])
        assertEquals("R2C1", executedLabels[2])
    }
}
