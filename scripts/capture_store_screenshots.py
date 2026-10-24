#!/usr/bin/env python3
import os
import subprocess
import time

PHONE = "emulator-5554"
TABLET = "emulator-5556"

PHONE_DIR = os.path.abspath("screenshots/phone")
TABLET_DIR = os.path.abspath("screenshots/tablet")
os.makedirs(PHONE_DIR, exist_ok=True)
os.makedirs(TABLET_DIR, exist_ok=True)

def adb(device, cmd):
    full_cmd = f"adb -s {device} {cmd}"
    res = subprocess.run(full_cmd, shell=True, capture_output=True, text=True)
    return res.stdout.strip()

def tap(device, x, y):
    adb(device, f"shell input tap {x} {y}")
    time.sleep(0.4)

def swipe(device, x1, y1, x2, y2, duration=300):
    adb(device, f"shell input swipe {x1} {y1} {x2} {y2} {duration}")
    time.sleep(0.5)

def capture(device, out_path):
    subprocess.run(f"adb -s {device} exec-out screencap -p > '{out_path}'", shell=True, check=True)
    print(f"Captured: {out_path}")
    time.sleep(0.3)

def setup_environment(device):
    adb(device, "shell appops set app.kiddos.autoclicker SYSTEM_ALERT_WINDOW allow")
    adb(device, "shell pm grant app.kiddos.autoclicker android.permission.POST_NOTIFICATIONS")
    adb(device, "shell settings put secure enabled_accessibility_services app.kiddos.autoclicker/app.kiddos.autoclicker.service.AutoClickerAccessibilityService")
    adb(device, "shell settings put secure accessibility_enabled 1")
    
    # Clean Play Store Demo Mode
    adb(device, "shell settings put global sysui_demo_allowed 1")
    adb(device, "shell am broadcast -a com.android.systemui.demo -e command enter")
    adb(device, "shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 1200")
    adb(device, "shell am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false")
    adb(device, "shell am broadcast -a com.android.systemui.demo -e command network -e wifi show -e level 4 -e mobile hide")
    adb(device, "shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false")

    # Bring app to front
    adb(device, "shell am start -n app.kiddos.autoclicker/.MainActivity")
    time.sleep(1.0)

def capture_phone():
    print("=== Processing Phone (Pixel 6 - 1080x2400) ===")
    setup_environment(PHONE)

    # Make sure we start at Dashboard
    tap(PHONE, 173, 2232)
    time.sleep(0.5)
    tap(PHONE, 212, 1000) # Ensure Single mode card selected
    time.sleep(0.3)

    # 1. Phone Dashboard
    capture(PHONE, os.path.join(PHONE_DIR, "01_phone_dashboard.png"))

    # 2. Phone Settings - Single Click
    tap(PHONE, 540, 2232) # Settings bottom tab
    time.sleep(0.5)
    tap(PHONE, 180, 420)  # Single Click tab
    tap(PHONE, 350, 840)  # Select 100ms preset chip
    time.sleep(0.3)
    capture(PHONE, os.path.join(PHONE_DIR, "02_phone_single_click_settings.png"))

    # 3. Phone Settings - Anti-Detection / Jitter (scroll down)
    swipe(PHONE, 540, 1800, 540, 950, 350)
    capture(PHONE, os.path.join(PHONE_DIR, "03_phone_anti_detection_settings.png"))

    # 4. Phone Settings - Multiple Click Tab
    swipe(PHONE, 540, 950, 540, 1800, 350) # Scroll back up
    tap(PHONE, 540, 420) # Multiple Click tab
    capture(PHONE, os.path.join(PHONE_DIR, "04_phone_multi_click_settings.png"))

    # 5. Phone Settings - 16x2 Grid Tab
    tap(PHONE, 900, 420) # 16x2 Grid tab
    capture(PHONE, os.path.join(PHONE_DIR, "05_phone_grid_mode_settings.png"))

    # 6. Phone Click Test Bench - Speed & CPS Test (with live rapid clicks)
    tap(PHONE, 907, 2232) # Click Test bottom tab
    time.sleep(0.5)
    tap(PHONE, 200, 360)  # Speed & CPS Test tab
    tap(PHONE, 880, 1592) # Reset clicks first
    time.sleep(0.2)
    for _ in range(25):
        adb(PHONE, "shell input tap 348 1592")
        time.sleep(0.04)
    time.sleep(0.2)
    capture(PHONE, os.path.join(PHONE_DIR, "06_phone_click_test_bench.png"))

    # 7. Phone Click Test Bench - Sequence Test (1 -> 2 -> 3)
    tap(PHONE, 550, 360) # Sequence Test tab
    time.sleep(0.4)
    tap(PHONE, 380, 1200) # Simulate Step 1 Hit
    time.sleep(0.3)
    capture(PHONE, os.path.join(PHONE_DIR, "07_phone_sequence_test.png"))

    # 8. Phone Floating Overlay Active over Click Test
    tap(PHONE, 173, 2232) # Dashboard tab
    time.sleep(0.5)
    tap(PHONE, 580, 1586) # Launch Side Bar
    time.sleep(0.8)
    tap(PHONE, 907, 2232) # Switch to Click Test tab
    time.sleep(0.5)
    tap(PHONE, 200, 360)  # Speed & CPS Test
    time.sleep(0.3)
    tap(PHONE, 85, 430)   # Tap Play button on floating sidebar
    time.sleep(1.8)       # Allow auto-clicking gesture to fire
    capture(PHONE, os.path.join(PHONE_DIR, "08_phone_floating_overlay_active.png"))
    tap(PHONE, 85, 430)   # Pause clicking
    time.sleep(0.3)
    tap(PHONE, 85, 870)   # Close overlay sidebar

def capture_tablet():
    print("=== Processing Tablet (Pixel Tablet - 2560x1600) ===")
    setup_environment(TABLET)

    # 1. Start at Dashboard
    tap(TABLET, 80, 112) # Dashboard rail item
    time.sleep(0.5)
    # Ensure overlay is closed if active
    tap(TABLET, 2370, 1212) # Tap Dismiss if present
    time.sleep(0.3)
    tap(TABLET, 597, 740) # Select Single mode card
    time.sleep(0.3)
    capture(TABLET, os.path.join(TABLET_DIR, "01_tablet_dashboard.png"))

    # 2. Tablet Settings - Single Click
    tap(TABLET, 80, 232)  # Settings rail item
    time.sleep(0.5)
    tap(TABLET, 560, 248) # Single Click tab
    time.sleep(0.3)
    tap(TABLET, 360, 480) # 100ms preset chip
    time.sleep(0.3)
    capture(TABLET, os.path.join(TABLET_DIR, "02_tablet_settings_single.png"))

    # 3. Tablet Settings - Multiple Click Tab
    tap(TABLET, 1360, 248) # Multiple Click tab
    time.sleep(0.4)
    capture(TABLET, os.path.join(TABLET_DIR, "03_tablet_settings_multi.png"))

    # 4. Tablet Settings - 16x2 Grid Tab
    tap(TABLET, 2160, 248) # 16x2 Grid tab
    time.sleep(0.4)
    capture(TABLET, os.path.join(TABLET_DIR, "04_tablet_settings_grid.png"))

    # 5. Tablet Click Test Bench - Speed & CPS Test (with rapid clicks)
    tap(TABLET, 80, 352)  # Click Test rail item
    time.sleep(0.5)
    tap(TABLET, 338, 248) # Speed & CPS Test tab
    time.sleep(0.3)
    tap(TABLET, 2415, 1164) # Reset button
    time.sleep(0.2)
    for _ in range(25):
        adb(TABLET, "shell input tap 1235 1164")
        time.sleep(0.04)
    time.sleep(0.2)
    capture(TABLET, os.path.join(TABLET_DIR, "05_tablet_click_test_bench.png"))

    # 6. Tablet Click Test Bench - Sequence Test (1 -> 2 -> 3)
    tap(TABLET, 678, 248)  # Sequence Test tab
    time.sleep(0.4)
    tap(TABLET, 2443, 1012) # Reset sequence
    time.sleep(0.2)
    tap(TABLET, 1263, 1012) # Simulate Step 1 Hit
    time.sleep(0.3)
    capture(TABLET, os.path.join(TABLET_DIR, "06_tablet_sequence_test.png"))

    # 7. Tablet Floating Overlay Active over Click Test
    tap(TABLET, 80, 112)   # Dashboard rail item
    time.sleep(0.5)
    tap(TABLET, 1360, 1092) # Launch Side Bar button
    time.sleep(0.8)
    tap(TABLET, 80, 352)   # Click Test rail item
    time.sleep(0.5)
    tap(TABLET, 338, 248)  # Speed & CPS Test tab
    time.sleep(0.3)
    tap(TABLET, 122, 290)  # Tap Play button on horizontal floating sidebar
    time.sleep(1.8)        # Allow gesture auto-clicking to run & update CPS
    capture(TABLET, os.path.join(TABLET_DIR, "07_tablet_floating_overlay.png"))
    tap(TABLET, 122, 290)  # Pause clicking
    time.sleep(0.3)
    tap(TABLET, 458, 290)  # Close floating sidebar

if __name__ == "__main__":
    capture_phone()
    capture_tablet()
    print("All screenshots successfully captured!")
