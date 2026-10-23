package app.kiddos.autoclicker.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.kiddos.autoclicker.controller.ClickerController
import app.kiddos.autoclicker.model.ClickerMode

@Composable
fun DashboardScreen(
    hasOverlayPermission: Boolean,
    isAccessibilityEnabled: Boolean,
    onRequestOverlayPermission: () -> Unit,
    onRequestAccessibilityPermission: () -> Unit,
    onStartOverlay: () -> Unit,
    onStopOverlay: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToTest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isOverlayRunning by ClickerController.isOverlayRunning.collectAsState()
    val isPlaying by ClickerController.isPlaying.collectAsState()
    val currentMode by ClickerController.currentMode.collectAsState()
    val clicksExecuted by ClickerController.clicksExecuted.collectAsState()
    val activePoints by ClickerController.activePoints.collectAsState()

    val allPermissionsGranted = hasOverlayPermission && isAccessibilityEnabled

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Permissions Status Card
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "System Permissions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Accessibility Service Item
                PermissionStatusRow(
                    title = "Accessibility Service",
                    description = "Required to simulate touch gestures & clicks",
                    isGranted = isAccessibilityEnabled,
                    onGrantClick = onRequestAccessibilityPermission
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Overlay Permission Item
                PermissionStatusRow(
                    title = "Display Over Other Apps",
                    description = "Required to show the floating control sidebar",
                    isGranted = hasOverlayPermission,
                    onGrantClick = onRequestOverlayPermission
                )
            }
        }

        // 2. Mode Selection Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Clicker Mode",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Single Click Option
                    ModeOptionCard(
                        title = "Single",
                        description = "1 target point rapid clicks",
                        icon = Icons.Default.TouchApp,
                        isSelected = currentMode == ClickerMode.SINGLE,
                        onClick = { ClickerController.setMode(ClickerMode.SINGLE) },
                        modifier = Modifier.weight(1f)
                    )

                    // Multi Click Option
                    ModeOptionCard(
                        title = "Multi",
                        description = "Custom sequential targets",
                        icon = Icons.Default.Layers,
                        isSelected = currentMode == ClickerMode.MULTI,
                        onClick = { ClickerController.setMode(ClickerMode.MULTI) },
                        modifier = Modifier.weight(1f)
                    )

                    // Grid Mode Option
                    ModeOptionCard(
                        title = "Grid",
                        description = "Custom bottom grid (16×2, 10×2, 5×3)",
                        icon = Icons.Default.Apps,
                        isSelected = currentMode == ClickerMode.GRID,
                        onClick = { ClickerController.setMode(ClickerMode.GRID) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 3. Floating Sidebar Launcher & Status
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isOverlayRunning) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                }
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Floating Side Bar",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isOverlayRunning) {
                                if (isPlaying) "Active • Clicking in progress" else "Active • Paused"
                            } else {
                                "Inactive • Tap to launch side bar"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Live Status Badge
                    Surface(
                        shape = CircleShape,
                        color = when {
                            !isOverlayRunning -> MaterialTheme.colorScheme.surfaceDim
                            isPlaying -> Color(0xFF4CAF50)
                            else -> Color(0xFFFFA000)
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when {
                                    !isOverlayRunning -> "Off"
                                    isPlaying -> "Running"
                                    else -> "Paused"
                                },
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Stats row when active
                AnimatedVisibility(visible = isOverlayRunning) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatBox(label = "Total Clicks", value = "$clicksExecuted")
                            StatBox(
                                label = "Mode",
                                value = when (currentMode) {
                                    ClickerMode.SINGLE -> "Single (1)"
                                    ClickerMode.MULTI -> "Multi (${activePoints.size})"
                                    ClickerMode.GRID -> "Grid 16×2 (${activePoints.size})"
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!isOverlayRunning) {
                        Button(
                            onClick = onStartOverlay,
                            enabled = allPermissionsGranted,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Launch Side Bar")
                        }
                    } else {
                        Button(
                            onClick = { ClickerController.togglePlayPause() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isPlaying) Color(0xFFE65100) else Color(0xFF2E7D32)
                            )
                        ) {
                            Icon(
                                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isPlaying) "Pause Clicks" else "Start Clicks")
                        }

                        OutlinedButton(
                            onClick = onStopOverlay,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Dismiss")
                        }
                    }
                }

                if (!allPermissionsGranted) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Please grant the permissions above to launch the sidebar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // 4. Quick Navigation Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FilledTonalButton(
                onClick = onNavigateToSettings,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Configure Settings")
            }

            FilledTonalButton(
                onClick = onNavigateToTest,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Test Clicks")
            }
        }

        // 5. User Guide Card
        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "How to use the Side Bar",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                InstructionItem(number = "1", text = "Launch the Floating Side Bar using the button above.")
                InstructionItem(number = "2", text = "Drag the sidebar vertically along the edge to position it.")
                InstructionItem(number = "3", text = "Drag the circular target marker(s) to where you want clicks to land.")
                InstructionItem(number = "4", text = "Press ▶ on the sidebar to start auto-clicking, and ⏸ to pause anytime.")
                InstructionItem(number = "5", text = "In Multi-click mode, tap + or - on the sidebar to add or remove target points.")
            }
        }
    }
}

@Composable
private fun PermissionStatusRow(
    title: String,
    description: String,
    isGranted: Boolean,
    onGrantClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (isGranted) Color(0xFF4CAF50) else Color(0xFFFFA000),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (!isGranted) {
            Button(
                onClick = onGrantClick,
                shape = RoundedCornerShape(8.dp),
                contentPadding = ButtonDefaults.TextButtonContentPadding
            ) {
                Text("Enable", fontSize = 12.sp)
            }
        } else {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0x1F4CAF50)
            ) {
                Text(
                    text = "Ready",
                    color = Color(0xFF2E7D32),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun ModeOptionCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun StatBox(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun InstructionItem(number: String, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(20.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = number,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
