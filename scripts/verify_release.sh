#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

ARTIFACT="${1:-app/build/outputs/apk/release/app-release.apk}"
test -s "$ARTIFACT"

grep -Eq '<uses-permission[^>]*android.permission.MANAGE_EXTERNAL_STORAGE' app/src/main/AndroidManifest.xml
grep -Eq '<uses-permission[^>]*android.permission.QUERY_ALL_PACKAGES' app/src/main/AndroidManifest.xml

if [[ "$ARTIFACT" == *.apk ]]; then
  AAPT="${ANDROID_HOME:?}/build-tools/36.0.0/aapt"
  VERSION_CODE="$("$AAPT" dump badging "$ARTIFACT" | sed -n "s/.*versionCode='\([^']*\)'.*/\1/p" | head -1)"
else
  VERSION_CODE="derived-from-bundle"
fi
test -n "$VERSION_CODE"
if [[ "$VERSION_CODE" != "derived-from-bundle" ]]; then
  [[ "$VERSION_CODE" =~ ^[0-9]+$ ]]
  test "$VERSION_CODE" -gt 0
fi

if [[ "$ARTIFACT" == *.apk ]]; then
  APK_SIGNER="${ANDROID_HOME:?}/build-tools/36.0.0/apksigner"
  if [[ "$ARTIFACT" != *unsigned* ]]; then
    "$APK_SIGNER" verify --verbose "$ARTIFACT"
  fi
fi

if [[ -f app/build/outputs/mapping/release/mapping.txt ]]; then
  test -s app/build/outputs/mapping/release/mapping.txt
fi

echo "Release artifact checks passed: $ARTIFACT (versionCode=$VERSION_CODE)"
