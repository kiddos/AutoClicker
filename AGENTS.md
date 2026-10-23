# AutoClicker Developer & Agent Guide

Welcome to the **AutoClicker** repository. This document provides an overview of the project architecture, build system, testing procedures, directory layout, and coding conventions for AI agents and human developers.

---

## 1. Project Overview

**AutoClicker** is a native Android application designed to provide automated tapping and click automation using Android's Accessibility Service and Floating System Alert Overlays.

### Core Features
- **Accessibility Gesture Automation**: Programmatic dispatch of touch gestures (`dispatchGesture`) without requiring root access.
- **Floating Overlay Controls**: Draggable overlay sidebar (`WindowManager`) allowing users to start, pause, add/remove target points, and configure clicks directly on top of other applications.
- **Dual Operating Modes**:
  - **Single Click Mode**: Continuously clicks a single target with customizable interval, press duration, and stop conditions (infinite, count limit, duration).
  - **Multi Click Mode**: Sequentially clicks multiple user-positioned targets with per-point or global intervals, durations, and repeat loops.
- **Anti-Detection / Randomization**: Customizable random interval jitter ($\pm \Delta t$) and random spatial radius offsets ($\pm \Delta r$).
- **Integrated Test Bench**: In-app testing screen for validating clicks, tracking clicks-per-second (CPS), inspecting real-time coordinate logs, and testing target accuracy.
- **Modern Jetpack Compose UI**: Built with Material 3 and single-activity architecture (`MainActivity`).

---

## 2. Tech Stack & Build System

### Technologies & SDKs
- **Language**: Kotlin 2.2.10
- **Build System**: Gradle 9.3.1 with Kotlin DSL (`build.gradle.kts`)
- **Android Gradle Plugin (AGP)**: 9.1.1
- **Gradle JVM / Toolchain**: Java 21 (managed via Foojay toolchain resolver)
- **Source & Target Compatibility**: Java 11 (`JavaVersion.VERSION_11`)
- **Android SDK**:
  - `minSdk`: 28 (Android 9.0 Pie)
  - `compileSdk`: 36 (Android 16 preview / vanilla)
  - `targetSdk`: 36
  - `namespace` / `applicationId`: `app.kiddos.autoclicker`
- **UI Toolkit**: Jetpack Compose with Material 3 (Compose BOM 2024.09.00)
- **Dependency Catalog**: Gradle Version Catalog (`gradle/libs.versions.toml`)

### Key Build Commands

Always use the Gradle wrapper (`./gradlew`) from the project root.

| Command | Description |
|---|---|
| `./gradlew assembleDebug` | Compiles and builds the debug APK (`app/build/outputs/apk/debug/app-debug.apk`) |
| `./gradlew assembleRelease` | Compiles and builds the release APK |
| `./gradlew clean` | Deletes build outputs and temporary caches |
| `./gradlew lint` | Runs Android Lint inspections |
| `./gradlew lintDebug` | Runs Android Lint on the debug build variant |
| `./gradlew tasks` | Lists all available Gradle tasks |

> [!NOTE]
> Gradle requires Java 21 for the daemon. Ensure `JAVA_HOME` points to a valid JDK 21 installation or let Gradle Foojay resolver download the required toolchain.

---

## 3. Test Commands & Strategy

### Test Suites
- **Unit Tests**: Host-based JVM tests located in `app/src/test/java/`.
- **Instrumentation Tests**: Device-based Android tests located in `app/src/androidTest/java/`.

### Test Commands

| Command | Description |
|---|---|
| `./gradlew test` | Runs all local unit tests across all build variants |
| `./gradlew testDebugUnitTest` | Runs local unit tests specifically for the Debug variant |
| `./gradlew testReleaseUnitTest` | Runs local unit tests specifically for the Release variant |
| `./gradlew connectedAndroidTest` | Runs all instrumented tests on an attached emulator or physical device |
| `./gradlew connectedDebugAndroidTest` | Runs instrumented tests for the Debug variant |
| `./gradlew check` | Runs all verification tasks including unit tests and lint checks |

### Test Guidelines
- Place unit tests for controllers, models, and calculation utilities in `app/src/test/java/app/kiddos/autoclicker/`.
- Avoid adding Android framework dependencies directly to pure unit tests when possible; use instrumentation tests or mock Android APIs if needed.
- Test changes to `ClickerController` state handling, jitter calculations, and point persistence to ensure regression resistance.

---

## 4. Directory Structure

```
AutoClicker/
├── app/
│   ├── build.gradle.kts             # App-level build config (dependencies, SDK versions, compose)
│   ├── proguard-rules.pro           # ProGuard / R8 obfuscation and optimization rules
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml  # Manifest declaring permissions, Activity, and Services
│       │   ├── java/app/kiddos/autoclicker/
│       │   │   ├── MainActivity.kt                      # Main Activity entry point & navigation scaffold
│       │   │   ├── controller/
│       │   │   │   └── ClickerController.kt             # Central singleton for state, loop execution, & events
│       │   │   ├── data/
│       │   │   │   └── ClickerPreferences.kt            # SharedPreferences persistence layer
│       │   │   ├── model/
│       │   │   │   └── ClickerConfig.kt                 # Data classes & enums (ClickPoint, Configs, Logs)
│       │   │   ├── service/
│       │   │   │   ├── AutoClickerAccessibilityService.kt # Accessibility service dispatching gestures
│       │   │   │   └── FloatingOverlayService.kt        # Foreground service managing floating overlay window
│       │   │   ├── ui/
│       │   │   │   ├── screen/
│       │   │   │   │   ├── DashboardScreen.kt           # Main dashboard, permissions, and sidebar triggers
│       │   │   │   │   ├── SettingsScreen.kt            # Configuration tabs for Single and Multi click modes
│       │   │   │   │   └── TestAppScreen.kt             # Interactive click testing & verification bench
│       │   │   │   └── theme/
│       │   │   │       ├── Color.kt                     # Compose color palette definitions
│       │   │   │       ├── Theme.kt                     # Material 3 theme wrapper
│       │   │   │       └── Type.kt                      # Typography configurations
│       │   │   └── util/
│       │   │       └── PermissionUtils.kt               # Helpers for Overlay & Accessibility permissions
│       │   └── res/
│       │       ├── drawable/        # Vector icons and XML drawable shapes (buttons, targets, sidebar)
│       │       ├── mipmap-*/        # Launcher icons
│       │       ├── values/          # Strings, colors, and XML themes
│       │       └── xml/
│       │           ├── accessibility_service_config.xml # Accessibility service capabilities & filters
│       │           ├── backup_rules.xml
│       │           └── data_extraction_rules.xml
│       ├── test/java/app/kiddos/autoclicker/            # JVM unit tests (ClickerConfigTest, ExampleUnitTest)
│       └── androidTest/java/app/kiddos/autoclicker/     # Android instrumentation tests
├── gradle/
│   ├── gradle-daemon-jvm.properties # Daemon JVM specification (Java 21)
│   ├── libs.versions.toml           # Version Catalog defining plugins and libraries
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties# Gradle wrapper properties (v9.3.1)
├── build.gradle.kts                 # Root Gradle build script
├── gradle.properties                # JVM heap configuration and Kotlin code style setting
├── gradlew                          # Gradle wrapper shell script (Linux/macOS)
├── gradlew.bat                      # Gradle wrapper batch script (Windows)
├── local.properties                 # Local SDK path (not tracked in VCS)
└── settings.gradle.kts              # Gradle plugin & dependency repositories, module includes
```

---

## 5. Architectural Patterns

### 1. State Management & Unidirectional Data Flow (UDF)
- `ClickerController` acts as the single source of truth for all clicker operations, runtime configurations, and active points.
- Mutable state is held internally via private `MutableStateFlow`s (e.g., `_isPlaying`, `_activePoints`, `_singleConfig`) and exposed publicly as read-only `StateFlow`s (`asStateFlow()`).
- Transient events (such as click triggers on the test bench) are broadcast via `MutableSharedFlow` (`_testClickEvents`).
- Compose screens collect states using `.collectAsState()` and dispatch user actions directly through methods on `ClickerController`.

### 2. Service Separation & Android Permissions
- **Gesture Dispatching (`AutoClickerAccessibilityService`)**:
  - Declared with `BIND_ACCESSIBILITY_SERVICE` permission and configured via `accessibility_service_config.xml`.
  - Uses `dispatchGesture(GestureDescription)` to click coordinate pairs without requiring root permissions.
  - Automatically attaches itself to `ClickerController` on connection and detaches on unbind/destruction.
- **Overlay Management (`FloatingOverlayService`)**:
  - Runs as a Foreground Service with `foregroundServiceType="specialUse"` and `SYSTEM_ALERT_WINDOW` permission.
  - Dynamically attaches and moves overlay views using `WindowManager` (`TYPE_APPLICATION_OVERLAY`).
  - Supports dragging sidebar controls and individual target pins across the display.

### 3. Coroutine Scope & Threading
- The core click execution loop runs in a long-lived `CoroutineScope(SupervisorJob() + Dispatchers.Default)` inside `ClickerController`.
- The floating service uses `serviceScope` (`SupervisorJob() + Dispatchers.Main`) to observe controller flows and safely update Android Views.
- When paused or stopped, running jobs are cleanly cancelled with `clickLoopJob?.cancel()`.

---

## 6. Coding Conventions & Best Practices

When contributing code or modifying this repository, follow these standards:

### Kotlin & Code Style
- **Code Style**: Follow the official Kotlin coding conventions (`kotlin.code.style=official`).
- **File Naming**: PascalCase for files containing classes/objects/composables (e.g., `ClickerController.kt`, `DashboardScreen.kt`).
- **Enums & Constants**: UPPER_SNAKE_CASE for enum entries and `const val` declarations; camelCase for regular properties.
- **Immutability**: Prefer `val` over `var` whenever possible. Use immutable collections (`List`) for public API surfaces and copy-on-write patterns (`copy()`) for data classes.

### Jetpack Compose Standards
- Always annotate composable functions with `@Composable`.
- Accept an optional `modifier: Modifier = Modifier` as the first optional parameter in reusable composables, and chain it onto the root layout.
- Hoist state where appropriate, passing event callbacks up (`onClick: () -> Unit`) rather than mutating states deep inside UI components.
- Rely on `MaterialTheme.colorScheme` and `MaterialTheme.typography` rather than hardcoding colors and text sizes.

### Android Services & Permissions
- Verify both `Settings.canDrawOverlays(context)` and accessibility service enabled status before launching overlays.
- Never block the UI thread (`Dispatchers.Main`) with delay loops or disk I/O; delegate long-running operations or timing loops to background coroutines.
- Keep the `NotificationChannel` importance low (`IMPORTANCE_LOW`) for foreground overlay controls to avoid disruptive audio alerts.

### Dependencies & Version Catalog
- Add all new library dependencies and plugin versions to `gradle/libs.versions.toml`.
- Do not hardcode version strings in `app/build.gradle.kts`. Reference catalog items using `libs.<name>` aliases.
