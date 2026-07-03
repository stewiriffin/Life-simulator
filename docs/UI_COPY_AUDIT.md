# UI Copy Audit — Prompt 44

Voice standard: **warm, encouraging, lightly playful, never condescending, never manipulative, never somber except where genuinely appropriate (death, serious consequences — and even then, gentle rather than grim).**

Capitalization standard: **Title Case for CTAs, screen titles, and dialog titles**; sentence case for body copy and descriptions.

---

## Category 1: Primary Actions & CTAs

| Resource ID | Before | After | Reasoning |
|-------------|--------|-------|-----------|
| `btn_visit_doctor` | Visit doctor | **Visit Doctor** | Title Case CTA consistency |
| `btn_break_up` | Break Up | **End Relationship** | Warmer; `btn_divorce` when married |
| `btn_age_up` | + Age Up | *(unchanged)* | Canonical term |
| `btn_start_life` | Start Life | *(unchanged)* | Imperative, Title Case |
| `btn_continue_legacy` | Continue Your Legacy | *(unchanged)* | Warm, clear |
| `btn_watch_second_wind` | Watch Ad for a Second Wind | *(unchanged)* | Honest, non-manipulative |

**CTA audit notes:** No "Advance Year" / "Next Year" drift — **Age Up** is the single English term.

---

## Category 2: Confirmation Dialog Copy

| Resource ID | Change | Reasoning |
|-------------|--------|-----------|
| `settings_reset_warning` | Expanded to name all 3 slots + achievements + settings | Unambiguous destructive scope |
| `dialog_confirm_purchase_title` | **Confirm Purchase** | Title Case |
| Dating break-up (P49) | NEUTRAL `ConfirmableActionHost` when not married | Divorce stays immediate |

---

## Category 3: Empty States

| Resource ID | After | Reasoning |
|-------------|-------|-----------|
| `empty_family` | **Your circle starts with you — family and friends will appear as your story grows.** | Invitation, not deficiency |
| `empty_family_title` | **Room to Grow** | Dedicated title |
| `empty_achievements_title` | **Achievements Await** | New string |
| `empty_achievements_message` | **Milestones unlock as you live…** | Invitation to play |
| `empty_career_no_eligible` | **No openings yet… tap Age Up** | Encouraging |
| `empty_assets` | **Nothing in your name yet…** | Warmer framing |
| `empty_event_log` | **No events yet — your story is just getting started.** | Softer absence |

---

## Category 4: Event & Achievement Result Copy

| Resource ID | After |
|-------------|-------|
| `achievement_unlocked_title` | **Achievement Unlocked!** |
| `event_dialog_subtitle` | Life Event *(unchanged)* |

Event JSON `resultText` not re-audited (Prompt 30 scope).

---

## Category 5: Settings & System Copy

| Resource ID | Change |
|-------------|--------|
| `notification_nudge_comeback_title` | **Pick Up Your Story** (was "We miss you") |
| `notification_nudge_comeback_body` | **%1$s's life is waiting right where you left off.** |

---

## Category 6: Error & Failure States

| Resource ID | After |
|-------------|-------|
| `msg_treatment_failed` | **The treatment didn't help this time, or care wasn't affordable…** |
| Ad load failure | *(no user string)* — `AdManager` fails silently |

---

## Consistency Sweep

### Terminology

| Concept | Canonical term (EN) |
|---------|---------------------|
| Year advance | **Age Up** |
| Save container | **Slot** |
| Legacy continuation | **Continue Your Legacy** / **Choose Your Heir** |
| Relationship end (dating) | **End Relationship** |
| Relationship end (married) | **Divorce** |

### Relationship tier labels

| Tier | Range |
|------|-------|
| Estranged | 0–16 |
| Distant | 17–33 |
| Cool | 34–50 |
| Friendly | 51–67 |
| Close | 68–84 |
| Inseparable | 85+ |

All screens route through `relationshipTierLabel()`. Sw `tier_inseparable` bugfix (P49): **"Wasioachanika"**.

---

## Localization flags

| Locale | Coverage |
|--------|----------|
| `values/` (EN) | 100 strings — full |
| `values-sw/` | 100 strings — full |
| `values-fr/`, `-es/`, `-pt/`, `-hi/` | 66 strings — UI chrome only |

fr/es/pt/hi remain **partial overlays** — spot-check with fluent speakers before release.

Wealth achievement descriptions use `%1$s` + `formatMoney()` at display time (P49).

---

## Voice read-through (post-revision)

The revised English set reads as **one consistent warm narrator**: invitations over deficits, honest stakes on destructive actions, no guilt notifications, gentle failure messages.

Residual acceptable items:
1. Crime action cards — factual, not glamorizing
2. "Argue" / "Insult" — blunt labels, intentional
3. Partial locale overlays use localized age-up verbs, not literal "Age Up"

---

*Prompt 44 — UI Copywriting Pass. Updated Prompt 49 (July 2026).*
