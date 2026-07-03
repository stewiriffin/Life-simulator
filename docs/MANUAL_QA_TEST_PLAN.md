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

*Can run standalone after clearing app data.*

- [ ] **A1.** Launch app after fresh install/clear data. Branded splash appears briefly; no white flash or flicker before onboarding.
- [ ] **A2.** Onboarding shows **5 slides** (Welcome → Age Up → Choices → World/Identity → Start Life). Swipe or tap Continue through each; copy matches current features (worldwide countries, legacy, achievements — not Kenya-only wording).
- [ ] **A3.** **Skip** button appears on slides **1–4 only** (not on the final slide). Tap Skip from slide 2 — confirm you still reach character creation (same as finishing).
- [ ] **A4.** Complete onboarding (Skip or final **Start First Life** button). Confirm navigation goes to **`CharacterCreationScreen` for slot 0** — **not** `SlotPickerScreen` on this first-run path.
- [ ] **A5.** Force-close app (swipe away from recents). Relaunch. Confirm **onboarding does not appear**; app opens to **`SlotPickerScreen`**.
- [ ] **A6.** Complete character creation (Section B) and perform **one Age Up**. Confirm **`POST_NOTIFICATIONS` system dialog** appears **after that first age-up**, not during onboarding slides. *(Only on Android 13+; skipped if notifications already granted.)*

---

## Section B: Character Creation

*Requires fresh onboarding path (A) or empty slot via Slot Picker → Start New Life.*

- [ ] **B1.** Name field: enter leading/trailing spaces (e.g. `"  Amina  "`). Confirm saved name is **trimmed** in-game.
- [ ] **B2.** Enter emoji in name (e.g. `"Kamau 🎉"`). Confirm no crash; name displays or is handled sensibly.
- [ ] **B3.** Enter a very long name (paste 50+ characters). Confirm input **caps at 40 characters** (`MAX_NAME_LENGTH`); no crash.
- [ ] **B4.** Tap **Continue** with **empty name**. Confirm error message; blocked from avatar picker.
- [ ] **B5.** Open country selector. Confirm list is **scrollable and searchable** at 360dp; tap a country (e.g. Nigeria). Confirm **flag renders** and selection sticks.
- [ ] **B6.** Tap **Continue** → **`AvatarPickerScreen`**. Cycle **all options**:
  - Skin tones: **8** swatches
  - Hair styles: **8** options (with mini previews)
  - Hair colors: **6** swatches
  - Outfit colors: **8** swatches
  - Facial features: **5** options (if shown)
  - Confirm **live preview** updates immediately for each change.
- [ ] **B7.** On `AvatarPickerScreen`, tap **Start Life** with valid name. Confirm no crash; character saves.
- [ ] **B8.** Confirm landing screen is **`LifeScreen` (slot 0)** with character **age 0**, correct name, country, and avatar. *(First-run skips Slot Picker; returning users see the slot on Slot Picker after back navigation.)*
- [ ] **B9.** Press back from `LifeScreen`, open **Slot Picker**. Confirm slot 0 shows the new life (name, age 0, alive, flag/generation if shown).

**Note:** Country defaults to **Kenya (KE)** if unchanged. There is no "no country" state — validation is on **empty name**, not missing country.

---

## Section C: Core Age-Up Loop (Extended Playthrough)

*Use one dedicated slot; expect 30–60+ minutes. Can pause between subsections.*

### C1 — Childhood & education (ages 0–17)

- [ ] **C10.** Age up repeatedly. At **age 6**, confirm **primary school auto-enrollment** (education stage updates).
- [ ] **C11.** During primary/secondary, confirm at least one **study-effort event** appears (choices referencing slack/normal/hard study — not every year).
- [ ] **C12.** Reach primary completion (~grade 8, **age 13+**). Confirm **country-appropriate primary exam** result event (e.g. KCPE for Kenya, BECE for Nigeria, GCSE for UK).
- [ ] **C13.** If exam passed, at **age 14** confirm **secondary enrollment**. If failed, note re-sit path in event text.
- [ ] **C14.** Confirm avatar **`AgeStage`** visibly progresses: **BABY (0–2)** → **CHILD (3–12)** → **TEEN (13–17)**.
- [ ] **C15.** During childhood, note at least one **positive** expression change (HAPPY) and one **negative** (SAD/ANGRY) from events or stat shifts.

### C2 — Adulthood, career, assets, ads

- [ ] **C16.** Reach **age 17+**. Confirm **secondary exam** event fires at end of secondary (grade 8).
- [ ] **C17.** After secondary graduation path, open **Career** tab. Confirm jobs appear; jobs above education tier show **locked state with reason** (education/age/already employed).
- [ ] **C18.** Apply for an **eligible** job. Confirm hire **or** rejection message (probabilistic). Repeat once to observe both outcomes if possible.
- [ ] **C19.** While employed, age up multiple years. Confirm **work-effort events** appear sometimes (not every year). Observe performance bar changes.
- [ ] **C20.** Continue working until **promotion**, **firing**, or **downsizing** event appears (may take several years; downsizing is random).
- [ ] **C21.** **Assets tab:** purchase an asset you can afford. Confirm cash decreases.
- [ ] **C22.** Age up **3+ years** with the asset. Confirm **annual upkeep** deducts money and **condition bar** degrades.
- [ ] **C23.** Sell the asset. Confirm payout ≈ **current value** (depreciated), not original price.
- [ ] **C24.** Count age-ups. Confirm **interstitial ad** appears on **every 5th age-up** (5th, 10th, 15th…). *(Code: `INTERSTITIAL_EVERY_N_AGE_UPS = 5`.)*
- [ ] **C25.** Trigger a **celebration overlay** (marriage, child, achievement, graduation, or age milestone). On the same age-up that would show an ad, confirm **no interstitial during the celebration**; ad may appear after dismissal (deferred interstitial).
- [ ] **C26.** On any stat-affecting event choice, confirm **floating +/- indicators** appear and **stat bars flash** briefly.

---

## Section D: Relationships (Deep Pass)

*Continue same character or use a slot dedicated to family testing; reach age 18+.*

- [ ] **D27.** Open **Family** → tap a **parent**. Use each interaction: **Spend Time, Gift** (small/medium/large with confirm), **Compliment, Insult, Travel Together** (with confirm), **Ask for Advice, Prank**. Confirm feedback snackbar/message; no crash.
- [ ] **D28.** After significant interactions, open person detail **memories/timeline**. Confirm **`RelationshipMilestone`** entries appear for major moments (e.g. quality time, gift, travel).
- [ ] **D29.** Watch **relationship tier label** as you raise/lower level. Confirm transitions at boundaries:
  - 0–16 Estranged → 17–33 Distant → 34–50 Cool → 51–67 Friendly → 68–84 Close → 85+ Inseparable
- [ ] **D30.** Leave a **sibling** un-interacted for **3+ consecutive age-ups**. Confirm gentle **decay toward 50** (may see tier-drop notice).
- [ ] **D31.** At **18+**, tap **Find a Date**. Confirm **3 prospects** appear.
- [ ] **D32.** Repeat Find a Date several times across lives/sessions. Confirm **~15%** of prospects show a **different country flag** than the player (foreign prospect).
- [ ] **D33.** **Start dating** a prospect. Raise relationship (Spend Time, gifts, etc.) to **≥ 70** (`PROPOSAL_THRESHOLD`). Tap **Propose**. Confirm **accept or reject** outcome; on accept, partner shows married state.
- [ ] **D34.** Confirm **marriage celebration overlay** plays.
- [ ] **D35.** Tap **Have a child**. Confirm **child celebration overlay**; child appears in **Family** list at age 0.
- [ ] **D36.** Age up several times. Confirm **child age increments** yearly.
- [ ] **D37.** *(Cross-country spouse)* If spouse country ≠ player country: confirm child shows **dual heritage flags** (`secondaryCountryCode`). Watch for **mixed-heritage flavor events** over subsequent years.
- [ ] **D38.** Over many age-ups (school/work years), confirm a **new friend** (`FRIEND` relation) may appear (~10% school ages, ~7% work ages). Interact via same detail sheet.
- [ ] **D39.** On a **friend or sibling**, use **Set Up on Date** (if available). Confirm interaction resolves without crash.

---

## Section E: Crime & Health

*Use a separate slot or mid-life character. Crime UI appears at **age 16+** (Actions tab).*

- [ ] **E40.** **Actions tab:** attempt **Pickpocket, Shoplift, Fraud** multiple times each. Observe **success** (money gained) and **Caught** (arrest) outcomes.
- [ ] **E41.** On arrest, confirm **sentence length differs by crime type** (fraud longest; pickpocket can be 0–1 years).
- [ ] **E42.** While **incarcerated**, age up. Confirm **education and career do not advance** (no job income; prison message shown).
- [ ] **E43.** Serve full sentence. Confirm **release log** and return to normal actions.
- [ ] **E44.** After release, apply for a skilled job. Note **lower hire success** with criminal record.
- [ ] **E45.** Age forward until **10+ clean years** since last arrest. Re-apply for jobs; confirm **improved hire rate** vs. fresh release (redemption path).
- [ ] **E46.** Allow health to drop (ignore doctor, stressful choices). Confirm **illness** can appear on age-up.
- [ ] **E47.** **Actions → Health:** treat same condition at **public clinic** vs **private hospital**. Confirm **different costs** and **different success rates** (private more expensive, higher success).
- [ ] **E48.** Leave a condition **untreated for 2+ years**. Confirm **contextual notification nudge** schedules (may appear ~4 hours later if app backgrounded; check notification shade).

---

## Section F: Achievements & Settings

*Can test on any active life; achievements are global.*

- [ ] **F49.** Unlock **≥ 3 achievements** from different categories (e.g. first job, first child, property owner). Each unlock shows **dialog + celebration overlay**.
- [ ] **F50.** Open **Achievements** screen. Confirm **locked** entries show silhouette/hidden state; **unlocked** show full art and unlock state.
- [ ] **F51.** **Settings → Sound OFF.** Age up, trigger events, unlock achievement. Confirm **no sound effects**.
- [ ] **F52.** **Settings → Haptics OFF.** Tap buttons, age up, confirm. Confirm **no vibration**.
- [ ] **F53.** **Settings → Language:** switch to **Swahili**, then try **French, Portuguese, Spanish, Hindi** (all supported). Confirm **UI chrome updates immediately** without app restart (tab labels, buttons, settings).
- [ ] **F54.** **Settings → Notifications OFF.** Confirm scheduled **daily reminder** is cancelled (no notification next day; verify via logcat or waiting).

---

## Section G: Death, Summary & Legacy

*Dedicated slot recommended; legacy test is long.*

- [ ] **G55.** Age character to old age **or** run health to critical until **death** occurs. Confirm death is handled gracefully.
- [ ] **G56.** Confirm **cause-of-death text** is gentle (e.g. "passed away peacefully", not graphic) for all observed causes.
- [ ] **G57.** **`LifeSummaryScreen`:** verify **final age, stats, career, education, family recap, net worth** match last lived year.
- [ ] **G58.** Confirm summary **avatar reflects final `AgeStage`** (senior if elderly).
- [ ] **G59.** Tap **Share My Life**. Preview card shows **avatar, key stats, achievement badges**. Tap share; **system share sheet** opens with **image attached**.
- [ ] **G60.** If **living children age ≥ 16** exist: confirm **Continue Your Legacy** / heir selection appears.
- [ ] **G61.** Select heir. Confirm preview shows **inheritance split** (money ÷ living children) and **family mapping** (surviving parent role, siblings).
- [ ] **G62.** Confirm new character: **inherited money**, **fresh education/career**, **`generationNumber` incremented**, **`ancestryHistory`** includes deceased, family relations remapped (parent/sibling/friends).
- [ ] **G63.** Play **2–3 more generations**. Include at least one **relocation** (accept abroad event) and one **cross-country marriage** with child.
- [ ] **G64.** Open **`AncestryScreen`**. Confirm **multi-generation timeline** with correct names, flags, ages at death, and **relocation arrows** where applicable.

---

## Section H: Multi-Slot & Cross-Session

- [ ] **H65.** Create/fill **all 3 slots** with different characters. Progress each independently. Confirm **no stat/family/asset bleed** between slots.
- [ ] **H66.** With different ages per slot, switch slots via **Slot Picker**. Each resumes correct state.
- [ ] **H67.** **Force-kill** app (swipe from recents) mid-play in slot 1. Relaunch. Confirm **exact state restored** (age, stats, relationships, assets, events pending).
- [ ] **H68.** Repeat force-kill check for **slots 0 and 2** briefly.
- [ ] **H69.** Let character in **one slot die**. Slot Picker shows **View Summary / Continue Legacy** for dead slot; other slots still show **Continue** normally.
- [ ] **H70.** Unlock an achievement in **slot A**. Switch to **slot B**, open Achievements. Confirm same achievement shows **unlocked** (global scope, not per-slot).

---

## Section I: Worldwide Content Spot-Check

- [ ] **I71.** Create character A in **Kenya**. Create character B in **Nigeria** (or any second country). Confirm differences:
  - Name pool (random name button)
  - **Currency symbol** in money labels (KSh vs ₦ etc.)
  - **Exam names** in education events
  - Job/asset catalog flavor (country-scoped shop)
  - **Holiday events** with local names (not Kenya holidays on Nigerian life)
- [ ] **I72.** Scan event text in both lives for **unresolved template placeholders** (`{examName}`, `{transportMode}`, `{moneyApp}`, `{holidayName}`). None should appear as raw braces.
- [ ] **I73.** *(Optional)* All **15 roster countries** currently have researched `CountryFlavor`. If testing generic fallback in a future build, expect generic exam/transport labels — **not** raw `{placeholder}` strings.

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
| Ad vs celebration | Ad deferred/suppressed during celebration overlays |
| Notification permission | After **first** age-up (not during onboarding) |
| Achievements | Global across all slots |
| Name max length | 40 characters |
| Foreign dating prospect | ~15% chance |
| Clean-record hire bonus | Penalty reduced at 5 / 10 / 15 clean years |

---

## Bug Report Template

Copy one block per issue found during testing.

```
Step #: 
Section: 
Device: itel A665L / emulator (360dp, Android __)

Expected:
(What the test plan says should happen)

Actual:
(What you observed — be specific: screen, age, slot #)

Severity:
[ ] Blocker — crash, data loss, cannot progress
[ ] Major — feature broken, wrong logic, missing UI
[ ] Minor — cosmetic, typo, edge case workaround exists
[ ] Trivial — polish only

Repro steps:
1. 
2. 
3. 

Screenshot/recording: (attach if possible)
Slot #: 
Character age/country: 
Build version/git commit: 
```

---

*Document version: Prompt 42 — aligned to codebase as of commit `a42230a`.*
