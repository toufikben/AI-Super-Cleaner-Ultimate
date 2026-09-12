# Android Test Environment

## Installed environment

The project now uses the official Android command-line tools on Linux x86_64. The SDK is installed at `$HOME/android-sdk`, with Android Platform 36, Build Tools 36.0.0, and Platform Tools 37.0.1. The project contains a generated `local.properties` pointing to this SDK. A full OpenJDK 21 installation, including `javac`, and Gradle 8.11.1 are available. A JRE-only installation is insufficient for Gradle test compilation.

The setup follows the official [Android command-line tools documentation](https://developer.android.com/tools), which describes `sdkmanager`, `adb`, Build Tools, Platform Tools, and the Emulator packages.

## Shell setup

```bash
export ANDROID_HOME="$HOME/android-sdk"
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"
```

To make this persistent for future shells, add the same two exports to `~/.bashrc` and reload it with `source ~/.bashrc`.

## Reproducible setup

From the repository root, run:

```bash
./scripts/setup_android_sdk.sh
```

The script installs the command-line tools if missing, accepts SDK licenses, installs Platform 36, Build Tools 36.0.0, and Platform Tools, and writes `local.properties`.

## Host-side verification

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug --no-daemon
```

These checks do not require a physical device. They validate Kotlin compilation, unit tests, lint, and the debug APK. They completed successfully in the prepared environment. Gradle emitted compatibility warnings because AGP 8.7.3 predates compileSdk 36, but the build completed successfully.

## Device-side verification

Connect an Android device with USB debugging enabled, authorize the computer on the device, and confirm:

```bash
adb devices -l
```

The device must appear with state `device`, not `unauthorized` or `offline`. Then run:

```bash
./gradlew connectedDebugAndroidTest --no-daemon
```

For phase 13, the device test suite includes JPEG codec round-tripping and EXIF orientation handling. A real device or emulator is additionally required to verify Media3 H.264/AAC transformation, cancellation cleanup, unsupported codec behavior, low-storage output failure, and preservation of the original media.

## Emulator option

If no physical device is available, install an emulator system image and create an API 35 or API 36 AVD. The command-line packages needed for that route are:

```bash
sdkmanager "emulator" "platform-tools" "system-images;android-35;google_apis;x86_64"
printf 'no\n' | avdmanager create avd -n phase13-api35 -k "system-images;android-35;google_apis;x86_64" --force
emulator -avd phase13-api35 -no-window -no-audio -no-boot-anim &
adb wait-for-device
adb shell getprop sys.boot_completed
./gradlew connectedDebugAndroidTest --no-daemon
```

The current sandbox has no emulator image or running device, so `adb devices -l` currently reports an empty device list. Consequently, host-side checks can run, but `connectedDebugAndroidTest` remains pending until a device or emulator is attached.
