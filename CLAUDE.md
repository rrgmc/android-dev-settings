# CLAUDE.md

Guidance for working in this repository. `README.md` describes the app for users; `docs/PROMPT.md` is the
original specification.

## Workflow

- `master` is protected. Every change reaches it through a pull request, and the `test` CI check must pass on
  the latest commit. Force pushes and deletion are blocked, and the rules also apply to administrators.
- Work on a feature branch. Merge pull requests with a merge commit (not squash) and delete the branch.
- Releases are tags, not commits. Push `vX.Y.Z` from `master`; `.github/workflows/release.yml` builds the signed
  APK and publishes a GitHub release. The version name and code come from the tag, so do not edit them in
  `app/build.gradle.kts`.

## Commands

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug   # what CI runs
./gradlew assembleRelease -PappVersion=1.2.3          # release build; debug-signed without SIGNING_* variables
```

- Lint treats warnings as errors. Fix the warning, or suppress it in code with a comment that says why.
- Run lint with network access before pushing. CI is online, and some lint checks only run online.

## Pinned versions

AGP 8.13.2, Kotlin 2.2.21, Gradle 8.14.3, `compileSdk` and `targetSdk` 36, `minSdk` 30. Newer library
versions need `compileSdk` 37, so lint's version checks are disabled in `app/build.gradle.kts`. Do not raise
versions as a side effect of other work.

## Code layout

Package `com.rangelreale.devsettings` under `app/src/main/java/`:

- `data/`: `SettingsStore` (interface over `Settings.Global`), `AndroidSettingsStore`, `DevSettingsRepository`
  (read and write logic), `DevSetting` (setting keys and "on" values), `WirelessDebugInfo` (IP address and
  adb port lookup).
- `keepon/`: keep-screen-on durations, state and the foreground service that holds the wake lock.
- `tiles/`: Quick Settings tiles. `BaseTileService` handles the permission check, observers and opening the app.
- `ui/`: Compose theme and the main screen.

## Conventions

- Read settings from the system every time. Do not cache them; the UI refreshes on resume and through a
  `ContentObserver`.
- Keep setting logic in `data/` free of Android framework calls, so it runs in plain JVM unit tests. Use literal
  setting keys rather than `Settings.Global` constants. Tests use `FakeSettingsStore`.
- Add a unit test for every change to read or write logic.
- User-facing text goes in `res/values/strings.xml`.

## Testing on a device or emulator

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell pm grant com.rangelreale.devsettings android.permission.WRITE_SECURE_SETTINGS
adb shell cmd statusbar add-tile com.rangelreale.devsettings/.tiles.StayAwakeTileService
adb shell cmd statusbar click-tile com.rangelreale.devsettings/.tiles.StayAwakeTileService
adb shell settings get global stay_on_while_plugged_in
```

Turning on wireless debugging the first time on a network opens a system dialog. The setting stays 0 until
the user allows the network.
