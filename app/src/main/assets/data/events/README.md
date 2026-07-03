# Life event content (`assets/data/events/`)

JSON files in this folder define narrative **life events** shown during gameplay. They are loaded at runtime by `EventRepository`, filtered by age/tags/character state, and optionally **flavor-interpolated** for the player's country.

---

## File inventory

| File | Scope / theme |
|------|----------------|
| `starter_events.json` | Age 0–2 newborn/infant moments |
| `education_events.json` | School, exams, study choices (`study_effort` tag) |
| `career_events.json` | Work scenarios (`work_effort`, `career` tags) |
| `finance_events.json` | Money, purchases, assets (`finance` tag) |
| `relationship_events.json` | Family, dating, marriage gates (`relationship`, spouse/child requirements) |
| `general_events.json` | Cross-cutting life moments (largest pool) |
| `holiday_events.json` | Country holiday celebrations (`holiday` tag) |

System-generated events (exam results, promotion, firing, downsizing, relocation offer) are built in code (`EducationEngine`, `CareerEngine`, `RelocationEngine`) and are **not** in these files.

---

## JSON schema

### Root

```json
{
  "events": [ /* LifeEvent[] */ ]
}
```

### `LifeEvent`

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | string | yes | Unique event id. One-time events use stable ids tracked in `triggeredEventIds`. |
| `minAge` | int | yes | Minimum character age (inclusive). |
| `maxAge` | int | yes | Maximum character age (inclusive). |
| `text` | string | yes | Event body shown to player. May contain flavor placeholders (see below). |
| `choices` | array | yes | At least one `EventChoice`. |
| `weight` | int | no (default 1) | Relative weight for `pickRandomEvent`. Higher = more likely. |
| `tags` | string[] | no | Gates and hooks — see Tags section. |
| `restrictedToCountry` | string | no | ISO country code (e.g. `"KE"`). If set, only offered when `character.countryCode` matches. |

### `EventChoice`

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `label` | string | — | Button text |
| `resultText` | string | — | Line prepended to event log on pick |
| `statEffects` | object | `{}` | Keys: `health`, `happiness`, `smarts`, `looks`, `money` (int deltas). Clamped via `Stats.applyEffects`. |
| `gpaEffect` | float | 0 | Added to education GPA (0–4) |
| `siblingRelationshipEffect` | int | 0 | Delta to first sibling's relationship level |
| `familyRelationshipEffect` | int | 0 | Delta to parent relationship |
| `spouseRelationshipEffect` | int | 0 | Delta to spouse relationship |
| `performanceEffect` | int | 0 | Delta to job performance score |
| `conditionEffect` | int | 0 | Delta to asset condition |
| `targetAssetType` | string | null | Asset type name for condition effect (e.g. `HOUSE`) |
| `universityCourse` | string | null | If set and eligible, enrolls in university course |
| `triggersHaveChild` | bool | false | Runs `haveChild` logic |
| `triggersCrime` | string | null | Crime type name: `PICKPOCKET`, `SHOPLIFT`, `FRAUD` |
| `triggersIllnessRoll` | bool | false | Rolls for new illness |
| `doctorCareTier` | string | null | `"private"` or other → doctor visit flow |
| `relocateToCountry` | string | null | ISO code → `RelocationEngine.relocate` |

---

## Tags (common values)

| Tag | Meaning |
|-----|---------|
| `one_time` | Fires once per life; id stored in `triggeredEventIds` |
| `study_effort` | Choice label parsed for study effort (slack/normal/hard) |
| `work_effort` | Choice label parsed for work effort (coast/normal/grind) |
| `exam_system` | Excluded from random pool (handled by EducationEngine) |
| `career_system` | Excluded from random pool (CareerEngine) |
| `relocation_system` | Relocation offer events |
| `finance` | Requires finance threshold (assets or ≥50k money) |
| `relationship` | Relationship-tagged content |
| `requires_spouse` / `requires_married` / `requires_child` / `requires_single` / etc. | Relationship gates |
| `requires_mixed_heritage` / `requires_mixed_heritage_child` | Mixed-country family gates |
| `holiday` | Holiday flavor + cooldown (`lastHolidayAge`) |
| `health` / `crime` | Thematic (filtering/documentation) |

---

## Country flavor placeholders

Use curly-brace tokens in `text`, `label`, or `resultText`. Resolved at runtime by `FlavorInterpolator` from `CountryCatalog.flavorFor(countryCode)`.

| Placeholder | Source field | Example (NG) |
|-------------|--------------|--------------|
| `{transportMode}` | `commonTransportMode` | danfo |
| `{primaryExam}` | `primaryExamName` | Primary School Leaving Certificate |
| `{secondaryExam}` | `secondaryExamName` | WAEC |
| `{moneyApp}` | `popularMoneyAppOrBank` | OPay (fallback: "mobile banking") |
| `{greeting}` | `greetingPhrase` | localized greeting |
| `{holidayName}` | `HolidayFlavor.name` | (varies) |
| `{holidayDescription}` | `HolidayFlavor.approxAgeRelevantDescription` | (varies) |

### Example: new templated general event

```json
{
  "id": "teen_side_hustle_apps",
  "minAge": 14,
  "maxAge": 17,
  "weight": 4,
  "text": "Classmates hustle extra cash selling {moneyApp} airtime after school.",
  "tags": [],
  "choices": [
    {
      "label": "Join them after class",
      "statEffects": { "money": 2000, "smarts": 1 },
      "resultText": "You learned how small commissions add up on {moneyApp}."
    },
    {
      "label": "Focus on homework",
      "statEffects": { "smarts": 2 },
      "resultText": "You stayed in — the {secondaryExam} timetable on the wall kept you honest."
    }
  ]
}
```

No `restrictedToCountry` → all countries see it with localized names.

### Kenya-only events

```json
"restrictedToCountry": "KE"
```

Use for content that cannot be templated (specific institutions). Prefer universal text + placeholders when possible. Many KE scenarios also have `_world` variant events without country restriction.

---

## Authoring checklist

1. Pick the correct JSON file by theme (table above).
2. Ensure `id` is globally unique across all event files.
3. Set `minAge` / `maxAge` to match intended life stage.
4. Use placeholders instead of hardcoding country names in universal events.
5. Add `one_time` tag if the event should not repeat.
6. Run unit tests: `GlobalContentCoverageTest` validates placeholder resolution for all roster countries.
7. Avoid trailing commas in JSON (breaks kotlinx.serialization parse).

---

## Runtime selection (summary)

1. `getEligibleEvents` filters merged pool from all seven files.
2. `pickRandomEvent` returns `null` on empty list (no crash).
3. Weighted random: sum of `weight`, uniform roll; if all weights ≤ 0, picks `random()`.
4. Resolved copy returned to UI; original JSON unchanged on disk.
