package app.kiddos.autoclicker.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.kiddos.autoclicker.controller.ClickerController
import app.kiddos.autoclicker.model.ClickLogEntry
import app.kiddos.autoclicker.model.ClickerMode
import app.kiddos.autoclicker.model.GridClickOrder
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TestAppScreen(
    modifier: Modifier = Modifier
) {
    var selectedTestTab by remember { mutableIntStateOf(0) }

    Column(modifier = modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = selectedTestTab,
            edgePadding = 16.dp
        ) {
            Tab(
                selected = selectedTestTab == 0,
                onClick = { selectedTestTab = 0 },
                text = { Text("Speed & CPS Test") },
                icon = { Icon(Icons.Default.Speed, contentDescription = null) }
            )
            Tab(
                selected = selectedTestTab == 1,
                onClick = { selectedTestTab = 1 },
                text = { Text("Sequence Test (1→2→3)") },
                icon = { Icon(Icons.Default.TouchApp, contentDescription = null) }
            )
            Tab(
                selected = selectedTestTab == 2,
                onClick = { selectedTestTab = 2 },
                text = { Text("Grid Auto-Click") },
                icon = { Icon(Icons.Default.Apps, contentDescription = null) }
            )
            Tab(
                selected = selectedTestTab == 3,
                onClick = { selectedTestTab = 3 },
                text = { Text("Click Logs") },
                icon = { Icon(Icons.Default.CheckCircle, contentDescription = null) }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            when (selectedTestTab) {
                0 -> SingleClickSpeedTestTab()
                1 -> MultiTargetSequenceTestTab()
                2 -> Landscape16x2GridTestTab()
                3 -> ClickLogsTab()
            }
        }
    }
}

// Data class for visual ripple
private data class VisualRipple(
    val id: Long,
    val x: Float,
    val y: Float,
    val animProgress: Animatable<Float, *>
)

@Composable
private fun SingleClickSpeedTestTab() {
    var clickCount by remember { mutableLongStateOf(0L) }
    var currentCps by remember { mutableFloatStateOf(0f) }
    var peakCps by remember { mutableFloatStateOf(0f) }
    var lastClickTime by remember { mutableLongStateOf(0L) }
    var averageIntervalMs by remember { mutableLongStateOf(0L) }

    val recentClickTimestamps = remember { mutableStateListOf<Long>() }
    val ripples = remember { mutableStateListOf<VisualRipple>() }

    // Listen to test clicks dispatched from controller
    LaunchedEffect(Unit) {
        ClickerController.testClickEvents.collect { log ->
            val now = System.currentTimeMillis()
            clickCount++
            recentClickTimestamps.add(now)

            if (lastClickTime > 0L) {
                val interval = now - lastClickTime
                averageIntervalMs = if (averageIntervalMs == 0L) interval else (averageIntervalMs + interval) / 2
            }
            lastClickTime = now
        }
    }

    // Ticker to calculate real-time CPS over sliding 1000ms window
    LaunchedEffect(Unit) {
        while (true) {
            val now = System.currentTimeMillis()
            // Retain timestamps within the last 1000ms
            recentClickTimestamps.removeAll { now - it > 1000 }
            val countInLastSec = recentClickTimestamps.size
            currentCps = countInLastSec.toFloat()
            if (currentCps > peakCps) {
                peakCps = currentCps
            }
            delay(100)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Meter Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                label = "Total Clicks",
                value = "$clickCount",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Live CPS",
                value = String.format(Locale.US, "%.1f", currentCps),
                color = Color(0xFF4CAF50),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Peak CPS",
                value = String.format(Locale.US, "%.1f", peakCps),
                color = Color(0xFFFFA000),
                modifier = Modifier.weight(1f)
            )
        }

        // Click Arena / Target Zone
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = { offset ->
                                val now = System.currentTimeMillis()
                                clickCount++
                                recentClickTimestamps.add(now)

                                if (lastClickTime > 0L) {
                                    val interval = now - lastClickTime
                                    averageIntervalMs = if (averageIntervalMs == 0L) interval else (averageIntervalMs + interval) / 2
                                }
                                lastClickTime = now

                                // Record in controller
                                ClickerController.recordTestClick(1, offset.x, offset.y)
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                // Crosshairs & concentric circles
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2, size.height / 2)
                    drawCircle(
                        color = Color.LightGray.copy(alpha = 0.35f),
                        radius = size.minDimension / 2.2f
                    )
                    drawCircle(
                        color = Color.LightGray.copy(alpha = 0.45f),
                        radius = size.minDimension / 3.2f
                    )
                    drawCircle(
                        color = Color.LightGray.copy(alpha = 0.6f),
                        radius = size.minDimension / 5.2f
                    )
                    // Crosshair lines
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.5f),
                        start = Offset(center.x - 40, center.y),
                        end = Offset(center.x + 40, center.y),
                        strokeWidth = 2f
                    )
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.5f),
                        start = Offset(center.x, center.y - 40),
                        end = Offset(center.x, center.y + 40),
                        strokeWidth = 2f
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.TouchApp,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "CLICK TEST TARGET ZONE",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Position target pointer here & press ▶ on Side Bar",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (averageIntervalMs > 0) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Avg Interval: ${averageIntervalMs}ms",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }

        // Manual Test & Reset Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    val now = System.currentTimeMillis()
                    clickCount++
                    recentClickTimestamps.add(now)
                    ClickerController.recordTestClick(1, 400f, 600f)
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Tap Click (+1)")
            }

            OutlinedButton(
                onClick = {
                    clickCount = 0L
                    currentCps = 0f
                    peakCps = 0f
                    lastClickTime = 0L
                    averageIntervalMs = 0L
                    recentClickTimestamps.clear()
                    ClickerController.resetStats()
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Reset")
            }
        }
    }
}

@Composable
private fun MultiTargetSequenceTestTab() {
    var step1Hits by remember { mutableIntStateOf(0) }
    var step2Hits by remember { mutableIntStateOf(0) }
    var step3Hits by remember { mutableIntStateOf(0) }
    var completedSequences by remember { mutableIntStateOf(0) }
    var currentExpectedStep by remember { mutableIntStateOf(1) } // 1 -> 2 -> 3

    val onTargetHit: (Int) -> Unit = { targetIndex ->
        when (targetIndex) {
            1 -> {
                step1Hits++
                currentExpectedStep = 2
            }
            2 -> {
                step2Hits++
                if (currentExpectedStep == 2) {
                    currentExpectedStep = 3
                }
            }
            3 -> {
                step3Hits++
                if (currentExpectedStep == 3) {
                    completedSequences++
                    currentExpectedStep = 1
                }
            }
        }
        ClickerController.recordTestClick(targetIndex, 0f, 0f)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Summary Card
        ElevatedCard(shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Sequence Tracker: 1 → 2 → 3",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Position target pointers 1, 2, and 3 over the boxes below to verify multi-click flow.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$completedSequences",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )
                        Text("Cycles Completed", fontSize = 12.sp)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Step $currentExpectedStep of 3",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text("Expected Step", fontSize = 12.sp)
                    }
                }
            }
        }

        // Target Boxes Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TargetBox(
                number = 1,
                hits = step1Hits,
                isExpected = currentExpectedStep == 1,
                onClick = { onTargetHit(1) },
                modifier = Modifier.weight(1f)
            )
            TargetBox(
                number = 2,
                hits = step2Hits,
                isExpected = currentExpectedStep == 2,
                onClick = { onTargetHit(2) },
                modifier = Modifier.weight(1f)
            )
            TargetBox(
                number = 3,
                hits = step3Hits,
                isExpected = currentExpectedStep == 3,
                onClick = { onTargetHit(3) },
                modifier = Modifier.weight(1f)
            )
        }

        // Simulate step button & reset button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { onTargetHit(currentExpectedStep) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Simulate Step $currentExpectedStep Hit")
            }

            OutlinedButton(
                onClick = {
                    step1Hits = 0
                    step2Hits = 0
                    step3Hits = 0
                    completedSequences = 0
                    currentExpectedStep = 1
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Reset")
            }
        }
    }
}

@Composable
private fun TargetBox(
    number: Int,
    hits: Int,
    isExpected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(130.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isExpected) 4.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (isExpected) MaterialTheme.colorScheme.primary else Color.Gray
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$number",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Target $number",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Text(
                text = "$hits hits",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ClickLogsTab() {
    val logs by ClickerController.recentLogs.collectAsState()
    val timeFormatter = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Real-Time Click Stream (${logs.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            OutlinedButton(
                onClick = { ClickerController.resetStats() },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Clear Logs", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (logs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No clicks logged yet.\nStart clicking or use the Side Bar to see live entries.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(logs, key = { it.id }) { log ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Target #${log.targetIndex} at (${log.x.toInt()}px, ${log.y.toInt()}px)",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = timeFormatter.format(Date(log.timestamp)),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (log.intervalFromLastMs > 0) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "+${log.intervalFromLastMs}ms",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun Landscape16x2GridTestTab() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val controllerGridConfig by ClickerController.gridConfig.collectAsState()
    var columns by rememberSaveable { mutableIntStateOf(controllerGridConfig.columns) }
    var rows by rememberSaveable { mutableIntStateOf(controllerGridConfig.rows) }

    // Map of (row, col) -> hit count
    val cellHits = remember { mutableStateMapOf<Pair<Int, Int>, Int>() }
    // Selected cells list (in order). If order == CUSTOM, defines the sequence!
    val selectedCells = remember { mutableStateListOf<Pair<Int, Int>>() }

    var activeCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var totalGridHits by remember { mutableLongStateOf(0L) }
    var currentCps by remember { mutableFloatStateOf(0f) }
    var peakCps by remember { mutableFloatStateOf(0f) }
    val recentClickTimestamps = remember { mutableStateListOf<Long>() }

    // Scheduler states
    var isScheduleRunning by remember { mutableStateOf(false) }
    var selectedInterval by remember { mutableLongStateOf(50L) }
    var selectedOrder by remember { mutableStateOf(GridClickOrder.BOTTOM_ROW_FIRST) }
    var selectedLoops by remember { mutableIntStateOf(1) } // 1, 3, 0 = continuous
    var scheduleStatusText by remember { mutableStateOf("Scheduler Idle • Select cells and run schedule") }
    var schedulerJob by remember { mutableStateOf<Job?>(null) }

    fun handleCellClick(r: Int, c: Int) {
        val pair = Pair(r, c)
        val current = cellHits[pair] ?: 0
        cellHits[pair] = current + 1
        totalGridHits++
        activeCell = pair
        val now = System.currentTimeMillis()
        recentClickTimestamps.add(now)
        val cellId = r * columns + c + 1
        ClickerController.recordTestClick(cellId, (c + 1) * 60f, (r + 1) * 60f)
    }

    fun toggleCellSelection(r: Int, c: Int) {
        val pair = Pair(r, c)
        if (selectedCells.contains(pair)) {
            selectedCells.remove(pair)
        } else {
            selectedCells.add(pair)
        }
    }

    // Ticker to calculate CPS over 1000ms window
    LaunchedEffect(Unit) {
        while (true) {
            val now = System.currentTimeMillis()
            recentClickTimestamps.removeAll { now - it > 1000 }
            val count = recentClickTimestamps.size.toFloat()
            currentCps = count
            if (count > peakCps) peakCps = count
            delay(100)
        }
    }

    // Collect controller click events
    LaunchedEffect(Unit) {
        ClickerController.testClickEvents.collect { log ->
            val cellIdx = log.targetIndex - 1
            if (cellIdx in 0 until (columns * rows)) {
                val r = cellIdx / columns
                val c = cellIdx % columns
                val pair = Pair(r, c)
                val current = cellHits[pair] ?: 0
                cellHits[pair] = current + 1
                totalGridHits++
                activeCell = pair
                val now = System.currentTimeMillis()
                recentClickTimestamps.add(now)
            }
        }
    }

    fun stopSchedule() {
        isScheduleRunning = false
        schedulerJob?.cancel()
        schedulerJob = null
        scheduleStatusText = "Schedule Stopped"
    }

    fun runSchedule() {
        if (isScheduleRunning) return
        isScheduleRunning = true
        scheduleStatusText = "Schedule Running..."

        // Build the sequence of (row, col) pairs based on selection and order
        val rawCells: List<Pair<Int, Int>> = if (selectedCells.isNotEmpty()) {
            selectedCells.filter { (r, c) -> r in 0 until rows && c in 0 until columns }
        } else {
            val all = mutableListOf<Pair<Int, Int>>()
            for (r in 0 until rows) {
                for (c in 0 until columns) {
                    all.add(Pair(r, c))
                }
            }
            all
        }

        val sequence: List<Pair<Int, Int>> = when (selectedOrder) {
            GridClickOrder.CUSTOM -> rawCells // Preserves user selection order
            GridClickOrder.BOTTOM_ROW_FIRST -> {
                rawCells.sortedWith(compareByDescending<Pair<Int, Int>> { it.first }.thenBy { it.second })
            }
            GridClickOrder.TOP_ROW_FIRST, GridClickOrder.LEFT_TO_RIGHT -> {
                rawCells.sortedWith(compareBy<Pair<Int, Int>> { it.first }.thenBy { it.second })
            }
            GridClickOrder.COLUMN_BY_COLUMN -> {
                rawCells.sortedWith(compareBy<Pair<Int, Int>> { it.second }.thenBy { it.first })
            }
            GridClickOrder.ALTERNATING -> {
                val result = mutableListOf<Pair<Int, Int>>()
                for (r in 0 until rows) {
                    val rowCells = rawCells.filter { it.first == r }
                    if (r % 2 == 0) result.addAll(rowCells.sortedBy { it.second })
                    else result.addAll(rowCells.sortedByDescending { it.second })
                }
                result
            }
            GridClickOrder.RANDOM -> rawCells.shuffled()
        }

        schedulerJob = coroutineScope.launch {
            var loop = 0
            val maxLoops = selectedLoops
            while (isScheduleRunning && (maxLoops <= 0 || loop < maxLoops)) {
                for ((r, c) in sequence) {
                    if (!isScheduleRunning) break
                    handleCellClick(r, c)
                    scheduleStatusText = "Auto-clicking R${r + 1}C${c + 1} (${selectedInterval}ms)"
                    delay(selectedInterval.coerceAtLeast(5L))
                }
                loop++
            }

            isScheduleRunning = false
            scheduleStatusText = "Schedule Completed ($totalGridHits total clicks recorded)"
        }
    }

    val activeTargetCells = if (selectedCells.isNotEmpty()) {
        selectedCells.filter { (r, c) -> r in 0 until rows && c in 0 until columns }
    } else {
        (0 until rows).flatMap { r -> (0 until columns).map { c -> Pair(r, c) } }
    }
    val coveredCellsCount = activeTargetCells.count { (cellHits[it] ?: 0) > 0 }
    val totalConfiguredCount = activeTargetCells.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Stats Cards Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                label = "Grid Clicks",
                value = "$totalGridHits",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Coverage",
                value = "$coveredCellsCount/$totalConfiguredCount",
                color = if (coveredCellsCount == totalConfiguredCount && totalConfiguredCount > 0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Live CPS",
                value = String.format(java.util.Locale.US, "%.1f", currentCps),
                color = Color(0xFF4CAF50),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Peak CPS",
                value = String.format(java.util.Locale.US, "%.1f", peakCps),
                color = Color(0xFFFFA000),
                modifier = Modifier.weight(1f)
            )
        }

        // Grid Dimensions & Presets Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Grid Configuration (${columns} × ${rows})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Presets:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(16 to 2, 10 to 2, 5 to 3, 8 to 2, 4 to 4).forEach { (c, r) ->
                        FilterChip(
                            selected = columns == c && rows == r,
                            onClick = {
                                columns = c
                                rows = r
                                cellHits.clear()
                                selectedCells.clear()
                                ClickerController.updateGridConfig(controllerGridConfig.copy(columns = c, rows = r))
                            },
                            label = { Text("${c}×${r}") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = "$columns",
                        onValueChange = { str ->
                            str.toIntOrNull()?.let {
                                columns = it.coerceIn(1, 32)
                                selectedCells.removeAll { (_, cc) -> cc >= columns }
                            }
                        },
                        label = { Text("Columns") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = "$rows",
                        onValueChange = { str ->
                            str.toIntOrNull()?.let {
                                rows = it.coerceIn(1, 8)
                                selectedCells.removeAll { (cr, _) -> cr >= rows }
                            }
                        },
                        label = { Text("Rows") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Interactive Grid Arena
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Interactive Bottom Grid (${columns} Cols × ${rows} Rows)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (selectedOrder == GridClickOrder.CUSTOM) {
                                "Tap cells to set custom click sequence order (#1, #2...)"
                            } else {
                                "Tap cells to toggle click target on/off"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isScheduleRunning) Color(0xFF4CAF50).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
                    ) {
                        Text(
                            text = if (isScheduleRunning) "RUNNING" else "READY",
                            color = if (isScheduleRunning) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Quick selector buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            selectedCells.clear()
                            for (r in 0 until rows) for (c in 0 until columns) selectedCells.add(Pair(r, c))
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(2.dp)
                    ) {
                        Text("All", fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = { selectedCells.clear() },
                        modifier = Modifier.weight(1f),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(2.dp)
                    ) {
                        Text("Clear", fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            selectedCells.clear()
                            val bottomRow = rows - 1
                            for (c in 0 until columns) selectedCells.add(Pair(bottomRow, c))
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(2.dp)
                    ) {
                        Text("Bottom Row", fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            val newSelected = mutableListOf<Pair<Int, Int>>()
                            for (r in 0 until rows) {
                                for (c in 0 until columns) {
                                    val pair = Pair(r, c)
                                    if (!selectedCells.contains(pair)) newSelected.add(pair)
                                }
                            }
                            selectedCells.clear()
                            selectedCells.addAll(newSelected)
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(2.dp)
                    ) {
                        Text("Invert", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable container for the dynamic grid
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        for (r in 0 until rows) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "R${r + 1}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(28.dp),
                                    color = if (r == rows - 1) Color(0xFF00897B) else MaterialTheme.colorScheme.primary
                                )
                                for (c in 0 until columns) {
                                    val pair = Pair(r, c)
                                    val isSelected = selectedCells.isEmpty() || selectedCells.contains(pair)
                                    val seqIdx = if (selectedCells.contains(pair)) selectedCells.indexOf(pair) + 1 else null
                                    val hits = cellHits[pair] ?: 0
                                    val isActive = activeCell == pair

                                    GridCellBox(
                                        row = r + 1,
                                        col = c + 1,
                                        hits = hits,
                                        isActive = isActive,
                                        isSelected = isSelected,
                                        sequenceIndex = seqIdx,
                                        isCustomOrder = selectedOrder == GridClickOrder.CUSTOM,
                                        onClick = {
                                            if (isScheduleRunning) {
                                                handleCellClick(r, c)
                                            } else {
                                                toggleCellSelection(r, c)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = scheduleStatusText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Auto-Click Scheduler Control Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Grid Auto-Click Scheduler",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Configure click ordering and execute rapid taps across selected cells",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Speed / Interval Selector
                Text(
                    text = "Tap Interval: ${selectedInterval}ms (${if (selectedInterval > 0) 1000 / selectedInterval else 0} CPS)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(20L to "20ms (50 CPS)", 50L to "50ms (20 CPS)", 100L to "100ms (10 CPS)", 200L to "200ms").forEach { (speed, label) ->
                        FilterChip(
                            selected = selectedInterval == speed,
                            onClick = { selectedInterval = speed },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Schedule Sequence Order
                Text(
                    text = "Sequencing Order",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                val orderOptions = listOf(
                    GridClickOrder.BOTTOM_ROW_FIRST to "Bottom Row First (Bottom Row Left→Right, then upper rows)",
                    GridClickOrder.TOP_ROW_FIRST to "Top Row First (Top Row Left→Right, then lower rows)",
                    GridClickOrder.COLUMN_BY_COLUMN to "Column by Column (Col 1 → Col N)",
                    GridClickOrder.ALTERNATING to "Alternating / Snake",
                    GridClickOrder.CUSTOM to "Custom Selection Sequence (clicks in user-chosen order)",
                    GridClickOrder.RANDOM to "Random Blitz"
                )
                orderOptions.forEach { (ord, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedOrder == ord,
                            onClick = { selectedOrder = ord }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(label, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Loops Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Loops:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    listOf(1 to "1 Cycle", 3 to "3 Cycles", 0 to "Continuous").forEach { (lp, lbl) ->
                        FilterChip(
                            selected = selectedLoops == lp,
                            onClick = { selectedLoops = lp },
                            label = { Text(lbl, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Schedule Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!isScheduleRunning) {
                        Button(
                            onClick = { runSchedule() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("▶ Run Grid Schedule")
                        }
                    } else {
                        Button(
                            onClick = { stopSchedule() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Pause, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("⏹ Stop Schedule")
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            stopSchedule()
                            cellHits.clear()
                            totalGridHits = 0L
                            peakCps = 0f
                            currentCps = 0f
                            activeCell = null
                            recentClickTimestamps.clear()
                            scheduleStatusText = "Grid Reset"
                            ClickerController.resetStats()
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Deploy Grid into Floating Overlay
                Button(
                    onClick = {
                        val metrics = context.resources.displayMetrics
                        val count = if (selectedCells.isNotEmpty()) selectedCells.size else columns * rows
                        ClickerController.setMode(ClickerMode.GRID)
                        ClickerController.applyLandscapeGrid(
                            columns = columns,
                            rows = rows,
                            screenWidth = metrics.widthPixels.toFloat(),
                            screenHeight = metrics.heightPixels.toFloat(),
                            bottomHeightRatio = 0.28f,
                            intervalMs = selectedInterval,
                            order = selectedOrder,
                            selectedCells = selectedCells.toList()
                        )
                        scheduleStatusText = "Grid (${columns}×${rows}, $count cells) applied to Floating Overlay!"
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Apps, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("⚡ Deploy Grid to Floating Overlay ($totalConfiguredCount Targets)")
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun GridCellBox(
    row: Int,
    col: Int,
    hits: Int,
    isActive: Boolean,
    isSelected: Boolean,
    sequenceIndex: Int?,
    isCustomOrder: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = when {
            isActive -> Color(0xFF00E676) // Bright green glow when active click occurs
            sequenceIndex != null && isCustomOrder -> MaterialTheme.colorScheme.primary
            isSelected -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surfaceContainerHighest
        },
        shadowElevation = if (isActive) 6.dp else 1.dp,
        modifier = Modifier.size(width = 46.dp, height = 48.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "C$col",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = when {
                    isActive -> Color.Black
                    sequenceIndex != null && isCustomOrder -> Color.White
                    isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            if (sequenceIndex != null && isCustomOrder) {
                Text(
                    text = "#$sequenceIndex",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            } else if (hits > 0) {
                Text(
                    text = "${hits}x",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isActive) Color.Black else MaterialTheme.colorScheme.primary
                )
            } else {
                Text(
                    text = if (isSelected) "✓" else "off",
                    fontSize = 9.sp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray
                )
            }
        }
    }
}
