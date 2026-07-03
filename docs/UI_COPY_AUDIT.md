# UI Copy Audit — Prompt 44

Voice standard: **warm, encouraging, lightly playful, never condescending, never manipulative, never somber except where genuinely appropriate (death, serious consequences — and even then, gentle rather than grim).**

Capitalization standard chosen: **Title Case for CTAs, screen titles, and dialog titles**; sentence case for body copy and descriptions.

---

## Category 1: Primary Actions & CTAs

| Resource ID | Before | After | Reasoning | Locales to update |
|-------------|--------|-------|-----------|-------------------|
| `btn_visit_doctor` | Visit doctor | **Visit Doctor** | Title Case CTA consistency | sw ✅ |
| `btn_break_up` | Break Up | **End Relationship** | Warmer than “Break Up”; clarity kept; `btn_divorce` still used when married | sw ✅ |
| `btn_age_up` | + Age Up | *(unchanged)* | Canonical term — used consistently | — |
| `btn_start_life` | Start Life | *(unchanged)* | Imperative, Title Case | — |
| `btn_continue_legacy` | Continue Your Legacy | *(unchanged)* | Warm, clear | — |
| `btn_find_date` | Find a Date | *(unchanged)* | Playful, clear | — |
| `btn_propose_marriage` | Propose Marriage | *(unchanged)* | Clear significant action | — |
| `btn_divorce` | Divorce | *(unchanged)* | Direct label for married state — appropriate gravity | — |
| `btn_have_child` | Have a Child | *(unchanged)* | Warm imperative | — |
| `btn_apply` / `btn_buy` / `btn_sell` / `btn_quit_job` | — | *(unchanged)* | Consistent Title Case imperatives | — |
| `btn_watch_second_wind` | Watch Ad for a Second Wind | *(unchanged)* | Honest, non-manipulative (no false urgency) | — |
| `btn_share_my_life` | Share My Life | *(unchanged)* | Clear CTA | — |
| `btn_start_new_life` | Start New Life | *(unchanged)* | Consistent with slot picker | — |
| `btn_nice` | Nice! | *(unchanged)* | Brief celebratory dismiss — fits achievement dialog | — |

**CTA audit notes:** No passive or gerund CTAs found (“Life Starting” only appears as loading state `btn_starting` / `btn_aging`, which is acceptable). No “Advance Year” / “Next Year” drift — **Age Up** is the single English term.

---

## Category 2: Confirmation Dialog Copy

| Resource ID | Before | After | Reasoning | Locales |
|-------------|--------|-------|-----------|---------|
| `settings_reset_all_data` | Reset all data | **Reset All Data** | Title Case; matches destructive severity | — (label only EN casing) |
| `settings_reset_warning` | …all saved lives, achievement progress, and reset settings… | **This permanently deletes all three save slots, every saved life, all achievement progress, and restores settings to defaults. This cannot be undone.** | Unambiguous scope (3 slots + achievements + settings) | sw, fr, es, pt, hi ✅ |
| `dialog_confirm_purchase_title` | Confirm purchase | **Confirm Purchase** | Title Case; neutral severity (matches styling) | — |
| `dialog_crime_*` | — | *(unchanged)* | WARNING severity; copy already states arrest/record risk — distinct from doctor dialog | — |
| `dialog_seek_treatment_*` | — | *(unchanged)* | NEUTRAL severity; care-focused, not alarmist | — |
| `dialog_legacy_confirm_*` | — | *(unchanged)* | Warm continuation tone; slot scope clear | — |
| `confirm_gift_title` / `confirm_travel_title` | — | *(unchanged)* | Light neutral confirmations | — |

**Gap noted (no change — out of scope):** Break up / divorce has no `ConfirmActionDialog`; tap is immediate. Future UX could add confirmation, but that would be a behavior change.

---

## Category 3: Empty States

| Resource ID | Before | After | Reasoning | Locales |
|-------------|--------|-------|-----------|---------|
| `empty_family` | No family members found. | **Your circle starts with you — family and friends will appear as your story grows.** | Invitation, not deficiency | sw ✅ |
| `empty_family_title` | *(used `screen_family`)* | **Room to Grow** | Dedicated encouraging title (wired in `FamilyScreen`) | sw ✅ |
| `empty_achievements_title` | *(misused `screen_achievements`)* | **Achievements Await** | New string; wired in `AchievementsScreen` | sw ✅ |
| `empty_achievements_message` | *(misused `empty_event_log`)* | **Milestones unlock as you live. Tap Age Up, explore, and see what you can earn.** | Invitation to play, not empty scoreboard | sw ✅ |
| `empty_career_no_eligible` | No jobs match… Keep studying or age up. | **No openings yet for your education and age. Keep learning or tap Age Up — something may open up.** | Encouraging; reinforces Age Up term | sw ✅ |
| `empty_assets` | No assets yet. Browse the shop below. | **Nothing in your name yet. Browse the shop below when you're ready to invest.** | Warmer framing | sw ✅ |
| `empty_event_log` | No events yet. | **No events yet — your story is just getting started.** | Softer absence framing | sw ✅ |
| `empty_no_character` | No saved character found. | **No saved life found here.** | “Life” matches player mental model | sw ✅ |
| `empty_actions_title` / `empty_actions_body` | — | *(unchanged)* | Already positive (“healthy and keeping out of trouble”) | — |
| `empty_ancestry_*` | — | *(unchanged)* | Already inviting (“Your story is just beginning”) | — |
| `empty_person_memories` | — | *(unchanged)* | Encourages action (“Spend time together…”) | — |

---

## Category 4: Event & Achievement Result Copy

| Resource ID | Before | After | Reasoning | Locales |
|-------------|--------|-------|-----------|---------|
| `achievement_unlocked_title` | Achievement Unlocked | **Achievement Unlocked!** | Brief celebration; achievement title + description carry specifics | sw ✅ |
| `btn_nice` | Nice! | *(unchanged)* | Single dismiss label — achievement-specific body text is sufficient; varying dismiss per achievement adds little value | — |
| `event_dialog_subtitle` | Life Event | *(unchanged)* | Warm, neutral wrapper; dynamic event text stands alone (no “You chose:” label) | — |

Event JSON `resultText` not re-audited (Prompt 30 scope).

---

## Category 5: Settings & System Copy

| Resource ID | Before | After | Reasoning | Locales |
|-------------|--------|-------|-----------|---------|
| `settings_sound` / `haptics` / `notifications` | — | *(unchanged)* | Bare labels appropriate — toggles are self-explanatory | — |
| `settings_reset_warning` | *(see Category 2)* | Expanded | All slots + achievements + settings | 5 locales ✅ |
| `notification_nudge_comeback_title` | We miss you | **Pick Up Your Story** | Removes subtle guilt/urgency (Prompt 18) | sw ✅ |
| `notification_nudge_comeback_body` | …life is on pause… | **%1$s's life is waiting right where you left off.** | Inviting, not pressuring | sw ✅ |
| `notification_daily_body_1–6` | — | *(unchanged)* | Varied structures; no FOMO language | — |
| `notification_channel_description` | — | *(unchanged)* | “Friendly reminders” — on voice | — |

---

## Category 6: Error & Failure States

| Resource ID | Before | After | Reasoning | Locales |
|-------------|--------|-------|-----------|---------|
| `msg_treatment_failed` | Treatment failed or you couldn't afford care. | **The treatment didn't help this time, or care wasn't affordable. Try again when you can.** | States outcome without blaming player | sw ✅ |
| `msg_job_rejected` | Application rejected. Try again… | *(unchanged)* | Already constructive | — |
| `msg_proposal_rejected` | They said no. The relationship took a hit. | *(unchanged)* | Clear, not player-blaming | — |
| `msg_purchase_insufficient` / `label_insufficient_funds` | — | *(unchanged)* | Neutral factual wording | — |
| `share_error` | Couldn't share… Please try again. | *(unchanged)* | Polite, non-blaming | — |
| Ad load failure | — | *(no user string)* | Confirmed: `AdManager` fails silently — no accidental user-facing copy | — |

Exam failure copy lives in event JSON / engine-generated text (not `strings.xml`).

---

## Category 7: Onboarding & First-Use Copy

| Resource ID | Before | After | Reasoning | Locales |
|-------------|--------|-------|-----------|---------|
| `onboarding_welcome_body` → `onboarding_ready_body` | — | *(unchanged)* | Read as cohesive sequence: tagline → Age Up → choices → ready → world slide | — |
| `onboarding_age_up_body` | Tap Age Up to grow older… | *(unchanged)* | Reinforces canonical CTA term | — |
| `tip_family_dating` / `tip_first_death_achievements` | — | *(unchanged)* | Gentle, helpful tips | — |

---

## Consistency Sweep

### Terminology

| Concept | Canonical term (EN) | Drift found | Resolution |
|---------|---------------------|-------------|------------|
| Year advance | **Age Up** (`btn_age_up`, onboarding, empty states) | None in EN UI | — |
| Save container | **Slot** (slot picker, legacy confirm) | None | — |
| Legacy continuation | **Continue Your Legacy** / **Choose Your Heir** | None | — |
| Relationship end (dating) | **End Relationship** | Was “Break Up” | Standardized |
| Relationship end (married) | **Divorce** | None | — |
| Net worth / money labels | `formatMoney` + “Net Worth” label | “Inheritance” uses engine/log text — consistent patterns | — |

### Relationship tier labels

| Tier | EN (`tier_*`) | Issue | Fix |
|------|---------------|-------|-----|
| Estranged → Inseparable | Used via `relationshipTierLabel()` everywhere | Swahili `tier_inseparable` was **“Tofauti”** (wrong — means “different”) | **“Toshelezi”** in sw |

No paraphrased tier names found in other screens — all route through `relationshipTierLabel`.

### Capitalization

Mixed sentence/title case on a few dialog titles was normalized (`Confirm Purchase`, `Reset All Data`, `Visit Doctor`). Body copy remains sentence case.

---

## Localization flags

| Change | values | values-sw | values-fr | values-es | values-pt | values-hi |
|--------|--------|-----------|-----------|-----------|-----------|-----------|
| All Category 1–4 EN changes | ✅ | ✅ | partial* | partial* | partial* | partial* |
| `settings_reset_warning` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `empty_*` new/changed | ✅ | ✅ | falls back to EN | falls back | falls back | falls back |
| `tier_inseparable` (sw bugfix) | — | ✅ | — | — | — | — |

\*fr/es/pt/hi are **UI-chrome-only overlays** (~40 strings). Unchanged keys fall back to English defaults — acceptable per Prompt 31; full translation pass is out of scope.

**New keys requiring sw update (done):** `empty_family_title`, `empty_achievements_title`, `empty_achievements_message`, `empty_career_no_eligible` (updated).

---

## Voice read-through (post-revision)

The revised English set reads as **one consistent warm narrator**: invitations over deficits, honest stakes on destructive actions, no guilt notifications, gentle failure messages. Residual off-voice items (acceptable / deferred):

1. **Crime action cards** — factual descriptions of illegal acts (tone is neutral, not glamorizing — OK for gameplay context).
2. **“Scold” / “Argue”** — blunt labels for negative interactions; clear over cute (intentional).
3. **Partial locale overlays** — French/Spanish/etc. use localized “age up” verbs (“Vieillir”, “Cumplir años”) rather than literal “Age Up”; correct for localization.
4. **Achievement wealth copy** still says “KSh” in descriptions — currency display issue from Prompt 20, not microcopy voice.

---

*Prompt 44 — UI Copywriting & Microcopy Quality Pass. July 2026.*
