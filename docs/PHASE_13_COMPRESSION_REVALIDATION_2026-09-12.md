# Phase 13 Compression Revalidation — 12 September 2026

## Implemented

Compression now returns a structured `CompressionError` classification for permission denial, missing files, storage exhaustion, unsupported codecs, unavailable output, cancellation, and unknown failures. User-facing messages explicitly state that the original file was not changed when compression fails.

Image compression now reads EXIF orientation and applies the required rotation or mirror transform before exporting a JPEG copy. The original URI is never overwritten. Temporary image and video outputs are deleted on success, failure, or cancellation, and a cancelled coroutine is rethrown rather than incorrectly recorded as an ordinary failure.

A codec policy recognizes the supported image and video MIME families, and output names use safe copy extensions (`.jpg` and `.mp4`). Export still uses MediaStore pending-item semantics on Android 10 and later and deletes a partially created output URI if export fails.

## Tests added

Unit coverage was added for supported and unsupported codec MIME types, permission/missing-file/codec/storage/output/cancellation error classification, and safe output extensions. Device instrumentation coverage was added for JPEG BitmapFactory round-tripping and EXIF 90-degree orientation preservation.

## Verification status

The tests were added but not executed in this sandbox because Android SDK, `ANDROID_HOME`, `sdk.dir`, `adb`, and an emulator/device are unavailable. The phase therefore remains **in progress**. Required commands when an Android SDK is available are:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug --no-daemon
./gradlew connectedDebugAndroidTest --no-daemon
```

The device run must additionally exercise actual H.264/AAC Media3 transformation, unsupported video codec behavior, low-storage output failure, cancellation cleanup, and confirmation that the original media remains byte-for-byte unchanged.
