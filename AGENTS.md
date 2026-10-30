# AutoClicker Developer & Agent Guide

Welcome to the **AutoClicker** repository. This document provides an overview of the project architecture, build system, testing procedures, directory layout, and coding conventions for AI agents and human developers.

---

## 1. Project Overview & Architecture

**AutoClicker** is a native Android application designed to provide automated tapping and click automation using Android's Accessibility Service and Floating System Alert Overlays.

### Module Structure
The project is currently structured as a single monolithic module:
- `:app`: Contains all application code, UI screens, services, models, and data persistence.

As there are no feature modules, all dependencies and components reside within `:app`.

### Architectural Pattern
The application adheres to **Unidirectional Data Flow (UDF)**:
- **State Management (`ClickerController`)**: Acts as a central singleton engine and the single source of truth. It holds mutable state internally via `MutableStateFlow` and exposes it publicly as read-only `StateFlow`.
- **Event Handling**: Transient events (like test clicks) are broadcast via `MutableSharedFlow`.
- **UI Interaction**: Jetpack Compose screens collect states using `.collectAsState()` and dispatch user actions directly through methods on the `ClickerController`.

---

## 2. Environment & Build Commands

### Environment Setup
- **Java / JDK**: Gradle requires **JDK 21** for the daemon. The project uses the Foojay toolchain resolver to manage this.
- **Java Compatibility**: The project is compiled with source and target compatibility set to Java 11 (`JavaVersion.VERSION_11`).
- **Android SDK**:
  - `minSdk`: 28 (Android 9.0 Pie)
  - `compileSdk`: 36
  - `targetSdk`: 36

### Key Build Commands
Always use the Gradle wrapper (`./gradlew`) from the project root.

- **Build Debug APK**: `./gradlew assembleDebug` (compiles to `app/build/outputs/apk/debug/app-debug.apk`)
- **Build Release APK**: `./gradlew assembleRelease`
- **Clean Project**: `./gradlew clean` (deletes build outputs and caches)

---

## 3. Testing & Verification Workflow

### Test Commands
- **Local JVM Unit Tests**:
  - Run all: `./gradlew test`
  - Run for debug variant: `./gradlew testDebugUnitTest`
  - Agents should rely on these JVM tests for quick verification, located in `app/src/test/`.
- **Android Instrumented Tests**:
  - Run for debug variant: `./gradlew connectedDebugAndroidTest`
  - *Note: Instrumented tests located in `app/src/androidTest/` require an attached emulator or physical device to run.*

### Static Analysis & Linting
- **Android Lint**: `./gradlew lintDebug` (runs Android Lint inspections on the debug build variant).
- **Checks**: `./gradlew check` (runs all verification tasks including unit tests and lint checks).

---

## 4. Kotlin & Android Coding Conventions

### UI Layer
- **Toolkit**: Built exclusively with **Jetpack Compose** and Material 3.
- **Patterns**: Follow state hoisting and UDF. Always annotate composable functions with `@Composable`.
- **Modifiers**: Accept an optional `modifier: Modifier = Modifier` as the first optional parameter in reusable composables, chaining it onto the root layout.
- **Theming**: Rely on `MaterialTheme.colorScheme` and `MaterialTheme.typography` from the `app/src/main/java/app/kiddos/autoclicker/ui/theme/` package instead of hardcoding values.

### Concurrency & State
- **Coroutines**: Use Kotlin Coroutines for asynchronous work. Long-running operations (like the click execution loop) should use `Dispatchers.Default`, while UI or window updates (like the floating service) should use `Dispatchers.Main`.
- **Flows**: Use `StateFlow` for state that requires an initial value and ongoing updates. Use `SharedFlow` for one-off events. Compose should collect states reactively.

### Dependency Injection & Libraries
- **Dependency Management**: All new dependencies **must** be added to the Gradle Version Catalog first (`gradle/libs.versions.toml`), and referenced in `app/build.gradle.kts` via aliases (e.g., `libs.androidx.core.ktx`). Do not hardcode versions in the build scripts.
- **Dependency Injection**: The app uses manual DI via singletons (like `ClickerController`) instead of heavy frameworks like Hilt or Koin.

---

## 5. Agent Guardrails (Do's and Don'ts)

- **Do NOT manually edit generated files**: Never modify files inside the `build/` directories.
- **Verification First**: Always run the relevant unit tests (`./gradlew testDebugUnitTest`) and linter tasks (`./gradlew lintDebug`) before finalizing a task.
- **Resource Handling**:
  - Define strings, dimensions, and colors in `app/src/main/res/values/`.
  - Drawables and vector icons should be placed in `app/src/main/res/drawable/`.
- **Offline & Privacy**: The app is 100% offline. Do not add `android.permission.INTERNET` or integrate tracking/analytics SDKs.
- **Permissions**: Respect Android's permission model. Ensure both `Settings.canDrawOverlays(context)` and accessibility service enabled status are verified appropriately before modifying service flows.
