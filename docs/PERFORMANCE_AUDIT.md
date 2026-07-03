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
| Cold start | < ~2 s (splash → first interactive frame) | **Needs device** | JVM cannot measure; use Android Studio Startup Profiler on itel or constrained AVD |
| `ageUp()` hook chain | < 16 ms (one frame) | **Pass (JVM)** | `PerformanceBenchmarkTest.ageUp_medianUnder16ms_onJvm` — median after optimizations |
| Scroll jank (Family / Achievements) | Zero dropped frames | **Needs device** | `LazyColumn` + stable `key`; Compose Layout Inspector on device |
| Memory (50+ year session) | Stable, no climb | **Partial pass** | `eventLog` capped at 150; `ancestryHistory` now capped at 25 on save + legacy |
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

Device frame impact should be re-verified on itel; visual density slightly lower.

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

## Borderline / further investigation

1. **Cold start & frame timing** — Must be validated on physical itel A665L or 2 GB AVD; not available in headless CI.
2. **`FinanceEngine.calculateNetWorth()`** — Called every achievement check; cheap today but watch if asset list grows.
3. **Avatar vector layers** — `@Immutable` helps skip; if Family list still janks on device, consider `drawWithCache` or rasterized avatar cache.
4. **Full event JSON load** — 100 events is fine; if pool exceeds ~300, consider lazy file loading per category.
5. **50+ year memory soak** — Run Profiler memory timeline on device after this pass to confirm heap plateau.

---

## How to re-run benchmarks

```bash
./gradlew testDebugUnitTest --tests "com.maisha.game.domain.PerformanceBenchmarkTest"
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

---

*Prompt 43 — Performance Profiling & Budget-Device Optimization. Last updated: July 2026.*
