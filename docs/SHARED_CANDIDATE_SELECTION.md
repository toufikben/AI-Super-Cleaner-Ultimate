# Shared Candidate Selection Surface — 12 September 2026

## Implemented

The Analyze screen now uses one selection model keyed by URI across all indexed cleanup candidates. Every visible row has a checkbox and contributes to a shared selected count and selected-byte total.

Selection is available for:

- Large files at or above 500 MB.
- Downloads exposed through MediaStore.
- APK files.
- Documents.
- Archives and other general files exposed through MediaStore.Files.

The user can select multiple items across these lists and press `Move selected to Trash`. The confirmation dialog shows the selected item count and total bytes. The existing `CleanupManager` then revalidates every URI and compares current metadata before mutation. Stale, missing, or inaccessible results are skipped rather than deleted from an old scan snapshot.

After the operation, selection is cleared and the existing cross-feature invalidation callback clears in-memory duplicate/recommendation reports. Room-backed file counts, byte totals, Trash, and dashboard flows update from the database transition.

## Verification

Host verification passed:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug --no-daemon --console=plain
```

Result: `BUILD SUCCESSFUL`. Device-side verification of MediaStore.Files visibility and Trash mutation remains part of the deferred instrumentation work.
