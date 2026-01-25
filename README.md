# Remote PC Control App

Remote PC Control App is an Android application that allows you to **wake up** and
**shut down** a computer remotely over the local network.

It is designed to be simple, lightweight and reliable, without unnecessary background services or complex configuration.

---

## How does it work?

The application, when configured to do so, tries to connect to target device's port **6800** and, if succeed, retrieve updated information.

Supported commands are:

- `INFO` — Retrieve information about the target computer (hostname, OS, IP and MAC addresses).
- `SHUTDOWN` — Shut down the computer remotely.
- `CANCEL_SHUTDOWN` — Cancel a previous delayed shut down command.

On the target computer, a **system tray icon** is available to:

- Display detected network interfaces (Interface type, IP and MAC)
- Exit the application

That’s all — simple, explicit and enough for the intended use case.

---

Project prepared for Android Studio Otter (2025.2.1).
Includes:
 - Jetpack Compose + Material3 UI
 - Navigation Compose
 - Room (KSP)
 - Network scanner (simple InetAddress.isReachable loop)
 - Repository with socket shutdown message and Wake-on-LAN (UDP magic packet)
 - ViewModel and basic UI (Main + Add device)

How to open:
1. Open Android Studio Otter and choose 'Open' -> select this folder.
2. Let Gradle sync. If you don't have Gradle wrapper, Android Studio will offer to download it.
3. Build & run on a device/emulator (minSdk 26).

Notes:
 - KSP plugin is included in app build script.
 - Some APIs (network scanning, socket) require a real network environment; the emulator may not reflect LAN devices.
 - The sample sends a plain text message to port 9999 for shutdown (you must have a server on the PC side to receive it).

Copy/paste this script as `PROJECT_HOME/.git/hooks/pre-commit` to get automated version change when commiting changes
```bash
#!/bin/bash

echo "[pre-commit] Updating version..."
./gradlew incrementVersion

git add gradle.properties
```

