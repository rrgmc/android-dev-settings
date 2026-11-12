# Android Dev Settings

An Android app that toggles the developer settings I use most, without opening Developer options:

- USB debugging
- Wireless debugging
- Stay awake while charging (AC, USB and wireless)
- Keep screen on for a chosen time, charging or not

The app is for my own phones. It is not published on the Play Store. It needs Android 11 (API 30) or later.

## Build

Requirements: JDK 17 or later, and the Android SDK with platform 36. Put the SDK path in
`local.properties` (`sdk.dir=...`) or in the `ANDROID_HOME` environment variable.

```bash
./gradlew assembleDebug
```

The APK is `app/build/outputs/apk/debug/app-debug.apk`. Install it with:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Run the unit tests and lint with:

```bash
./gradlew testDebugUnitTest lintDebug
```

## Grant the permission

Changing these settings needs `android.permission.WRITE_SECURE_SETTINGS`. Only a computer can grant it, and
only once per installation:

1. Turn on USB debugging in Developer options and connect the phone to the computer.
2. Run:

   ```bash
   adb shell pm grant com.rangelreale.devsettings android.permission.WRITE_SECURE_SETTINGS
   ```

Until the permission is granted, the main screen shows a setup card with this command and a copy button, and every
switch stays disabled. The app notices the grant within a few seconds. It needs neither root nor Shizuku.

Developer options must be on for the settings to have effect. When they are off, the main screen explains how to
turn them on: open About phone and tap Build number seven times.

## Main screen

Each row shows the setting, its current value and a switch. Values come from the system on every resume and
through a content observer, so changes made in system Settings appear at once.

- Turning off USB or wireless debugging asks for confirmation, because a connected computer loses its connection.
- When wireless debugging is on, the screen shows the phone's address and port for `adb connect`, with a copy
  button. The port is found through the `service.adb.tls.port` property or, failing that, mDNS.
- The first time wireless debugging is turned on for a Wi-Fi network, Android asks whether to allow that network.
- "Open wireless debugging (pairing)" opens Developer options with the wireless debugging row highlighted. Tap the
  row title to reach pairing. Android has no public intent for the pairing screen itself.
- "Open Developer options" opens the system Developer options screen.

## Quick Settings tiles

Add the tiles from the Quick Settings edit screen. Every tile shows the real system state, and a long press opens
the app. Without the permission, a tile shows "Setup needed" and a tap opens the app on the setup card. (Android
does not deliver taps to tiles in the unavailable state, so these tiles stay inactive instead.)

| Tile | Tap |
| --- | --- |
| USB debugging | Turns USB debugging on or off. On a locked phone, it asks to unlock first. |
| Wireless debugging | Turns wireless debugging on or off. On a locked phone, it asks to unlock first. |
| Stay awake | Turns stay awake while charging on (AC, USB and wireless) or off. |
| Keep screen on | Moves to the next step: 5 min, 15 min, 30 min, 1 hour, until stopped, off. Each step restarts the countdown. The subtitle shows the time left. |

Tiles do not ask for confirmation.

## Keep screen on

A foreground service holds a screen wake lock. Its ongoing notification counts down the time left and has a Stop
action. On Android 13 and later, the app asks for notification permission the first time; without it, the screen
still stays on but the notification is hidden. The power button still turns the screen off.

## Releases

CI runs on every pull request and every push to `master`: unit tests, lint and a debug build
(`.github/workflows/ci.yml`).

Pushing a tag such as `v1.2.0` runs `.github/workflows/release.yml`. It tests, builds an APK signed with the
release key, and publishes it as a GitHub release. The version name comes from the tag (`1.2.0`) and the version
code is `major * 10000 + minor * 100 + patch`.

```bash
git tag v1.2.0
git push origin v1.2.0
```

The release workflow needs two repository secrets:

| Secret | Value |
| --- | --- |
| `RELEASE_KEYSTORE_BASE64` | The keystore file, base64-encoded. |
| `RELEASE_KEYSTORE_PASSWORD` | The keystore password. It is also used as the key password. |

The key alias defaults to `rrgmc`. Set the repository variable `RELEASE_KEY_ALIAS` to use another one.

To build a signed release locally, set `SIGNING_STORE_FILE`, `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS` and
`SIGNING_KEY_PASSWORD`, then run `./gradlew assembleRelease -PappVersion=1.2.0`. Without these variables, release
builds are signed with the debug key.
