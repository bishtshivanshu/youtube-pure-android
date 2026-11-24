# TICKET-003: Multi-Stage Shorts Sanitizer Engine & Tests

- **Status:** ready-for-agent
- **Priority:** p0-critical
- **Spec:** [SPEC-001](../specs/SPEC-001-youtube-android16-no-shorts.md)
- **Dependencies:** TICKET-001

## Description
Implement the core pure Kotlin domain filtering engine that guarantees zero Shorts:
- `ShortsFilterEngine`: Inspects `VideoItem` models and raw feed structures.
- Filter criteria:
  1. Duration heuristic: `durationSeconds <= 60` and `aspectRatio < 1.0` (vertical).
  2. Text heuristic: Title, description, or tags containing `#shorts`, `#short`, or `shorts`.
  3. Raw renderer heuristic: InnerTube `reelItemRenderer`, `shortsShelfRenderer`, `reelShelfRenderer`, `richSectionRenderer`.
  4. Endpoint heuristic: Direct links to `/shorts/` or `reelWatchEndpoint`.
- Includes unit tests with edge cases verifying that standard videos ($\ge 61$s, normal landscape $16:9$, or educational short videos without vertical shorts tags) are preserved while all Shorts reels are purged.

## Acceptance Criteria
- 100% of Shorts items stripped from lists passed through `ShortsFilterEngine.sanitizeList()`.
- Deterministic unit tests with 100% pass rate.
