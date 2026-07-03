# Prompt 41: Duplication & Refactoring Audit

## Part 1: Effort-Based Year Processing — **CONFIRMED, CONSOLIDATED**

`EducationEngine` and `CareerEngine` duplicated three-branch effort logic with separate tuned ranges. Crime clean-streak and relationship decay use different semantics (hire penalty curve, drift-to-neutral) — **left separate**.

**Extracted:** `domain/EffortResolver.kt`

| Call site | Preserved values |
|-----------|------------------|
| Study GPA | SLACK: -0.1..-0.31, NORMAL: +0.05..+0.16, HARD: +0.1..+0.31 |
| Study smarts | SLACK: -1, NORMAL: +1, HARD: +2 |
| Study happiness | HARD only: -1 |
| Work year performance | COAST: -5..-15, NORMAL: -2..+5, GRIND: +5..+15 |
| Work year happiness | COAST: +1..+3, NORMAL: +0..+1, GRIND: -2..-5 |
| Work year health | GRIND only: -1..-3 |
| Work event performance | COAST: -8..-17, NORMAL: 0..+5, GRIND: +8..+17 |
| Work event happiness | COAST: +2..+5, NORMAL: 0, GRIND: -4..-9 |
| Work event health | GRIND only: -2..-5 |

## Part 2: Confirm-Then-Execute UI — **PARTIALLY CONFIRMED, CONSOLIDATED**

**Extracted:** `ui/components/ConfirmableAction.kt` (`rememberConfirmableAction`, `ConfirmableActionHost`)

| Site | Before | After |
|------|--------|-------|
| Crime / doctor (`ActionsScreen`) | Hand-rolled `mutableStateOf` + `ConfirmActionDialog` | `ConfirmableActionHost` |
| Asset purchase (`AssetsScreen`) | Raw `AlertDialog` (no haptic, different button styling) | `ConfirmActionDialog` via host — **visual alignment only** |
| Gift / travel (`PersonDetailSheet`) | Separate boolean + tier state | `ConfirmableActionHost` |
| Job apply (`CareerScreen`) | No confirmation (direct apply) | **Unchanged** — adding confirm would be behavior change |
| Marriage propose (`PersonDetailSheet`) | Direct `onPropose` | **Unchanged** — never had confirm step |
| Relocation | Life-event choice in `GameEngine` | **Unchanged** — different flow by design |

All consolidated dialogs dismiss on backdrop tap via `ConfirmActionDialog.onDismissRequest`.

## Part 3: Random Person Generation — **CONFIRMED, CONSOLIDATED**

**Extracted:** `domain/PersonGenerator.kt`

| Call site | Preserved parameters |
|-----------|---------------------|
| `DatingPool` | 15% foreign country, age offset -5..+5, min age 18, relationship 30..60, stats ranges |
| `RelationshipEngine.generateFriendshipOpportunity` | Age offset -3..+3, relationship 40..60, same NPC stat ranges |
| `FamilyGenerator` | Parent age 20..40, sibling chance 40%, sibling offset -5..+5, relationship 50, parent/sibling stat ranges, shared surname logic stays in generator |

Family structure (mother/father/siblings with shared surname) remains in `FamilyGenerator`; shared `buildPerson` + stat helpers deduplicate boilerplate.

## Part 4: Stat Clamping — **CONFIRMED, CONSOLIDATED**

**Extracted:** `util/ClampUtils.kt` (`clampStat`, `clampRelationshipLevel`, `clampPerformanceScore`, `clampCondition`, `clampGpa`)

Routed through helpers in: `Stats`, `Person`, all domain engines with stat mutations. Left alone: `HealthEngine.illnessChance` normalization `(100 - health).coerceIn(0, 100)` — not a stat clamp.

## Maintenance benefit

- Effort balance: **1 file** (`EffortResolver.kt`) instead of 2 engines × 2 code paths
- Person tuning: **1 file** (`PersonGenerator.kt`) for dating/friend/NPC stat constants
- Stat bounds: grep for raw `.coerceIn(0, 100)` outside `ClampUtils` / `illnessChance` = review flag
- Confirm flows: **1 host** pattern for action screens; ~60 lines of duplicated state/dialog wiring removed
