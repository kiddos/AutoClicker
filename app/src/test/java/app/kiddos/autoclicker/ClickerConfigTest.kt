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
}
