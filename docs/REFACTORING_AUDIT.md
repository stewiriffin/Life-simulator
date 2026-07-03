# Prompt 41: Duplication & Refactoring Audit

## Part 1: Effort-Based Year Processing — CONFIRMED, CONSOLIDATED

`EducationEngine` and `CareerEngine` duplicated three-branch effort logic. Crime clean-streak and relationship decay use different semantics — **left separate**.

**Extracted:** `domain/EffortResolver.kt`

| Call site | Preserved values |
|-----------|------------------|
| Study GPA | SLACK: -0.1..-0.31, NORMAL: +0.05..+0.16, HARD: +0.1..+0.31 |
| Study smarts | SLACK: -1, NORMAL: +1, HARD: +2 |
| Work year performance | COAST: -5..-15, NORMAL: -2..+5, GRIND: +5..+15 |
| Work event performance | COAST: -8..-17, NORMAL: 0..+5, GRIND: +8..+17 |

## Part 2: Confirm-Then-Execute UI — PARTIALLY CONFIRMED, CONSOLIDATED

**Extracted:** `ui/components/ConfirmableAction.kt` (`rememberConfirmableAction`, `ConfirmableActionHost`)

| Site | After |
|------|-------|
| Crime / doctor (`ActionsScreen`) | `ConfirmableActionHost` |
| Asset purchase (`AssetsScreen`) | `ConfirmActionDialog` via host |
| Gift / travel (`PersonDetailSheet`) | `ConfirmableActionHost` |
| Dating break-up (`PersonDetailSheet`) | **P49:** confirm when not married; divorce immediate |
| Job apply (`CareerScreen`) | **Unchanged** — no confirm |
| Marriage propose | **Unchanged** — no confirm step |

## Part 3: Random Person Generation — CONFIRMED, CONSOLIDATED

**Extracted:** `domain/PersonGenerator.kt`

| Call site | Preserved parameters |
|-----------|---------------------|
| `DatingPool` | 15% foreign country, age offset -5..+5, min age 18 |
| `RelationshipEngine.generateFriendshipOpportunity` | Age offset -3..+3 |
| `FamilyGenerator` | Parent age 20..40, sibling chance 40% |

## Part 4: Stat Clamping — CONFIRMED, CONSOLIDATED

**Extracted:** `util/ClampUtils.kt` (`clampStat`, `clampRelationshipLevel`, `clampPerformanceScore`, `clampCondition`, `clampGpa`)

Routed through helpers in: `Stats`, `Person`, all domain engines with stat mutations.

## Part 5: Bounded list caps — P37, P43, P54 (not Prompt 41, but same pattern)

| Utility | File | Purpose |
|---------|------|---------|
| `EventLogCap` | `domain/EventLogCap.kt` | Max 150 event log lines |
| `AncestryHistoryCap` | `domain/AncestryHistoryCap.kt` | Max 25 ancestry entries |
| `RelationshipMilestoneCap` | `domain/RelationshipMilestoneCap.kt` | Max 25 milestones per person |

All follow: `object` + `MAX_ENTRIES` + `trim()` + apply at mutation + `CharacterRepository.saveGame`.

## Maintenance benefit

- Effort balance: **1 file** (`EffortResolver.kt`) instead of 2 engines × 2 code paths
- Person tuning: **1 file** (`PersonGenerator.kt`) for dating/friend/NPC stat constants
- Stat bounds: grep for raw `.coerceIn(0, 100)` outside `ClampUtils` = review flag
- Confirm flows: **1 host** pattern for action screens
- Growth caps: **3 cap utilities** with identical architectural shape

---

*Prompt 41 audit. Updated Prompts 49, 54 (July 2026).*
