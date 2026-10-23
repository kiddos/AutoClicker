package app.kiddos.autoclicker.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.kiddos.autoclicker.controller.ClickerController
import app.kiddos.autoclicker.model.ClickPoint
import app.kiddos.autoclicker.model.ClickerMode
import app.kiddos.autoclicker.model.GridClickOrder
import app.kiddos.autoclicker.model.GridConfig
import app.kiddos.autoclicker.model.MultiClickConfig
import app.kiddos.autoclicker.model.SingleClickConfig
import app.kiddos.autoclicker.model.StopCondition

@Composable
fun SettingsScreen(
    onShowSnackbar: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val singleConfigState by ClickerController.singleConfig.collectAsState()
    val multiConfigState by ClickerController.multiConfig.collectAsState()
    val gridConfigState by ClickerController.gridConfig.collectAsState()
    val activePointsState by ClickerController.activePoints.collectAsState()
    val context = LocalContext.current

    var selectedTabIndex by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
    ) {
        // Tab Header: Single Click vs Multiple Click vs 16x2 Grid
        TabRow(selectedTabIndex = selectedTabIndex) {
            Tab(
                selected = selectedTabIndex == 0,
                onClick = { selectedTabIndex = 0 },
                text = { Text("Single Click") },
                icon = { Icon(Icons.Default.TouchApp, contentDescription = null) }
            )
            Tab(
                selected = selectedTabIndex == 1,
                onClick = { selectedTabIndex = 1 },
                text = { Text("Multiple Click") },
                icon = { Icon(Icons.Default.Layers, contentDescription = null) }
            )
            Tab(
                selected = selectedTabIndex == 2,
                onClick = { selectedTabIndex = 2 },
                text = { Text("${gridConfigState.columns}×${gridConfigState.rows} Grid") },
                icon = { Icon(Icons.Default.Apps, contentDescription = null) }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            when (selectedTabIndex) {
                0 -> SingleClickSettingsTab(
                    initialConfig = singleConfigState,
                    onSave = { updated ->
                        ClickerController.updateSingleConfig(updated)
                        onShowSnackbar("Single click settings saved!")
                    }
                )
                1 -> MultiClickSettingsTab(
                    initialConfig = multiConfigState,
                    activePoints = activePointsState,
                    onSave = { updated ->
                        ClickerController.updateMultiConfig(updated)
                        onShowSnackbar("Multiple click settings saved!")
                    }
                )
                2 -> GridSettingsTab(
                    initialConfig = gridConfigState,
                    onSave = { updated ->
                        ClickerController.updateGridConfig(updated)
                        onShowSnackbar("Grid settings saved!")
                    },
                    onApplyGrid = { updated ->
                        val metrics = context.resources.displayMetrics
                        val count = if (updated.selectedCells.isNotEmpty()) updated.selectedCells.size else updated.columns * updated.rows
                        ClickerController.setMode(ClickerMode.GRID)
                        ClickerController.applyLandscapeGrid(
                            columns = updated.columns,
                            rows = updated.rows,
                            screenWidth = metrics.widthPixels.toFloat(),
                            screenHeight = metrics.heightPixels.toFloat(),
                            bottomHeightRatio = updated.bottomHeightRatio,
                            intervalMs = updated.intervalMs,
                            durationMs = updated.pressDurationMs,
                            order = updated.order,
                            selectedCells = updated.selectedCells
                        )
                        onShowSnackbar("Applied Grid (${updated.columns}×${updated.rows}, $count targets) to screen!")
                    }
                )
            }
        }
    }
}

@Composable
private fun SingleClickSettingsTab(
    initialConfig: SingleClickConfig,
    onSave: (SingleClickConfig) -> Unit
) {
    var intervalMs by remember(initialConfig) { mutableLongStateOf(initialConfig.intervalMs) }
    var pressDurationMs by remember(initialConfig) { mutableLongStateOf(initialConfig.pressDurationMs) }
    var stopCondition by remember(initialConfig) { mutableStateOf(initialConfig.stopCondition) }
    var countLimit by remember(initialConfig) { mutableIntStateOf(initialConfig.countLimit) }
    var durationMinutes by remember(initialConfig) { mutableIntStateOf(initialConfig.durationMinutes) }
    var randomJitterMs by remember(initialConfig) { mutableLongStateOf(initialConfig.randomJitterMs) }
    var randomRadiusPx by remember(initialConfig) { mutableIntStateOf(initialConfig.randomRadiusPx) }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Interval Card
        ElevatedCard(shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Click Interval",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Time delay between consecutive clicks",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = intervalMs.toString(),
                    onValueChange = { str ->
                        str.toLongOrNull()?.let { intervalMs = it.coerceAtLeast(10L) }
                    },
                    label = { Text("Interval (milliseconds)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text("Quick Presets:", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(50L, 100L, 250L, 500L, 1000L).forEach { preset ->
                        FilterChip(
                            selected = intervalMs == preset,
                            onClick = { intervalMs = preset },
                            label = { Text(if (preset >= 1000) "${preset / 1000}s" else "${preset}ms") }
                        )
                    }
                }
            }
        }

        // Tap Duration Card
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Tap Press Duration: ${pressDurationMs}ms",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "How long the touch is held down per tap (50ms recommended)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = pressDurationMs.toFloat(),
                    onValueChange = { pressDurationMs = it.toLong() },
                    valueRange = 10f..500f,
                    steps = 48
                )
            }
        }

        // Stop Condition Card
        ElevatedCard(shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Stop Condition",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Infinite
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RadioButton(
                        selected = stopCondition == StopCondition.INFINITE,
                        onClick = { stopCondition = StopCondition.INFINITE }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Run indefinitely (until paused)")
                }

                // Count
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RadioButton(
                        selected = stopCondition == StopCondition.COUNT,
                        onClick = { stopCondition = StopCondition.COUNT }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Stop after number of clicks:")
                }
                if (stopCondition == StopCondition.COUNT) {
                    OutlinedTextField(
                        value = countLimit.toString(),
                        onValueChange = { str ->
                            str.toIntOrNull()?.let { countLimit = it.coerceAtLeast(1) }
                        },
                        label = { Text("Total Clicks Count") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 48.dp, bottom = 8.dp)
                    )
                }

                // Duration
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RadioButton(
                        selected = stopCondition == StopCondition.DURATION,
                        onClick = { stopCondition = StopCondition.DURATION }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Stop after duration (minutes):")
                }
                if (stopCondition == StopCondition.DURATION) {
                    OutlinedTextField(
                        value = durationMinutes.toString(),
                        onValueChange = { str ->
                            str.toIntOrNull()?.let { durationMinutes = it.coerceAtLeast(1) }
                        },
                        label = { Text("Minutes to run") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 48.dp, bottom = 8.dp)
                    )
                }
            }
        }

        // Anti-Detection / Randomization Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Anti-Detection / Randomization",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Adds subtle variance to clicks to simulate human taps",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                Text("Interval Jitter: ±${randomJitterMs}ms", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = randomJitterMs.toFloat(),
                    onValueChange = { randomJitterMs = it.toLong() },
                    valueRange = 0f..100f
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text("Position Jitter: ±${randomRadiusPx}px", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = randomRadiusPx.toFloat(),
                    onValueChange = { randomRadiusPx = it.toInt() },
                    valueRange = 0f..25f
                )
            }
        }

        // Action Buttons: Save & Reset
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    val updated = SingleClickConfig(
                        intervalMs = intervalMs,
                        pressDurationMs = pressDurationMs,
                        stopCondition = stopCondition,
                        countLimit = countLimit,
                        durationMinutes = durationMinutes,
                        randomJitterMs = randomJitterMs,
                        randomRadiusPx = randomRadiusPx
                    )
                    onSave(updated)
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Settings")
            }

            OutlinedButton(
                onClick = {
                    intervalMs = 500L
                    pressDurationMs = 50L
                    stopCondition = StopCondition.INFINITE
                    countLimit = 100
                    durationMinutes = 5
                    randomJitterMs = 0L
                    randomRadiusPx = 0
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Reset")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun MultiClickSettingsTab(
    initialConfig: MultiClickConfig,
    activePoints: List<ClickPoint>,
    onSave: (MultiClickConfig) -> Unit
) {
    var intervalMs by remember(initialConfig) { mutableLongStateOf(initialConfig.intervalMs) }
    var pressDurationMs by remember(initialConfig) { mutableLongStateOf(initialConfig.pressDurationMs) }
    var loopCount by remember(initialConfig) { mutableIntStateOf(initialConfig.loopCount) }
    var randomJitterMs by remember(initialConfig) { mutableLongStateOf(initialConfig.randomJitterMs) }
    var randomRadiusPx by remember(initialConfig) { mutableIntStateOf(initialConfig.randomRadiusPx) }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Multi-point Interval Card
        ElevatedCard(shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Default Delay Between Points",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Interval between consecutive target points in the sequence",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = intervalMs.toString(),
                    onValueChange = { str ->
                        str.toLongOrNull()?.let { intervalMs = it.coerceAtLeast(10L) }
                    },
                    label = { Text("Delay (milliseconds)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(100L, 250L, 500L, 1000L).forEach { preset ->
                        FilterChip(
                            selected = intervalMs == preset,
                            onClick = { intervalMs = preset },
                            label = { Text(if (preset >= 1000) "${preset / 1000}s" else "${preset}ms") }
                        )
                    }
                }
            }
        }

        // Tap Press Duration
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Default Press Duration: ${pressDurationMs}ms",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = pressDurationMs.toFloat(),
                    onValueChange = { pressDurationMs = it.toLong() },
                    valueRange = 10f..500f,
                    steps = 48
                )
            }
        }

        // Sequence Loops Card
        ElevatedCard(shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Sequence Cycles / Loops",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Set to 0 to loop sequence infinitely until paused",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = loopCount.toString(),
                    onValueChange = { str ->
                        str.toIntOrNull()?.let { loopCount = it.coerceAtLeast(0) }
                    },
                    label = { Text("Loop Count (0 = Infinite)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }

        // Target Points Overview Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Configured Click Points (${activePoints.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Points executed in sequence (1 → 2 → 3...)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = {
                            ClickerController.addPoint(500f, 500f)
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Point")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (activePoints.isEmpty()) {
                    Text(
                        text = "No points added yet. Use '+' or launch the side bar to add points.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    activePoints.forEachIndexed { index, point ->
                        PointItemRow(
                            point = point,
                            index = index + 1,
                            onRemove = { ClickerController.removePointById(point.id) }
                        )
                        if (index < activePoints.size - 1) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }

        // Anti-Detection / Randomization
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Anti-Detection / Randomization",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Text("Interval Jitter: ±${randomJitterMs}ms", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = randomJitterMs.toFloat(),
                    onValueChange = { randomJitterMs = it.toLong() },
                    valueRange = 0f..100f
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text("Position Jitter: ±${randomRadiusPx}px", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = randomRadiusPx.toFloat(),
                    onValueChange = { randomRadiusPx = it.toInt() },
                    valueRange = 0f..25f
                )
            }
        }

        // Save & Reset Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    val updated = MultiClickConfig(
                        intervalMs = intervalMs,
                        pressDurationMs = pressDurationMs,
                        loopCount = loopCount,
                        points = ClickerController.activePoints.value,
                        randomJitterMs = randomJitterMs,
                        randomRadiusPx = randomRadiusPx
                    )
                    onSave(updated)
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Settings")
            }

            OutlinedButton(
                onClick = {
                    intervalMs = 500L
                    pressDurationMs = 50L
                    loopCount = 0
                    randomJitterMs = 0L
                    randomRadiusPx = 0
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Reset")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun PointItemRow(
    point: ClickPoint,
    index: Int,
    onRemove: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$index",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Target #$index (ID: ${point.id})",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Pos: (${point.x.toInt()}px, ${point.y.toInt()}px) • Delay: ${point.delayMs}ms",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove point",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun GridSettingsTab(
    initialConfig: GridConfig,
    onSave: (GridConfig) -> Unit,
    onApplyGrid: (GridConfig) -> Unit
) {
    var columns by remember(initialConfig) { mutableIntStateOf(initialConfig.columns) }
    var rows by remember(initialConfig) { mutableIntStateOf(initialConfig.rows) }
    var bottomRatio by remember(initialConfig) { mutableStateOf(initialConfig.bottomHeightRatio) }
    var intervalMs by remember(initialConfig) { mutableLongStateOf(initialConfig.intervalMs) }
    var durationMs by remember(initialConfig) { mutableLongStateOf(initialConfig.pressDurationMs) }
    var order by remember(initialConfig) { mutableStateOf(initialConfig.order) }
    var jitterMs by remember(initialConfig) { mutableLongStateOf(initialConfig.randomJitterMs) }
    var radiusPx by remember(initialConfig) { mutableIntStateOf(initialConfig.randomRadiusPx) }
    var marginHorizontal by remember(initialConfig) { mutableStateOf(initialConfig.marginHorizontalPx) }

    // Selected cells list (in order). If order is CUSTOM, this represents the exact click sequence!
    val selectedCells = remember(initialConfig) {
        mutableStateListOf<Pair<Int, Int>>().apply {
            addAll(initialConfig.selectedCells)
        }
    }

    // Helper to toggle or add to custom sequence
    fun toggleCell(r: Int, c: Int) {
        val pair = Pair(r, c)
        if (selectedCells.contains(pair)) {
            selectedCells.remove(pair)
        } else {
            selectedCells.add(pair)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Grid Info Header Card
        ElevatedCard(shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Configurable Landscape Bottom Grid",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                val targetCount = if (selectedCells.isNotEmpty()) selectedCells.size else columns * rows
                Text(
                    text = "Configured: ${columns}×${rows} grid (${targetCount} target cells selected) positioned at the bottom rows of your landscape game screen.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Quick Presets: 16x2, 10x2, 5x3, 8x2, 4x4
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Grid Dimensions (Columns × Rows)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Select a quick preset or type custom dimensions:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

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
                                selectedCells.clear()
                            },
                            label = { Text("${c}×${r}") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = "$columns",
                        onValueChange = { str ->
                            str.toIntOrNull()?.let {
                                columns = it.coerceIn(1, 32)
                                selectedCells.removeAll { (_, cc) -> cc >= columns }
                            }
                        },
                        label = { Text("Columns (Width)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = "$rows",
                        onValueChange = { str ->
                            str.toIntOrNull()?.let {
                                rows = it.coerceIn(1, 10)
                                selectedCells.removeAll { (cr, _) -> cr >= rows }
                            }
                        },
                        label = { Text("Rows (Height)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Interactive Cell Selector & Click Ordering
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Interactive Cell Selector",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (order == GridClickOrder.CUSTOM) {
                                "Tap cells in your desired click sequence (#1, #2, #3...)"
                            } else {
                                "Tap cells to include/exclude from automated clicking"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                            for (r in 0 until rows) {
                                for (c in 0 until columns) {
                                    selectedCells.add(Pair(r, c))
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp)
                    ) {
                        Text("Select All", fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = { selectedCells.clear() },
                        modifier = Modifier.weight(1f),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp)
                    ) {
                        Text("Clear All", fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            selectedCells.clear()
                            val bottomRow = rows - 1
                            for (c in 0 until columns) selectedCells.add(Pair(bottomRow, c))
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp)
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
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp)
                    ) {
                        Text("Invert", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable Grid of Cell Buttons
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                                    val sequenceIndex = if (selectedCells.contains(pair)) {
                                        selectedCells.indexOf(pair) + 1
                                    } else null

                                    Surface(
                                        onClick = { toggleCell(r, c) },
                                        shape = RoundedCornerShape(8.dp),
                                        color = when {
                                            sequenceIndex != null && order == GridClickOrder.CUSTOM -> MaterialTheme.colorScheme.primary
                                            isSelected -> MaterialTheme.colorScheme.primaryContainer
                                            else -> MaterialTheme.colorScheme.surfaceContainerHighest
                                        },
                                        modifier = Modifier.size(width = 44.dp, height = 44.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxSize(),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = "C${c + 1}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (sequenceIndex != null && order == GridClickOrder.CUSTOM) {
                                                    Color.White
                                                } else MaterialTheme.colorScheme.onSurface
                                            )
                                            if (sequenceIndex != null) {
                                                Text(
                                                    text = "#$sequenceIndex",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (order == GridClickOrder.CUSTOM) Color.White else MaterialTheme.colorScheme.primary
                                                )
                                            } else {
                                                Text(
                                                    text = if (selectedCells.isEmpty()) "✓" else "off",
                                                    fontSize = 9.sp,
                                                    color = if (selectedCells.isEmpty()) MaterialTheme.colorScheme.primary else Color.Gray
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                val activeCount = if (selectedCells.isNotEmpty()) selectedCells.size else columns * rows
                Text(
                    text = "Active: $activeCount of ${columns * rows} cells configured to click" +
                            if (order == GridClickOrder.CUSTOM && selectedCells.isNotEmpty()) " (custom sequence active)" else "",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }

        // Click Sequencing Pattern
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Click Sequencing Pattern",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                val orders = listOf(
                    GridClickOrder.BOTTOM_ROW_FIRST to "Bottom Row First (Bottom Row Left→Right, then upper rows)",
                    GridClickOrder.TOP_ROW_FIRST to "Top Row First (Top Row Left→Right, then lower rows)",
                    GridClickOrder.COLUMN_BY_COLUMN to "Column by Column (Col 1 → Col N)",
                    GridClickOrder.ALTERNATING to "Alternating / Snake (Row 1 L→R, Row 2 R→L...)",
                    GridClickOrder.CUSTOM to "Custom Sequence (Clicks in the order you selected them)",
                    GridClickOrder.RANDOM to "Random Blitz"
                )

                orders.forEach { (ord, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = order == ord,
                            onClick = { order = ord }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        // Tap Speed & Duration Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Tap Interval & Speed",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Interval between taps: ${intervalMs}ms (${if (intervalMs > 0) 1000 / intervalMs else 0} CPS)",
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = intervalMs.toFloat(),
                    onValueChange = { intervalMs = it.toLong() },
                    valueRange = 10f..500f,
                    steps = 48
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(20L, 50L, 100L, 200L).forEach { speed ->
                        FilterChip(
                            selected = intervalMs == speed,
                            onClick = { intervalMs = speed },
                            label = { Text("${speed}ms") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Press Duration: ${durationMs}ms",
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = durationMs.toFloat(),
                    onValueChange = { durationMs = it.toLong() },
                    valueRange = 5f..100f,
                    steps = 18
                )
            }
        }

        // Screen Region Coverage Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Screen Position & Coverage",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Bottom Height Coverage: ${(bottomRatio * 100).toInt()}% of screen height",
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = bottomRatio,
                    onValueChange = { bottomRatio = it },
                    valueRange = 0.15f..0.60f
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Side Margin (Safe Inset): ${marginHorizontal.toInt()}px",
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = marginHorizontal,
                    onValueChange = { marginHorizontal = it },
                    valueRange = 0f..160f
                )
            }
        }

        // Anti-Detection Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Anti-Detection / Randomization",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Interval Jitter (±${jitterMs}ms)",
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = jitterMs.toFloat(),
                    onValueChange = { jitterMs = it.toLong() },
                    valueRange = 0f..50f
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Random Spatial Radius (±${radiusPx}px)",
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = radiusPx.toFloat(),
                    onValueChange = { radiusPx = it.toInt() },
                    valueRange = 0f..20f
                )
            }
        }

        // Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    val config = GridConfig(
                        columns = columns,
                        rows = rows,
                        bottomHeightRatio = bottomRatio,
                        intervalMs = intervalMs,
                        pressDurationMs = durationMs,
                        order = order,
                        randomJitterMs = jitterMs,
                        randomRadiusPx = radiusPx,
                        marginHorizontalPx = marginHorizontal,
                        selectedCells = selectedCells.toList()
                    )
                    onSave(config)
                    onApplyGrid(config)
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Apps, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                val targetCount = if (selectedCells.isNotEmpty()) selectedCells.size else columns * rows
                Text("Deploy Grid (${columns}×${rows}, $targetCount Targets)")
            }

            OutlinedButton(
                onClick = {
                    columns = 16
                    rows = 2
                    bottomRatio = 0.28f
                    intervalMs = 50L
                    durationMs = 20L
                    order = GridClickOrder.BOTTOM_ROW_FIRST
                    jitterMs = 0L
                    radiusPx = 0
                    selectedCells.clear()
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Reset")
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}
