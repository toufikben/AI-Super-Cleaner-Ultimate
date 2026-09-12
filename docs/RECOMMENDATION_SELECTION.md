# Recommendation Card Selection — 12 September 2026

Recommendation cards now expose per-file checkboxes instead of being read-only summaries. The card resolves its review category to concrete indexed files:

- `Large videos`: video files at or above 500 MB.
- `Large photos`: image files at or above 20 MB.
- `Screenshots`: image files whose names contain the same screenshot markers used by the recommendation aggregate.

The user can select individual files across multiple recommendation cards. A shared selected count and byte total appears below the Home content, followed by `Move selected to Trash`. The operation uses the existing `CleanupManager`, so every selected URI is revalidated against current provider metadata before mutation. The same Room/cache invalidation and stale-result protection used by Analyze selection applies here.

Cards with no current matching file rows show an explicit rescan message instead of presenting a false cleanup target. Recommendation selection is review-only until the user checks specific files and confirms the Trash operation.

Host verification passed with unit tests, lint, and debug assembly. Device-level MediaStore and Trash verification remains pending.
