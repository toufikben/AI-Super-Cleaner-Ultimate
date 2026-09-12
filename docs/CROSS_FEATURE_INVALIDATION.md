# Cross-feature Invalidation — 12 September 2026

## Contract

A scan result is a review snapshot, not authorization to mutate storage. Before `Move to Trash`, `Restore`, or `Permanent Delete`, the app re-queries the provider using the URI and compares the current display name, MIME type, size, modified time, and relative path with the snapshot.

If the URI is missing, changed, inaccessible, or invalid, the destructive action is skipped. The user receives a stale-result message and is asked to scan again. A changed or missing duplicate cannot silently proceed to cleanup.

## Invalidation graph

| Event | Cache | Duplicate results | Recommendations | Counters/dashboard | Cleanup selection |
|---|---|---|---|---|---|
| Scan completed | Rebuilt/reconciled | Recomputed | Recomputed | Room flows update | Replaced |
| Move to Trash | Removed for moved URI | Invalidated | Recomputed | Decremented from Room | Cleared |
| Restore | Reinserted with analysis fields cleared | Invalidated | Recomputed | Incremented from Room | Cleared |
| Permanent Delete | Removed | Invalidated | Recomputed | Decremented from Room | Cleared |
| External URI revalidation failure | Snapshot cannot authorize action | Invalidated | Invalidated | No destructive mutation | Cleared |

Room `file_metadata` is now updated as part of the cleanup transition: moved and permanently deleted files are removed; restored files are reinserted with hashes, blur, screenshot, and analysis version cleared so stale analysis cannot be reused. Compose clears in-memory duplicate and recommendation reports after cleanup attempts, and Room-backed flows update file counts, byte totals, Trash, recommendations, and dashboard score.

## Safety properties

The cleanup path revalidates each distinct URI immediately before mutation. It uses current metadata and current size for the operation, never the old snapshot. If writing the Trash row fails, the provider mutation is rolled back. The existing confirmation dialog and explicit selection requirement remain unchanged.

## Verification

The explicit invalidation graph has unit tests for Scan, Move to Trash, Restore, Permanent Delete, and external stale-result transitions. Host verification passed:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug --no-daemon --console=plain
```

Instrumentation on a real Android provider is still required to verify MediaStore trash-query visibility, URI permission changes, and restore/delete behavior on supported API levels.
