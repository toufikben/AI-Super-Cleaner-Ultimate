# Phase 14 QA and Release Report

## Scope

Phase 14 covers dependency and toolchain compatibility, monotonic versioning, R8 release behavior, signing configuration, CI workflows, artifact checks, Manifest permissions, and failure handling.

## Implemented in this audit

The main Android CI workflow now installs the exact Android Platform 36 and Build Tools 36.0.0 packages used by the project, enables Gradle caching, applies read-only workflow permissions, cancels superseded runs, and verifies the unsigned release artifact. The signed release workflow now installs the same SDK packages and uses Build Tools 36.0.0 for `apksigner` instead of assuming Build Tools 35.

A shared `scripts/verify_release.sh` now checks that the broad `MANAGE_EXTERNAL_STORAGE` permission is absent, derives a positive versionCode from the APK, verifies signed APKs when applicable, and confirms release mapping output when present. Release signing remains environment-driven and uses temporary keystore material supplied through GitHub Actions secrets; no production secret is stored in the repository or debug configuration.

R8 rules were narrowed to the application's Room reflection contracts. AndroidX, Media3, Ads, UMP, and Billing libraries are allowed to use their own consumer rules rather than being globally kept, reducing unnecessary retention in the release artifact.

## Executed checks

| Check | Result | Evidence |
|---|---|---|
| Clean build | Passed | `./gradlew clean ...` completed successfully |
| Unit tests | Passed | `testDebugUnitTest` completed successfully |
| Debug lint | Passed | `lintDebug` completed successfully |
| Debug APK | Passed | `assembleDebug` completed successfully |
| Release/R8 APK | Passed | `assembleRelease` completed successfully with minification and resource shrinking |
| Release artifact policy checks | Passed | `scripts/verify_release.sh` passed; positive versionCode `101`, Manifest check passed |
| R8 mapping | Passed | `app/build/outputs/mapping/release/mapping.txt` generated, approximately 54 MB |
| Instrumentation | Not executed | `adb devices -l` has no connected device or emulator |
| Signed APK/AAB | Not executed locally | Protected release keystore secrets are intentionally unavailable in the sandbox |

## Compatibility findings

The project uses AGP 8.7.3, Gradle 8.11.1, Kotlin 2.0.21, compileSdk/targetSdk 36, Java 17 source compatibility, Room 2.6.1, Media3 1.5.1, Billing 8.0.0, Ads 23.6.0, UMP 4.0.0, and Compose BOM 2024.12.01. The local build succeeded with the full JDK 21 and SDK Platform 36. Gradle reports a non-blocking warning that AGP 8.7.3 was tested through compileSdk 35; this should be addressed during a deliberate AGP upgrade rather than by an unplanned dependency change.

The build also reports a deprecated Compose `InsertDriveFile` icon, KAPT fallback from Kotlin language version 2.0 to 1.9, and an unstripped `libandroidx.graphics.path.so` packaged as-is. None blocked the release build, but they remain follow-up maintenance items.

## Release gate status

Phase 14 is **partially verified**. Host-side clean, test, lint, debug, release/R8, versionCode, Manifest, mapping, and CI configuration checks are complete. It cannot be marked fully complete until a connected API 26+ device or emulator runs instrumentation tests and a protected signing environment produces and verifies the signed AAB/APK. CI must also run successfully on GitHub after the workflow changes.
