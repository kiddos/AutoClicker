package app.kiddos.autoclicker.service

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import app.kiddos.autoclicker.MainActivity
import app.kiddos.autoclicker.R
import app.kiddos.autoclicker.controller.ClickerController
import app.kiddos.autoclicker.util.PermissionUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Quick Settings tile service that allows toggling the Auto Clicker floating sidebar
 * directly from Android's notification shade / Quick Settings panel.
 */
class AutoClickerTileService : TileService() {

    companion object {
        /**
         * Requests the system to update the listening state of this tile.
         * Safe to call from any context or thread.
         */
        fun requestTileUpdate(context: Context) {
            try {
                TileService.requestListeningState(
                    context.applicationContext,
                    ComponentName(context.applicationContext, AutoClickerTileService::class.java)
                )
            } catch (_: Throwable) {
                // Silently ignore if SystemUI is unavailable or tile service cannot be updated
            }
        }

        /**
         * Checks whether the current Android OS version supports requesting to add a tile
         * programmatically (Android 13 / API 33+).
         */
        fun isAddTileSupported(): Boolean {
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        }

        /**
         * Prompts the user to add the Auto Clicker tile to their Quick Settings shade.
         * Only functional on Android 13 (API 33) and above.
         */
        fun requestAddTile(context: Context, onResult: ((Int) -> Unit)? = null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val statusBarManager = context.getSystemService(StatusBarManager::class.java)
                statusBarManager?.requestAddTileService(
                    ComponentName(context.applicationContext, AutoClickerTileService::class.java),
                    context.getString(R.string.quick_settings_tile_label),
                    Icon.createWithResource(context.applicationContext, R.drawable.ic_quick_settings_tile),
                    context.mainExecutor
                ) { result ->
                    onResult?.invoke(result)
                }
            }
        }
    }

    private var listeningScope: CoroutineScope? = null
    private var listeningJob: Job? = null

    override fun onStartListening() {
        super.onStartListening()
        ClickerController.init(applicationContext)
        updateTileState()

        listeningScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        listeningJob = listeningScope?.launch {
            combine(
                ClickerController.isOverlayRunning,
                ClickerController.isPlaying
            ) { _, _ -> }
                .collect {
                    updateTileState()
                }
        }
    }

    override fun onStopListening() {
        super.onStopListening()
        listeningJob?.cancel()
        listeningScope?.cancel()
        listeningScope = null
        listeningJob = null
    }

    override fun onClick() {
        super.onClick()
        if (isLocked) {
            unlockAndRun {
                handleTileClick()
            }
        } else {
            handleTileClick()
        }
    }

    private fun handleTileClick() {
        ClickerController.init(applicationContext)

        val hasOverlay = PermissionUtils.hasOverlayPermission(this)
        val isAccessibility = PermissionUtils.isAccessibilityServiceEnabled(this)

        if (!hasOverlay || !isAccessibility) {
            Toast.makeText(
                this,
                getString(R.string.quick_settings_permission_required),
                Toast.LENGTH_LONG
            ).show()

            launchMainActivityAndCollapse()
            updateTileState()
            return
        }

        if (ClickerController.isOverlayRunning.value) {
            FloatingOverlayService.stop(this)
        } else {
            FloatingOverlayService.start(this)
        }

        updateTileState()
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun launchMainActivityAndCollapse() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    /**
     * Updates the UI elements (state, icon, label, subtitle) of the Quick Settings tile.
     */
    fun updateTileState() {
        val tile = qsTile ?: return

        val hasOverlay = PermissionUtils.hasOverlayPermission(this)
        val isAccessibility = PermissionUtils.isAccessibilityServiceEnabled(this)
        val isOverlayRunning = ClickerController.isOverlayRunning.value
        val isPlaying = ClickerController.isPlaying.value

        tile.label = getString(R.string.quick_settings_tile_label)
        tile.icon = Icon.createWithResource(this, R.drawable.ic_quick_settings_tile)

        if (!hasOverlay || !isAccessibility) {
            tile.state = Tile.STATE_INACTIVE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = getString(R.string.quick_settings_tile_subtitle_setup)
            }
        } else if (isOverlayRunning) {
            tile.state = Tile.STATE_ACTIVE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = if (isPlaying) {
                    getString(R.string.quick_settings_tile_subtitle_clicking)
                } else {
                    getString(R.string.quick_settings_tile_subtitle_active)
                }
            }
        } else {
            tile.state = Tile.STATE_INACTIVE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = getString(R.string.quick_settings_tile_subtitle_inactive)
            }
        }

        tile.updateTile()
    }
}
