# Phase 14 QA and Release Report

## Executed checks

| Check | Result | Notes |
|---|---|---|
| Unit tests | Passed | `testDebugUnitTest` completed successfully. Cache, Premium entitlement, and ad cooldown policies covered. |
| Debug build | Passed | `assembleDebug` completed successfully. |
| Debug lint | Passed | `lintDebug` completed successfully. |
| Automated QA script | Passed | `QA_ALLOW_TEST_ADS=1 ./scripts/qa_release.sh` passed permission, tests, build, lint, and development Test ID checks. |
| Release build | Passed | `assembleRelease` completed with `isMinifyEnabled` and `isShrinkResources`. |
| Instrumentation tests | Not executed in sandbox | `adb devices` returned no connected device or emulator. The test source is present and must run on a connected API 26+ emulator/device. |

## Release artifact

The unsigned artifact was generated at `app/build/outputs/apk/release/app-release-unsigned.apk`. It is not publishable until signed with a protected release keystore and verified in Play Console internal testing.

## Automated policy checks

The Android Manifest has no actual `MANAGE_EXTERNAL_STORAGE` uses-permission declaration. The QA script intentionally permits Google Test Ad IDs only when `QA_ALLOW_TEST_ADS=1` is explicitly set for development. A production run without that flag fails until production IDs replace the test IDs.

## Manual/device QA matrix

The following must be executed on an emulator and at least one physical Android device before release: first launch with consent required; consent rejected; privacy options reopened; ad request blocked; Rewarded unavailable; Rewarded completed; Billing unavailable; license test subscription; Lifetime purchase; purchase restore; permission denial; partial media permission; scan cancellation; 10,000+ media items; low-memory image analysis; file removed during scan; MediaStore trash restore; permanent delete confirmation; video codec failure; low storage; rotation/process recreation.

## Known non-blocking warnings

Compose reports one deprecated `InsertDriveFile` icon. Billing reports the Java deprecation of `enablePendingPurchases()`. Release reports that `libandroidx.graphics.path.so` could not be stripped and is packaged as-is. None blocked the build; they should be reviewed during dependency upgrades.

## Release decision

**Release Candidate buildable, not yet publish-ready.** Signing, production AdMob IDs, UMP configuration, Play Console products, Data Safety submission, and device/emulator instrumentation remain required before external release.
