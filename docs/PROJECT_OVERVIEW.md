# Maisha Life Simulator — Project Overview for External AI

**Document purpose:** Give any external AI (or human contributor) a complete, accurate picture of what this Android project is, what has been built, how it is structured, what remains unfinished, and how to work on it safely without breaking established patterns.

**Last updated:** July 3, 2026  
**Latest known prompt:** 48 (Play Store listing content)  
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
8. [Domain layer — game engines](#8-domain-layer--game-engines)
9. [Data layer — models, persistence, content](#9-data-layer--models-persistence-content)
10. [UI layer — Compose, ViewModels, polish](#10-ui-layer--compose-viewmodels-polish)
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
21. [Development prompt history (1–48)](#21-development-prompt-history-148)
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

Development followed a **sequential prompt series** (48 prompts as of July 2026), each adding or hardening a slice of the app. Early prompts (1–12) built core gameplay; mid prompts (13–35) added slots, settings, worldwide expansion, legacy, achievements, polish; late prompts (36–48) focused on audits, tests, performance, copy, integrity, compliance, and store prep **documentation**.

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
| Unit tests | **Strong** | 34 test files, domain + data coverage |
| Manual QA plan | **Documented** | 73-step device checklist |
| Performance pass | **Partial** | JVM benchmarks pass; device profiling flagged |
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
| Kotlin | 2.0 | Primary language |
| Android Gradle Plugin | 8.7.3 | Build |
| JDK | 17 | `jvmTarget = "17"` |
| compileSdk / targetSdk | 35 | `app/build.gradle.kts` |
| minSdk | 26 | Android 8.0+ |
| Jetpack Compose | BOM-managed | All UI |
| Material 3 | — | Theme, components |
| Navigation Compose | — | `MaishaNavHost` |
| Hilt | KSP processors | DI for ViewModels, engines, repos |
| Room | DB version **13** | `character_save`, `achievement` tables |
| DataStore Preferences | — | Settings, onboarding flags |
| Kotlinx Serialization | JSON | Events, Room blob fields |
| WorkManager + Hilt Worker | — | Notifications |
| Google Play Services Ads | `play-services-ads` only | **No Firebase BOM** |
| JUnit 4 | — | Unit tests on JVM |
| SplashScreen API | — | Cold start splash |

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
3. **Persistence boundary:** `CharacterRepository` maps `Character` ↔ `CharacterEntity` with JSON columns for nested state. `EventLogCap` (max **150** lines) and `AncestryHistoryCap` (max **25** entries) applied on save.
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

**Death-year rule (Prompt 39):** Achievements cannot unlock on the same tick as death. Promotions/events can still fire before mortality check.

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
│       │   │   ├── ads/              # AdManager, AdUnitConfig, frequency
│       │   │   ├── data/             # Catalogs, CountryCatalog, repos
│       │   │   ├── data/events/      # EventRepository
│       │   │   ├── data/local/       # Room, DAOs, mappers, DataStore
│       │   │   ├── data/model/       # Character, Person, LifeEvent, etc.
│       │   │   ├── di/               # DatabaseModule, Hilt bindings
│       │   │   ├── domain/           # All game engines
│       │   │   ├── feedback/         # FeedbackManager
│       │   │   ├── notifications/    # Workers, scheduler
│       │   │   ├── ui/               # Compose screens + ViewModels
│       │   │   └── util/             # Serialization, locale, share helpers
│       │   ├── res/
│       │   │   ├── values/           # English strings (primary)
│       │   │   ├── values-sw/        # Swahili
│       │   │   ├── values-fr/, -es/, -pt/, -hi/  # Additional UI locales
│       │   │   └── raw/              # Placeholder sound .wav files
│       │   └── ...
│       └── test/                     # 34 JVM test files
├── docs/                             # Architecture, audits, store/compliance
├── gradle/libs.versions.toml
└── README.md
```

**Scale (approximate):** ~152 Kotlin source files in `main`, ~34 test files.

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

## 8. Domain layer — game engines

All in `app/src/main/java/com/maisha/game/domain/`. Injected via Hilt, `@Singleton` where noted.

### `GameEngine` (~central orchestrator)
- `ageUp()` — full yearly simulation
- `applyChoice()` — event choice effects
- `introEventsForNewborn()` — age-0 starter events
- Delegates: job apply/quit, asset buy/sell, family interactions, dating/marriage/children, crime, doctor
- `applyLegacyFamilyMilestones()` after legacy handoff

### `EducationEngine`
- Pipeline: primary → secondary → university
- Country-specific exam naming via `ExamNames` / `CountryCatalog`
- GPA, study effort from event choice labels (`EffortResolver`)
- System exam events (`exam_system` tag) bypass random pool
- **Gap:** `EducationState.expelled` exists but is never set by gameplay events

### `CareerEngine`
- Country job pools (`JobPool`), education-gated eligibility
- Hire probability: smarts, GPA, criminal record penalty (reduced by clean streak)
- Annual salary, performance, promotion, firing, downsizing events
- Incarceration blocks work

### `FinanceEngine`
- Cash-only: houses, cars, generic assets from `AssetCatalog`
- Upkeep, degradation, net worth, multi-asset upkeep discount
- Finance event threshold gate (~50k money or owned assets)

### `RelationshipEngine` (largest engine, ~700+ lines)
- Family generation at birth (`FamilyGenerator`)
- Year tick: age family, relationship decay, decay notices
- **10 interaction types:** SPEND_TIME, ARGUE, ASK_FOR_MONEY, GIFT, COMPLIMENT, INSULT, TRAVEL_TOGETHER, ASK_FOR_ADVICE, PRANK, SET_UP_ON_DATE
- Dating pool (~15% foreign prospects), marriage, divorce, children
- Mixed-heritage children (`secondaryCountryCode`)
- Friendship opportunities during school/work years
- Relationship tiers and milestones

### `CrimeEngine`
- Crime types: PICKPOCKET, SHOPLIFT, FRAUD
- Arrest, incarceration (`serveYear`), criminal record
- Non-violent only — no combat/weapons

### `HealthEngine`
- Illness rolls, untreated drain
- Doctor visits: public vs private care tiers
- Active conditions list on Character

### `MortalityEngine`
- Age/health-based death + rare random causes
- Gentle death phrasing (`gentleCauseLabel`, `deathFlavorText`)
- Death log marker `::DEATH:` preserved by `EventLogCap`

### `AchievementEngine`
- Checks ~31 conditions against `AchievementCatalog`
- Called after age-up if alive; also mid-life on some actions

### `LegacyEngine`
- `eligibleHeirs()` — living children age ≥ threshold
- `createLegacyCharacter()` — new generation, money split, family remap, ancestry append
- `buildAncestryEntry()` for deceased character summary

### `RelocationEngine`
- Relocation offers via events
- `relocate()` updates `countryCode`, `relocationHistory`, counters

### Utilities
- `EventLogCap` — max 150 log entries
- `AncestryHistoryCap` — max 25 ancestry records on save
- `EffortResolver` — parses study/work effort from choice labels
- `PersonGenerator` — random persons for dating/friends
- `FamilyGenerator` — parents/siblings at birth
- `AncestrySummary` — display helpers

---

## 9. Data layer — models, persistence, content

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
Relationship level 0–100, milestones, `interactedThisYear`, per-person `countryCode` / `secondaryCountryCode`.

### Room database (`MaishaDatabase`, version 13)

| Entity | Table | Scope |
|--------|-------|-------|
| `CharacterEntity` | `character_save` | Per slot (PK: `slotId`) |
| `AchievementEntity` | achievements | Global |

**JSON columns on CharacterEntity:** `familyJson`, `educationJson`, `careerJson`, `assetsJson`, `criminalRecordJson`, `healthConditionsJson`, `eventLogJson`, `triggeredEventIdsJson`, `avatarConfigJson`, `relocationHistoryJson`, `ancestryHistoryJson`.

**Migrations:** Explicit in `DatabaseMigrations.kt` — **no** `fallbackToDestructiveMigration()`.

**Corruption handling (Prompt 45):**
- `SerializationUtils.safeDeserialize()` — per-field fallback on malformed JSON
- `DatabaseHealth` — DB open failure sets `isAvailable = false`
- `SlotSummary.isCorrupted` — slot picker shows "Save Data Issue" + clear option
- `SavedGameLoadResult` sealed type for load outcomes

### DataStore (`SettingsRepository`)

Keys include: sound, haptics, language, onboarding completed, seen tip IDs, notifications enabled, last opened timestamp, flag emoji fallback preference.

**Onboarding tips IDs:** `family_dating`, `family_detail`, `first_death_achievements`.

### Meta bonuses (`MetaBonusRepository`)

Tracks age-up count for ad frequency, Second Wind reward state.

---

## 10. UI layer — Compose, ViewModels, polish

### ViewModels (all `@HiltViewModel`)

| ViewModel | Responsibility |
|-----------|----------------|
| `LifeViewModel` | **Largest (~885 lines)** — age up, choices, all life actions, ads deferral, achievements, celebrations, stat deltas, tips |
| `LifeSummaryViewModel` | Death recap, legacy heir selection, share card |
| `SlotPickerViewModel` | 3 slots, corruption display, navigation |
| `CharacterCreationViewModel` | Name, gender, country search, avatar, slot overwrite confirm |
| `OnboardingViewModel` | 5-slide first-run |
| `AchievementsViewModel` | Global achievement grid |
| `SettingsViewModel` | Preferences, reset all data, notification toggle |

### Theme (`ui/theme/`)
- Teal primary, coral negative, gold accent
- Custom shapes, spacing, typography
- Designed for **360dp** width

### Voice / copy standard (Prompt 44)
- Warm, encouraging, lightly playful
- Title Case for CTAs and screen titles; sentence case for body
- Canonical term: **Age Up** (not "Next Year")
- Death/serious topics: gentle, not grim

### Polish features
- `CelebrationOverlay` — confetti for marriage, graduation, achievements, milestones
- `FloatingStatChangeLayer` — animated stat deltas after events
- Tab crossfade on `LifeScreen`
- `EmptyStateCard` + `MaishaIllustrations` for empty lists
- `FeedbackEffect` — sound + haptic cues
- Nav slide transitions (`NavAnimations`)

### Share feature (Prompt 17)
- `ShareCardComposable` + `ComposableToImage` → PNG
- `ShareIntentHelper` + FileProvider
- User-initiated via system share sheet only

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

### Core life simulation
- [x] Character creation (name, gender, country, avatar)
- [x] Yearly Age Up with stat changes
- [x] JSON-driven random events + choice dialogs
- [x] System events (exams, promotion, firing, downsizing, relocation)
- [x] Event log (newest first, capped at 150)
- [x] Contextual onboarding tips
- [x] 5-slide onboarding

### Family & relationships
- [x] Generated parents and optional siblings
- [x] Dating, marriage, divorce, children
- [x] 10 interaction types with relationship effects
- [x] Relationship tiers, milestones, memories timeline
- [x] Annual relationship decay if not interacted
- [x] Friends (annual opportunity)
- [x] Mixed-heritage children (dual flags)
- [x] ~15% foreign dating prospects

### Education & career
- [x] Primary → secondary → university pipeline
- [x] Country-flavored exam names and result events
- [x] GPA and study/work effort from choices
- [x] University course selection from exam results
- [x] Country-specific job pools
- [x] Criminal record hiring penalty + clean-streak reduction
- [x] Promotions, firing, downsizing

### Assets & finance
- [x] Asset shop (country-scoped + universal items)
- [x] Cash-only (no loans)
- [x] Upkeep, degradation, net worth
- [x] Multi-asset upkeep discount

### Crime & health
- [x] Pickpocket, shoplift, fraud
- [x] Arrest, jail years, criminal record
- [x] Illness rolls, untreated drain
- [x] Doctor visits (public/private)

### Death & legacy
- [x] Mortality engine (age, health, rare events)
- [x] Life summary screen
- [x] Start new life / slot management
- [x] Continue legacy as adult heir
- [x] Ancestry timeline screen
- [x] Estate money split among children
- [x] Generation counter + ancestry history

### Saves & settings
- [x] 3 save slots with overwrite confirmation
- [x] Corrupted slot detection and recovery UX
- [x] Settings: sound, haptics, notifications, language, reset all data
- [x] Privacy policy link (hidden until `PRIVACY_POLICY_URL` configured)

### Achievements
- [x] 31 achievements in 6 categories
- [x] Global progress (not per-slot)
- [x] Unlock dialog + celebration

### Worldwide
- [x] 15 playable countries with flavor data
- [x] Flavor interpolation in event text (`{secondaryExam}`, `{transportMode}`, etc.)
- [x] Relocation between countries
- [x] Holiday events with cooldown
- [x] Kenya-only events via `restrictedToCountry: "KE"` + `_world` variants

### Avatars
- [x] Procedural Canvas rendering (`AvatarRenderer`)
- [x] 8 skin tones, 8 hair styles
- [x] Age stages, expression flash on events
- [ ] Final sprite art (architecture ready, placeholders in use)

### Monetization
- [x] Banner on slot picker
- [x] Interstitial every 5 age-ups (deferred during celebrations)
- [x] Rewarded Second Wind on life summary
- [x] Session ad tracker
- [ ] Production ad unit IDs

### Retention
- [x] Daily reminder (WorkManager, 24h + 6h flex, KEEP policy)
- [x] Contextual nudges (e.g. after death)
- [x] Notification permission flow

---

## 13. Event content system

### JSON files (`app/src/main/assets/data/events/`)

| File | Event count (approx) | Theme |
|------|---------------------|-------|
| `starter_events.json` | 20 | Ages 0–2 |
| `education_events.json` | 13 | School, study effort |
| `career_events.json` | 10 | Work effort |
| `finance_events.json` | 8 | Money, assets |
| `relationship_events.json` | 8 | Family, dating gates |
| `general_events.json` | 39 | Cross-cutting (largest pool) |
| `holiday_events.json` | 2 | Country holidays |
| **Total** | **~100** | Plus code-generated system events |

**Loader:** `EventRepository` — age-bucket index (Prompt 43 perf), filters by tags, one-time IDs, country restrictions, relationship/crime/finance gates.

**Schema docs:** `app/src/main/assets/data/events/README.md`

**Authoring placeholders:** `{secondaryExam}`, `{primaryExam}`, `{transportMode}`, `{moneyApp}`, `{holidayName}`, `{holidayDescription}`, etc.

---

## 14. Worldwide / country system

### Playable countries (15) — **source of truth: `CountryCatalog.kt`**

| Code | Country | Currency | Sample exam flavor |
|------|---------|----------|-------------------|
| KE | Kenya | KSh | KCPE / KCSE |
| NG | Nigeria | ₦ | BECE / WAEC |
| ZA | South Africa | R | Systemic Evaluation / NSC Matric |
| EG | Egypt | E£ | Thanaweya Amma |
| US | United States | $ | State Assessment / SAT |
| CA | Canada | CA$ | Provincial Assessment / Diploma Exam |
| GB | United Kingdom | £ | GCSE / A-Levels |
| FR | France | € | Brevet / Baccalauréat |
| DE | Germany | € | Hauptschulabschluss / Abitur |
| IN | India | ₹ | CBSE Class 10 / 12 |
| JP | Japan | ¥ | Junior High Entrance / Common Test |
| PH | Philippines | ₱ | NAT |
| ID | Indonesia | Rp | UN SMP / UN SMA |
| BR | Brazil | R$ | SAEB / ENEM |
| MX | Mexico | MX$ | PLANEA / UNAM Entrance Exam |

Each country has: exam names, transport mode, money app/bank label, greeting, 2 notable holidays (except generic fallback `XX`).

**Economy scaling:** `EconomyScaler` adjusts salaries and asset prices per country.

**Names:** `NamePool` per-country first/last names.

**Jobs / assets:** `JobPool`, `AssetCatalog` with country overrides.

---

## 15. Achievements

**Count:** 31 (`AchievementCatalog` + `AchievementEngine` condition branches)

**Categories:** Career, Education, Family, Wealth, Longevity, Mischief (+ worldwide/legacy/social extras)

**Sample IDs:** `first_job`, `corner_office`, `graduate`, `tied_the_knot`, `first_child`, `six_figures`, `first_million`, `centenarian`, `brush_with_law`, `clean_record`, `second_generation`, `dynasty_builder`, `world_citizen`, `passport_stamped`, `global_family`, etc.

**Persistence:** `AchievementRepository` + Room `AchievementEntity` — unlocked state survives across slots and character resets (unless Reset All Data).

---

## 16. Monetization (AdMob)

**Publisher ID (manifest):** `ca-app-pub-9418386170210711` (in `strings.xml` as `admob_app_id`)

**Ad units (`AdUnitConfig.kt`):** Currently **Google test IDs** for interstitial, rewarded, banner.

**Placement:**
| Ad type | Where | Behavior |
|---------|-------|----------|
| Banner | SlotPickerScreen | Always visible on picker |
| Interstitial | After Age Up | Every **5** age-ups via `AdFrequencyController`; **deferred** if celebration overlay showing |
| Rewarded | LifeSummaryScreen | Second Wind — stat bonus stored in `MetaBonusRepository` for next new life |

**Classes:** `AdManager`, `SessionAdTracker`, `AdFrequencyController`

**Privacy:** AdMob collects device/ad data — see `docs/PRIVACY_POLICY.md` and `docs/PLAY_STORE_DATA_SAFETY_NOTES.md`.

---

## 17. Notifications and retention

**Workers:**
- `DailyReminderWorker` — periodic reminder to play
- `ContextualNudgeWorker` — one-time contextual messages (e.g. post-death)

**Scheduler (`NotificationScheduler`):**
- `ExistingPeriodicWorkPolicy.KEEP` (Prompt 46 — battery compliance)
- 24-hour interval + 6-hour flex
- Empty constraints (no network requirement)
- **No** battery optimization exemption flow

**Platform limits:** Documented in `docs/KNOWN_PLATFORM_LIMITATIONS.md` — OEM Doze may delay delivery.

---

## 18. Localization

| Resource | Coverage |
|----------|----------|
| `values/` | English (primary) |
| `values-sw/` | Swahili (actively maintained — Prompt 44 updates) |
| `values-fr/`, `values-es/`, `values-pt/`, `values-hi/` | Additional UI string files exist |
| Event JSON text | **English only** (flavor placeholders resolved at runtime) |

**`LocaleManager`:** Applies user language from DataStore; system default fallback.

**Country names / exams:** Localized via flavor data, not full UI translation of event narratives.

---

## 19. Testing

### Run commands

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

### Test file inventory (34 files)

**Domain:** `GameEngineTest`, `EducationEngineTest`, `CareerEngineTest`, `FinanceEngineTest`, `RelationshipEngineTest`, `RelationshipEngineTickTest`, `CrimeEngineTest`, `HealthEngineTest`, `MortalityEngineTest`, `LegacyEngineTest`, `LegacyAncestryTest`, `AchievementEngineTest`, `RelocationEngineTest`, `BalanceTuningTest`, `EffortResolverTest`, `FriendshipGenerationTest`, `MixedHeritageTest`, `EventLogCapTest`, `AncestryHistoryCapTest`, `PerformanceBenchmarkTest`

**Data:** `EventRepositoryEdgeCaseTest`, `EventRepositoryAgeIndexTest`, `FlavorInterpolatorTest`, `CountryCatalogContentTest`, `GlobalContentCoverageTest`, `CharacterSaveMapperTest`, `CharacterRepositoryCorruptionTest`

**Other:** `SerializationUtilsTest`, `StatsEdgeCaseTest`, `RelationshipTierTest`, `ExpressionResolverTest`, `EnglishOrdinalsTest`, `NotificationSchedulerTest`

### Manual QA
`docs/MANUAL_QA_TEST_PLAN.md` — 73 steps, sections A–I (onboarding through worldwide verification).

### Performance
`docs/PERFORMANCE_AUDIT.md` — JVM `ageUp()` median < 16ms passes; device cold start / scroll jank needs physical profiling.

---

## 20. Documentation corpus

| File | Purpose |
|------|---------|
| `README.md` | Feature inventory, setup, known gaps |
| `docs/PROJECT_OVERVIEW.md` | **This file** — external AI onboarding |
| `docs/ARCHITECTURE.md` | Age-up flow, slots/legacy, country cascade, engine API |
| `docs/MANUAL_QA_TEST_PLAN.md` | Device QA checklist |
| `docs/PERFORMANCE_AUDIT.md` | Profiling baseline and optimizations |
| `docs/UI_COPY_AUDIT.md` | Microcopy voice standard |
| `docs/SAVE_DATA_INTEGRITY.md` | Corruption resilience |
| `docs/KNOWN_PLATFORM_LIMITATIONS.md` | Battery/Doze, notifications |
| `docs/PRIVACY_POLICY.md` | Privacy policy source (needs hosting) |
| `docs/PLAY_STORE_DATA_SAFETY_NOTES.md` | Play Console Data Safety draft |
| `docs/PLAY_STORE_LISTING.md` | Store title, descriptions, screenshot plan |
| `docs/REFACTORING_AUDIT.md` | Duplication consolidation (Prompt 41) |
| `assets/data/events/README.md` | Event JSON schema |

---

## 21. Development prompt history (1–48)

High-level map of the prompt series. Prompts not listed individually in git messages are inferred from codebase features and conversation history.

| Prompt | Topic | Status |
|--------|-------|--------|
| 1 | Character creation + age-up loop | Done |
| 2 | Family & relationships foundation (`Person`, family gen) | Done |
| 3 | Education system (Kenya-flavored initially) | Done |
| 4 | Career & jobs | Done |
| 5 | Assets & spending | Done |
| 6 | Dating, marriage, kids | Done |
| 7 | UI polish pass (theme, components) | Done |
| 8 | Death & life summary | Done |
| 9 | AdMob integration | Done (test IDs) |
| 10 | Crime, health, event pool expansion | Done |
| 11 | Actions panel, health/record UI, character stats | Done |
| 12 | Achievements system | Done |
| 13 | Multiple save slots (3) | Done |
| 14 | Settings screen + DataStore | Done |
| 17 | Share life summary card | Done |
| 18 | Notifications (WorkManager) | Done |
| 19 | Firebase Analytics/Crashlytics | **Skipped / deprioritized** |
| 20–21 | Avatars, worldwide identity expansion | Done (placeholder art) |
| 25 / 34 | Legacy mode + ancestry | Done |
| 26 | Relationship depth (memories, interactions) | Done |
| 28 | Crime redemption / clean streak | Done |
| 29 | Settings polish | Done |
| 30 | Event content audit | **Partial** |
| 31–35 | Worldwide countries, flavor, relocation, holidays | Done |
| 36 | Pre-ship gap consolidation | Documented in README |
| 37 | Edge cases, event log cap, LifeViewModel patterns | Done |
| 38 | Domain unit test suite | Done |
| 39 | Edge-case hardening (death-year achievements, money floor) | Done |
| 40 | README feature inventory | Done |
| 41 | Refactoring / deduplication audit | Done |
| 42 | Manual QA test plan | Done |
| 43 | Performance profiling & optimization | Done (partial device metrics) |
| 44 | UI copywriting pass | Done |
| 45 | Save data integrity / corruption UX | Done |
| 46 | Battery & background execution compliance | Done |
| 47 | Privacy policy + Data Safety docs + Settings link | Done (manual hosting pending) |
| 48 | Play Store listing content | Done (screenshots pending) |

---

## 22. Recent git history

```
e87e207 Fix Play Store listing character counts
0f24d0e Add Play Store listing content (Prompt 48)
c24ad21 Add privacy policy docs + in-app disclosure (Prompt 47)
f3db973 Notification scheduling battery compliance (Prompt 46)
1957540 Save data integrity hardening (Prompt 45)
0874758 UI copywriting pass (Prompt 44)
7206697 Performance profiling (Prompt 43)
cf4e998 Manual QA test plan
a42230a Refactoring consolidation (Prompt 41)
4883fbf Codebase documentation pass
b9e70f0 Edge-case hardening (Prompt 39)
8d662f6 Domain unit tests (Prompt 38)
f3a96bb Ancestry, holidays, event log cap
64388df Worldwide flavor, localization, relocation
83107da first commit
```

---

## 23. Known gaps and pre-ship checklist

### Must complete before Play Store release

1. **Production AdMob ad unit IDs** in `AdUnitConfig.kt`
2. **Privacy policy hosted** at public HTTPS URL; update `PRIVACY_POLICY_URL` in `app/build.gradle.kts`
3. **Play Console:** Data Safety form, content rating (IARC), store listing paste, screenshots
4. **App signing** + consider `minifyEnabled` / ProGuard for release
5. **Final art:** launcher icon, feature graphic (1024×500), avatar sprites, illustration polish
6. **Real sound assets** (replace silent `res/raw/*.wav` placeholders)
7. **Audience decision:** under-13 / child-directed → AdMob restricted ads configuration
8. **Device profiling:** cold start, scroll jank on itel A665L or equivalent

### Known gameplay/content gaps (non-blocking)

| Gap | Detail |
|-----|--------|
| `EducationState.expelled` | Field exists; no event sets it |
| Voluntary dropout events | Not implemented |
| Prompt 30 full event audit | Partial — decade content not fully audited |
| Prompt 27 formal UI audit | Fixes landed; no formal report |
| Event JSON language | English only |

---

## 24. Compliance and store-prep status

| Requirement | Code | Documentation | Manual action remaining |
|-------------|------|---------------|-------------------------|
| Privacy policy | Settings link (when URL set) | `docs/PRIVACY_POLICY.md` | Host URL, fill contact |
| Data Safety | — | `docs/PLAY_STORE_DATA_SAFETY_NOTES.md` | Submit in Play Console |
| Store listing | — | `docs/PLAY_STORE_LISTING.md` | Paste + screenshots |
| Ad disclosure | AdMob integrated | Policy + Data Safety notes | AdMob console audience tags |
| In-app policy access | `SettingsScreen` → browser intent | — | Set real URL |
| Content rating | — | Notes in PLAY_STORE_LISTING.md | Complete IARC questionnaire |

**Data collected by third parties:** Google AdMob (advertising ID, IP, ad interactions). **Not collected by app code:** accounts, email, cloud saves, Firebase analytics.

---

## 25. Constraints for future AI work

1. **Do not add Firebase** unless explicitly requested — Prompt 19 was deprioritized; policy docs assume its absence.
2. **Do not scatter AdMob calls** — use `AdManager` / `AdFrequencyController`.
3. **Do not put Android APIs in domain engines** — keeps tests runnable on JVM.
4. **Do not use `fallbackToDestructiveMigration()`** — explicit Room migrations only.
5. **Preserve Age Up ordering** — mortality must stay last; achievements after death check.
6. **Achievements are global** — not per-slot.
7. **Extend `Character` persistence pattern** — JSON blobs + mapper, not parallel save systems.
8. **Match Prompt 44 voice** for user-facing strings.
9. **Honest store/compliance copy** — only claim shipped features.
10. **Commit only when user asks** — project convention via Cursor rules.

---

## 26. Quick reference — where to change things

| Task | Primary files |
|------|---------------|
| Add life event | `assets/data/events/*.json`, schema in `events/README.md` |
| Change age-up logic order | `domain/GameEngine.kt` |
| Add achievement | `data/AchievementCatalog.kt`, `domain/AchievementEngine.kt`, `res/values/strings.xml` |
| Add country | `data/CountryCatalog.kt`, `JobPool`, `AssetCatalog`, `NamePool` as needed |
| New screen | `ui/...`, route in `MaishaNavHost.kt`, ViewModel |
| Save/load bug | `data/local/CharacterRepository.kt`, `CharacterSaveMapper.kt`, `SerializationUtils.kt` |
| Ad frequency | `ads/AdFrequencyController.kt`, `LifeViewModel` celebration deferral |
| Notification timing | `notifications/NotificationScheduler.kt` |
| Settings preference | `data/local/SettingsRepository.kt`, `SettingsViewModel.kt` |
| Strings / copy | `res/values/strings.xml`, `values-sw/strings.xml` |
| Theme / colors | `ui/theme/` |
| Unit test for engine | `app/src/test/java/.../domain/` |

---

## 27. Known documentation drift

External AIs should trust **source code** over older prose when they conflict:

| Topic | README / older docs may say | Code reality (July 2026) |
|-------|----------------------------|--------------------------|
| Country roster | KE, NG, ZA, IN, US, GB, JP, MX, PH, EG, BR, CA, **AU, ID, TH** | 15 countries: includes **FR, DE**; **no AU or TH** in `CountryCatalog.kt` |
| Achievement count | ~30 | **31** in `AchievementCatalog` |
| Event count | "hundreds" (marketing) | **~100** JSON events + system-generated events |
| Localization | EN + SW only | `values-fr`, `values-es`, `values-pt`, `values-hi` also exist |
| Database version | README may lag | Room version **13** |

When updating this overview, re-verify `CountryCatalog.kt`, `AchievementCatalog.kt`, and `MaishaDatabase.kt` version first.

---

## Appendix A: `LifeViewModel` responsibilities (high coupling note)

`LifeViewModel` is intentionally monolithic (~885 lines). It coordinates:
- Slot load/save via `CharacterRepository`
- All `GameEngine` player actions
- `triggeredEventIds` tracking (persisted separately from Character model fields)
- Achievement unlock queues and dialogs
- Celebration overlay state and ad deferral
- Stat delta animations
- Expression flash on avatar
- Contextual tips
- Notification permission prompts
- Navigation events to summary/stats/ancestry

Refactoring it was considered out of scope in audit prompts — any split should preserve StateFlow shape and one-shot event patterns.

---

## Appendix B: BuildConfig fields

| Field | Current value | Purpose |
|-------|---------------|---------|
| `PRIVACY_POLICY_URL` | `https://REPLACE-WITH-HOSTED-PRIVACY-POLICY-URL` | Settings privacy link; hidden while placeholder |

---

*End of project overview. For deep dives, follow links in [Documentation corpus](#20-documentation-corpus). Re-generate or extend this file when major systems are added (especially Firebase, cloud save, or IAP).*
