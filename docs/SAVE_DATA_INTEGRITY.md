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

## Slot corruption UX

`SlotSummary.isCorrupted` — row exists but core load failed (e.g. invalid gender enum).

- Slot picker shows **Save Data Issue** + **Clear & Start Fresh**
- Other slots unaffected
- `LifeViewModel` / `LifeSummaryViewModel` navigate back to slot picker on corrupted load

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
