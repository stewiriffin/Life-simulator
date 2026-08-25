# Maisha / Life-simulator — AI Continuity Prompt

> **Purpose:** Paste this document (or point an AI agent at it) at the start of any new session so the assistant understands **what Maisha is**, **what has already been built**, **what just shipped**, **what is still missing**, and **how to work in this repo without undoing progress**.
>
> **Last updated:** 2026-08-25 (career ladder + office politics depth)  
> **Repo path:** `/home/dr-rank/Documents/Apps/Life-simulator`  
> **Remote:** `https://github.com/stewiriffin/Life-simulator` (`origin/main`)  
> **Package:** `com.maisha.game`

---

## 1. One-paragraph product summary

**Maisha** (Swahili for *life*) is a single-player Android **life simulation** game in the BitLife / Age Sim genre. The player creates a character, ages **one year at a time**, and makes choices across school, career, relationships, crime, health, finance, politics, leisure, and legacy. The game is **offline-first**: narrative content lives in JSON under `app/src/main/assets/data/events/`, and game rules live in pure Kotlin **domain engines**. It targets mid-range phones (**~360dp width**, **minSdk 26**). The product is intentionally **worldwide** across **15 countries**, not Kenya-only — though the project started as a Kenya prototype and some Kenya-specific events remain behind `restrictedToCountry: "KE"`.

**Design north star:** every year should feel like there is something meaningful to *do* (actions, NPCs, systems), not only a random event card. Recent work has pushed hard on character creation UX, BitLife-style art, depth waves of content, and a full **school life** rewrite with classmates and activities.

---

## 2. How you (the AI) should behave in this repo

1. **Read code before rewriting.** Prefer extending existing engines/screens over inventing parallel systems.
2. **Domain engines stay pure.** No `Context`, Room, Compose, or Android APIs inside `domain/`. Engines take immutable models and return new copies.
3. **UI reads StateFlow only.** ViewModels (`@HiltViewModel`) own orchestration; screens call ViewModel methods.
4. **Small, focused diffs.** Match existing Compose patterns, Maisha theme tokens (`TealPrimary`, `GoldAccent`, `MaishaRadius`, etc.), and `strings.xml` for user-facing copy.
5. **Events are content + gates.** New JSON must be wired in `EventRepository.loadAllFromAssets` and listed in `GlobalContentCoverageTest`.
6. **School NPCs are not family.** Classmates/teachers live on `education.schoolPeople` (`SchoolPerson`), not `Character.family`.
7. **Git:** commit **only when asked**; push **only when asked**; install with `./gradlew :app:installDebug` when asked. Do not force-push, amend others’ commits, or update git config.
8. **Docs:** This continuity file is the primary handoff doc. Older docs under `docs/` were deleted on 2026-08-25; do not assume `ARCHITECTURE.md`, `PROJECT_OVERVIEW.md`, etc. still exist unless recreated.
9. **Country bias:** Prefer catalogs + `{placeholders}` over hardcoding Kenya as the default experience.
10. **Prefer player agency.** When the user asks for “more to do,” prefer actions, NPCs, yearly choices, and UI surfaces — not only dumping more event JSON.

---

## 3. Tech stack

| Technology | Role |
|------------|------|
| **Kotlin 2.0** | App language |
| **Jetpack Compose + Material 3** | UI |
| **MVVM + Hilt** | DI + ViewModels |
| **Room** | Multi-slot character saves (stats + JSON blobs) |
| **DataStore** | Preferences (sound, haptics, language, onboarding, tips) |
| **Kotlinx Serialization** | Event JSON + serialized nested state |
| **WorkManager** | Daily reminders / contextual nudges |
| **Google Mobile Ads** | Banner, interstitial, rewarded (Second Wind) — **test IDs in dev** |
| **JUnit 4** | Domain unit tests under `app/src/test/` |

**Build targets:** `compileSdk` / `targetSdk` **35**, `minSdk` **26**, JDK **17**, AGP ~8.7.

**Useful commands:**

```bash
./gradlew assembleDebug
./gradlew :app:installDebug
./gradlew testDebugUnitTest
./gradlew :app:testDebugUnitTest --tests "com.maisha.game.domain.EducationEngineTest"
```

Primary physical QA devices seen in development: **itel A665L**, **TECNO LI6** (360dp-class Android 13 phones).

---

## 4. Architecture

```
Compose Screens / Tabs
        ↓
  ViewModels (StateFlow)   e.g. LifeViewModel
        ↓
  GameEngine (orchestrates yearly tick + facades)
        ↓
  Feature engines (Education, Career, Relationship, …)
        ↓
  Repositories (Room / DataStore / EventRepository + assets)
```

### Age-up loop (conceptual)

1. Player taps **Age Up** on the Life tab.
2. `GameEngine` advances age, ticks school year / relationships / finance / health / crime / jobs, etc.
3. `EventRepository.getEligibleEvents` filters JSON + system events by age, tags, and character gates.
4. Player may get **NoEvent**, **SingleEvent**, or **MultipleEvents**.
5. Choices apply `statEffects` and specialty fields (GPA, relationships, crime triggers, relocation, university course, etc.).
6. UI shows event dialog, floating stat deltas, celebrations, ads (interstitial every N age-ups), and updates header expression / net worth.

### Persistence

- **Room:** durable per-slot character state.
- **DataStore:** app settings that should survive character resets without schema migrations.
- Nested game objects (education, family, career, assets, school people, etc.) are typically stored as **serialized JSON blobs** inside Room entities — adding fields with **defaults** is preferred so old saves still load without a hard migration when possible.

---

## 5. Project layout

```
app/src/main/java/com/maisha/game/
  domain/           # Pure game engines (rules)
  data/             # Catalogs, repositories, EventRepository
  data/model/       # Character, Person, EducationState, LifeEvent, …
  data/events/      # EventRepository
  data/local/       # Room
  ui/main/          # LifeScreen, CareerScreen, FamilyScreen, ActionsScreen, AssetsScreen, LifeViewModel
  ui/charactercreation/  # Creation flow + stats picker
  ui/avatar/        # AvatarImage, DiceBear, ExpressionResolver
  ui/components/    # Shared Compose widgets
  ui/theme/         # Colors, radius, spacing
  ui/navigation/    # MaishaNavHost
  di/               # Hilt modules
  notifications/    # WorkManager
  feedback/         # Sound / haptics
  ads/              # AdMob wrappers
  util/             # clampStat, formatMoney, etc.

app/src/main/assets/data/events/   # Narrative event JSON packs
app/src/test/java/com/maisha/game/ # Domain + content coverage tests
docs/                              # Continuity / product docs (this file)
```

---

## 6. Main UI map

### Navigation / tabs (`MainTab`)

| Tab | Screen | What the player does |
|-----|--------|----------------------|
| **LIFE** | `LifeTabContent` / Life tab | Age Up, year quests, recap, live status |
| **ACTIONS** | `ActionsScreen` | Categorized action board (leisure, crime, lifestyle, childhood actions, etc.) |
| **FAMILY** | `FamilyScreen` | Family, dating, pets, bond interactions, parties |
| **CAREER** | `CareerScreen` | Jobs, **school** (education + school life + clubs), business, politics, career tracks |
| **ASSETS** | `AssetsScreen` | Homes/cars/etc., investments, living standard, will, portfolio |

Other flows: splash, onboarding, slot picker, character creation → avatar → **stats allocation**, settings, achievements, life summary / share card, arrest/trial dialogs, event dialogs.

### Career → School (important)

School is **not** its own bottom tab. It lives under **Career**, category **SCHOOL**:

1. **Education** card — stage, GPA, study effort, drop out  
2. **School life** card — reputation, classmates/teachers, activities  
3. **School clubs** card — Debate / Football / Drama / Coding / Music with ranks, skill/prestige/fame bars, Light/Normal/Intense practice, rival challenges, letter jacket, officer fundraisers, captain major events, awards/scholarship resume  
4. Career tracks, jobs, business, politics elsewhere in the same screen  

---

## 7. Domain engines inventory

These are the rule brains. Prefer editing the right engine instead of stuffing logic into ViewModels.

| Engine | Responsibility |
|--------|----------------|
| `GameEngine` | Age-up orchestration, facades into other engines |
| `EducationEngine` | Enroll, grades, exams, university, clubs, **school roster + activities**, study effort |
| `CareerEngine` | Jobs, performance, promotions, tracks, military, `requires_job` gates |
| `RelationshipEngine` | Family/dating/friends/enemies, interactions, gates (`requires_spouse`, etc.) |
| `FinanceEngine` | Money, assets, rentals, portfolio tags |
| `CrimeEngine` | Crimes, arrest, incarceration, trial-related rules |
| `HealthEngine` | Illness, doctor visits, lifestyle health effects |
| `BusinessEngine` | Start/sell businesses |
| `PoliticsEngine` | Campaigns, offices, tax policy |
| `RelocationEngine` | Move countries, visas, expat gates |
| `LeisureEngine` | Leisure / lifestyle actions |
| `SkillEngine` | Skills progression |
| `SocialMediaEngine` | Social media loop |
| `AchievementEngine` | Achievements / unlocks |
| `LegacyEngine` | Heir continuation, ancestry |
| `MortalityEngine` | Death checks |
| `MilestoneEngine` | Life milestones |
| `YearQuestEngine` | Yearly quests / streaks |
| `WeeklyChallengeEngine` | Weekly challenges (engine exists; dedicated UI may still be thin) |
| `BucketListEngine` | Bucket list goals |
| `LifeArchetypeEngine` | Life archetypes (Wave 2) |

---

## 8. Feature inventory (what is already built)

### Core loop
- Multi-slot saves (3 slots) with overwrite confirmation  
- Yearly Age Up + event dialogs + event log  
- Floating stat deltas, celebrations (graduation, marriage, milestones)  
- Life summary on death + share card  
- Onboarding + contextual tips  
- Daily reminders / nudges  

### Character creation (recently heavily polished)
1. Country / identity setup  
2. **Avatar** setup — DiceBear faces; simplified / white-minimal avatar page  
3. **Stats picker** — budget allocation with **EXTRA_BUDGET 140**, presets that spend the full budget, sliders, hold-to-repeat controls, BitLife-style emoji icons  
4. Starting stats **feed into engines** (not cosmetic)  

Relevant commits: `c8e955c`, `21b94db`, `0cbf506`, `eaf0adf`, `d811187`, `222589e`, `b010245`

### Family & relationships
- Generated parents/siblings; dating, marriage, children, divorce  
- Multiple interaction types (spend time, gift, travel, argue, etc.)  
- Relationship tiers, annual decay, milestones  
- Friends / enemies; mixed-heritage children  
- Pet care depth on Relationships/Family surfaces  

### Education (classic pipeline + new school life)
- Primary → secondary → university with country-flavored exam names  
- Study effort (slack / normal / hard)  
- Clubs, GPA, dropout / expelled flags  
- **NEW:** school people roster + reputation + yearly school activities (see §10)  

### Career & money
- Country job pools; education gates; performance / promotion / firing  
- Businesses, politics, career tracks (entertainment / pro sports)  
- Assets, upkeep, investments, living standard, portfolio strategies, wills  
- Side hustles / leisure / skills  

### Crime & health
- Crime attempts, arrest, incarceration, lawyer tiers / trial UI  
- Illness rolls, doctor care tiers, chronic illness depth (Wave 3)  

### Wave 3 systems (examples)
Medical/legal tracks, adoption, prenup, expungement, renovation, portfolio, chronic illness, weekly challenges, avatar polish  

### Wave 4 content packs (events-first depth)
Files under `assets/data/events/`:

- `wave4_workplace_events.json`  
- `wave4_hobbies_events.json`  
- `wave4_side_hustle_events.json`  
- `wave4_love_sex_events.json`  
- `wave4_children_events.json`  
- `wave4_crime_events.json`  

Also: `CareerEngine.REQUIRES_JOB_TAG = "requires_job"` + `EventRepository.passesCareerGate`  

Commit: `d043fa7`

### Worldwide / localization
- 15 countries: KE, NG, ZA, EG, US, CA, GB, FR, DE, IN, JP, PH, ID, BR, MX  
- Name pools, jobs, assets, exams, holidays, economy scaling  
- Flavor placeholders in events: `{transportMode}`, `{secondaryExam}`, `{moneyApp}`, etc.  
- UI strings: English + Swahili (`values-sw`); event narrative JSON is English  

### Monetization / polish
- Banner on slot picker; interstitial on age-ups; rewarded Second Wind  
- BitLife-style illustrated raster art / heroes / launcher icon (globe-and-heart)  
- Sound/haptics placeholders still silent `.wav`s in places  

### Legacy
- Continue as adult child heir; ancestry timeline; generation numbering; money split  

---

## 9. Event system (authoring rules)

Events live in `app/src/main/assets/data/events/`. Loaded by `EventRepository`.

### Core packs
`starter_events.json`, `education_events.json`, `career_events.json`, `finance_events.json`, `relationship_events.json`, `general_events.json`, `holiday_events.json`, `midlife_events.json`, `crime_events.json`, `wave3_events.json`, Wave 4 packs, **`school_life_events.json`**

### Schema essentials
- Root: `{ "events": [ ... ] }`  
- Each event: `id`, `minAge`, `maxAge`, `text`, `choices[]`, optional `weight`, `tags`, `restrictedToCountry`  
- Choices: `label`, `resultText`, `statEffects`, plus optional specialty fields (`gpaEffect`, relationship deltas, `triggersCrime`, `universityCourse`, `relocateToCountry`, …)  

### Important tags / gates
- `one_time` — once per life  
- `education` — only while enrolled (PRIMARY / SECONDARY / UNIVERSITY) via `passesEducationGate`  
- `requires_job` — employed gate  
- Relationship requirement tags (`requires_spouse`, `requires_child`, …)  
- `positive` / `negative` — karma weight bias  
- System tags excluded from random pool (`exam_system`, `career_system`, etc.)  

### When adding events
1. Create/update JSON pack  
2. Add load line in `EventRepository.loadAllFromAssets`  
3. Add path to `GlobalContentCoverageTest` event asset list  
4. Use gates so events don’t fire for ineligible characters  

More schema detail: `app/src/main/assets/data/events/README.md`

---

## 10. Latest major feature — School life rewrite (`56091a4`)

This is the **most recent gameplay system** and must not be casually reverted.

### Why it was built
Players asked to **rewrite the school feature** with more things to do and people to interact with. School previously felt thin compared to family/career.

### Models (`data/model/Education.kt`)
- `SchoolRole`: CLASSMATE, BEST_CLASSMATE, BULLY, TEACHER, CRUSH  
- `SchoolPerson`: id, name, role, gender, age, relationshipLevel, subject?, avatarConfig, interactedThisYear  
- `SchoolActivity`: STUDY_GROUP, LIBRARY_STUDY, ASK_TEACHER_HELP, HANG_OUT, CONFRONT_BULLY, SKIP_CLASS, SCHOOL_DANCE, CLUB_PRACTICE, GROUP_PROJECT  
- `EducationState` additions: `schoolPeople`, `schoolReputation`, `academicActionDoneThisYear`, `socialActionDoneThisYear`, `detentionYears` (defaults keep old saves loadable)

### Engine (`EducationEngine`)
- Generate roster on primary enroll, secondary transition, university enroll (`ensureSchoolRoster`)  
- Clear roster on dropout / expulsion / graduation  
- `tickSchoolYear` — age NPCs, decay unattended bonds, reset yearly action flags (called from `GameEngine` age-up before enroll)  
- `availableSchoolActivities` / `performSchoolActivity` → `SchoolActionResult` (Success / Ineligible / AlreadyDone / PersonNotFound)  
- **Limits:** one academic + one social activity per year  
  - Social set includes hang out, confront bully, skip class, school dance  
- Activities can boost GPA, smarts, happiness, reputation, health, and person bond  

### UI wiring
- `LifeViewModel.onPerformSchoolActivity`  
- `LifeScreen` + `MaishaNavHost` pass callback  
- `CareerScreen.SchoolLifeSectionCard` — reputation bar, people list, activity buttons, person picker dialog  

### Content
- `school_life_events.json` — classmate secrets, group projects, crush, bully, detention rumor, club showcase, exam week, transfer student, university office hours, etc.  
- Tagged `education` so they only appear while enrolled  

### Tests
- `EducationEngineTest`: roster on enroll; library study raises GPA and blocks second academic; hang out uses social slot  

### Player-facing path
Create/load life → enroll in school (auto at age 6+) → **Career tab → School** → see people → do activities → Age Up for new year / new flags / events  

---

## 10b. Latest major feature — University & higher education

Structured university majors with tuition funding, student loans, multi-year programs, graduation honors, campus jobs/internships, and a Career-tab dashboard.

### Models
- `UniversityMajor` (CS, Law, Medicine, Business, Communications, Engineering, Nursing) with `programYears` + `careerTrack`
- `UniversityFunding` (`CASH` / `LOAN` / `SCHOLARSHIP`), `GraduationHonors`
- `EducationState`: major/funding/loan/tuition/scholarship/honors + `campusJobDoneThisYear`, `internshipDoneThisYear`, `internshipYearsCompleted`, `pendingCareerTrackOffer`
- `StudentFinance` / `StudentLiabilitySnapshot` in `data/model/Finance.kt`
- Career tracks: `SOFTWARE`, `CORPORATE` added beside MEDICAL / LEGAL / etc.
- `EventChoice.careerTrackStart` for post-grad track offers

### Engines
- `EducationEngine.enrollInUniversity` + eligibility per major; tuition billing each year; honors on graduate
- Campus work-study + major internship (year 2+); graduation career-track Age Up event
- Age-up + KCSE result enrollment events offer major × funding choices
- `FinanceEngine.tickStudentLoan` + `repayStudentLoan`; loans reduce net worth
- `CareerEngine` track gates use majors (CS→SOFTWARE, Law→LEGAL, Medicine→MEDICAL, Business→CORPORATE, Comms→ENTERTAINMENT)

### UI
- **University Dashboard** on Career → School: major, year progress, funding/tuition, campus job, internship, loan repay buttons, locked-major hints, post-grad track CTA
- Wired through `LifeViewModel` → `GameEngine` → engines

### Player-facing path
Finish secondary → enroll (Age Up or Career) with cash/loan/scholarship → campus job / internship during years → graduate with honors → career track offer → repay loans  

---

## 11. Recent commit timeline (high signal)

Newest first:

| Commit | Meaning |
|--------|---------|
| `30b316a` | Career ladder, office politics (PIP/credit/burnout/network), Career dashboard |
| `74ec22c` | University majors, loans, campus jobs, internships, grad track, Career dashboard |
| `c68bf45` | Detention deepen: serve hall, principal appeal, transfer hearing, rebel achievements |
| `57bcd1e` | Misbehavior & detention: rebel activities, expulsion hearing, discipline badge |
| `68a1124` | Clubs: rivalry matches, letter jacket, fame, scout whispers, achievements |
| `40f57ac` | School clubs BitLife depth: ranks, intensity, awards, fundraisers, scholarships |
| `1633d57` | Gamified exams: prep, stress, cheat stakes |
| `5546774` | BitLife-style school person profiles and interactions |
| `56091a4` | School life rewrite: classmates, activities, events, Career UI |
| `d043fa7` | Wave 4 depth events (work, love, crime, family/hobbies/hustles) |
| `b010245` … `eaf0adf` | Stats picker UX (icons, sliders, presets, budget) |
| `21b94db` | Starting stats shape core outcomes |
| `c8e955c` | Avatar simplify + stat allocation flow |
| `afa94bd` | Wave 3 + avatar polish |
| `315c619` | Reduce Kenya-default bias; redesign post-age event card |
| `f29cd6a` / `af77230` | BitLife-style art + official launcher icon |
| `b1f55aa` | Wave 2: clubs, tracks, prison, archetypes |
| `c01684b` | Wave 1 life-stage / UI depth |

Use `git log` / `git show <sha>` for exact diffs when continuing related work.

---

## 12. Known gaps / deferred work (honest backlog)

These are **known unfinished** areas. Do not claim they are done.

### Product depth still thin
- Wave 4 was largely **content + gates**, not full new mechanic UIs for every fantasy (jobs/hobbies/crime/love systems beyond events)  
- New skill / crime **enums and loops** beyond existing types  
- Relationship **stages**, addiction, multi-year story arcs as first-class systems  
- Dedicated polished UI for **weekly challenges** (engine exists)  
- Even more “always something to do” density between age-ups  

### Store / production prep (from historical README)
- Production AdMob IDs (still test units in `AdUnitConfig`)  
- Real sound assets (many silent placeholders)  
- Final avatar/illustration art pass (Canvas + DiceBear + raster mix)  
- Firebase/Analytics deliberately deprioritized historically  
- Release minify / signing / Play Store listing polish  
- Some older education flags (e.g. expulsion set only by certain paths) may still be sparse  

### Docs debt
- On 2026-08-25 the user deleted the previous `docs/` corpus (architecture, QA plan, Play Store drafts, etc.).  
- **This continuity prompt replaces that as the AI onboarding source of truth** until more docs are rewritten on purpose.

---

## 13. Coding conventions specific to Maisha

- User-facing strings → `app/src/main/res/values/strings.xml` (and `values-sw` when localizing)  
- Money formatting → `formatMoney(..., countryCode)`  
- Stat clamping → `clampStat` / `clampGpa` / `clampRelationshipLevel`  
- Event log growth → `EventLogCap.prepend` patterns  
- Prefer `@Composable` private section cards inside tab screens (see `CareerScreen`) matching existing Card / GoldAccent label style  
- Confirm destructive actions with existing confirm dialogs (`ConfirmableActionHost`, etc.)  
- When adding ViewModel actions: update UI state `careerMessage` / snackbars consistently with nearby handlers  
- Keep Kenya-only content behind `restrictedToCountry` or country catalogs  

---

## 14. Suggested next work directions (if user says “continue” without specifics)

In priority order that matches recent player feedback:

1. **University social life** — classmates/office hours depth while enrolled in university  
2. **Turn Wave 4 themes into systems** — hobbies/side hustles/crime/love with dedicated Actions/Career surfaces, not only JSON  
3. **Weekly challenges UI** — surface `WeeklyChallengeEngine` clearly  
4. **Childhood / teen action density** — Actions tab for ages where career is empty  
5. **Production prep** only when asked (ads, audio, store listing)

Always confirm with the user’s latest message if the request is ambiguous.

---

## 15. Quick “start of session” checklist for the AI

1. Read this file.  
2. `git status` + `git log -10 --oneline` to see if HEAD moved.  
3. Locate the engine + screen for the requested feature.  
4. Implement with tests for domain changes.  
5. Wire events into repository + coverage test if adding JSON.  
6. Compile / targeted unit tests.  
7. Install / commit / push **only if the user asks**.  

---

## 16. Copy-paste short briefing (for tiny context windows)

> Maisha is an Android BitLife-style life sim (Compose, Hilt, Room, pure domain engines). Worldwide 15 countries. Latest: career ladders + office politics (performance/stress/boss bonds, PIP/credit/burnout, Career dashboard) (`30b316a`); university majors/loans/campus jobs (`74ec22c`); school life + detention/clubs depth. Prefer agency/UI/systems over event spam. Docs folder was cleared except this continuity prompt. Commit/push/install only on request.

---

*End of AI continuity prompt.*
