# Privacy Policy for Auto Clicker

**Last Updated:** October 24, 2026  
**Effective Date:** October 24, 2026  
**Application:** Auto Clicker  
**Package Identifier:** `app.kiddos.autoclicker`  

---

## 1. Introduction

This Privacy Policy explains how **Auto Clicker** ("we", "our", or "the app") treats user data and respects your privacy. We are committed to transparency and protecting your rights. 

**Auto Clicker does not collect, transmit, store, sell, or share any personal information, usage analytics, or device identifiers.** The application functions completely offline on your device, and no data leaves your phone or tablet.

---

## 2. Prominent Disclosure: AccessibilityService API

To comply with Google Play Developer Policies and ensure total transparency with our users, this section provides an explicit and prominent disclosure regarding our use of the **Android AccessibilityService API**.

### Why We Use the AccessibilityService API
Auto Clicker requires the `AccessibilityService` API (`AutoClickerAccessibilityService`) solely to perform automated touch gestures on your screen. Specifically, the service uses the official Android `dispatchGesture(...)` API to simulate taps and clicks at the precise screen coordinates and time intervals configured by you.

### What the Accessibility Service Does and Does Not Do
- **Automated Clicks Only**: The Accessibility Service is used strictly to execute touch gestures that you have defined and explicitly started.
- **NO Screen Content Monitoring or Logging**: The service **does not** read, observe, inspect, parse, or capture any on-screen content, window hierarchies, text, photos, messages, or user interfaces.
- **NO Keystroke or Input Recording**: The service **never** logs, monitors, or captures keystrokes, personal passwords, credit card numbers, payment details, or credentials.
- **NO Personal Data Collection**: The service collects zero personal data, telemetry, or diagnostic records.
- **NO System Circumvention**: The service does not alter system settings without user control, bypass user interface permissions, or prevent users from disabling or uninstalling any application.

### User Control and Revocation
Granting Accessibility permission is entirely voluntary and requires your explicit authorization in your device's System Settings. You can enable, disable, or revoke Accessibility permission at any time by navigating to:
> **Settings > Accessibility > Downloaded Apps / Installed Services > Auto Clicker**

Disabling this permission will prevent the app from performing automated gestures, but will not affect the rest of your device.

---

## 3. System Permissions and Justifications

Auto Clicker requests minimal permissions strictly necessary to deliver its core click automation features. The permissions and their specific purposes are:

| Permission | Technical Name | Purpose |
|---|---|---|
| **Accessibility Service** | `android.permission.BIND_ACCESSIBILITY_SERVICE` | Required to simulate screen taps and click gestures (`dispatchGesture`) on your screen. |
| **Display Over Other Apps** | `android.permission.SYSTEM_ALERT_WINDOW` | Required to display the floating overlay toolbar, action buttons, and target pins on top of other running applications so you can configure and control clicks without switching apps. |
| **Foreground Service** | `android.permission.FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_SPECIAL_USE` | Keeps the floating controls and click execution engine active in the background without being unexpectedly terminated by the Android operating system while you interact with other applications. |
| **Post Notifications** | `android.permission.POST_NOTIFICATIONS` | Displays a persistent notification while the floating overlay service is running (required by Android 13 and above), allowing you to see the service status and quickly stop or return to the app. |

---

## 4. No Internet Access & Zero Data Transmission

- **No Internet Permission**: The app does **not** declare or request the `android.permission.INTERNET` permission in its Android Manifest.
- **Completely Offline**: Because the app lacks network capabilities, it cannot communicate with external servers, cloud storage, analytics backends, or third-party networks.
- **No Third-Party SDKs**: Auto Clicker does not incorporate third-party advertising SDKs, behavioral analytics (such as Google Analytics or Firebase), social media trackers, or external crash-reporting suites.

---

## 5. Local Data Storage

All user configurations—such as click intervals, press duration, random jitter parameters, target point coordinates $(X, Y)$, and grid configuration presets—are stored strictly locally on your device using Android's private `SharedPreferences` (`auto_clicker_prefs`).

- **Storage Location**: Private app sandbox directory on internal device storage.
- **Access Restrictions**: Stored configurations are accessible only by the Auto Clicker app; other applications cannot read or access this data.
- **Data Deletion**: You can erase all locally saved configurations at any time by:
  1. Opening **Settings > Apps > Auto Clicker > Storage & Cache**.
  2. Selecting **Clear Storage** or **Clear Data**.
  3. Uninstalling the application (which permanently deletes all associated local data).

---

## 6. Children's Privacy (COPPA / GDPR Compliance)

Auto Clicker is a general utility tool and is not specifically directed at children under the age of 13 (or under 16 within the European Economic Area). Because the application does not collect, store, or transmit any personal information from any user, we do not knowingly collect personal data from children.

---

## 7. Security

We prioritize the security of your device and user experience:
- All operations are sandboxed within standard Android security boundaries.
- No remote code execution or dynamic code downloading occurs.
- The app operates in adherence to modern Android target SDK guidelines (Target SDK 36) and follows the principle of least privilege.

---

## 8. Changes to This Privacy Policy

We may update this Privacy Policy from time to time, for example, to reflect updates in Android platform requirements, Google Play policies, or app functionality. Any modifications will be reflected with an updated "Last Updated" date at the top of this document. We recommend reviewing this policy periodically.

---

## 9. Contact Us

If you have questions, feedback, or concerns regarding this Privacy Policy or the security and privacy practices of Auto Clicker, please open an issue in the project repository or contact the application developer:

- **Project Repository:** Auto Clicker (`app.kiddos.autoclicker`)
- **Contact:** Open a GitHub Issue / Developer Support
