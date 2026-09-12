# Phase 15 Test Suite Report — 12 September 2026

## Implemented coverage

Unit coverage now includes exact duplicate grouping versus same-size candidates, recoverable-byte calculation, screenshot and blur classification thresholds, and a 100,000-item candidate-grouping edge case. Existing cache, recommendation, entitlement, ad cooldown, compression, and UI-state tests remain part of the unit suite.

Instrumentation coverage now includes an in-memory Room database test for file upsert, scan-token reconciliation, analysis updates, scan history, Trash persistence/removal, and compression history. Additional device tests verify MediaStore collection availability and the absence of `MANAGE_EXTERNAL_STORAGE`. The phase 13 JPEG/EXIF device tests remain included in the instrumentation source set.

The GitHub Actions Android workflow now contains a separate emulator-backed instrumentation job using API 35 Google APIs. It installs the emulator image and runs `connectedDebugAndroidTest` after the emulator boots.

## Host verification

The following command completed successfully after correcting one test expectation to match the engine's anchor-file policy:

```bash
./gradlew testDebugUnitTest lintDebug compileDebugAndroidTestKotlin assembleDebug --no-daemon
```

The result was `BUILD SUCCESSFUL`; 38 unit tests completed successfully in the corrected run, instrumentation sources compiled, lint passed, and the debug APK was assembled.

## Device/CI verification status

The local environment has no connected Android device or running emulator, so `connectedDebugAndroidTest` has not been executed locally. The emulator job is configured but must run on GitHub Actions after the workflow is pushed. Database migration upgrade tests from historical Room versions, permission revoke/partial access, MediaStore insertion/trash/restore, Compose interaction, Billing, and large real-media performance remain device-level checks.

Accordingly, phase 15 remains **in progress** until the emulator CI job completes and any failures are corrected. No claim is made that device tests passed before that run.
