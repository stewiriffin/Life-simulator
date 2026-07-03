# Save Data Integrity — Prompt 45

Hardening against corrupted or unreadable persisted data. No backup/restore system — confirmation dialogs remain the only safety net for destructive actions (Reset All Data, slot overwrite, legacy).

## Room policy

- **No** `fallbackToDestructiveMigration()` — see `MaishaDatabaseFactory.kt`
- All schema changes use explicit migrations in `DatabaseMigrations.kt`
- `DatabaseHealth` catches open failures and sets `isAvailable = false` (logged via `Log.e`)

## JSON blob resilience

`SerializationUtils.safeDeserialize<T>()` in `util/SerializationUtils.kt` — used by `CharacterSaveMapper` for every JSON column:

- `eventLog`, `triggeredEventIds`, `family`, `education`, `career`, `assets`
- `criminalRecord`, `activeConditions`, `avatarConfig`
- `relocationHistory`, `ancestryHistory`

Malformed blobs log `Log.e` and fall back to empty/default for **that field only**.

## Bounded persisted lists (P54 audit)

| Field | Location | Cap | Applied |
|-------|----------|-----|---------|
| `eventLog` | `Character` | 150 (`EventLogCap`) | On prepend/append + `CharacterRepository.saveGame` |
| `ancestryHistory` | `Character` | 25 (`AncestryHistoryCap`) | `LegacyEngine.createLegacyCharacter` + save |
| `Person.milestones` | nested in `family` JSON | **25 per person** (`RelationshipMilestoneCap`) | `RelationshipEngine` on append + save |

### `Person.milestones` — growth assessment (P54)

**Before P54:** Uncapped. `SerializationUtils.safeDeserialize` for `familyJson` does not trim nested lists.

**Recording rules (significance filter — Prompt 21):** Only major interactions append milestones (`ARGUE`, `INSULT`, `TRAVEL_TOGETHER`, `SET_UP_ON_DATE`, `GIFT` medium/large). `QUALITY_TIME` and `LEGACY_CONTINUED` are once per person. Lifecycle adds `STARTED_DATING` / `MARRIED` once each.

**Realistic worst case (uncapped):** A player repeating milestone-qualifying interactions with the same person every year for ~60 active years could accumulate **~3 milestones/year** (e.g. argue + travel + large gift) → **~180/person**. With ~10 family members (parents, siblings, spouse, children, friends) → **~1,800** milestone objects in one save. Legacy carry-over preserves `Person` rows (siblings/friends), so counts **compound across generations** on inherited members — same risk profile as pre-cap `ancestryHistory`.

**Decision:** Cap at **25 per person** (keeps newest; UI already shows 8 with “earlier memories” hint). Slower than `eventLog` growth but same nested, multi-generation shape — worth bounding on save.

### `::DEATH:` marker robustness (P54)

`EventLogCap` uses **`startsWith("::DEATH:")`** on whole log lines only — not substring search. `MortalityEngine` writes markers as dedicated log lines (`::DEATH:CAUSE::flavor`). Ordinary flavor text containing `::DEATH:` mid-string is **not** protected and is trimmed like any other line. Case-sensitive; collision with player-facing event copy is implausible.

## Slot corruption UX

`SlotSummary.isCorrupted` — row exists but core load failed (e.g. invalid gender enum).

- Slot picker shows **Save Data Issue** + **Clear & Start Fresh**
- Other slots unaffected
- `LifeViewModel` / `LifeSummaryViewModel` navigate back to slot picker on corrupted load

## Whole-database unavailable UX (P52)

When `DatabaseHealth.isAvailable = false` (Room failed to open — distinct from per-slot `isCorrupted`):

- `SlotPickerViewModel` sets `isDatabaseUnavailable = true` (does not collect slot flow)
- `SlotPickerScreen` shows a **full-screen** `DatabaseUnavailableScreen` instead of three empty slot cards (which would misleadingly look like “no saves yet”)
- **Settings remains reachable** via **Open Settings** — `SettingsRepository` uses DataStore, not Room
- Recovery path: **Reset All Data** in Settings (honest guidance; no in-app DB reopen without process restart)

## DataStore

`SettingsRepository.preferencesFlow()` uses `.catch { }` + `emptyPreferences()` defaults.

## Tests

- `SerializationUtilsTest`
- `CharacterSaveMapperTest` (malformed family JSON, invalid gender)
- `CharacterRepositoryCorruptionTest` (database unavailable)

## Manual verification

1. Corrupt `familyJson` in a save row → character loads with empty family, stats intact
2. Corrupt core row (invalid `gender`) → slot shows Save Data Issue; clear restores slot

*July 2026*
