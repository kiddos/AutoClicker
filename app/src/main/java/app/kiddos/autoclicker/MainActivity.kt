package app.kiddos.autoclicker

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.kiddos.autoclicker.controller.ClickerController
import app.kiddos.autoclicker.service.FloatingOverlayService
import app.kiddos.autoclicker.ui.screen.DashboardScreen
import app.kiddos.autoclicker.ui.screen.SettingsScreen
import app.kiddos.autoclicker.ui.screen.TestAppScreen
import app.kiddos.autoclicker.ui.theme.AutoClickerTheme
import app.kiddos.autoclicker.util.PermissionUtils
import kotlinx.coroutines.launch

sealed class NavItem(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : NavItem("dashboard", "Dashboard", Icons.Default.Home)
    object Settings : NavItem("settings", "Settings", Icons.Default.Settings)
    object TestApp : NavItem("test_app", "Click Test", Icons.Default.TouchApp)
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        ClickerController.init(this)

        setContent {
            AutoClickerTheme {
                AutoClickerMainApp(
                    onRequestOverlayPermission = {
                        PermissionUtils.openOverlaySettings(this)
                    },
                    onRequestAccessibilityPermission = {
                        PermissionUtils.openAccessibilitySettings(this)
                    },
                    onStartOverlay = {
                        FloatingOverlayService.start(this)
                    },
                    onStopOverlay = {
                        FloatingOverlayService.stop(this)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoClickerMainApp(
    onRequestOverlayPermission: () -> Unit,
    onRequestAccessibilityPermission: () -> Unit,
    onStartOverlay: () -> Unit,
    onStopOverlay: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var hasOverlayPermission by remember {
        mutableStateOf(PermissionUtils.hasOverlayPermission(context))
    }
    var isAccessibilityEnabled by remember {
        mutableStateOf(PermissionUtils.isAccessibilityServiceEnabled(context))
    }

    // Refresh permissions on resume
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasOverlayPermission = PermissionUtils.hasOverlayPermission(context)
                isAccessibilityEnabled = PermissionUtils.isAccessibilityServiceEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val navItems = listOf(
        NavItem.Dashboard,
        NavItem.Settings,
        NavItem.TestApp
    )

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val currentTitle = when (selectedTab) {
        0 -> "Auto Clicker"
        1 -> "Clicker Settings"
        2 -> "Click Test Bench"
        else -> "Auto Clicker"
    }

    if (isLandscape) {
        Row(modifier = Modifier.fillMaxSize()) {
            NavigationRail(
                modifier = Modifier.fillMaxHeight(),
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ) {
                navItems.forEachIndexed { index, item ->
                    NavigationRailItem(
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title) },
                        selected = selectedTab == index,
                        onClick = { selectedTab = index }
                    )
                }
            }

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = currentTitle,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                },
                snackbarHost = { SnackbarHost(snackbarHostState) },
                modifier = Modifier.weight(1f)
            ) { innerPadding ->
                when (selectedTab) {
                    0 -> DashboardScreen(
                        hasOverlayPermission = hasOverlayPermission,
                        isAccessibilityEnabled = isAccessibilityEnabled,
                        onRequestOverlayPermission = onRequestOverlayPermission,
                        onRequestAccessibilityPermission = onRequestAccessibilityPermission,
                        onStartOverlay = onStartOverlay,
                        onStopOverlay = onStopOverlay,
                        onNavigateToSettings = { selectedTab = 1 },
                        onNavigateToTest = { selectedTab = 2 },
                        modifier = Modifier.padding(innerPadding)
                    )
                    1 -> SettingsScreen(
                        onShowSnackbar = { msg ->
                            scope.launch {
                                snackbarHostState.showSnackbar(msg)
                            }
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                    2 -> TestAppScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = currentTitle,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            },
            bottomBar = {
                NavigationBar {
                    navItems.forEachIndexed { index, item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.title) },
                            label = { Text(item.title) },
                            selected = selectedTab == index,
                            onClick = { selectedTab = index }
                        )
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            when (selectedTab) {
                0 -> DashboardScreen(
                    hasOverlayPermission = hasOverlayPermission,
                    isAccessibilityEnabled = isAccessibilityEnabled,
                    onRequestOverlayPermission = onRequestOverlayPermission,
                    onRequestAccessibilityPermission = onRequestAccessibilityPermission,
                    onStartOverlay = onStartOverlay,
                    onStopOverlay = onStopOverlay,
                    onNavigateToSettings = { selectedTab = 1 },
                    onNavigateToTest = { selectedTab = 2 },
                    modifier = Modifier.padding(innerPadding)
                )
                1 -> SettingsScreen(
                    onShowSnackbar = { msg ->
                        scope.launch {
                            snackbarHostState.showSnackbar(msg)
                        }
                    },
                    modifier = Modifier.padding(innerPadding)
                )
                2 -> TestAppScreen(
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}