#!/usr/bin/env bash
set -euo pipefail

SDK_ROOT="${ANDROID_HOME:-$HOME/android-sdk}"
CMDLINE_TOOLS_VERSION="15859902"
CMDLINE_TOOLS_URL="https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_TOOLS_VERSION}_latest.zip"

if ! command -v javac >/dev/null 2>&1; then
  echo "A full JDK is required. Install OpenJDK 21 (including javac) before continuing." >&2
  exit 1
fi

mkdir -p "$SDK_ROOT/cmdline-tools"
if [[ ! -x "$SDK_ROOT/cmdline-tools/latest/bin/sdkmanager" ]]; then
  archive="$(mktemp /tmp/android-cmdline-tools.XXXXXX.zip)"
  trap 'rm -f "$archive"' EXIT
  curl -fL "$CMDLINE_TOOLS_URL" -o "$archive"
  rm -rf "$SDK_ROOT/cmdline-tools/latest" "$SDK_ROOT/cmdline-tools/cmdline-tools"
  unzip -q "$archive" -d "$SDK_ROOT/cmdline-tools"
  mv "$SDK_ROOT/cmdline-tools/cmdline-tools" "$SDK_ROOT/cmdline-tools/latest"
fi

export ANDROID_HOME="$SDK_ROOT"
export PATH="$SDK_ROOT/cmdline-tools/latest/bin:$SDK_ROOT/platform-tools:$SDK_ROOT/emulator:$PATH"

yes | sdkmanager --sdk_root="$SDK_ROOT" --licenses >/dev/null || true
sdkmanager --sdk_root="$SDK_ROOT" \
  "platform-tools" \
  "platforms;android-36" \
  "build-tools;36.0.0"

printf 'sdk.dir=%s\n' "$SDK_ROOT" > "$(cd "$(dirname "$0")/.." && pwd)/local.properties"

cat <<EOF
Android SDK is ready.
SDK: $SDK_ROOT
Project local.properties updated.

For the current shell:
  export ANDROID_HOME="$SDK_ROOT"
  export PATH="\$ANDROID_HOME/cmdline-tools/latest/bin:\$ANDROID_HOME/platform-tools:\$ANDROID_HOME/emulator:\$PATH"

Run host checks:
  ./gradlew testDebugUnitTest lintDebug assembleDebug --no-daemon
  adb devices -l

Run device tests after connecting an API 26+ device or starting an emulator:
  ./gradlew connectedDebugAndroidTest --no-daemon
EOF
