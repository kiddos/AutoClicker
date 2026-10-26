#!/usr/bin/env python3
import os
import subprocess
import time

DEVICE = "emulator-5554"
REMOTE_VIDEO = "/sdcard/special_use_demo.mp4"
OUTPUT_DIR = os.path.abspath("screenshots")
LOCAL_VIDEO = os.path.join(OUTPUT_DIR, "foreground_service_special_use_demo.mp4")

def adb(cmd):
    full_cmd = f"adb -s {DEVICE} {cmd}"
    res = subprocess.run(full_cmd, shell=True, capture_output=True, text=True)
    return res.stdout.strip()

def tap(x, y, delay=0.5):
    print(f"Tap ({x}, {y})")
    adb(f"shell input tap {x} {y}")
    time.sleep(delay)

def swipe(x1, y1, x2, y2, duration=400, delay=0.5):
    print(f"Swipe ({x1}, {y1}) -> ({x2}, {y2})")
    adb(f"shell input swipe {x1} {y1} {x2} {y2} {duration}")
    time.sleep(delay)

def main():
    print("=== Step 1: Setting up environment ===")
    adb("shell rm -f /sdcard/special_use_demo.mp4")
    adb("shell appops set app.kiddos.autoclicker SYSTEM_ALERT_WINDOW allow")
    adb("shell pm grant app.kiddos.autoclicker android.permission.POST_NOTIFICATIONS")
    adb("shell settings put secure enabled_accessibility_services app.kiddos.autoclicker/app.kiddos.autoclicker.service.AutoClickerAccessibilityService")
    adb("shell settings put secure accessibility_enabled 1")
    adb("shell settings put system show_touches 1")

    # Stop any running overlay service first
    adb("shell am start -n app.kiddos.autoclicker/.MainActivity")
    time.sleep(1.0)
    tap(173, 2232, 0.5) # Dashboard
    adb("shell input tap 830 1765") # Dismiss if running
    time.sleep(0.5)

    print("=== Step 2: Starting screen recording (38s) ===")
    rec_proc = subprocess.Popen(
        f"adb -s {DEVICE} shell 'screenrecord --bit-rate 4000000 --time-limit 38 {REMOTE_VIDEO}'",
        shell=True
    )
    time.sleep(2.0) # Wait for screenrecord to initialize

    print("=== Step 3: Demonstrating in-app setup ===")
    # Ensure on Dashboard
    tap(173, 2232, 1.0)

    # Select Multi Mode
    tap(470, 1100, 1.2)

    # Launch Floating Side Bar (starts Foreground Service)
    tap(580, 1607, 1.8)

    print("=== Step 4: Showing Ongoing Foreground Service Notification ===")
    adb("shell cmd statusbar expand-notifications")
    time.sleep(3.2) # Reviewers read 'Auto Clicker Active: Floating sidebar is running'
    adb("shell cmd statusbar collapse")
    time.sleep(1.2)

    print("=== Step 5: Exit App to Home Screen (Tasks outside app) ===")
    adb("shell input keyevent KEYCODE_HOME")
    time.sleep(1.8)

    print("=== Step 6: Drag Target Point 1 outside the app ===")
    swipe(540, 1200, 300, 1000, 500, 1.2)

    print("=== Step 7: Add Target Point 2 via floating sidebar (+) ===")
    tap(95, 553, 1.2) # Add button (+)

    print("=== Step 8: Drag Target Point 2 to second location ===")
    swipe(540, 1200, 750, 1400, 500, 1.2)

    print("=== Step 9: Start Auto-Clicking via floating Play button ===")
    tap(95, 443, 0.5) # Play button (▶)
    # Let clicks run outside the app for 5 seconds
    time.sleep(5.0)

    print("=== Step 10: Pause Auto-Clicking via floating Pause button ===")
    tap(95, 443, 1.2) # Pause button (⏸)

    print("=== Step 11: Return to App via floating Settings button ===")
    tap(95, 993, 2.0) # Settings button on floating sidebar

    print("=== Step 12: Dismiss / Stop Foreground Service ===")
    tap(830, 1765, 2.0) # Dismiss button on Dashboard

    print("=== Waiting for recording to finalize ===")
    rec_proc.wait()
    time.sleep(1.5)

    print(f"=== Pulling video to {LOCAL_VIDEO} ===")
    os.makedirs(OUTPUT_DIR, exist_ok=True)
    subprocess.run(f"adb -s {DEVICE} pull {REMOTE_VIDEO} '{LOCAL_VIDEO}'", shell=True, check=True)
    
    print("=== Video recording complete! ===")
    subprocess.run(f"ffprobe '{LOCAL_VIDEO}'", shell=True)

if __name__ == "__main__":
    main()
