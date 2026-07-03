# Maisha — Life Simulator

**Maisha** (Swahili for *life*) is a single-player life-simulation game for Android. Players create a character, age year-by-year through school, career, relationships, crime, health, and finance, and either die naturally or continue their family line across generations. The app targets mid-range Android phones (360dp width, API 26+) with a Compose-first UI and offline-first local saves.

The project began as a **Kenya-focused** prototype (KCPE/KCSE exams, KSh currency, Nairobi health references) and was expanded in later development passes to a **global roster of 15 countries** (KE, NG, ZA, IN, US, GB, JP, MX, PH, EG, BR, CA, AU, ID, TH). Kenya-specific content remains where historically authored (`restrictedToCountry: "KE"` events); universal events use `{placeholder}` flavor resolution so the same JSON works worldwide.

---

## Tech stack

| Technology | Role |
|------------|------|
| **Kotlin 2.0** | Primary language |
| **Jetpack Compose** | UI — declarative screens, Material 3, bottom-nav main loop |
| **MVVM + Hilt** | ViewModels are `@HiltViewModel` with constructor injection; UI reads `StateFlow` only |
| **Room** | Structured per-slot character saves (stats, JSON blobs for family/education/career/assets) |
| **DataStore** | Lightweight app preferences (sound, haptics, language, onboarding, tips) |
| **Kotlinx Serialization** | JSON event content + Room field serialization |
| **WorkManager + Hilt** | Daily reminders and contextual nudge notifications |
| **Google Mobile Ads** | Banner, interstitial, rewarded (Second Wind) — **test IDs in dev** |
| **JUnit 4** | Domain engine unit tests (pure functions, no emulator) |

Room holds durable game state; DataStore holds user settings that should survive character resets without a schema migration.

---

## Architecture (summary)

```
Compose Screens  →  ViewModels (StateFlow)  →  Domain Engines (pure Kotlin)
                              ↓
                    Repositories (Room / DataStore)
```

**Domain engines** (`GameEngine`, `EducationEngine`, `CareerEngine`, etc.) contain all game rules. They take immutable `Character` (and related) inputs and return new copies — no `Context`, no Room, no Android APIs. This was an intentional constraint from day one so the core loop is unit-testable without an emulator (see `app/src/test/.../domain/` and Prompt 38's test suite).

`GameEngine` orchestrates the yearly tick; feature-specific engines own their rules. `EventRepository` loads JSON from assets and filters eligible events; it lives in the data layer because it needs `AssetManager`.

Full walkthroughs: **[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)**.

---

## Feature inventory

### Core loop
- Yearly **Age Up** with random/system events, stat changes, and event log
- **Event choice** dialogs with stat, GPA, career, relationship, crime, health, and relocation effects
- **Multi-slot saves** (3 slots) with overwrite confirmation
- **Life summary** on death; share card export
- **Onboarding** + contextual tips

### Family & relationships
- Generated family (parents, siblings); dating, marriage, children, divorce
- **10 interaction types** (spend time, gift, travel, compliment, argue, etc.)
- Relationship tiers, annual decay, milestones
- **Friends** (annual opportunity during school/work years)
- **Mixed-heritage children** when parents are from different countries

### Education
- Primary → secondary → university pipeline with exams (country-flavored names)
- Study/work effort from event choices
- University course selection from exam results

### Career
- Country-specific job pools; education-gated eligibility
- Hire chance (smarts, GPA, criminal record penalty with **clean-streak reduction**)
- Annual salary, performance, promotion, firing, downsizing events

### Assets & finance
- Cash-only purchases (no loans); asset upkeep and degradation
- Net worth; multi-asset upkeep discount

### Crime & health
- Pickpocket / shoplift / fraud with arrest and incarceration
- Illness rolls, untreated drain, doctor visits (public vs private care)

### Achievements
- ~30 achievements across categories; global progress (not per-slot)
- Unlock dialogs + celebration overlay; persist across lives

### Legacy & ancestry
- **Continue legacy** as an adult child heir (same slot, new generation)
- **Ancestry timeline** (deceased generations + current character)
- `generationNumber` increments; `ancestryHistory` appended on legacy continuation
- Money split among living children on legacy handoff

### Worldwide / localization
- 15-country roster: names, jobs, assets, exams, holidays, economy scaling
- `{transportMode}`, `{secondaryExam}`, `{moneyApp}`, etc. in event JSON
- UI strings: EN + SW (`values-sw`); event text remains English in JSON
- Relocation between countries; holiday events with cooldown

### Avatars
- Procedural Canvas avatar (`AvatarRenderer`); 8 skin tones, 8 hair styles
- Age-stage rendering; expression flash on events
- Placeholder art architecture ready for final sprites

### Monetization
- Banner ad on slot picker; interstitial every N age-ups (deferred during celebrations)
- **Second Wind** rewarded ad → stat bonus on next new life
- Session ad tracker

### Feedback & polish
- Sound + haptics via `FeedbackManager` (placeholder silent `.wav` files)
- Celebration overlays (graduation, marriage, milestones)
- Floating stat deltas; tab crossfade; empty-state illustrations

### Retention
- Daily reminder + contextual nudges (WorkManager)
- Notification permission flow

---

## Setup & running

### Requirements
- **Android Studio** Ladybug or newer (AGP 8.7.3)
- **JDK 17**
- **compileSdk / targetSdk 35**, **minSdk 26**
- Gradle wrapper included — open project root in Android Studio

### Build & test
```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

### Run on device / emulator
1. Open **Life Simulator** in Android Studio → Sync Gradle.
2. Create or select a device:
   - **Physical:** itel A665L (or similar **360dp × ~640dp**, Android 13) — primary design target.
   - **Emulator:** Pixel 4a or custom AVD, **360×640 dp**, API 33+.
3. Run the `app` configuration.

Splash → onboarding (first launch) → slot picker → character creation → avatar picker → main life screen.

---

## Project layout (high level)

```
app/src/main/java/com/maisha/game/
  domain/          # Pure game engines
  data/            # Repositories, catalogs, EventRepository
  data/model/      # Character, Person, LifeEvent, etc.
  ui/              # Compose screens + ViewModels
  di/              # Hilt modules (DatabaseModule)
  notifications/   # WorkManager workers
  feedback/        # Sound/haptics
  ads/             # AdMob wrappers
app/src/main/assets/data/events/   # Life event JSON
app/src/test/                      # Domain unit tests
docs/ARCHITECTURE.md               # Deep architecture notes
```

---

## Known gaps / deferred work (pre-ship checklist)

Consolidated from Prompt 36 and subsequent audits. **Not blockers for development**; required before store release.

| Item | Status | Notes |
|------|--------|-------|
| **Production AdMob IDs** | Open | `AdUnitConfig.kt` uses Google test units |
| **Real sound assets** | Open | `res/raw/*.wav` are silent placeholders |
| **Avatar / icon / illustration art** | Open | Canvas placeholders; vector launcher icons |
| **Firebase / Analytics** | Skipped | Explicitly deprioritized (Prompt 19) |
| **Play Store signing & ProGuard** | Open | Release build has `minifyEnabled = false` |
| **Production prep pass** | Open | Swap ad IDs, real audio, final art, signing |
| **Voluntary dropout / expulsion events** | Open | `EducationState.expelled` never set by gameplay |
| **Prompt 30 event content audit** | Partial | P20 tagging done; full decade audit not delivered |
| **Prompt 27 formal UI audit** | Partial | Many fixes landed; no formal report |

Unit tests (Prompt 38) and edge-case hardening (Prompt 39) are in place for domain logic.

---

## Documentation index

| Document | Contents |
|----------|----------|
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | Age-up flow, slots/legacy, country cascade, engine API |
| [docs/MANUAL_QA_TEST_PLAN.md](docs/MANUAL_QA_TEST_PLAN.md) | Device QA checklist (Sections A–I, 73 steps) |
| [docs/PERFORMANCE_AUDIT.md](docs/PERFORMANCE_AUDIT.md) | Prompt 43 profiling baseline, optimizations, pass/fail thresholds |
| [docs/REFACTORING_AUDIT.md](docs/REFACTORING_AUDIT.md) | Prompt 41 duplication consolidation notes |
| [app/src/main/assets/data/events/README.md](app/src/main/assets/data/events/README.md) | Event JSON schema & authoring |
| KDoc in `domain/` and `data/model/` | Per-class and per-function reference |

---

## License

Private project — see repository owner for terms.
