# Performance Audit — Prompt 43 (itel A665L / budget Android)

Measured optimization pass for Maisha Life Simulator. No new gameplay features — behavior-preserving performance work only.

## Target device profile

| Spec | itel A665L (reference) |
|------|------------------------|
| OS | Android 13 |
| RAM | Low (~2–3 GB class) |
| Display | ~360dp width |
| Emulator stand-in | AVD: API 33, 2 GB RAM, 4 CPU cores |

## Part 3 thresholds (pass/fail)

| Criterion | Threshold | Status | Notes |
|-----------|-----------|--------|-------|
| Cold start | < ~2 s (splash → first interactive frame) | **Not measured (Prompt 49)** | Capture via Android Studio Startup Profiler |
| `ageUp()` hook chain | < 16 ms (one frame) | **Pass (JVM)** | `PerformanceBenchmarkTest.ageUp_medianUnder16ms_onJvm` |
| Scroll jank (Family / Achievements) | Zero dropped frames | **Not measured (Prompt 49)** | Manual Layout Inspector pass still required |
| Memory (50+ year session) | Stable, no climb | **Pass (JVM proxy)** | `MemorySoakTest` — event log, ancestry, per-person milestone caps |
| APK size | Tracked | **Pass** | See baseline table below |

---

## Baseline metrics (before optimizations)

| Metric | Before | Method |
|--------|--------|--------|
| Debug APK size | **24,734,838 bytes (~23.6 MB)** | `app-debug.apk` |
| Event pool size | **100 events** (7 JSON files) | Asset count |
| `getEligibleEvents()` | Full-list filter every call | Code audit |
| `checkAchievements()` | `calculateNetWorth()` up to **2×** per age-up | Code audit |
| `ancestryHistory` cap | **None** (unbounded across legacy) | Code audit |
| `eventLog` cap | **150 entries** (`EventLogCap`) | P37 |
| `ancestryHistory` cap | **25 entries** (`AncestryHistoryCap`) | P43 |
| `Person.milestones` cap | **25 per person** (`RelationshipMilestoneCap`) | P54 — nested in `familyJson` |
| Confetti particles | **28** → **18** | `CelebrationOverlay` |
| `AvatarConfig` stability | No `@Immutable` | Code audit |
| `PersonCard` expression | Recomputed every recomposition | Code audit |

---

## Optimizations (before → after)

### 1. `EventRepository` — age-bucket index at load

Build `Array<List<LifeEvent>>` indexed by age when events load; `getEligibleEvents` scans only the age bucket (~10–40 events vs 100).

**Behavior preserved** — `EventRepositoryAgeIndexTest` asserts identical eligibility vs flat filter.

### 2. `AchievementEngine` — single `calculateNetWorth()` per check

Compute net worth once per `checkAchievements()` call; reuse for `six_figures` and `first_million`.

### 3. `AncestryHistoryCap` — bound legacy JSON growth

`MAX_ENTRIES = 25`; applied in `LegacyEngine.createLegacyCharacter` and `CharacterRepository.save`.

### 4. `RelationshipMilestoneCap` — bound per-person milestone JSON (P54)

`MAX_ENTRIES = 25` per `Person.milestones`; applied in `RelationshipEngine` on append and `CharacterRepository.saveGame` via `trimFamily`.

**Justification:** Nested list inside `familyJson` — same multi-generation carry-over risk as ancestry, but slower growth due to significance-only logging (~1–3 milestones/year/person in heavy play vs 1 event log line per age-up). Uncapped worst case ~180/person × ~10 family ≈ 1,800 objects/save before cap.

| Metric | Before | After |
|--------|--------|-------|
| Max milestones / person | Unbounded | **25** (newest kept) |
| `familyJson` growth from milestones | Compounds with legacy | Bounded at save |

### 5. Compose stability

| Component | Change |
|-----------|--------|
| `AvatarConfig` | `@Immutable` — skips redundant avatar subtree when config unchanged |
| `PersonCard` | `remember(person.id, person.relationshipLevel)` for expression + tier |
| `LifeScreen` | Already split (`CharacterHeader`, `StatsCard`, tab `AnimatedContent`) — verified |

### 6. `CelebrationOverlay` — particle budget

| Metric | Before | After |
|--------|--------|-------|
| Particle count | 28 | **18** (~36% fewer draw ops/frame) |
| Duration | 1.8 s | 1.8 s (unchanged) |

**Device re-verification (P52 — closed):** Physical **itel A665L**, debug APK 2026-07-03. GPU 50th/90th percentile **6 ms / 12 ms**; 18 particles acceptable.

### 7. `PerformanceBenchmarkTest` (JVM)

Automated guardrails:
- `ageUp_medianUnder16ms_onJvm`
- `achievementCheck_medianUnder2ms_onJvm`
- `eventEligibility_age30_under5ms_withSyntheticPool`

---

## Post-optimization APK size

| Build | Size |
|-------|------|
| Before (baseline) | 24,734,838 bytes (~23.6 MB) |
| After (`assembleDebug`) | **24,764,532 bytes (~23.6 MB)** |

Delta ~+30 KB from new indexing/cap code — negligible vs install size.

---

## Compose / animation audit summary

| Area | Finding | Action |
|------|---------|--------|
| `FamilyScreen` | `LazyColumn` + `key = { it.id }` | OK |
| `AchievementsScreen` | Lazy grid with keys | OK (verify on device) |
| `FloatingStatChange` | Short-lived overlay | OK |
| `StatBar` flash | Single stat row | OK |
| `AgeUpButton` spring | Single button | OK |

---

## Memory / leak audit

| Component | Result |
|-----------|--------|
| `AdManager` | Uses `applicationContext` for preload; `Activity` only in show parameter |
| `FeedbackManager` | `@ApplicationContext` only |
| `eventLog` | Capped at 150 on save |
| `ancestryHistory` | Capped at 25 on save + legacy handoff |
| `Person.milestones` | Capped at 25 per person on save (`RelationshipMilestoneCap`) |
| `rememberCoroutineScope` in screens | No Activity capture found |

---

## Borderline / further investigation — Resolved (Prompt 49)

| # | Item | Resolution |
|---|------|------------|
| 1 | Cold start & frame timing | **Open — device capture pending** |
| 2 | `calculateNetWorth()` redundancy | **Closed — intentional split** (achievement check vs UI display) |
| 3 | Avatar vector layer caching | **Closed — not needed without jank evidence** |
| 4 | Event JSON pool > ~300 | **Closed — defer** (current: 100 events) |
| 5 | 50+ year memory soak | **Pass (JVM proxy).** `MemorySoakTest` — event log, ancestry, per-person milestone caps |

---

## How to re-run benchmarks

```bash
./gradlew testDebugUnitTest --tests "com.maisha.game.domain.PerformanceBenchmarkTest"
./gradlew testDebugUnitTest --tests "com.maisha.game.domain.MemorySoakTest"
./gradlew assembleDebug
```

---

*Prompt 43 — Performance Profiling. Updated Prompts 49, 52, 54 (July 2026).*
