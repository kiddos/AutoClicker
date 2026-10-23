package app.kiddos.autoclicker.controller

import android.content.Context
import android.os.SystemClock
import app.kiddos.autoclicker.data.ClickerPreferences
import app.kiddos.autoclicker.model.ClickLogEntry
import app.kiddos.autoclicker.model.ClickPoint
import app.kiddos.autoclicker.model.ClickerMode
import app.kiddos.autoclicker.model.GridClickOrder
import app.kiddos.autoclicker.model.GridConfig
import app.kiddos.autoclicker.model.GridGenerator
import app.kiddos.autoclicker.model.MultiClickConfig
import app.kiddos.autoclicker.model.SingleClickConfig
import app.kiddos.autoclicker.model.StopCondition
import app.kiddos.autoclicker.service.AutoClickerAccessibilityService
import app.kiddos.autoclicker.service.AutoClickerTileService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

object ClickerController {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var clickLoopJob: Job? = null

    private var accessibilityService: AutoClickerAccessibilityService? = null
    private var preferences: ClickerPreferences? = null

    // Flows
    private val _isAccessibilityEnabled = MutableStateFlow(false)
    val isAccessibilityEnabled: StateFlow<Boolean> = _isAccessibilityEnabled.asStateFlow()

    private val _isOverlayRunning = MutableStateFlow(false)
    val isOverlayRunning: StateFlow<Boolean> = _isOverlayRunning.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentMode = MutableStateFlow(ClickerMode.SINGLE)
    val currentMode: StateFlow<ClickerMode> = _currentMode.asStateFlow()

    private val _singleConfig = MutableStateFlow(SingleClickConfig())
    val singleConfig: StateFlow<SingleClickConfig> = _singleConfig.asStateFlow()

    private val _multiConfig = MutableStateFlow(MultiClickConfig())
    val multiConfig: StateFlow<MultiClickConfig> = _multiConfig.asStateFlow()

    private val _gridConfig = MutableStateFlow(GridConfig())
    val gridConfig: StateFlow<GridConfig> = _gridConfig.asStateFlow()

    private val _activePoints = MutableStateFlow<List<ClickPoint>>(emptyList())
    val activePoints: StateFlow<List<ClickPoint>> = _activePoints.asStateFlow()

    private val _targetsVisible = MutableStateFlow(true)
    val targetsVisible: StateFlow<Boolean> = _targetsVisible.asStateFlow()

    private val _clicksExecuted = MutableStateFlow(0L)
    val clicksExecuted: StateFlow<Long> = _clicksExecuted.asStateFlow()

    private val _currentPointIndex = MutableStateFlow(0)
    val currentPointIndex: StateFlow<Int> = _currentPointIndex.asStateFlow()

    private val _activeTargetHighlight = MutableStateFlow<Int?>(null)
    val activeTargetHighlight: StateFlow<Int?> = _activeTargetHighlight.asStateFlow()

    private val _recentLogs = MutableStateFlow<List<ClickLogEntry>>(emptyList())
    val recentLogs: StateFlow<List<ClickLogEntry>> = _recentLogs.asStateFlow()

    // Test app event flow for real-time visual pulses
    private val _testClickEvents = MutableSharedFlow<ClickLogEntry>(extraBufferCapacity = 64)
    val testClickEvents: SharedFlow<ClickLogEntry> = _testClickEvents.asSharedFlow()

    private var executionStartTime = 0L
    private var lastClickTimestamp = 0L
    private var appContext: Context? = null

    fun init(context: Context) {
        if (appContext == null) {
            appContext = context.applicationContext
        }
        if (preferences == null) {
            preferences = ClickerPreferences(context.applicationContext)
            loadConfigs()
        }
    }

    fun loadConfigs() {
        preferences?.let { prefs ->
            _currentMode.value = prefs.getMode()
            _singleConfig.value = prefs.getSingleClickConfig()
            val multi = prefs.getMultiClickConfig()
            _multiConfig.value = multi
            if (multi.points.isNotEmpty() && _activePoints.value.isEmpty()) {
                _activePoints.value = multi.points
            }
            _gridConfig.value = prefs.getGridConfig()
        }
    }

    fun updateSingleConfig(config: SingleClickConfig) {
        _singleConfig.value = config
        preferences?.saveSingleClickConfig(config)
    }

    fun updateMultiConfig(config: MultiClickConfig) {
        val oldInterval = _multiConfig.value.intervalMs
        val isIntervalChanged = config.intervalMs != oldInterval

        val basePoints = if (config.points.isNotEmpty()) config.points else _activePoints.value
        val updatedPoints = basePoints.map { pt ->
            if (isIntervalChanged && (pt.delayMs == oldInterval || pt.delayMs == 500L || pt.delayMs <= 0L)) {
                pt.copy(delayMs = 0L)
            } else if (pt.delayMs == 500L && config.intervalMs != 500L) {
                pt.copy(delayMs = 0L)
            } else {
                pt
            }
        }

        val finalConfig = config.copy(points = updatedPoints)
        _multiConfig.value = finalConfig
        if (_currentMode.value == ClickerMode.MULTI) {
            _activePoints.value = updatedPoints
        }
        preferences?.saveMultiClickConfig(finalConfig)
    }

    fun updateGridConfig(config: GridConfig) {
        _gridConfig.value = config
        preferences?.saveGridConfig(config)
    }

    fun setMode(mode: ClickerMode) {
        if (_isPlaying.value) {
            pauseClicking()
        }
        _currentMode.value = mode
        preferences?.setMode(mode)
        when (mode) {
            ClickerMode.SINGLE -> {
                if (_activePoints.value.size != 1) {
                    val p = _activePoints.value.firstOrNull()
                    if (p != null) {
                        _activePoints.value = listOf(p.copy(id = 1, label = "1"))
                    }
                }
            }
            ClickerMode.MULTI -> {
                if (_multiConfig.value.points.isNotEmpty()) {
                    _activePoints.value = _multiConfig.value.points
                }
            }
            ClickerMode.GRID -> {
                // Preserved for grid
            }
        }
    }

    fun attachAccessibilityService(service: AutoClickerAccessibilityService) {
        accessibilityService = service
        _isAccessibilityEnabled.value = true
    }

    fun detachAccessibilityService() {
        accessibilityService = null
        _isAccessibilityEnabled.value = false
        if (_isPlaying.value) {
            pauseClicking()
        }
    }

    fun setOverlayRunning(running: Boolean) {
        _isOverlayRunning.value = running
        if (!running && _isPlaying.value) {
            pauseClicking()
        }
        appContext?.let { ctx ->
            AutoClickerTileService.requestTileUpdate(ctx)
        }
    }

    fun toggleTargetsVisibility() {
        _targetsVisible.value = !_targetsVisible.value
    }

    fun togglePlayPause() {
        if (_isPlaying.value) {
            pauseClicking()
        } else {
            startClicking()
        }
    }

    fun startClicking() {
        if (_isPlaying.value) return
        _isPlaying.value = true
        executionStartTime = SystemClock.elapsedRealtime()
        _clicksExecuted.value = 0L
        _currentPointIndex.value = 0

        clickLoopJob?.cancel()
        clickLoopJob = scope.launch {
            runClickLoop()
        }
        appContext?.let { ctx ->
            AutoClickerTileService.requestTileUpdate(ctx)
        }
    }

    fun pauseClicking() {
        _isPlaying.value = false
        clickLoopJob?.cancel()
        clickLoopJob = null
        _activeTargetHighlight.value = null
        appContext?.let { ctx ->
            AutoClickerTileService.requestTileUpdate(ctx)
        }
    }

    fun stopAndDismiss() {
        pauseClicking()
        _isOverlayRunning.value = false
        appContext?.let { ctx ->
            AutoClickerTileService.requestTileUpdate(ctx)
        }
    }

    fun resetStats() {
        _clicksExecuted.value = 0L
        _recentLogs.value = emptyList()
    }

    // Points management
    fun initializePointsForCurrentMode(screenWidth: Int, screenHeight: Int) {
        val centerX = (screenWidth / 2).toFloat().coerceAtLeast(200f)
        val centerY = (screenHeight / 2).toFloat().coerceAtLeast(300f)

        when (_currentMode.value) {
            ClickerMode.SINGLE -> {
                _activePoints.value = listOf(
                    ClickPoint(id = 1, x = centerX, y = centerY, label = "1")
                )
            }
            ClickerMode.MULTI -> {
                val currentPoints = _activePoints.value
                if (currentPoints.isEmpty()) {
                    val savedPoints = _multiConfig.value.points
                    if (savedPoints.isNotEmpty()) {
                        _activePoints.value = savedPoints
                    } else {
                        _activePoints.value = listOf(
                            ClickPoint(id = 1, x = centerX, y = centerY - 150f, delayMs = 0L, durationMs = _multiConfig.value.pressDurationMs, label = "1"),
                            ClickPoint(id = 2, x = centerX, y = centerY + 150f, delayMs = 0L, durationMs = _multiConfig.value.pressDurationMs, label = "2")
                        )
                    }
                }
            }
            ClickerMode.GRID -> {
                val config = _gridConfig.value
                val points = GridGenerator.generateBottomGrid(
                    columns = config.columns,
                    rows = config.rows,
                    screenWidth = screenWidth.toFloat(),
                    screenHeight = screenHeight.toFloat(),
                    bottomHeightRatio = config.bottomHeightRatio,
                    marginHorizontalPx = config.marginHorizontalPx,
                    marginVerticalPx = config.marginVerticalPx,
                    intervalMs = config.intervalMs,
                    durationMs = config.pressDurationMs,
                    order = config.order,
                    selectedCells = config.selectedCells
                )
                _activePoints.value = points
            }
        }
    }

    /**
     * Applies a configurable (e.g. 16x2, 10x2, 5x3) bottom landscape grid to active points.
     */
    fun applyLandscapeGrid(
        columns: Int = 16,
        rows: Int = 2,
        screenWidth: Float,
        screenHeight: Float,
        bottomHeightRatio: Float = 0.28f,
        intervalMs: Long = 50L,
        durationMs: Long = 20L,
        order: GridClickOrder = GridClickOrder.BOTTOM_ROW_FIRST,
        marginHorizontalPx: Float = 0f,
        marginVerticalPx: Float = 0f,
        selectedCells: List<Pair<Int, Int>> = emptyList()
    ): List<ClickPoint> {
        val updatedConfig = _gridConfig.value.copy(
            columns = columns,
            rows = rows,
            bottomHeightRatio = bottomHeightRatio,
            intervalMs = intervalMs,
            pressDurationMs = durationMs,
            order = order,
            marginHorizontalPx = marginHorizontalPx,
            marginVerticalPx = marginVerticalPx,
            selectedCells = selectedCells
        )
        updateGridConfig(updatedConfig)

        val points = GridGenerator.generateBottomGrid(
            columns = columns,
            rows = rows,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            bottomHeightRatio = bottomHeightRatio,
            marginHorizontalPx = marginHorizontalPx,
            marginVerticalPx = marginVerticalPx,
            intervalMs = intervalMs,
            durationMs = durationMs,
            order = order,
            selectedCells = selectedCells
        )
        _activePoints.value = points
        return points
    }

    /**
     * Rescales or clamps points when the screen orientation / size changes.
     */
    fun handleScreenSizeChange(newWidth: Int, newHeight: Int) {
        if (_currentMode.value == ClickerMode.GRID) {
            // Re-generate grid points to cleanly match the new dimensions
            val config = _gridConfig.value
            _activePoints.value = GridGenerator.generateBottomGrid(
                columns = config.columns,
                rows = config.rows,
                screenWidth = newWidth.toFloat(),
                screenHeight = newHeight.toFloat(),
                bottomHeightRatio = config.bottomHeightRatio,
                marginHorizontalPx = config.marginHorizontalPx,
                marginVerticalPx = config.marginVerticalPx,
                intervalMs = config.intervalMs,
                durationMs = config.pressDurationMs,
                order = config.order,
                selectedCells = config.selectedCells
            )
        } else {
            // Clamp existing points inside screen boundaries
            val clamped = _activePoints.value.map { pt ->
                val cx = pt.x.coerceIn(24f, (newWidth - 24).toFloat().coerceAtLeast(24f))
                val cy = pt.y.coerceIn(24f, (newHeight - 24).toFloat().coerceAtLeast(24f))
                if (cx != pt.x || cy != pt.y) pt.copy(x = cx, y = cy) else pt
            }
            _activePoints.value = clamped
        }
    }

    /**
     * Schedules a quick auto-click execution sequence across specified or active grid points.
     */
    fun scheduleGridClicks(
        points: List<ClickPoint> = _activePoints.value,
        intervalMs: Long = _gridConfig.value.intervalMs,
        pressDurationMs: Long = _gridConfig.value.pressDurationMs,
        loops: Int = 1,
        onCellExecuted: ((ClickPoint, Int) -> Unit)? = null,
        onComplete: (() -> Unit)? = null
    ): Job {
        return scope.launch {
            var currentLoop = 0
            while (loops <= 0 || currentLoop < loops) {
                for ((index, pt) in points.withIndex()) {
                    executeSingleClick(pt, pressDurationMs, 0L, 0)
                    onCellExecuted?.invoke(pt, index)
                    delay(intervalMs.coerceAtLeast(5L))
                }
                currentLoop++
            }
            onComplete?.invoke()
        }
    }

    fun addPoint(defaultX: Float, defaultY: Float): ClickPoint {
        val currentList = _activePoints.value.toMutableList()
        val nextId = (currentList.maxOfOrNull { it.id } ?: 0) + 1
        // Offset slightly if near previous point
        val offsetX = (currentList.size * 30 % 150).toFloat()
        val offsetY = (currentList.size * 30 % 150).toFloat()
        val newPoint = ClickPoint(
            id = nextId,
            x = defaultX + offsetX,
            y = defaultY + offsetY,
            delayMs = 0L, // 0L means use sequence default interval
            durationMs = _multiConfig.value.pressDurationMs,
            label = "$nextId"
        )
        currentList.add(newPoint)
        _activePoints.value = currentList
        saveActivePointsToMultiConfig()
        return newPoint
    }

    fun updatePointSettings(id: Int, delayMs: Long, durationMs: Long) {
        val currentList = _activePoints.value.map {
            if (it.id == id) it.copy(delayMs = delayMs, durationMs = durationMs) else it
        }
        _activePoints.value = currentList
        saveActivePointsToMultiConfig()
    }

    fun removeLastPoint(): ClickPoint? {
        val currentList = _activePoints.value.toMutableList()
        if (currentList.size > 1) { // keep at least 1 point
            val removed = currentList.removeAt(currentList.lastIndex)
            _activePoints.value = currentList
            saveActivePointsToMultiConfig()
            return removed
        }
        return null
    }

    fun removePointById(id: Int) {
        val currentList = _activePoints.value.toMutableList()
        if (currentList.size > 1) {
            currentList.removeAll { it.id == id }
            _activePoints.value = currentList
            saveActivePointsToMultiConfig()
        }
    }

    fun updatePointPosition(id: Int, x: Float, y: Float) {
        val currentList = _activePoints.value.map {
            if (it.id == id) it.copy(x = x, y = y) else it
        }
        _activePoints.value = currentList
        saveActivePointsToMultiConfig()
    }

    private fun saveActivePointsToMultiConfig() {
        if (_currentMode.value == ClickerMode.MULTI) {
            val updated = _multiConfig.value.copy(points = _activePoints.value)
            updateMultiConfig(updated)
        }
    }

    // Direct recording for the interactive Test Bench inside the app
    fun recordTestClick(targetIndex: Int, x: Float, y: Float) {
        val now = System.currentTimeMillis()
        val interval = if (lastClickTimestamp > 0L) now - lastClickTimestamp else 0L
        lastClickTimestamp = now

        val log = ClickLogEntry(
            id = now,
            targetIndex = targetIndex,
            x = x,
            y = y,
            timestamp = now,
            intervalFromLastMs = interval
        )
        _testClickEvents.tryEmit(log)

        val updated = (_recentLogs.value.take(29) + log).sortedByDescending { it.timestamp }
        _recentLogs.value = updated
        _clicksExecuted.value += 1
    }

    // Main execution loop
    private suspend fun runClickLoop() {
        var loopIteration = 0

        while (_isPlaying.value) {
            when (_currentMode.value) {
                ClickerMode.SINGLE -> {
                    val config = _singleConfig.value
                    val point = _activePoints.value.firstOrNull() ?: ClickPoint(1, 500f, 500f)

                    // Check stop condition
                    when (config.stopCondition) {
                        StopCondition.COUNT -> {
                            if (_clicksExecuted.value >= config.countLimit) {
                                pauseClicking()
                                break
                            }
                        }
                        StopCondition.DURATION -> {
                            val elapsedMs = SystemClock.elapsedRealtime() - executionStartTime
                            if (elapsedMs >= config.durationMinutes * 60 * 1000L) {
                                pauseClicking()
                                break
                            }
                        }
                        StopCondition.INFINITE -> { /* keep going */ }
                    }

                    executeSingleClick(point, config.pressDurationMs, config.randomJitterMs, config.randomRadiusPx)

                    val effectiveInterval = if (config.randomJitterMs > 0) {
                        val jitter = Random.nextLong(-config.randomJitterMs, config.randomJitterMs + 1)
                        (config.intervalMs + jitter).coerceAtLeast(10L)
                    } else {
                        config.intervalMs.coerceAtLeast(10L)
                    }

                    delay(effectiveInterval)
                }
                ClickerMode.MULTI -> {
                    val config = _multiConfig.value
                    val points = _activePoints.value

                    if (points.isEmpty()) {
                        delay(500)
                        continue
                    }

                    for (i in points.indices) {
                        if (!_isPlaying.value) break

                        val point = points[i]
                        _currentPointIndex.value = i

                        executeSingleClick(point, point.durationMs, config.randomJitterMs, config.randomRadiusPx)

                        val pointDelay = point.delayMs
                        val delayAfter = if (pointDelay > 0 && !(pointDelay == 500L && config.intervalMs != 500L)) {
                            pointDelay
                        } else {
                            config.intervalMs
                        }
                        val effectiveDelay = if (config.randomJitterMs > 0) {
                            val jitter = Random.nextLong(-config.randomJitterMs, config.randomJitterMs + 1)
                            (delayAfter + jitter).coerceAtLeast(10L)
                        } else {
                            delayAfter.coerceAtLeast(10L)
                        }

                        delay(effectiveDelay)
                    }

                    loopIteration++
                    if (config.loopCount > 0 && loopIteration >= config.loopCount) {
                        pauseClicking()
                        break
                    }
                }
                ClickerMode.GRID -> {
                    val config = _gridConfig.value
                    val points = _activePoints.value

                    if (points.isEmpty()) {
                        delay(200)
                        continue
                    }

                    for (i in points.indices) {
                        if (!_isPlaying.value) break

                        val point = points[i]
                        _currentPointIndex.value = i

                        executeSingleClick(point, config.pressDurationMs, config.randomJitterMs, config.randomRadiusPx)

                        val delayAfter = if (point.delayMs > 0) point.delayMs else config.intervalMs
                        val effectiveDelay = if (config.randomJitterMs > 0) {
                            val jitter = Random.nextLong(-config.randomJitterMs, config.randomJitterMs + 1)
                            (delayAfter + jitter).coerceAtLeast(5L)
                        } else {
                            delayAfter.coerceAtLeast(5L)
                        }

                        delay(effectiveDelay)
                    }

                    loopIteration++
                    if (config.loopCount > 0 && loopIteration >= config.loopCount) {
                        pauseClicking()
                        break
                    }
                }
            }
        }
    }

    private fun executeSingleClick(
        point: ClickPoint,
        durationMs: Long,
        randomJitterMs: Long,
        randomRadiusPx: Int
    ) {
        var targetX = point.x
        var targetY = point.y

        if (randomRadiusPx > 0) {
            targetX += Random.nextInt(-randomRadiusPx, randomRadiusPx + 1).toFloat()
            targetY += Random.nextInt(-randomRadiusPx, randomRadiusPx + 1).toFloat()
        }

        val now = System.currentTimeMillis()
        val interval = if (lastClickTimestamp > 0L) now - lastClickTimestamp else 0L
        lastClickTimestamp = now

        _activeTargetHighlight.value = point.id

        // Dispatched via AccessibilityService if bound
        accessibilityService?.performClick(targetX, targetY, durationMs) {
            // Callback when gesture completes
        }

        val log = ClickLogEntry(
            id = now,
            targetIndex = point.id,
            x = targetX,
            y = targetY,
            timestamp = now,
            intervalFromLastMs = interval
        )

        _testClickEvents.tryEmit(log)

        val updated = (_recentLogs.value.take(29) + log).sortedByDescending { it.timestamp }
        _recentLogs.value = updated
        _clicksExecuted.value += 1
    }
}
