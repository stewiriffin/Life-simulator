# Maisha — Architecture

This document explains how the major systems connect. It reflects **current implementation** (post Prompts 1–39), including balance and edge-case fixes from later audit passes.

---

## 1. Age-up data flow (core loop)

When the player taps **Age Up** on the Life tab, the following chain runs. This is the most-touched path in the app.

### UI layer

1. **`LifeScreen`** — `AgeUpButton` calls `onAgeUp()` (wired from `MaishaNavHost` to `LifeViewModel.onAgeUp()`).
2. Button is disabled while `isAgingUp`, while an event dialog is open, or if character is dead.

### ViewModel layer (`LifeViewModel`)

3. Guards: character alive, not already aging, no blocking event.
4. Enqueues age-up **feedback cue** (sound/haptic) into `pendingFeedbackCues` (consumed by `FeedbackEffect` in Compose — not played in the ViewModel).
5. **`viewModelScope.launch`**:
   - Sets `isAgingUp = true`.
   - Loads achievement progress snapshot from **`AchievementRepository`** (global, not per-slot).
   - Calls **`gameEngine.ageUp(character, triggeredEventIds, progress, slotId)`**.
   - On newly unlocked achievements: persists via `achievementRepository.unlockAchievements`, queues dialogs/celebration.
   - **`applyAgeUpResult()`** — persists character + updates `LifeUiState` (character, jobs, net worth, events, death → navigate to summary).
   - Stat deltas, celebrations, expression flash, ad frequency (`AdFrequencyController`), optional notification permission prompt.
   - Sets `isAgingUp = false`.

`triggeredEventIds` is held in the ViewModel (not inside `Character`) and saved with each `CharacterRepository.saveGame()`.

**Process-death safety (verified P52):** On age-up, one-time event ids from the offered event(s) are added to `triggeredEventIds` **in the same `persist()` call** that saves the advanced character (`LifeViewModel.applyAgeUpResult`, `persistAge = true`). This closes the window where age was saved but a one-time milestone could re-eligible on a later age-up after OS kill before the player tapped a choice. Intro-at-birth (`persistAge = false`) still marks ids on choice resolution only — character is not advanced until then. Duplicate stat effects from re-fired one-time JSON events were the gameplay risk; cosmetic-only re-shows without stat re-apply were already unlikely because `currentEvent` is not restored from disk.

### Domain layer (`GameEngine.ageUp`)

Order is fixed and has been stable since the mortality-last rule (Prompt 8):

| Step | What runs | Skipped if incarcerated at year start? |
|------|-----------|--------------------------------------|
| 1 | `age + 1` | No |
| 2 | `educationEngine.enrollIfEligible` | **Yes** |
| 3 | `processEducationProgression` (grade advance / university year) | **Yes** |
| 4 | `processCareerProgression` → `careerEngine.workYear` | **Yes** |
| 5 | `processFinanceProgression` (upkeep + degrade assets) | No |
| 6 | `relationshipEngine.tickFamilyYear` (age family, decay, notices) | No |
| 7 | `processHealthProgression` (illness roll, untreated drain) | No |
| 8 | `generateFriendshipOpportunity` (maybe add FRIEND) | **Yes** |
| 9 | `crimeEngine.serveYear` if incarcerated | Only if incarcerated |
| 10 | Event resolution: **career system event** OR **exam event** OR **random events** (relocation offer + weighted pick) | Incarcerated: random events only, no career/exam |
| 11 | **`finalizeYear`** → **`mortalityEngine.checkDeath`** LAST | No |
| 12 | If alive: `achievementEngine.checkAchievements`; if dead: **no achievements** (Prompt 39) | — |
| 13 | `scheduleNotificationNudges` (WorkManager — side effect) | Skipped if dead |

**Death ordering:** Promotion/firing/exam/random events resolve *before* mortality. A character can be promoted and die in the same year; they cannot unlock achievements after death in that same tick.

### Persistence layer

14. **`CharacterRepository.saveGame(slotId, character, triggeredEventIds)`** — maps `Character` → `CharacterEntity`, JSON-serializes nested lists, applies **`EventLogCap.trim`** on save (max 150 log lines), **`AncestryHistoryCap.trim`** on ancestry, and **`RelationshipMilestoneCap.trimFamily`** on family milestones (max 25 per person).
15. Room DAO write; no `StateFlow` from repository on Life screen — ViewModel holds authoritative UI state updated synchronously after save.

### UI recomposition

16. `LifeViewModel.uiState` (`StateFlow`) emits new `LifeUiState`.
17. **`LifeScreen`** recomposes: `LifeTabContent` (stats, log), or event dialog overlay, or celebration/achievement dialogs.
18. **`FeedbackEffect`** plays pending cues; **`FloatingStatChangeLayer`** animates stat deltas.

### Event choices (same session, separate action)

`onChoiceSelected` → `gameEngine.applyChoice` → stat/GPA/career/relationship/crime/health/relocation hooks → `persist` → mid-life achievement check → UI update. Does not age the character.

---

## 2. Multi-slot saves, legacy, and ancestry

### Three independent slots

- **`MAX_SLOTS = 3`** — each slot is a row in `character_save` keyed by `slotId` (0..2).
- **`CharacterRepository`** exposes `getAllSlots()`, `loadGame(slotId)`, `saveGame`, `clearSlot`.
- **Achievements** and **settings** are **global** (separate tables / DataStore), not per-slot.

### One active character per slot

Each slot stores one `Character` at a time: name, age, stats, `familyJson`, `educationJson`, `careerJson`, `assetsJson`, `criminalRecordJson`, `healthConditionsJson`, `ancestryHistoryJson`, `generationNumber`, `triggeredEventIdsJson`, etc.

### Legacy continuation (same slot, new generation)

When a character **dies**:

1. **`LifeSummaryScreen`** shows recap; **`LegacyEngine.eligibleHeirs()`** returns living children age ≥ minimum.
2. If heirs exist, **Continue Legacy** → heir picker → confirmation.
3. **`LegacyEngine.createLegacyCharacter(deceased, heir)`**:
   - Heir becomes the new `Character` (same slot).
   - `generationNumber = deceased.generationNumber + 1`.
   - **`ancestryHistory`** += `buildAncestryEntry(deceased)` (name, birth country, relocations, age/cause at death).
   - Money split evenly among living children; heir gets their share added.
   - Surviving spouse remapped to MOTHER/FATHER; other children → SIBLING; friends may carry over.
   - Fresh `career`, `assets`, `criminalRecord`, `eventLog` (one legacy intro line); education from heir's age.
4. **`GameEngine.applyLegacyFamilyMilestones`** tags family with legacy milestone.
5. Save with **empty `triggeredEventIds`** (new life event pool).
6. Navigate back to **Life** screen for continued play.

### Ancestry display (read-only history)

- **`AncestryScreen`** (from Character Stats): shows `ancestryHistory` oldest→top, current character at bottom.
- Distinct from **playing** as heir — history is append-only across generations.

### Conceptual diagram

```
Slot 0
 └── Character (generation 3, playing)
      ├── ancestryHistory: [Gen1 entry, Gen2 entry]
      ├── generationNumber: 3
      └── family: [surviving parent?, siblings, friends, …]

On death + legacy pick child "A":
 └── New Character (generation 4)
      ├── ancestryHistory: […, Gen3 entry just died]
      ├── generationNumber: 4
      └── name/age/stats from heir A
```

---

## 3. Country / worldwide content cascade

`countryCode` on `Character` is the **current residence**. `birthCountryCode` is where they were born (for relocation/heritage). Content systems read these codes consistently.

### Example: Nigerian character sees a templated exam event

1. **Event JSON** (`education_events.json` or `general_events.json`) contains text like:  
   `"{secondaryExam} season looms…"`  
   No `restrictedToCountry` → eligible for all countries (subject to age/tags).

2. **Age up** → `EventRepository.getEligibleEvents(age, usedIds, character)` filters by age, one-time tags, relationship/crime/finance gates, and **`restrictedToCountry`** if set.

3. **`FlavorInterpolator.resolveEvent(event, "NG")`** replaces placeholders using **`CountryCatalog.flavorFor("NG")`**:
   - `{secondaryExam}` → e.g. **WAEC** (from `CountryFlavor.secondaryExamName`)
   - `{transportMode}` → country-specific transport string
   - `{moneyApp}` → e.g. **OPay** (or fallback `"mobile banking"`)

4. **Education system events** (`exam_system` tag) bypass random pool — `EducationEngine.buildExamResultEvent` uses **`ExamNames.primaryExamName` / `ExamNames.secondaryExamName(countryCode)`** for display text. Internal `ExamType.KCPE` / `ExamType.KCSE` enum values are legacy names only; trigger logic is country-agnostic (see §4 `EducationEngine`).

5. **Jobs** — `JobPool.getJobsForCountry("NG")` returns NG overrides + universal jobs; salaries scaled by **`EconomyScaler`**.

6. **Assets** — `AssetCatalog` country-specific entries (e.g. self-contain) + universal; prices scaled.

7. **Names** — `NamePool.randomFullName(gender, "NG")` for prospects, children, friends.

8. **Holidays** — events tagged `holiday` resolve `{holidayName}` / `{holidayDescription}` from `CountryFlavor.notableHolidays`; gated by `lastHolidayAge` cooldown.

9. **Kenya-only events (verified P53)** — **9** events carry `restrictedToCountry: "KE"`; **all 9** have a universal counterpart at the same age range/theme (4 use `_world` ids; 5 use parallel universal ids). Non-Kenyan players are not missing a life-stage bucket. Inventory:

| KE-only id | Universal counterpart | Age range |
|------------|----------------------|-----------|
| `birth` | `birth_world` | 0 |
| `matatu_ride` | `school_bus_ride` | 6–8 |
| `losing_tooth` | `losing_tooth_world` | 6–8 |
| `church_sunday` | `church_sunday_world` | 5–16 |
| `helping_at_home` | `helping_at_home_world` | 9–16 |
| `choose_secondary_school` | `choose_secondary_school_world` | 14–15 |
| `side_hustle_matatu` | `side_hustle_delivery` | 20–40 |
| `motorbike_repair` | `scooter_repair` | 18–45 |
| `teen_matatu_wash` | `teen_car_wash` | 14–18 |

KE-restricted events remain for Kenya-flavor authenticity; they are never offered outside KE.

---

## 4. Domain engine API reference

One-line responsibility + key public entry points. See KDoc on each class for parameter/return detail.

### `GameEngine`
**Orchestrates the yearly tick and cross-engine player actions.**

| Function | Purpose |
|----------|---------|
| `ageUp` | Full year simulation; returns character, events, achievements, decay notices |
| `introEventsForNewborn` | Age-0 starter event |
| `applyChoice` | Apply event choice effects to character |
| `applyForJob` / `quitJob` | Career actions (blocked if incarcerated) |
| `getEligibleJobs` | Delegate to CareerEngine |
| `purchaseAsset` / `sellAsset` / `calculateNetWorth` | Finance delegates |
| `interactWithFamilyMember` | Relationship interaction |
| `findDatingProspects` / `startDating` / `proposeMarriage` / `breakUpOrDivorce` / `haveChild` | Relationship lifecycle |
| `applyLegacyFamilyMilestones` | Post-legacy family tagging |
| `attemptCrime` / `visitDoctor` | Crime & health |

### `EducationEngine`
**School enrollment, grades, exams, university.**

`enrollIfEligible`, `advanceGrade`, `applyStudyEffort`, `advanceUniversityYear`, `takeExam`, `applyToUniversity`, `isEligibleForUniversity`, `shouldTriggerPrimaryExam` / `shouldTriggerSecondaryExam`, `buildExamResultEvent`, `applyGpaEffect`

**Exam triggers (verified P53):** `shouldTriggerPrimaryExam` / `shouldTriggerSecondaryExam` (renamed from `shouldTriggerKcpe` / `shouldTriggerKcse`) check **school stage, final grade, and universal age thresholds** (13 / 17) only — **no `countryCode` gate**. Display localization is in `buildExamResultEvent` via `ExamNames`. `EducationState.kcpePassed` / `kcseGrade` field names are persistence legacy; behavior applies to all countries. `EducationEngineTest` confirms triggers for KE, NG, US/GB at exit ages.

### `CareerEngine`
**Jobs, work years, promotions, firing.**

`isJobEligible`, `getEligibleJobs`, `applyForJob`, `workYear`, `applyWorkEffort`, `evaluatePromotion`, `evaluateFiring`, `quitJob`, `applyPerformanceEffect`, `buildPromotionEvent` / `buildFiringEvent` / `buildDownsizingEvent`, `shouldTriggerDownsizing`, `applyDownsizing`, `calculateAnnualSalary`

### `FinanceEngine`
**Assets, upkeep, net worth.**

`purchaseAsset`, `sellAsset`, `applyUpkeep`, `degradeAssets`, `calculateNetWorth`, `applyConditionToAssetType`, `applyConditionToFirstAsset`, `meetsFinanceEventThreshold`, `recalculateValue`

### `RelationshipEngine`
**Family, dating, interactions, children, decay.**

`tickFamilyYear`, `findDatingProspects`, `generateFriendshipOpportunity`, `progressRelationship`, `proposeMarriage`, `startDating`, `breakUpOrDivorce`, `haveChild`, `applySpouseRelationshipEffect`, `applyLegacyFamilyMilestones`, `getRelationshipTier`, `canTravelTogether`

### `CrimeEngine`
**Crime attempts, arrest, prison years.**

`attemptCrime`, `processArrest`, `serveYear`

### `HealthEngine`
**Illness and treatment.**

`rollForIllness`, `addCondition`, `applyUntreatedConditions`, `visitDoctor`, `visitFirstUntreatedCondition`

### `MortalityEngine`
**Death rolls and death log markers.**

`checkDeath`, `applyDeath`, `parseDeathCause`, `deathFlavorText`, `gentleCauseLabel`

### `AchievementEngine`
**Achievement condition checks.**

`checkAchievements` — returns newly qualifying achievements not already in progress list

### `LegacyEngine`
**Heir selection and legacy character creation.**

`eligibleHeirs`, `createLegacyCharacter`, `buildAncestryEntry`, `mapSurvivingParent`

### `RelocationEngine`
**Country relocation offers and moves.**

`hasRelocated`, `shouldOfferRelocation`, `getRelocationOpportunities`, `buildRelocationOpportunityEvent`, `relocate`

**Multi-relocation (verified P52):** `hasRelocated(character): Boolean` is a yes/no helper (`relocationCount > 0` or birth ≠ current country) — not a count API. Full history lives on `Character.relocationCount` (int), `Character.relocationHistory` (list of country codes), and `Character.lastRelocationAge`. `shouldOfferRelocation` uses **per-index** one-time event ids (`relocation_opportunity_system_0`, `_1`, …) and enforces `MIN_YEARS_BETWEEN_RELOCATIONS` (4) after the first move — a second relocation is supported. `AchievementEngine.checkWorldTraveler` requires `relocationCount >= 2` (achievement id `world_traveler`) — achievable with the shipped engine.

### `EventLogCap` (utility)
**Bounds event log size on disk.**

`prepend`, `append`, `trim` — preserves lines starting with `::DEATH:` (prefix match at line start only; see `SAVE_DATA_INTEGRITY.md` P54).

### `RelationshipMilestoneCap` (utility)
**Bounds per-person milestone list inside `familyJson`.**

`trim`, `trimFamily` — keeps newest 25 entries per `Person.milestones` (append-oldest order).

### `FamilyGenerator`
**Initial family at character creation.**

`generateFamily`

---

## 5. Cross-cutting notes

- **One-shot UI events** (ads, achievements, celebrations, navigation): boolean flags + `*Handled()` callbacks in `UiState`, not `SharedFlow`.
- **`LifeViewModel`** is intentionally monolithic — see **ADR-001** (§6). Do not re-audit sizing unless revisit triggers are met.
- **Notifications:** `NotificationScheduler` enqueues `DailyReminderWorker` (24h + 6h flex, `KEEP` policy) and one-time `ContextualNudgeWorker` jobs. No battery-optimization exemption — see [KNOWN_PLATFORM_LIMITATIONS.md](KNOWN_PLATFORM_LIMITATIONS.md).
- **Tests:** `EventRepository.forTesting()` and `NotificationScheduler.forTesting()` exist for JVM unit tests only. All domain tests should use these via `TestFixtures.gameEngine()` or direct `forTesting()` — never the production Android-dependent constructors. See [TEST_COVERAGE.md](TEST_COVERAGE.md) for the full engine × test map and fixture convention.
- **Test fixtures:** `app/src/test/java/com/maisha/game/domain/TestFixtures.kt` — shared `character()` / `person()` / `child()` / `gameEngine()` builders.

---

## 6. Architecture Decision Records

### ADR-001: Keep `LifeViewModel` monolithic (2026-07-03)

| | |
|---|---|
| **Question** | Should `LifeViewModel` be split into smaller per-concern ViewModels (career, family, assets, etc.)? |
| **Decision** | **No** — keep a single `LifeViewModel` per life screen. |
| **Context** | Earlier audit passes (Prompts 26–37) noted accumulated pass-through methods (`onAgeUp`, `onChoiceSelected`, family interactions, crime, doctor visits, ad/achievement handlers). `LifeViewModel` currently exposes ~34 public methods. |
| **Reasoning** | Shared slot state, ad-deferral during achievement celebrations, and achievement-queue sequencing all need visibility into the same `ageUp()` result within one ViewModel to emit one-shot UI events (`showInterstitial`, achievement dialog, celebration overlay, navigation) in the correct order. Splitting would reintroduce cross–ViewModel coordination for every age-up tick without a clear benefit at the current method count. |
| **Revisit when** | (1) `LifeViewModel` exceeds **~40 public methods**, or (2) a genuinely independent concern unrelated to age-up / event / achievement / ad sequencing needs its own lifecycle (e.g. a standalone screen with no slot coupling). |
| **Status** | **Closed.** Future audits must **not** re-flag ViewModel sizing unless a revisit trigger above is met. This ADR is the final word on that question. |

Prior note (§5): *"`LifeViewModel` is intentionally monolithic — shared slot state, ad deferral, and achievement queues."* — superseded in detail by ADR-001 above; the decision is unchanged.
