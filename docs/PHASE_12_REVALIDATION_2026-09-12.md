# Phase 12 Revalidation — 12 September 2026

## Result

The previous roadmap claim that phase 12 was complete was not sufficient for release verification. The source was re-audited and the following corrections were implemented:

- Ad loading is explicitly gated by UMP `canRequestAds()` and is disabled when consent is absent or premium entitlement is active.
- Changing Privacy Options now updates the ad-request gate through the observable consent state.
- Ad initialization is idempotent and ad preloading is blocked until consent is available.
- Billing reconnects after service disconnection, refreshes products and purchases, handles `ITEM_ALREADY_OWNED`, and retries service access safely.
- Entitlement is derived only from completed purchases for the two supported product IDs; pending purchases do not activate Premium.
- Purchase acknowledgement is attempted for every completed, unacknowledged purchase.
- Unit-testable policies now cover consent gating, premium ad removal, unknown products, cooldowns, and clock rollback.

## Verification status

Static review and `git diff --check` were completed. Gradle verification could not run in this sandbox because no Android SDK, `ANDROID_HOME`, `sdk.dir`, `adb`, or emulator is available. Therefore this report does **not** claim that unit, lint, APK, or device tests passed for this revision.

The following still require an Android SDK and a connected API 26+ device or emulator:

- `testDebugUnitTest`, `lintDebug`, and `assembleDebug` on this revision.
- UMP consent acceptance, rejection, and Privacy Options behavior.
- No ad request before consent and no ad request for Premium.
- Rewarded and interstitial loading/showing/cooldown behavior.
- Monthly and lifetime purchase, pending, already-owned, restore, acknowledgement, cancellation, and offline behavior.

## Phase status

Phase 12 remains **in progress** until the above automated and device checks are executed and the Play Console/AdMob configuration is verified in the authenticated account.
