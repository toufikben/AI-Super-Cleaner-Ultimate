# General File Scan Implementation — 12 September 2026

## What changed

`StorageScanner` now scans a fourth source in addition to Images, Videos, and Audio: `MediaStore.Files.getContentUri("external")`. The query selects `MEDIA_TYPE_NONE`, preventing duplicate rows for media already returned by the image, video, and audio collections.

Non-media files are classified into `download`, `apk`, `archive`, `document`, or `other`. The classifier covers common ZIP/RAR/7Z/TAR/GZ archives, APK packages, PDF/DOC/DOCX/XLS/XLSX/PPT/PPTX/TXT/CSV/RTF/ODT/ODS documents, and paths under Downloads. Relative path and MIME type are both considered.

The files remain in the same metadata database and therefore participate in scan reconciliation, cache checks, size aggregation, duplicate candidate discovery, and the existing explicit-selection cleanup safety model.

## Important Android boundary

This expands the scanner to all non-media files that Android exposes through the MediaStore Files provider under the app's granted access. It does not claim access to app-private directories, files hidden from MediaStore, or every arbitrary filesystem path. On Android versions where general non-media access is restricted, a user-selected Storage Access Framework directory or a narrowly justified special access flow is required; `MANAGE_EXTERNAL_STORAGE` was intentionally not added.

## Tests

Unit tests cover ZIP, APK, PDF, DOCX, XLSX, TXT, Downloads, and the platform-independent classification rules. Instrumentation tests verify that the Android MediaStore Files URI is included as a non-media source and that the source uses `MEDIA_TYPE_NONE` to avoid duplicate media rows.

The following host checks passed after moving framework URI assertions out of JVM tests:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug --no-daemon --console=plain
```

Result: `BUILD SUCCESSFUL`. Device-side query visibility still requires the deferred instrumentation run on an emulator or physical Android device.
