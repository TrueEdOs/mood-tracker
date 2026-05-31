# Mood Tracker

Currently it's 100% vibe coded.

A minimal, robust Android app for logging your current mood. Tap one of three
buttons — **Bad**, **Ok**, or **Good** — and the app appends the mood plus a
timestamp to a CSV file. To avoid spammy logging, the buttons gray out for
5 minutes after each entry and show when they'll be available again. A **Share**
button exports the CSV via the standard Android share sheet.

<p align="center">
  <em>Bad (blue) · Ok (yellow) · Good (green) — gray while locked.</em>
</p>

## CSV format

Stored at the app's internal `files/moods.csv`. One header row, then one row per
tap:

```csv
timestamp,mood
2026-05-31T13:57:26,Ok
2026-05-31T14:02:10,Good
```

`timestamp` is local time in ISO-8601 (`yyyy-MM-dd'T'HH:mm:ss`).

## Requirements

- **JDK 17** (Android Gradle Plugin 8.5 requires it).
- **Android SDK** with `platform-34`, `build-tools;34.0.0`, and
  `platform-tools`.
- Gradle is provided via the wrapper (`./gradlew`), pinned to 8.7 — no system
  Gradle needed.

Minimum supported device: **Android 7.0 (API 24)**. Target: API 34.

### One-time toolchain setup (macOS / Homebrew)

```sh
brew install openjdk@17
brew install --cask android-commandlinetools

export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
export ANDROID_HOME="$HOME/Library/Android/sdk"

sdkmanager --sdk_root="$ANDROID_HOME" \
  "platform-tools" "platforms;android-34" "build-tools;34.0.0"
yes | sdkmanager --sdk_root="$ANDROID_HOME" --licenses
```

> Alternatively, open the project in **Android Studio**, which installs the SDK
> and builds automatically.

## Build

```sh
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
export ANDROID_HOME="$HOME/Library/Android/sdk"

./gradlew assembleDebug
```

The APK is written to:

```
app/build/outputs/apk/debug/app-debug.apk
```

## Install & run on a device

1. On the phone, enable **Developer options** (Settings → About → tap *Build
   number* 7×) and turn on **USB debugging**. Connect via USB and accept the
   "Allow USB debugging?" prompt.
2. Confirm the device is visible, then install and launch:

```sh
adb devices                       # your device should be listed
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.example.moodtracker/.MainActivity
```

## Usage

1. Open **Mood Tracker**.
2. Tap **Bad**, **Ok**, or **Good** to record how you feel. The entry is saved
   immediately.
3. The buttons gray out for 5 minutes and show when they unlock. They re-enable
   on their own.
4. Tap **Share** any time to export `moods.csv` to another app.

To read the log directly over adb:

```sh
adb shell run-as com.example.moodtracker cat files/moods.csv
```

## License

MIT — do whatever you like with it.
