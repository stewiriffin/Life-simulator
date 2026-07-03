# Maisha Life Simulator — Manual QA Test Plan

**Purpose:** End-to-end verification on a real device. This is a checklist to execute, not pre-filled results.

**Target device:** itel A665L (or equivalent: **360dp width**, **Android 13+**)

**Build:** Debug or release APK with test ad units (production ads not configured yet).

**Before you start:**
- [ ] Install fresh build OR **Settings → Apps → Maisha → Storage → Clear data**
- [ ] Enable **Developer options → Don't keep activities** OFF (normal app-kill testing in Section H)
- [ ] Have a notepad or the Bug Report Template at the bottom ready
- [ ] Optional: second device language set to Swahili to spot-check locale fallbacks

**How to use:** Work section by section. Each step is numbered for bug reports ("failed at step 27"). Check `[ ]` when passed; leave blank or note "FAIL" with step #.

---

## Section A: Fresh Install & Onboarding

- [ ] **A1.** Launch app after fresh install/clear data. Branded splash appears briefly; no white flash or flicker before onboarding.
- [ ] **A2.** Onboarding shows **5 slides** (Welcome → Age Up → Choices → World/Identity → Start Life). Copy matches current features (worldwide countries, legacy, achievements — not Kenya-only wording).
- [ ] **A3.** **Skip** button appears on slides **1–4 only** (not on the final slide). Tap Skip from slide 2 — confirm you still reach character creation.
- [ ] **A4.** Complete onboarding. Confirm navigation goes to **`CharacterCreationScreen` for slot 0** — **not** `SlotPickerScreen` on first-run path.
- [ ] **A5.** Force-close app. Relaunch. Confirm **onboarding does not appear**; app opens to **`SlotPickerScreen`**.
- [ ] **A6.** Complete character creation and perform **one Age Up**. Confirm **`POST_NOTIFICATIONS` system dialog** appears **after that first age-up**, not during onboarding. *(Android 13+ only.)*

---

## Section B: Character Creation

- [ ] **B1.** Name field: enter leading/trailing spaces. Confirm saved name is **trimmed** in-game.
- [ ] **B2.** Enter emoji in name. Confirm no crash.
- [ ] **B3.** Enter a very long name (50+ characters). Confirm input **caps at 40 characters**; no crash.
- [ ] **B4.** Tap **Continue** with **empty name**. Confirm error message; blocked from avatar picker.
- [ ] **B5.** Open country selector. Confirm list is **scrollable and searchable** at 360dp; tap a country (e.g. Nigeria). Confirm **flag renders**.
- [ ] **B6.** On **`AvatarPickerScreen`**, cycle all options: 8 skin tones, 8 hair styles, 6 hair colors, 8 outfit colors, 5 facial features. Confirm **live preview** updates.
- [ ] **B7.** Tap **Start Life**. Confirm no crash; character saves.
- [ ] **B8.** Confirm landing screen is **`LifeScreen` (slot 0)** with character **age 0**, correct name, country, and avatar.
- [ ] **B9.** Press back, open **Slot Picker**. Confirm slot 0 shows the new life.

**Note:** Country defaults to **Kenya (KE)** if unchanged.

---

## Section C: Core Age-Up Loop (Extended Playthrough)

### C1 — Childhood & education (ages 0–17)

- [ ] **C10.** Age up repeatedly. At **age 6**, confirm **primary school auto-enrollment**.
- [ ] **C11.** During primary/secondary, confirm at least one **study-effort event** appears.
- [ ] **C12.** Reach primary completion (~grade 8, **age 13+**). Confirm **country-appropriate primary exam** result event.
- [ ] **C13.** If exam passed, at **age 14** confirm **secondary enrollment**.
- [ ] **C14.** Confirm avatar **`AgeStage`** progresses: **BABY (0–2)** → **CHILD (3–12)** → **TEEN (13–17)**.
- [ ] **C15.** Note at least one **positive** expression change (HAPPY) and one **negative** (SAD/ANGRY).

### C2 — Adulthood, career, assets, ads

- [ ] **C16.** Reach **age 17+**. Confirm **secondary exam** event fires at end of secondary.
- [ ] **C17.** Open **Career** tab. Confirm jobs appear; locked jobs show **reason**.
- [ ] **C18.** Apply for an **eligible** job. Confirm hire **or** rejection message.
- [ ] **C19.** While employed, age up. Confirm **work-effort events** appear sometimes.
- [ ] **C20.** Continue until **promotion**, **firing**, or **downsizing** event appears.
- [ ] **C21.** **Assets tab:** purchase an asset. Confirm cash decreases.
- [ ] **C22.** Age up **3+ years** with asset. Confirm **upkeep** and **condition degradation**.
- [ ] **C23.** Sell the asset. Confirm payout reflects **depreciated value**.
- [ ] **C24.** Confirm **interstitial ad** on **every 5th age-up** (5th, 10th, 15th…).
- [ ] **C25.** Trigger a **celebration overlay**. Confirm **no interstitial during celebration**; ad may appear after dismissal.
- [ ] **C26.** On stat-affecting event choice, confirm **floating +/- indicators** and **stat bar flash**.

---

## Section D: Relationships (Deep Pass)

- [ ] **D27.** Open **Family** → tap a **parent**. Use each interaction type. Confirm feedback; no crash.
- [ ] **D28.** After significant interactions, open **memories/timeline**. Confirm **`RelationshipMilestone`** entries appear.
- [ ] **D29.** Watch **relationship tier label** at boundaries: Estranged (0–16) → Distant → Cool → Friendly → Close → Inseparable (85+).
- [ ] **D30.** Leave a **sibling** un-interacted for **3+ age-ups**. Confirm gentle **decay toward 50**.
- [ ] **D31.** At **18+**, tap **Find a Date**. Confirm **3 prospects** appear.
- [ ] **D32.** Repeat Find a Date. Confirm **~15%** foreign-country prospects.
- [ ] **D33.** **Start dating**. Raise relationship to **≥ 70**. Tap **Propose**. Confirm accept/reject outcome.
- [ ] **D34.** Confirm **marriage celebration overlay**.
- [ ] **D35.** Tap **Have a child**. Confirm **child celebration**; child at age 0 in Family list.
- [ ] **D36.** Age up. Confirm **child age increments** yearly.
- [ ] **D37.** Cross-country spouse: confirm child shows **dual heritage flags**.
- [ ] **D38.** Over many age-ups, confirm a **new friend** may appear (~10% school, ~7% work).
- [ ] **D39.** Use **Set Up on Date** on friend/sibling. Confirm resolves without crash.

---

## Section E: Crime & Health

- [ ] **E40.** **Actions tab:** attempt **Pickpocket, Shoplift, Fraud**. Observe success and **Caught** outcomes.
- [ ] **E41.** On arrest, confirm **sentence length differs by crime type**.
- [ ] **E42.** While **incarcerated**, age up. Confirm **education and career do not advance**.
- [ ] **E43.** Serve full sentence. Confirm **release** and normal actions return.
- [ ] **E44.** After release, apply for skilled job. Note **lower hire success** with record.
- [ ] **E45.** After **10+ clean years**, re-apply. Confirm **improved hire rate**.
- [ ] **E46.** Allow health to drop. Confirm **illness** can appear.
- [ ] **E47.** Treat at **public clinic** vs **private hospital**. Confirm **different costs and success rates**.
- [ ] **E48.** Leave condition **untreated 2+ years**. Confirm **contextual notification nudge** (~4h after backgrounding).

---

## Section F: Achievements & Settings

- [ ] **F49.** Unlock **≥ 3 achievements** from different categories. Each shows **dialog + celebration**.
- [ ] **F50.** Open **Achievements** screen. Locked vs unlocked states correct.
- [ ] **F51.** **Sound OFF.** Confirm **no sound effects**.
- [ ] **F52.** **Haptics OFF.** Confirm **no vibration**.
- [ ] **F53.** **Language:** switch to **Swahili**, then **French, Portuguese, Spanish, Hindi**. Confirm **UI chrome updates** without restart.
- [ ] **F54.** **Notifications OFF.** Confirm **daily reminder** cancelled.

---

## Section G: Death, Summary & Legacy

- [ ] **G55.** Age to death or critical health. Confirm graceful handling.
- [ ] **G56.** Confirm **gentle cause-of-death text**.
- [ ] **G57.** **`LifeSummaryScreen`:** verify recap matches last lived year.
- [ ] **G58.** Summary **avatar reflects final `AgeStage`**.
- [ ] **G59.** Tap **Share My Life**. System share sheet with **image attached**.
- [ ] **G60.** If **living children age ≥ 16**: confirm **Continue Your Legacy** appears.
- [ ] **G61.** Select heir. Confirm **inheritance split** and **family mapping** preview.
- [ ] **G62.** Confirm new character: **inherited money**, **fresh career**, **`generationNumber` incremented**, **`ancestryHistory`** includes deceased.
- [ ] **G63.** Play **2–3 more generations**. Include **relocation** and **cross-country marriage**.
- [ ] **G64.** Open **`AncestryScreen`**. Confirm **multi-generation timeline** with flags and relocation arrows.

---

## Section H: Multi-Slot & Cross-Session

- [ ] **H65.** Fill **all 3 slots**. Confirm **no bleed** between slots.
- [ ] **H66.** Switch slots via **Slot Picker**. Each resumes correct state.
- [ ] **H67.** **Force-kill** mid-play in slot 1. Relaunch. Confirm **exact state restored**.
- [ ] **H68.** Repeat force-kill for **slots 0 and 2**.
- [ ] **H69.** Let character die in one slot. Picker shows correct dead-slot actions; other slots normal.
- [ ] **H70.** Unlock achievement in **slot A**. Switch to **slot B**. Same achievement **unlocked** (global).

---

## Section I: Worldwide Content Spot-Check

- [ ] **I71.** Create character in **Kenya** and **Nigeria**. Confirm differences: names, currency, exams, jobs/assets, holidays.
- [ ] **I72.** Scan event text for **unresolved placeholders** (`{secondaryExam}`, `{transportMode}`, etc.). None as raw braces.
- [ ] **I73.** All **15 roster countries** have researched `CountryFlavor` in `CountryCatalog.kt`.

---

## Section J: Data integrity spot-checks (P45/P52/P54)

- [ ] **J74.** Corrupt one slot's `familyJson` via adb/DB browser (dev only). Slot loads with empty family; stats intact; slot not marked fully corrupted unless core fields fail.
- [ ] **J75.** Simulate DB unavailable (dev build hook or corrupted DB file). Confirm **full-screen DatabaseUnavailableScreen** — not three empty slots.
- [ ] **J76.** Play 30+ years with heavy family interaction. Re-open person memories — list stays bounded (max 25 milestones per person on save; UI shows 8 + hint).

---

## Quick Reference (expected values)

| Topic | Expected behavior |
|-------|-------------------|
| Primary enrollment | Age 6 |
| Secondary enrollment | Age 14 after primary exam pass |
| Primary exam trigger | Age 13+, final primary grade |
| Secondary exam trigger | Age 17+, final secondary grade |
| Job applications | Age 18+, secondary+ education |
| Dating | Age 18+ |
| Proposal threshold | Relationship level 70 |
| Legacy heir minimum age | 16 |
| Interstitial ads | Every **5th** age-up |
| Notification permission | After **first** age-up |
| Achievements | Global across all slots |
| Name max length | 40 characters |
| Event log cap | 150 lines |
| Ancestry history cap | 25 entries |
| Milestones per person cap | 25 entries |

---

## Bug Report Template

```
Step #: 
Section: 
Device: itel A665L / emulator (360dp, Android __)

Expected:
Actual:

Severity:
[ ] Blocker  [ ] Major  [ ] Minor  [ ] Trivial

Repro steps:
1. 
2. 
3. 

Slot #: 
Character age/country: 
Build version/git commit: 
```

---

*Document version: Prompt 42 — updated through Prompt 54 (July 2026).*
