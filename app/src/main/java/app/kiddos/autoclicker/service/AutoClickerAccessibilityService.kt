package app.kiddos.autoclicker.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import app.kiddos.autoclicker.controller.ClickerController

class AutoClickerAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "AutoClickerService"
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Accessibility Service Connected")
        ClickerController.attachAccessibilityService(this)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "Accessibility Service Unbind")
        ClickerController.detachAccessibilityService()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Accessibility Service Destroyed")
        ClickerController.detachAccessibilityService()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not used, gestures are driven by ClickerController
    }

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility Service Interrupted")
    }

    fun performClick(x: Float, y: Float, durationMs: Long, onComplete: () -> Unit) {
        val clickPath = Path().apply {
            moveTo(x.coerceAtLeast(0f), y.coerceAtLeast(0f))
        }
        val stroke = GestureDescription.StrokeDescription(
            clickPath,
            0,
            durationMs.coerceIn(1L, 1000L)
        )
        val gesture = GestureDescription.Builder()
            .addStroke(stroke)
            .build()

        dispatchGesture(
            gesture,
            object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    super.onCompleted(gestureDescription)
                    onComplete()
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    super.onCancelled(gestureDescription)
                    onComplete()
                }
            },
            null
        )
    }
}
