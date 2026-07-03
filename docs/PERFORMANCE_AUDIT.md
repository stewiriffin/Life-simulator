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
| Cold start | < ~2 s (splash → first interactive frame) | **Not measured (Prompt 49)** | No AVD/device attached during automated pass; capture via Android Studio Startup Profiler on 2 GB API 33 AVD (5 runs, median) — see § Prompt 49 device capture |
| `ageUp()` hook chain | < 16 ms (one frame) | **Pass (JVM)** | `PerformanceBenchmarkTest.ageUp_medianUnder16ms_onJvm` |
| Scroll jank (Family / Achievements) | Zero dropped frames | **Not measured (Prompt 49)** | No device attached; manual Layout Inspector pass still required |
| Memory (50+ year session) | Stable, no climb | **Pass (JVM proxy)** | `MemorySoakTest` — 55 age-ups, `eventLog` ≤ 150; legacy ancestry cap ≤ 25 |
| APK size | Tracked | **Pass** | See baseline table below |

---

## Baseline metrics (before optimizations)

Captured in CI/dev environment (Windows, debug build). Device-only metrics flagged.

| Metric | Before | Method |
|--------|--------|--------|
| Debug APK size | **24,734,838 bytes (~23.6 MB)** | `app/build/outputs/apk/debug/app-debug.apk` |
| Event pool size | **100 events** (7 JSON files) | Asset count |
| `getEligibleEvents()` | Full-list filter every call | Code audit |
| `checkAchievements()` | `calculateNetWorth()` up to **2×** per age-up | Code audit |
| `ancestryHistory` cap | **None** (unbounded across legacy) | Code audit |
| `eventLog` cap | **150 entries** (`EventLogCap`) | Already implemented (P37) |
| Confetti particles | **28** | `CelebrationOverlay` |
| `AvatarConfig` stability | No `@Immutable` | Code audit |
| `PersonCard` expression | Recomputed every recomposition | Code audit |
| AdManager / FeedbackManager | Activity leak risk | **No leak** — `Activity` passed per call; `@ApplicationContext` for preload |
| Cold start | Not measured | Requires device |
| Frame timing (scroll / age-up) | Not measured | Requires Android Studio Profiler |

### Recommended device profiling checklist

1. **Cold start**: Android Studio → Profiler → App Startup; 5 runs, median.
2. **CPU**: Record while tapping Age Up 20× rapidly; inspect `GameEngine.ageUp` flame chart.
3. **Compose**: Enable recomposition counts; scroll Family with 10+ members, Achievements grid.
4. **Memory**: Heap dump on `SlotPickerScreen` at launch vs after 50 age-ups in one session.

---

## Optimizations (before → after)

### 1. `EventRepository` — age-bucket index at load

**Change:** Build `Array<List<LifeEvent>>` indexed by age when events load; `getEligibleEvents` scans only the age bucket (~10–40 events vs 100).

**Behavior:** Preserved — `EventRepositoryAgeIndexTest` asserts identical eligibility vs flat filter for synthetic pool.

| Metric | Before (est.) | After (JVM) |
|--------|---------------|-------------|
| `getEligibleEvents(30)` median (100-event pool) | ~0.05–0.2 ms scan all | **< 5 ms** (test ceiling); typical **< 0.1 ms** on JVM |

### 2. `AchievementEngine` — single `calculateNetWorth()` per check

**Change:** Compute net worth once per `checkAchievements()` call; reuse for `six_figures` and `first_million`.

| Metric | Before | After |
|--------|--------|-------|
| Net worth calculations / age-up | Up to 2 | **1** |
| `checkAchievements()` median (JVM) | — | **< 2 ms** (`PerformanceBenchmarkTest`) |

All 31 achievement conditions remain field comparisons / small collection scans — no `eventLog` or `ancestryHistory` iteration.

### 3. `AncestryHistoryCap` — bound legacy JSON growth

**Change:** `MAX_ENTRIES = 25`; applied in `LegacyEngine.createLegacyCharacter` and `CharacterRepository.save`.

**Justification:** Long dynasty chains were the remaining unbounded persisted list (Prompt 37 flag).

### 4. Compose stability

| Component | Change |
|-----------|--------|
| `AvatarConfig` | `@Immutable` — skips redundant avatar subtree when config unchanged |
| `PersonCard` | `remember(person.id, person.relationshipLevel)` for expression + tier |
| `LifeScreen` | Already split (`CharacterHeader`, `StatsCard`, tab `AnimatedContent`) — verified, no change |

### 5. `CelebrationOverlay` — particle budget

| Metric | Before | After |
|--------|--------|-------|
| Particle count | 28 | **18** (~36% fewer draw ops/frame) |
| Duration | 1.8 s | 1.8 s (unchanged) |

**Device re-verification (P52 — closed):** Physical **itel A665L** (ITEL, Android 13 / API 33), debug APK installed 2026-07-03 via `installDebug`.

| Check | Result |
|-------|--------|
| Frame smoothness (overlay draw path) | **Pass** — `adb shell dumpsys gfxinfo com.maisha.game` during Life screen use: GPU 50th/90th percentile **6 ms / 12 ms**; CelebrationOverlay adds 18 `drawCircle` calls/frame on one `Canvas` (negligible vs full Compose tree). Age-up taps can still jank on the UI thread from `GameEngine.ageUp` — that is separate from confetti. |
| Visual density | **Acceptable** — 18 particles with drift/gravity still read as celebratory; reduction from 28 is intentional, not sparse enough to undermine feedback |

No particle-count adjustment needed; 18 remains the shipped budget.

### 6. `PerformanceBenchmarkTest` (JVM)

Automated guardrails for regressions:

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
| `FloatingStatChange` | Short-lived overlay | OK — isolate recomposition |
| `StatBar` flash | Single stat row | OK |
| `AgeUpButton` spring | Single button | OK |

---

## Memory / leak audit

| Component | Result |
|-----------|--------|
| `AdManager` | Uses `applicationContext` for preload; `Activity` only in `showInterstitial` parameter — no retained Activity |
| `FeedbackManager` | `@ApplicationContext` only |
| `eventLog` | Capped at 150 on save |
| `ancestryHistory` | Capped at 25 on save + legacy handoff |
| `rememberCoroutineScope` in screens | Standard ViewModel-scoped work; no Activity capture found |

---

## Borderline / further investigation — **Resolved (Prompt 49)**

| # | Item (Prompt 43) | Resolution |
|---|------------------|------------|
| 1 | Cold start & frame timing | **Open — device capture pending.** No emulator/device connected in Prompt 49 CI pass. Methodology unchanged: Startup Profiler, 2 GB API 33 AVD, 5-run median. |
| 2 | `calculateNetWorth()` redundancy | **Closed — intentional split.** `AchievementEngine.checkAchievements()` computes net worth **once** per call. `LifeViewModel` computes net worth **once** after state mutations for UI display — not duplicate work inside the achievement check path. |
| 3 | Avatar vector layer caching | **Closed — not needed without jank evidence.** `@Immutable` on `AvatarConfig` + `remember` on `PersonCard` retained. No `drawWithCache` added; scroll jank not measured on device in this pass. Re-open only if device profiling shows Family list jank traced to avatars. |
| 4 | Event JSON pool > ~300 | **Closed — defer.** Current pool: **100 events** (7 files, unchanged). Lazy per-category loading **not implemented**; revisit if pool exceeds ~300. |
| 5 | 50+ year memory soak | **Pass (JVM proxy).** `MemorySoakTest.fiftyAgeUps_eventLogStaysWithinCap` + `legacyChain_ancestryHistoryStaysWithinCap`. Device heap timeline still recommended before ship. |

### Prompt 49 — device capture checklist (manual)

When an AVD (API 33, 2 GB RAM, 4 cores) or itel A665L is available:

1. **Cold start:** Profiler → App Startup, 5 cold launches, record median ms to first interactive frame.
2. **Family scroll:** Seed 10+ family members, fling `FamilyScreen`, note dropped frames.
3. **Achievements scroll:** Unlock ~15 achievements, scroll grid, note dropped frames.
4. Paste results into the table in § Part 3 above.

---

## How to re-run benchmarks

```bash
./gradlew testDebugUnitTest --tests "com.maisha.game.domain.PerformanceBenchmarkTest"
./gradlew testDebugUnitTest --tests "com.maisha.game.domain.MemorySoakTest"
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

---

*Prompt 43 — Performance Profiling & Budget-Device Optimization. Updated Prompt 49 (July 2026).*
