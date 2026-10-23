package app.kiddos.autoclicker

import android.os.Build
import app.kiddos.autoclicker.controller.ClickerController
import app.kiddos.autoclicker.service.AutoClickerTileService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoClickerTileServiceTest {

    @Test
    fun testIsAddTileSupportedReflectsSdkVersion() {
        val expected = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        assertEquals(expected, AutoClickerTileService.isAddTileSupported())
    }

    @Test
    fun testOverlayRunningStateTransitions() {
        ClickerController.setOverlayRunning(false)
        assertFalse(ClickerController.isOverlayRunning.value)

        ClickerController.setOverlayRunning(true)
        assertTrue(ClickerController.isOverlayRunning.value)

        ClickerController.setOverlayRunning(false)
        assertFalse(ClickerController.isOverlayRunning.value)
    }

    @Test
    fun testTargetsVisibilityToggle() {
        val initial = ClickerController.targetsVisible.value
        ClickerController.toggleTargetsVisibility()
        assertEquals(!initial, ClickerController.targetsVisible.value)
        ClickerController.toggleTargetsVisibility()
        assertEquals(initial, ClickerController.targetsVisible.value)
    }
}
