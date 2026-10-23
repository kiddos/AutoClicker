package app.kiddos.autoclicker.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.widget.TextViewCompat
import app.kiddos.autoclicker.MainActivity
import app.kiddos.autoclicker.R
import app.kiddos.autoclicker.controller.ClickerController
import app.kiddos.autoclicker.model.ClickPoint
import app.kiddos.autoclicker.model.ClickerMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class FloatingOverlayService : Service() {

    companion object {
        const val CHANNEL_ID = "auto_clicker_overlay_channel"
        const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            val intent = Intent(context, FloatingOverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, FloatingOverlayService::class.java)
            context.stopService(intent)
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var windowManager: WindowManager

    private var sideBarView: View? = null
    private var sideBarParams: WindowManager.LayoutParams? = null
    private var isSidebarHorizontal: Boolean = false

    // Target pointer views mapped by pointId
    private val targetViews = mutableMapOf<Int, Pair<View, WindowManager.LayoutParams>>()

    // UI references on sidebar
    private var btnPlayPause: ImageView? = null
    private var btnAddPoint: ImageView? = null
    private var btnRemovePoint: ImageView? = null
    private var btnVisibility: ImageView? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        val notification = buildForegroundNotification()
        startForeground(NOTIFICATION_ID, notification)

        ClickerController.setOverlayRunning(true)

        val metrics = resources.displayMetrics
        isSidebarHorizontal = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        ClickerController.initializePointsForCurrentMode(metrics.widthPixels, metrics.heightPixels)

        createSideBarView()
        syncTargetPointers()
        observeControllerStates()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()

        // Remove sidebar
        sideBarView?.let {
            safeRemoveView(it)
        }
        sideBarView = null

        // Remove all target pointers
        targetViews.values.forEach { (view, _) ->
            safeRemoveView(view)
        }
        targetViews.clear()

        ClickerController.setOverlayRunning(false)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_description)
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(R.drawable.ic_play)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val metrics = resources.displayMetrics
        val isLandscape = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE

        // Auto-switch sidebar orientation to horizontal when entering landscape
        if (isLandscape && !isSidebarHorizontal) {
            isSidebarHorizontal = true
            recreateSideBarView()
        }

        ClickerController.handleScreenSizeChange(metrics.widthPixels, metrics.heightPixels)
        clampViewsToScreen(metrics.widthPixels, metrics.heightPixels)
        syncTargetPointers()
    }

    private fun clampViewsToScreen(screenWidth: Int, screenHeight: Int) {
        sideBarParams?.let { params ->
            val maxW = (screenWidth - dpToPx(80)).coerceAtLeast(0)
            val maxH = (screenHeight - dpToPx(80)).coerceAtLeast(0)
            params.x = params.x.coerceIn(0, maxW)
            params.y = params.y.coerceIn(0, maxH)
            sideBarView?.let { v ->
                try {
                    windowManager.updateViewLayout(v, params)
                } catch (_: Exception) {}
            }
        }
    }

    private fun toggleSidebarOrientation() {
        isSidebarHorizontal = !isSidebarHorizontal
        recreateSideBarView()
    }

    private fun recreateSideBarView() {
        sideBarView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {}
        }
        sideBarView = null
        createSideBarView()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createSideBarView() {
        val isHorizontal = isSidebarHorizontal
        val layout = LinearLayout(this).apply {
            orientation = if (isHorizontal) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_sidebar)
            setPadding(dpToPx(6), dpToPx(6), dpToPx(6), dpToPx(6))
            gravity = Gravity.CENTER
        }

        val btnSize = dpToPx(38)
        val iconPadding = dpToPx(7)

        fun createBtnLayoutParams(): LinearLayout.LayoutParams {
            return if (isHorizontal) {
                LinearLayout.LayoutParams(btnSize, btnSize).apply {
                    rightMargin = dpToPx(4)
                }
            } else {
                LinearLayout.LayoutParams(btnSize, btnSize).apply {
                    bottomMargin = dpToPx(4)
                }
            }
        }

        // 1. Drag Handle
        val dragHandle = ImageView(this).apply {
            layoutParams = if (isHorizontal) {
                LinearLayout.LayoutParams(dpToPx(20), btnSize).apply {
                    rightMargin = dpToPx(4)
                }
            } else {
                LinearLayout.LayoutParams(btnSize, dpToPx(20)).apply {
                    bottomMargin = dpToPx(4)
                }
            }
            setImageResource(R.drawable.ic_drag)
            setColorFilter(Color.parseColor("#B0BEC5"))
            scaleType = ImageView.ScaleType.CENTER_INSIDE
        }
        layout.addView(dragHandle)

        // 2. Play / Pause Button
        btnPlayPause = ImageView(this).apply {
            layoutParams = createBtnLayoutParams()
            val isPlaying = ClickerController.isPlaying.value
            setImageResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play)
            setColorFilter(if (isPlaying) Color.parseColor("#FFA000") else Color.parseColor("#4CAF50"))
            setPadding(iconPadding, iconPadding, iconPadding, iconPadding)
            setOnClickListener {
                ClickerController.togglePlayPause()
            }
        }
        layout.addView(btnPlayPause)

        val isMultiMode = ClickerController.currentMode.value == ClickerMode.MULTI

        // 3. Add Point Button (Multi Mode)
        btnAddPoint = ImageView(this).apply {
            layoutParams = createBtnLayoutParams()
            setImageResource(R.drawable.ic_add)
            setColorFilter(Color.WHITE)
            setPadding(iconPadding, iconPadding, iconPadding, iconPadding)
            visibility = if (isMultiMode) View.VISIBLE else View.GONE
            setOnClickListener {
                val metrics = resources.displayMetrics
                val newPoint = ClickerController.addPoint(
                    (metrics.widthPixels / 2).toFloat(),
                    (metrics.heightPixels / 2).toFloat()
                )
                addTargetPointerView(newPoint)
            }
        }
        layout.addView(btnAddPoint)

        // 4. Remove Point Button (Multi Mode)
        btnRemovePoint = ImageView(this).apply {
            layoutParams = createBtnLayoutParams()
            setImageResource(R.drawable.ic_remove)
            setColorFilter(Color.WHITE)
            setPadding(iconPadding, iconPadding, iconPadding, iconPadding)
            visibility = if (isMultiMode) View.VISIBLE else View.GONE
            setOnClickListener {
                val removed = ClickerController.removeLastPoint()
                if (removed != null) {
                    removeTargetPointerView(removed.id)
                }
            }
        }
        layout.addView(btnRemovePoint)

        // 5. Visibility Toggle Button
        btnVisibility = ImageView(this).apply {
            layoutParams = createBtnLayoutParams()
            val visible = ClickerController.targetsVisible.value
            setImageResource(if (visible) R.drawable.ic_visibility else R.drawable.ic_visibility_off)
            setColorFilter(if (visible) Color.WHITE else Color.parseColor("#B0BEC5"))
            setPadding(iconPadding, iconPadding, iconPadding, iconPadding)
            setOnClickListener {
                ClickerController.toggleTargetsVisibility()
            }
        }
        layout.addView(btnVisibility)

        // 6. Orientation Toggle (Vertical / Horizontal sidebar)
        val btnOrientationToggle = ImageView(this).apply {
            layoutParams = createBtnLayoutParams()
            setImageResource(R.drawable.ic_rotate)
            setColorFilter(Color.parseColor("#80D8FF"))
            setPadding(iconPadding, iconPadding, iconPadding, iconPadding)
            setOnClickListener {
                toggleSidebarOrientation()
            }
        }
        layout.addView(btnOrientationToggle)

        // 7. Settings / Open App Button
        val btnSettings = ImageView(this).apply {
            layoutParams = createBtnLayoutParams()
            setImageResource(R.drawable.ic_settings)
            setColorFilter(Color.WHITE)
            setPadding(iconPadding, iconPadding, iconPadding, iconPadding)
            setOnClickListener {
                val intent = Intent(this@FloatingOverlayService, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                startActivity(intent)
            }
        }
        layout.addView(btnSettings)

        // 8. Close Button
        val btnClose = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(btnSize, btnSize)
            setImageResource(R.drawable.ic_close)
            setColorFilter(Color.parseColor("#EF5350"))
            setPadding(iconPadding, iconPadding, iconPadding, iconPadding)
            setOnClickListener {
                ClickerController.stopAndDismiss()
                stopSelf()
            }
        }
        layout.addView(btnClose)

        // Setup Window LayoutParams for SideBar
        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val existingParams = sideBarParams
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
            x = existingParams?.x ?: dpToPx(12)
            y = existingParams?.y ?: dpToPx(120)
        }

        // Dragging handler for the sidebar
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        dragHandle.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(layout, params)
                    true
                }
                else -> false
            }
        }

        sideBarView = layout
        sideBarParams = params
        windowManager.addView(layout, params)
    }

    private fun safeRemoveView(view: View) {
        try {
            if (view.isAttachedToWindow) {
                windowManager.removeViewImmediate(view)
            } else {
                view.post {
                    try {
                        windowManager.removeViewImmediate(view)
                    } catch (_: Exception) {
                        try {
                            windowManager.removeView(view)
                        } catch (_: Exception) {}
                    }
                }
            }
        } catch (_: Exception) {
            try {
                windowManager.removeView(view)
            } catch (_: Exception) {}
        }
    }

    private fun syncTargetPointers() {
        val points = ClickerController.activePoints.value
        val pointMap = points.associateBy { it.id }

        // 1. Remove pointers that no longer exist
        val existingIds = targetViews.keys.toList()
        for (id in existingIds) {
            if (!pointMap.containsKey(id)) {
                targetViews.remove(id)?.let { (view, _) ->
                    safeRemoveView(view)
                }
            }
        }

        // 2. Add or update pointers
        val totalPoints = points.size
        val isCompact = totalPoints > 10 || ClickerController.currentMode.value == ClickerMode.GRID
        val targetSize = dpToPx(if (isCompact) 32 else 46)
        val visible = ClickerController.targetsVisible.value

        points.forEach { point ->
            val existing = targetViews[point.id]
            if (existing != null) {
                val (view, params) = existing
                params.width = targetSize
                params.height = targetSize
                params.x = (point.x - targetSize / 2).toInt().coerceAtLeast(0)
                params.y = (point.y - targetSize / 2).toInt().coerceAtLeast(0)
                view.visibility = if (visible) View.VISIBLE else View.GONE
                (view as? FrameLayout)?.let { fl ->
                    (fl.getChildAt(0) as? TextView)?.text = point.label
                }
                try {
                    windowManager.updateViewLayout(view, params)
                } catch (_: Exception) {}
            } else {
                addTargetPointerView(point)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun addTargetPointerView(point: ClickPoint) {
        val totalPoints = ClickerController.activePoints.value.size
        val isCompact = totalPoints > 10 || ClickerController.currentMode.value == ClickerMode.GRID
        val targetSize = dpToPx(if (isCompact) 32 else 46)
        val textSizeSp = if (isCompact) 10f else 15f

        val frameLayout = FrameLayout(this).apply {
            setBackgroundResource(R.drawable.bg_target_point)
            visibility = if (ClickerController.targetsVisible.value) View.VISIBLE else View.GONE
        }

        val textView = TextView(this).apply {
            text = point.label
            setTextColor(Color.WHITE)
            isSingleLine = true
            maxLines = 1
            includeFontPadding = false
            gravity = Gravity.CENTER
            TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                this,
                5,
                if (isCompact) 10 else 15,
                1,
                TypedValue.COMPLEX_UNIT_SP
            )
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        frameLayout.addView(textView)

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            targetSize,
            targetSize,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
            x = (point.x - targetSize / 2).toInt().coerceAtLeast(0)
            y = (point.y - targetSize / 2).toInt().coerceAtLeast(0)
        }

        var startX = 0
        var startY = 0
        var touchStartX = 0f
        var touchStartY = 0f

        frameLayout.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params.x
                    startY = params.y
                    touchStartX = event.rawX
                    touchStartY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = startX + (event.rawX - touchStartX).toInt()
                    params.y = startY + (event.rawY - touchStartY).toInt()
                    windowManager.updateViewLayout(frameLayout, params)

                    // Update center coordinate in controller
                    val centerX = params.x + targetSize / 2f
                    val centerY = params.y + targetSize / 2f
                    ClickerController.updatePointPosition(point.id, centerX, centerY)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val centerX = params.x + targetSize / 2f
                    val centerY = params.y + targetSize / 2f
                    ClickerController.updatePointPosition(point.id, centerX, centerY)
                    true
                }
                else -> false
            }
        }

        targetViews[point.id] = Pair(frameLayout, params)
        windowManager.addView(frameLayout, params)
    }

    private fun removeTargetPointerView(pointId: Int) {
        targetViews.remove(pointId)?.let { (view, _) ->
            safeRemoveView(view)
        }
    }

    private fun observeControllerStates() {
        // 1. Observe isPlaying to toggle play/pause icon and color
        serviceScope.launch {
            ClickerController.isPlaying.collectLatest { isPlaying ->
                btnPlayPause?.apply {
                    if (isPlaying) {
                        setImageResource(R.drawable.ic_pause)
                        setColorFilter(Color.parseColor("#FFA000")) // Amber pause
                    } else {
                        setImageResource(R.drawable.ic_play)
                        setColorFilter(Color.parseColor("#4CAF50")) // Green play
                    }
                }
            }
        }

        // 2. Observe activeTargetHighlight to give pulse feedback
        serviceScope.launch {
            ClickerController.activeTargetHighlight.collectLatest { activeId ->
                targetViews.forEach { (id, pair) ->
                    val (view, _) = pair
                    if (id == activeId) {
                        view.setBackgroundResource(R.drawable.bg_target_point_active)
                        view.scaleX = 1.15f
                        view.scaleY = 1.15f
                    } else {
                        view.setBackgroundResource(R.drawable.bg_target_point)
                        view.scaleX = 1.0f
                        view.scaleY = 1.0f
                    }
                }
            }
        }

        // 3. Observe targetsVisible to toggle target overlays visibility
        serviceScope.launch {
            ClickerController.targetsVisible.collectLatest { visible ->
                btnVisibility?.apply {
                    if (visible) {
                        setImageResource(R.drawable.ic_visibility)
                        setColorFilter(Color.WHITE)
                    } else {
                        setImageResource(R.drawable.ic_visibility_off)
                        setColorFilter(Color.parseColor("#B0BEC5"))
                    }
                }
                targetViews.values.forEach { (view, _) ->
                    view.visibility = if (visible) View.VISIBLE else View.GONE
                }
            }
        }

        // 4. Observe isOverlayRunning to stop service if cancelled from elsewhere
        serviceScope.launch {
            ClickerController.isOverlayRunning.collectLatest { running ->
                if (!running) {
                    stopSelf()
                }
            }
        }

        // 5. Observe mode to show/hide add & remove buttons
        serviceScope.launch {
            ClickerController.currentMode.collectLatest { mode ->
                val isMulti = mode == ClickerMode.MULTI
                btnAddPoint?.visibility = if (isMulti) View.VISIBLE else View.GONE
                btnRemovePoint?.visibility = if (isMulti) View.VISIBLE else View.GONE
            }
        }

        // 6. Observe active points changes (e.g., grid presets applied)
        serviceScope.launch {
            ClickerController.activePoints.collectLatest {
                syncTargetPointers()
            }
        }
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }
}
