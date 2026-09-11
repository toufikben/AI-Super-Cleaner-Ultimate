#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

if grep -E "<uses-permission[^>]*MANAGE_EXTERNAL_STORAGE" -n app/src/main/AndroidManifest.xml; then
  echo "ERROR: broad MANAGE_EXTERNAL_STORAGE permission must remain absent from the manifest." >&2
  exit 1
fi

./gradlew testDebugUnitTest assembleDebug lintDebug --no-daemon

if [[ "${QA_ALLOW_TEST_ADS:-0}" != "1" ]] && grep -R "ca-app-pub-3940256099942544" -n app/src/main; then
  echo "ERROR: Google test AdMob IDs are present. Set production IDs before release." >&2
  exit 1
fi

echo "QA checks passed. For development with Google test IDs, run QA_ALLOW_TEST_ADS=1 ./scripts/qa_release.sh."
