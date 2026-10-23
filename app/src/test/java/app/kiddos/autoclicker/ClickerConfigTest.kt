package app.kiddos.autoclicker

import app.kiddos.autoclicker.controller.ClickerController
import app.kiddos.autoclicker.model.ClickPoint
import app.kiddos.autoclicker.model.ClickerMode
import app.kiddos.autoclicker.model.MultiClickConfig
import app.kiddos.autoclicker.model.SingleClickConfig
import app.kiddos.autoclicker.model.StopCondition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClickerConfigTest {

    @Test
    fun testSingleClickConfigDefaults() {
        val config = SingleClickConfig()
        assertEquals(500L, config.intervalMs)
        assertEquals(50L, config.pressDurationMs)
        assertEquals(StopCondition.INFINITE, config.stopCondition)
        assertEquals(100, config.countLimit)
        assertEquals(5, config.durationMinutes)
        assertEquals(0L, config.randomJitterMs)
        assertEquals(0, config.randomRadiusPx)
    }

    @Test
    fun testMultiClickConfigDefaults() {
        val config = MultiClickConfig()
        assertEquals(500L, config.intervalMs)
        assertEquals(50L, config.pressDurationMs)
        assertEquals(0, config.loopCount)
        assertTrue(config.points.isEmpty())
    }

    @Test
    fun testPointManagementInController() {
        ClickerController.setMode(ClickerMode.MULTI)
        ClickerController.initializePointsForCurrentMode(1080, 2400)

        val initialCount = ClickerController.activePoints.value.size
        assertTrue(initialCount >= 1)

        val newPoint = ClickerController.addPoint(500f, 600f)
        assertNotNull(newPoint)
        assertEquals(initialCount + 1, ClickerController.activePoints.value.size)

        ClickerController.updatePointPosition(newPoint.id, 750f, 850f)
        val updated = ClickerController.activePoints.value.firstOrNull { it.id == newPoint.id }
        assertNotNull(updated)
        assertEquals(750f, updated?.x)
        assertEquals(850f, updated?.y)

        val removed = ClickerController.removeLastPoint()
        assertEquals(newPoint.id, removed?.id)
        assertEquals(initialCount, ClickerController.activePoints.value.size)
    }

    @Test
    fun testModeSwitching() {
        ClickerController.setMode(ClickerMode.SINGLE)
        assertEquals(ClickerMode.SINGLE, ClickerController.currentMode.value)

        ClickerController.setMode(ClickerMode.MULTI)
        assertEquals(ClickerMode.MULTI, ClickerController.currentMode.value)
    }

    @Test
    fun testMultiClickDelayUpdateRespectsNewInterval() {
        ClickerController.setMode(ClickerMode.MULTI)
        ClickerController.initializePointsForCurrentMode(1080, 2400)

        // Set interval to 120ms
        ClickerController.updateMultiConfig(MultiClickConfig(intervalMs = 120L))
        assertEquals(120L, ClickerController.multiConfig.value.intervalMs)

        // Active points should have 0L (meaning follow sequence default of 120ms)
        for (pt in ClickerController.activePoints.value) {
            assertEquals(0L, pt.delayMs)
        }

        // Add a new point and verify it also defaults to 0L
        val added = ClickerController.addPoint(300f, 400f)
        assertEquals(0L, added.delayMs)

        // Change interval again to 30ms
        ClickerController.updateMultiConfig(MultiClickConfig(intervalMs = 30L))
        assertEquals(30L, ClickerController.multiConfig.value.intervalMs)
        for (pt in ClickerController.activePoints.value) {
            assertEquals(0L, pt.delayMs)
        }
    }

    @Test
    fun testCustomPointDelayPreservation() {
        ClickerController.setMode(ClickerMode.MULTI)
        ClickerController.initializePointsForCurrentMode(1080, 2400)

        val firstPt = ClickerController.activePoints.value.first()
        // Set a custom delay of 1500ms on first point
        ClickerController.updatePointSettings(firstPt.id, 1500L, 50L)

        // Update sequence default to 100ms
        ClickerController.updateMultiConfig(MultiClickConfig(intervalMs = 100L))

        val updatedFirst = ClickerController.activePoints.value.first { it.id == firstPt.id }
        assertEquals(1500L, updatedFirst.delayMs)

        // Other points should still be 0L (using default 100ms)
        val otherPoints = ClickerController.activePoints.value.filter { it.id != firstPt.id }
        for (pt in otherPoints) {
            assertEquals(0L, pt.delayMs)
        }
    }
}
