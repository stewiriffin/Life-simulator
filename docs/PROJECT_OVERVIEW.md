# Maisha Life Simulator — Project Overview for External AI

**Document purpose:** Give any external AI (or human contributor) a complete, accurate picture of what this Android project is, what has been built, how it is structured, what remains unfinished, and how to work on it safely without breaking established patterns.

**Last updated:** July 3, 2026  
**Latest known prompt:** 54 (per-person relationship milestone cap audit)  
**Repository:** `https://github.com/stewiriffin/Life-simulator.git` (branch `main`)  
**Package / applicationId:** `com.maisha.game`  
**Version:** `versionCode 1`, `versionName "1.0"` (pre-release)

---

## Table of contents

1. [Executive summary](#1-executive-summary)
2. [How this project was built](#2-how-this-project-was-built)
3. [Release readiness snapshot](#3-release-readiness-snapshot)
4. [Tech stack (detailed)](#4-tech-stack-detailed)
5. [Architecture and design rules](#5-architecture-and-design-rules)
6. [Repository layout](#6-repository-layout)
7. [Navigation, screens, and user flows](#7-navigation-screens-and-user-flows)
8. [UI layer — theme, language, components](#8-ui-layer--theme-language-components)
9. [Domain layer — game engines](#9-domain-layer--game-engines)
10. [Data layer — models, persistence, content](#10-data-layer--models-persistence-content)
11. [Cross-cutting systems](#11-cross-cutting-systems)
12. [Feature inventory (shipped)](#12-feature-inventory-shipped)
13. [Event content system](#13-event-content-system)
14. [Worldwide / country system](#14-worldwide--country-system)
15. [Achievements](#15-achievements)
16. [Monetization (AdMob)](#16-monetization-admob)
17. [Notifications and retention](#17-notifications-and-retention)
18. [Localization](#18-localization)
19. [Testing](#19-testing)
20. [Documentation corpus](#20-documentation-corpus)
21. [Development prompt history (1–54)](#21-development-prompt-history-154)
22. [Recent git history](#22-recent-git-history)
23. [Known gaps and pre-ship checklist](#23-known-gaps-and-pre-ship-checklist)
24. [Compliance and store-prep status](#24-compliance-and-store-prep-status)
25. [Constraints for future AI work](#25-constraints-for-future-ai-work)
26. [Quick reference — where to change things](#26-quick-reference--where-to-change-things)
27. [Known documentation drift](#27-known-documentation-drift)

---

## 1. Executive summary

**Maisha** (Swahili for *life*) is a **single-player, offline-first life simulation game** for Android. Players:

1. Create a character (name, gender, country, avatar).
2. Age year-by-year via **Age Up**, facing random events and making choices.
3. Progress through school, career, relationships, assets, crime, and health systems.
4. Die naturally and view a **life summary**, optionally **continue as an heir** (Legacy Mode).
5. Play up to **three parallel save slots** with **global achievements** across slots.

The app targets **mid-range phones** (~360dp width, **minSdk 26**, **targetSdk 35**), built with **Kotlin 2.0**, **Jetpack Compose**, **MVVM + Hilt**, **Room**, and **DataStore**.

**Genre positioning:** BitLife-style life sim, differentiated by:
- **15-country worldwide roster** with per-country exams, jobs, assets, holidays, and flavor text.
- **Legacy Mode** — continue as adult children across generations with an **ancestry timeline**.
- **Relationship depth** — memories, tiers, 10 interaction types, mixed-heritage children.
- **No login/account system** — all gameplay data stays on-device.

**Not integrated (explicitly):** Firebase Analytics, Crashlytics, cloud sync, multiplayer, IAP (only AdMob ads).

---

## 2. How this project was built

Development followed a **sequential prompt series** (54 prompts as of July 2026), each adding or hardening a slice of the app. Early prompts (1–12) built core gameplay; mid prompts (13–35) added slots, settings, worldwide expansion, legacy, achievements, polish; late prompts (36–54) focused on audits, tests, performance, copy, integrity, compliance, store prep, and bounded-growth hardening.

**Working style across prompts:**
- Extend existing code; avoid parallel systems.
- Domain logic stays **pure Kotlin** (no Android APIs in engines).
- UI reads **StateFlow** from ViewModels only.
- Many prompts explicitly deferred Play Store signing, production ad IDs, and final art.

**Primary developer context:** Private project; AI-assisted implementation via Cursor; device reference is **itel A665L** (360dp, Android 13).

---

## 3. Release readiness snapshot

| Area | Status | Notes |
|------|--------|-------|
| Core gameplay loop | **Complete** | Age up, events, all major life systems |
| Multi-slot saves | **Complete** | 3 slots + corruption recovery UX |
| Legacy / ancestry | **Complete** | Heir continuation + timeline screen |
| Worldwide content | **Complete** | 15 countries with researched flavor |
| Bounded persisted lists | **Complete** | `eventLog` (150), `ancestryHistory` (25), `Person.milestones` (25/person) |
| Unit tests | **Strong** | 38 test classes, domain + data coverage |
| Manual QA plan | **Documented** | 73-step device checklist |
| Performance pass | **Partial** | JVM benchmarks pass; device cold start flagged |
| UI copy pass | **Done** | Prompt 44 voice standard |
| Privacy policy | **Drafted** | Needs public HTTPS hosting |
| Play Store listing | **Drafted** | Needs screenshots + paste into Console |
| Production AdMob IDs | **Not done** | Test units in `AdUnitConfig.kt` |
| Release signing / ProGuard | **Not done** | `minifyEnabled = false` |
| Final art (avatar, icon, sounds) | **Placeholders** | Canvas avatars, silent `.wav` files |
| Firebase Analytics | **Skipped** | Prompt 19 deprioritized permanently so far |

**Bottom line:** Feature-rich and testable **beta-quality** app; **not store-submission-ready** without manual steps (hosting policy, ad IDs, art, signing, Console forms, screenshots).

---

## 4. Tech stack (detailed)

| Technology | Version / notes | Role |
|------------|-----------------|------|
| Kotlin | 2.0.21 | Primary language |
| Android Gradle Plugin | 8.7.3 | Build |
| JDK | 17 | `jvmTarget = "17"` |
| compileSdk / targetSdk | 35 | `app/build.gradle.kts` |
| minSdk | 26 | Android 8.0+ |
| Jetpack Compose | BOM 2024.12.01 | All UI |
| Material 3 | — | Theme, components |
| Navigation Compose | 2.8.5 | `MaishaNavHost` |
| Hilt | 2.52 (KSP) | DI for ViewModels, engines, repos |
| Room | 2.6.1, DB version **13** | `character_save`, `achievement` tables |
| DataStore Preferences | 1.1.1 | Settings, onboarding flags |
| Kotlinx Serialization | 1.7.3 | Events, Room blob fields |
| WorkManager + Hilt Worker | 2.10.0 | Notifications |
| Google Play Services Ads | 23.6.0 | **No Firebase BOM** |
| JUnit 4 | 4.13.2 | Unit tests on JVM |
| SplashScreen API | 1.0.1 | Cold start splash |

**Dependencies explicitly absent:** Firebase, Retrofit/OkHttp (no network gameplay API), Coil/Glide (avatars are Canvas-drawn), billing library.

---

## 5. Architecture and design rules

### Layer diagram

```
Compose Screens
    ↓ collect StateFlow / callbacks
ViewModels (@HiltViewModel)
    ↓ suspend / sync calls
Domain Engines (pure Kotlin, @Singleton @Inject)
    ↓
Repositories (Room, DataStore, AssetManager for events)
```

### Non-negotiable patterns

1. **Engines are pure:** `GameEngine`, `EducationEngine`, etc. take immutable `Character` (and related models), return updated copies. No `Context`, no Room, no Compose.
2. **Single orchestrator:** `GameEngine.ageUp()` runs the yearly tick in a **fixed order** (education → career → finance → family tick → health → friendship → crime serve → events → mortality **last** → achievements if alive).
3. **Persistence boundary:** `CharacterRepository` maps `Character` ↔ `CharacterEntity` with JSON columns. Three cap utilities applied on save: `EventLogCap` (150), `AncestryHistoryCap` (25), `RelationshipMilestoneCap` (25 per person).
4. **Global vs per-slot:** Character saves are **per slot** (0–2). Achievements and settings are **app-wide**.
5. **One-shot UI events:** Navigation, ads, achievement dialogs use boolean flags in `UiState` + `*Handled()` methods — **not** `SharedFlow` for navigation.
6. **Feedback decoupling:** Sound/haptics queued as cues, played by `FeedbackEffect` in Compose — not in ViewModels.
7. **Testability:** `EventRepository.forTesting()`, `NotificationScheduler.forTesting()` exist for JVM tests.

### Age-up order (critical)

Documented in `docs/ARCHITECTURE.md`. Summary:

| Step | System | Skipped when incarcerated? |
|------|--------|---------------------------|
| Age increment | — | No |
| Education enroll/progress | EducationEngine | Yes |
| Career work year | CareerEngine | Yes |
| Asset upkeep/degrade | FinanceEngine | No |
| Family year tick | RelationshipEngine | No |
| Health progression | HealthEngine | No |
| Friendship opportunity | RelationshipEngine | Yes |
| Crime serve year | CrimeEngine | If incarcerated only |
| Events (career/exam/random/relocation) | EventRepository + engines | Limited when incarcerated |
| Death check | MortalityEngine | No (always last) |
| Achievements | AchievementEngine | Only if still alive |
| Notification nudges | NotificationScheduler | Skipped if dead |

**Death-year rule (Prompt 39):** Achievements cannot unlock on the same tick as death.

**Process-death safety (Prompt 52):** One-time event IDs from offered events are added to `triggeredEventIds` in the same `persist()` call as the age advance.

### ADR-001: Monolithic LifeViewModel

`LifeViewModel` (~900 lines, ~34 public methods) stays single-file per ADR-001 in `ARCHITECTURE.md`. Shared slot state, ad deferral during celebrations, and achievement-queue sequencing require one age-up result visibility. Revisit only if >~40 public methods or a genuinely independent screen emerges.

---

## 6. Repository layout

```
Life Simulator/
├── app/
│   ├── build.gradle.kts          # SDK 35, BuildConfig.PRIVACY_POLICY_URL
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── assets/data/events/   # 7 JSON event files (~100 events)
│       │   ├── java/com/maisha/game/
│       │   │   ├── MaishaApplication.kt
│       │   │   ├── MainActivity.kt
│       │   │   ├── ads/              # AdManager, AdUnitConfig, frequency (4 files)
│       │   │   ├── data/             # Catalogs, repositories (46 files)
│       │   │   │   ├── events/EventRepository.kt
│       │   │   │   ├── local/        # Room, DAOs, mappers, DataStore
│       │   │   │   └── model/        # Character, Person, LifeEvent, etc. (20 files)
│       │   │   ├── di/DatabaseModule.kt
│       │   │   ├── domain/           # Game engines + caps (21 files)
│       │   │   ├── feedback/         # FeedbackManager
│       │   │   ├── notifications/    # Workers, scheduler
│       │   │   ├── ui/               # Compose screens + ViewModels (66 files)
│       │   │   │   ├── achievements/
│       │   │   │   ├── avatar/
│       │   │   │   ├── celebration/
│       │   │   │   ├── charactercreation/
│       │   │   │   ├── components/   # 20+ shared components
│       │   │   │   ├── legacy/
│       │   │   │   ├── main/         # Life tabs + LifeViewModel
│       │   │   │   ├── navigation/
│       │   │   │   ├── onboarding/
│       │   │   │   ├── settings/
│       │   │   │   ├── slots/
│       │   │   │   ├── summary/
│       │   │   │   ├── splash/
│       │   │   │   └── theme/
│       │   │   └── util/             # Serialization, locale, share (8 files)
│       │   └── res/
│       │       ├── values/           # English strings (100 keys)
│       │       ├── values-sw/        # Kiswahili (100 keys)
│       │       ├── values-fr/, -es/, -pt/, -hi/  # Partial UI chrome (66 keys)
│       │       └── raw/              # Placeholder sound .wav files
│       └── test/                     # 38 JVM test classes
├── docs/                             # This documentation set
├── gradle/libs.versions.toml
└── README.md
```

**Scale:** ~155 Kotlin source files in `main`, 38 test classes in `test`.

---

## 7. Navigation, screens, and user flows

### Routes (`ui/navigation/MaishaNavHost.kt`)

| Route | Screen | ViewModel |
|-------|--------|-----------|
| `onboarding` | OnboardingScreen (5 slides) | OnboardingViewModel |
| `slot_picker` | SlotPickerScreen (+ banner ad) | SlotPickerViewModel |
| `character_creation/{slotId}` | CharacterCreationScreen | CharacterCreationViewModel |
| `avatar_picker/{slotId}` | AvatarPickerScreen | CharacterCreationViewModel (shared) |
| `life/{slotId}` | LifeScreen (main game) | LifeViewModel |
| `life_summary/{slotId}` | LifeSummaryScreen | LifeSummaryViewModel |
| `character_stats/{slotId}` | CharacterStatsScreen | LifeViewModel |
| `ancestry/{slotId}` | AncestryScreen | LifeViewModel |
| `achievements` | AchievementsScreen | AchievementsViewModel |
| `settings` | SettingsScreen | SettingsViewModel |

**Start destination:** `onboarding` if not completed, else `slot_picker`.

### First-launch flow

```
Splash → Onboarding (if not completed) → Character Creation (slot 0) → Avatar Picker → LifeScreen
```

### Returning user flow

```
Splash → Slot Picker → [existing slot → Life] or [empty slot → Character Creation]
```

### LifeScreen structure

**Bottom navigation tabs** (`MainTab` enum):
1. **LIFE** — stats, avatar, event log, Age Up button
2. **FAMILY** — family list, dating, PersonDetailSheet (memories, interactions)
3. **CAREER** — education status, job, apply/quit
4. **ASSETS** — owned assets, shop, net worth
5. **ACTIONS** — crime attempts, doctor visits

**Overlays / modals:** Event choice dialog, celebration overlay, achievement unlock dialog, floating stat deltas, contextual tips.

**Top bar actions:** Navigate to Character Stats, Achievements, Settings.

### Death flow

```
Age Up → death detected → LifeSummaryScreen
    → Start New Life (slot picker / creation)
    → Continue Legacy (heir picker → same slot, new generation)
    → Share My Life (system share sheet with generated image)
    → Optional: Second Wind rewarded ad (stat bonus on next new life)
```

---

## 8. UI layer — theme, language, components

### Design system (`ui/theme/`)

**Dark-mode-first** Material 3 theme (`MaishaTheme`).

| Token | Values |
|-------|--------|
| Primary | Teal `#1A8A8A` |
| Accent | Gold `#F4B942` |
| Background | Navy deep `#0B1628` |
| Surface | Navy `#152238` |
| Error / negative | Coral `#E85D5D` |
| Text | `#F0F4FA` primary, `#9AADCC` secondary |

**Spacing:** `MaishaSpacing` — xs 4dp, sm 8dp, md 12dp, lg 16dp, xl 24dp  
**Radii:** `MaishaRadius` — card 14dp, button 12dp, sheet 20dp  
**Stat colors:** Dedicated colors per stat (health green, happiness amber, smarts blue, looks purple, money gold)

**Target width:** 360dp (itel A665L reference device).

### Voice / copy standard (Prompt 44)

- Warm, encouraging, lightly playful
- Title Case for CTAs and screen titles; sentence case for body
- Canonical term: **Age Up** (not "Next Year")
- Death/serious topics: gentle, not grim
- Full audit: `docs/UI_COPY_AUDIT.md`

### Key shared components (`ui/components/`)

| Component | Purpose |
|-----------|---------|
| `StatBar` | Animated stat display with flash on change |
| `PersonCard` | Family list row with avatar, tier, expression |
| `PersonDetailSheet` | Bottom sheet: interactions, memories timeline |
| `MilestoneTimeline` | Newest-first milestone display (shows 8 + hint) |
| `CelebrationOverlay` | Confetti (18 particles) for milestones |
| `FloatingStatChangeLayer` | Animated +/- stat deltas |
| `ConfirmableActionHost` | Confirm-then-execute pattern |
| `AchievementUnlockedDialog` | Achievement reveal |
| `AdaptiveBannerAd` | Slot picker banner |
| `EmptyStateCard` | Illustrated empty states |
| `DismissibleTipCard` | Contextual tips |
| `AppLoadingIndicator` | Loading states |

### Avatars (`ui/avatar/`)

- `AvatarRenderer` — procedural Canvas drawing
- 8 skin tones, 8 hair styles, 6 hair colors, 8 outfit colors, 5 facial features
- `AgeStage`: BABY (0–2), CHILD (3–12), TEEN (13–17), ADULT (18–49), SENIOR (50+)
- `ExpressionResolver` — HAPPY, SAD, ANGRY, NEUTRAL flash on events

### Illustrations (`ui/illustrations/`)

`MaishaIllustrations` — vector placeholders for empty states (family, career, assets, etc.).

---

## 9. Domain layer — game engines

All in `app/src/main/java/com/maisha/game/domain/`. Injected via Hilt.

### `GameEngine` (~central orchestrator)
- `ageUp()` — full yearly simulation
- `applyChoice()` — event choice effects
- `introEventsForNewborn()` — age-0 starter events
- Delegates: job apply/quit, asset buy/sell, family interactions, dating/marriage/children, crime, doctor
- `applyLegacyFamilyMilestones()` after legacy handoff

### `EducationEngine`
- Pipeline: primary → secondary → university
- Country-specific exam **display** via `ExamNames` / `CountryCatalog`
- `shouldTriggerPrimaryExam` / `shouldTriggerSecondaryExam` — country-agnostic (age 13 / 17)
- GPA, study effort from event choice labels (`EffortResolver`)
- **Gap:** `EducationState.expelled` exists but is never set by gameplay events

### `CareerEngine`
- Country job pools (`JobPool`), education-gated eligibility
- Hire probability: smarts, GPA, criminal record penalty (reduced by clean streak)
- Annual salary, performance, promotion, firing, downsizing events

### `FinanceEngine`
- Cash-only: houses, cars, motorbikes from `AssetCatalog`
- Upkeep, degradation, net worth, multi-asset upkeep discount
- Finance event threshold gate (~50k money or owned assets)

### `RelationshipEngine` (~700+ lines)
- Family generation at birth (`FamilyGenerator`)
- Year tick: age family, relationship decay, decay notices
- **10 interaction types:** SPEND_TIME, ARGUE, ASK_FOR_MONEY, GIFT, COMPLIMENT, INSULT, TRAVEL_TOGETHER, ASK_FOR_ADVICE, PRANK, SET_UP_ON_DATE
- Dating pool (~15% foreign prospects), marriage, divorce, children
- Mixed-heritage children (`secondaryCountryCode`)
- Friendship opportunities during school/work years
- Relationship tiers and milestones (capped at 25 per person)

### `CrimeEngine`
- Crime types: PICKPOCKET, SHOPLIFT, FRAUD
- Arrest, incarceration (`serveYear`), criminal record
- Non-violent only — no combat/weapons

### `HealthEngine`
- Illness rolls, untreated drain
- Doctor visits: public vs private care tiers

### `MortalityEngine`
- Age/health-based death + rare random causes
- Gentle death phrasing
- Death log marker `::DEATH:` preserved by `EventLogCap` (prefix match at line start only)

### `AchievementEngine`
- Checks 31 conditions against `AchievementCatalog`
- Called after age-up if alive; also mid-life on some actions

### `LegacyEngine`
- `eligibleHeirs()` — living children age ≥ 16
- `createLegacyCharacter()` — new generation, money split, family remap, ancestry append
- `buildAncestryEntry()` for deceased character summary

### `RelocationEngine`
- Relocation offers via events; multi-move supported (`relocationCount`, `relocationHistory`)
- `world_traveler` achievement requires `relocationCount >= 2`

### Cap utilities

| Utility | Max | Strategy |
|---------|-----|----------|
| `EventLogCap` | 150 lines | Newest kept; `::DEATH:` lines always preserved |
| `AncestryHistoryCap` | 25 entries | Earliest generations kept (lowest `generationNumber`) |
| `RelationshipMilestoneCap` | 25 per person | Newest kept (`takeLast`); applied in `RelationshipEngine` + save |

### Supporting domain

- `EffortResolver` — parses study/work effort from choice labels
- `PersonGenerator` — random persons for dating/friends
- `FamilyGenerator` — parents/siblings at birth
- `AncestrySummary` — display helpers
- `GiftTier` — small/medium/large gift economics

---

## 10. Data layer — models, persistence, content

### Core model: `Character`

Key fields (`data/model/Character.kt`):
- Identity: `name`, `age`, `gender`, `birthYear`, `alive`
- Location: `countryCode`, `birthCountryCode`, `secondaryCountryCode`, `relocationCount`, `relocationHistory`
- Legacy: `generationNumber`, `ancestryHistory`
- Gameplay: `stats`, `family`, `education`, `career`, `assets`, `criminalRecord`, `activeConditions`
- Presentation: `avatarConfig`, `eventLog`
- Defaults: country `KE`, generation `1`

### `Person`

Relations: MOTHER, FATHER, SIBLING, SPOUSE, CHILD, FRIEND  
Relationship level 0–100, `milestones` (capped 25), `interactedThisYear`, per-person country codes.

### Room database (`MaishaDatabase`, version 13)

| Entity | Table | Scope |
|--------|-------|-------|
| `CharacterEntity` | `character_save` | Per slot (PK: `slotId`) |
| `AchievementEntity` | achievements | Global |

**JSON columns:** `familyJson`, `educationJson`, `careerJson`, `assetsJson`, `criminalRecordJson`, `healthConditionsJson`, `eventLogJson`, `triggeredEventIdsJson`, `avatarConfigJson`, `relocationHistoryJson`, `ancestryHistoryJson`.

**Migrations:** Explicit in `DatabaseMigrations.kt` — **no** `fallbackToDestructiveMigration()`.

**Corruption handling:**
- `SerializationUtils.safeDeserialize()` — per-field fallback on malformed JSON
- `DatabaseHealth` — DB open failure sets `isAvailable = false`
- `SlotSummary.isCorrupted` — slot picker shows "Save Data Issue"
- Full-screen `DatabaseUnavailableScreen` when entire DB fails (P52)

### DataStore (`SettingsRepository`)

Keys: sound, haptics, language, onboarding completed, seen tip IDs, notifications enabled, last opened timestamp.

### Meta bonuses (`MetaBonusRepository`)

Tracks age-up count for ad frequency, Second Wind reward state.

---

## 11. Cross-cutting systems

### Hilt modules
- `DatabaseModule` — Room DB, DAOs, `GameDatabaseAccess` binding
- Worker factories for `DailyReminderWorker`, `ContextualNudgeWorker`

### `MaishaApplication`
- Initializes MobileAds, FeedbackManager preload, notification channel
- Schedules daily reminder if notifications enabled
- Custom WorkManager configuration with Hilt worker factory

### Permissions (`AndroidManifest.xml`)
- `INTERNET` — AdMob
- `VIBRATE` — haptics
- `POST_NOTIFICATIONS` — Android 13+
- `AD_ID` — AdMob advertising ID

---

## 12. Feature inventory (shipped)

See README.md for checkbox list. Highlights:
- Core loop, family/relationships, education/career, assets, crime/health
- Death, legacy, ancestry, 3 slots, achievements (31), worldwide (15 countries)
- Procedural avatars, AdMob monetization, WorkManager notifications
- Bounded growth on all three persisted list types (P37, P43, P54)

---

## 13. Event content system

### JSON files (`app/src/main/assets/data/events/`)

| File | Count | Theme |
|------|-------|-------|
| `starter_events.json` | 20 | Ages 0–2 |
| `education_events.json` | 13 | School, study effort |
| `career_events.json` | 10 | Work effort |
| `finance_events.json` | 8 | Money, assets |
| `relationship_events.json` | 8 | Family, dating gates |
| `general_events.json` | 39 | Cross-cutting (largest pool) |
| `holiday_events.json` | 2 | Country holidays |
| **Total** | **~100** | Plus code-generated system events |

**Loader:** `EventRepository` — age-bucket index, filters by tags, one-time IDs, country restrictions, relationship/finance gates.

**Schema docs:** `app/src/main/assets/data/events/README.md`

**KE-only events (P53):** 9 events with `restrictedToCountry: "KE"` — all have universal counterparts.

---

## 14. Worldwide / country system

### Playable countries (15) — source of truth: `CountryCatalog.kt`

| Code | Country | Currency |
|------|---------|----------|
| KE | Kenya | KSh |
| NG | Nigeria | ₦ |
| ZA | South Africa | R |
| EG | Egypt | E£ |
| US | United States | $ |
| CA | Canada | CA$ |
| GB | United Kingdom | £ |
| FR | France | € |
| DE | Germany | € |
| IN | India | ₹ |
| JP | Japan | ¥ |
| PH | Philippines | ₱ |
| ID | Indonesia | Rp |
| BR | Brazil | R$ |
| MX | Mexico | MX$ |

Each country has: exam names, transport mode, money app/bank label, greeting, 2 notable holidays.

**Economy scaling:** `EconomyScaler` adjusts salaries and asset prices per country.

---

## 15. Achievements

**Count:** 31 (`AchievementCatalog`)

**Categories (7):** CAREER (3), EDUCATION (3), FAMILY (10), WEALTH (4), LONGEVITY (3), MISCHIEF (3), WORLDLY (5)

**Persistence:** `AchievementRepository` + Room — global across slots.

**Wealth thresholds:** Currency-scaled via `AchievementWealth` + `EconomyScaler` (P49).

---

## 16. Monetization (AdMob)

**Publisher ID (manifest):** `ca-app-pub-9418386170210711`

**Ad units (`AdUnitConfig.kt`):** Google **test IDs** in dev.

| Ad type | Where | Behavior |
|---------|-------|----------|
| Banner | SlotPickerScreen | Always visible |
| Interstitial | After Age Up | Every **5** age-ups; deferred during celebrations |
| Rewarded | LifeSummaryScreen | Second Wind — stat bonus for next new life |

---

## 17. Notifications and retention

- `DailyReminderWorker` — periodic 24h + 6h flex, `KEEP` policy
- `ContextualNudgeWorker` — untreated condition (4h), pending decision (6h)
- Platform limits: `docs/KNOWN_PLATFORM_LIMITATIONS.md`

---

## 18. Localization

| Resource | Coverage |
|----------|----------|
| `values/` | English (100 strings — full) |
| `values-sw/` | Kiswahili (100 strings — full) |
| `values-fr/`, `-es/`, `-pt/`, `-hi/` | 66 strings — UI chrome only |
| Event JSON text | English only (flavor placeholders resolved at runtime) |

**`LocaleManager`:** `en`, `sw`, `fr`, `pt`, `es`, `hi` via `AppCompatDelegate.setApplicationLocales`.

---

## 19. Testing

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

**38 test classes** — see `docs/TEST_COVERAGE.md` for engine × test map.

**Manual QA:** `docs/MANUAL_QA_TEST_PLAN.md` — 73 steps.

**Performance:** `docs/PERFORMANCE_AUDIT.md` — JVM `ageUp()` median < 16ms passes.

---

## 20. Documentation corpus

| File | Purpose |
|------|---------|
| `README.md` | Feature inventory, setup, known gaps |
| `docs/PROJECT_OVERVIEW.md` | **This file** — external AI onboarding |
| `docs/ARCHITECTURE.md` | Age-up flow, slots/legacy, engine API |
| `docs/TEST_COVERAGE.md` | Engine × test coverage map |
| `docs/MANUAL_QA_TEST_PLAN.md` | Device QA checklist |
| `docs/PERFORMANCE_AUDIT.md` | Profiling baseline |
| `docs/UI_COPY_AUDIT.md` | Microcopy voice standard |
| `docs/SAVE_DATA_INTEGRITY.md` | Corruption resilience + bounded lists |
| `docs/KNOWN_PLATFORM_LIMITATIONS.md` | Battery/Doze, notifications |
| `docs/PRIVACY_POLICY.md` | Privacy policy source |
| `docs/PLAY_STORE_DATA_SAFETY_NOTES.md` | Data Safety draft |
| `docs/PLAY_STORE_LISTING.md` | Store listing + IARC notes |
| `docs/REFACTORING_AUDIT.md` | Duplication consolidation |
| `assets/data/events/README.md` | Event JSON schema |

---

## 21. Development prompt history (1–54)

| Prompt | Topic | Status |
|--------|-------|--------|
| 1–12 | Core gameplay, ads, crime, health, achievements | Done |
| 13–14 | Multi-slot saves, settings | Done |
| 17–18 | Share card, notifications | Done |
| 19 | Firebase Analytics | **Skipped** |
| 20–35 | Avatars, worldwide, legacy, relationships, countries | Done |
| 36 | Pre-ship gap consolidation | Documented |
| 37–39 | Event log cap, unit tests, edge-case hardening | Done |
| 40–42 | README, refactoring audit, manual QA plan | Done |
| 43 | Performance profiling | Done (partial device) |
| 44 | UI copywriting pass | Done |
| 45 | Save data integrity | Done |
| 46 | Battery compliance | Done |
| 47 | Privacy policy + Data Safety docs | Done (hosting pending) |
| 48 | Play Store listing | Done (screenshots pending) |
| 49 | Currency achievements, locales, audit closures | Done |
| 50 | Test coverage gaps + LifeViewModel ADR | Done |
| 51 | IARC content rating verification | Done |
| 52 | Architecture findings (relocation, death persist, DB UX, confetti) | Done |
| 53 | Exam trigger naming + KE event variant audit | Done |
| 54 | Per-person milestone cap + `::DEATH:` marker audit | Done |

---

## 22. Recent git history

```
e88f316 Cap per-person relationship milestones and document P54 audit.
24f1dff Clarify exam triggers and verify KE event variant coverage (Prompt 53).
017fff6 Verify four architecture findings and close remaining gaps (Prompt 52).
172de1a Verify IARC content rating against shipped event JSON (Prompt 51).
7c09397 Close engine test coverage gaps and formalize LifeViewModel ADR (Prompt 50).
b4a3e33 Grounded fix pass: currency-aware achievements, locales, audit closures (Prompt 49).
```

---

## 23. Known gaps and pre-ship checklist

### Must complete before Play Store release

1. Production AdMob ad unit IDs in `AdUnitConfig.kt`
2. Privacy policy hosted at public HTTPS URL
3. Play Console: Data Safety, content rating, listing, screenshots
4. App signing + consider ProGuard for release
5. Final art: launcher icon, feature graphic, avatar sprites
6. Real sound assets (replace silent `res/raw/*.wav`)
7. Audience decision: under-13 / child-directed → AdMob restricted ads
8. Device profiling: cold start, scroll jank on itel A665L

### Known gameplay/content gaps (non-blocking)

| Gap | Detail |
|-----|--------|
| `EducationState.expelled` | Field exists; no event sets it |
| Voluntary dropout events | Not implemented |
| Prompt 30 full event audit | Partial |
| Event JSON language | English only |

---

## 24. Compliance and store-prep status

| Requirement | Code | Documentation | Manual action |
|-------------|------|---------------|---------------|
| Privacy policy | Settings link (when URL set) | `PRIVACY_POLICY.md` | Host URL |
| Data Safety | — | `PLAY_STORE_DATA_SAFETY_NOTES.md` | Submit in Console |
| Store listing | — | `PLAY_STORE_LISTING.md` | Paste + screenshots |
| Content rating | — | IARC notes in listing doc | Complete questionnaire |

---

## 25. Constraints for future AI work

1. Do not add Firebase unless explicitly requested.
2. Do not scatter AdMob calls — use `AdManager` / `AdFrequencyController`.
3. Do not put Android APIs in domain engines.
4. Do not use `fallbackToDestructiveMigration()`.
5. Preserve Age Up ordering — mortality last; achievements after death check.
6. Achievements are global — not per-slot.
7. Extend `Character` persistence pattern — JSON blobs + mapper.
8. Match Prompt 44 voice for user-facing strings.
9. New bounded lists must follow `EventLogCap` / `AncestryHistoryCap` / `RelationshipMilestoneCap` pattern.
10. Commit only when user asks.

---

## 26. Quick reference — where to change things

| Task | Primary files |
|------|---------------|
| Add life event | `assets/data/events/*.json` |
| Change age-up logic order | `domain/GameEngine.kt` |
| Add achievement | `AchievementCatalog.kt`, `AchievementEngine.kt`, `strings.xml` |
| Add country | `CountryCatalog.kt`, `JobPool`, `AssetCatalog`, `NamePool` |
| New screen | `ui/...`, `MaishaNavHost.kt`, ViewModel |
| Save/load bug | `CharacterRepository.kt`, `CharacterSaveMapper.kt`, `SerializationUtils.kt` |
| Bounded list cap | `domain/*Cap.kt`, apply in engine + `CharacterRepository` |
| Ad frequency | `AdFrequencyController.kt`, `LifeViewModel` |
| Strings / copy | `res/values/strings.xml`, `values-sw/strings.xml` |
| Theme / colors | `ui/theme/` |
| Unit test | `app/src/test/java/.../domain/` |

---

## 27. Known documentation drift

Trust **source code** over older prose when they conflict:

| Topic | Older docs may say | Code reality (July 2026) |
|-------|-------------------|--------------------------|
| Country roster | AU, TH | **FR, DE** — no AU or TH in `CountryCatalog.kt` |
| Achievement count | ~30 | **31** |
| Event count | "hundreds" | **~100** JSON + system events |
| Test count | 34 files | **38** test classes |
| Play Store listing countries | AU, TH | Use `CountryCatalog.kt` list (15 countries) |

Re-verify `CountryCatalog.kt`, `AchievementCatalog.kt`, and `MaishaDatabase` version before updating docs.

---

*End of project overview. Re-generate when major systems are added.*
