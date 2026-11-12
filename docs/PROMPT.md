# Android Dev Settings application

Create an Android application that toggles the developer settings I use most, without navigating to the Developer options menu. The application targets my own phones. It will not be published on the Play Store.

## Settings to control

The application must control these settings:

- **USB debugging** (`Settings.Global.ADB_ENABLED`).
- **Wireless debugging** (`adb_wifi_enabled` in `Settings.Global`).
- **Stay awake while charging** (`Settings.Global.STAY_ON_WHILE_PLUGGED_IN`). When enabled, set it to cover AC, USB and wireless charging (value `7`). When disabled, set it to `0`.
- **Keep screen on (timed)**. This keeps the screen awake for a chosen time, whether or not the phone is charging. Offer fixed durations (5 min, 15 min, 30 min, 1 hour, indefinitely). Hold the screen on from a foreground service with an ongoing notification that shows the time left and a "Stop" action.

Developer options must be enabled on the phone for these settings to have effect. The application must detect when they are disabled (`Settings.Global.DEVELOPMENT_SETTINGS_ENABLED`) and explain how to enable them.

## Permission

Changing these settings requires `android.permission.WRITE_SECURE_SETTINGS`. Declare it in the manifest, and grant it once from a computer:

```
adb shell pm grant <package> android.permission.WRITE_SECURE_SETTINGS
```

The application must check whether it holds the permission. When it does not, the main screen must show a setup card with the exact `adb` command (including the real package name), a button that copies it, and short instructions. Every toggle must stay disabled until the permission is granted.

Do not require root or Shizuku.

## Main screen

The main screen shows one row per setting. Each row has the setting name, its current state, and a switch. The state must reflect the real system value, not a cached one. Refresh it when the screen resumes and observe changes with a `ContentObserver`, so changes made in the system Settings app appear immediately.

For wireless debugging, show the phone's current IP address and the debugging port when it is enabled, so I can run `adb connect` from the computer. Add a button that opens the system wireless debugging screen for pairing.

Add a button that opens the system Developer options screen.

## Quick Settings tiles

Provide one Quick Settings tile per setting (USB debugging, wireless debugging, stay awake, keep screen on). Each tile must:

- show its active or inactive state from the real system value;
- toggle the setting on tap;
- open the application on long press;
- show as unavailable when the permission is missing, and open the setup card on tap.

The keep screen on tile cycles through the durations on each tap, then turns off.

## Safety

Turning off USB debugging while a computer is connected drops the connection. Show a confirmation before turning off USB or wireless debugging from the main screen. Tiles toggle without confirmation.

## Technical requirements

- Kotlin with Jetpack Compose and Material 3.
- `minSdk` 30 (wireless debugging needs Android 11), `targetSdk` and `compileSdk` 36.
- Gradle Kotlin DSL with a version catalog.
- Light theme, following dynamic color where available.
- Unit tests for the setting read and write logic, using a fake settings layer.
- A `README.md` with build steps, the `pm grant` command, and a description of each tile.
