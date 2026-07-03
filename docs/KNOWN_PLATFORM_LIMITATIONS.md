# Known Platform Limitations

Constraints that affect Maisha on real devices but **cannot be fully fixed in app code**. Documented so debugging time is not wasted chasing phantom app bugs.

## OEM battery management (critical for this app's market)

Maisha targets budget Android devices common in Kenya and across Africa — itel, Tecno, Infinix, and similar brands. These often ship with aggressive battery skins (HiOS, XOS, etc.) that:

- Restrict background execution more than stock Android
- Auto-sleep apps not on a user whitelist
- Delay or skip WorkManager jobs even when notifications are enabled in system settings

**This is platform/OEM behavior, not an app defect.** WorkManager is still the correct API (better than raw alarms for battery-aware scheduling), but it does not guarantee delivery on every device every day.

### What the app does (within policy)

| Approach | Status |
|----------|--------|
| WorkManager periodic + one-time workers | ✅ Implemented |
| No network/charging constraints on reminder work | ✅ |
| `ExistingPeriodicWorkPolicy.KEEP` on daily schedule | ✅ Avoids resetting the window every cold start |
| 24h repeat + 6h flex window | ✅ Lets the OS batch under Doze |
| Elapsed-time gate (20+ hours since last open) in `DailyReminderWorker` | ✅ Resilient to imprecise firing time |
| General "check back" notification copy (no promised clock time) | ✅ Prompt 44 / 18 |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` / exemption prompts | ❌ **Deliberately not implemented** — Play policy restricts this to ongoing-task apps; a casual game's reminder does not qualify |

### What we do NOT do (deliberate)

- **No battery-optimization exemption flow** — would risk Play policy issues and user distrust for minimal gain on casual reminders.
- **No foreground service** for reminders — inappropriate for optional retention nudges.
- **No exact-alarm scheduling** — would fight Doze and drain battery; misaligned with target hardware.

### If notifications seem "broken" in the field

Before rewriting notification copy or WorkManager code, check:

1. Device manufacturer battery settings (auto-start / background activity / sleeping apps list)
2. Android notification permission for Maisha (Android 13+)
3. In-app Settings → Notifications toggle
4. Whether the user opened the app recently (`lastOpenedTimestamp` suppresses daily reminder if &lt; 20 hours)

Once analytics exist (currently deprioritized), **low notification CTR on budget OEMs should be investigated as platform killing first**, not copy failure first.

## WorkManager timing expectations

- Minimum periodic interval is **15 minutes** (not relevant for our 24h job, but contextual one-time nudges respect hour-scale delays).
- Under **Doze**, work is deferred to maintenance windows — normal.
- **~15-minute jitter** and multi-hour flex are expected; the daily worker does not assume a fixed clock time.

## Ad preloading (battery-related, not background)

`AdManager.preloadInterstitial()` / `preloadRewarded()` run on:

- `MainActivity.onCreate` (foreground entry)
- After an interstitial/rewarded ad is dismissed
- `LifeSummaryScreen` entry (rewarded preload for Second Wind)

There is **no** periodic or background schedule for ad loads.

## Flow collection lifecycle

Long-lived collectors use `viewModelScope` (cancelled when the ViewModel is cleared) or `stateIn(..., SharingStarted.WhileSubscribed(5_000))` in Settings. Composable observation uses `collectAsStateWithLifecycle`. No leaked Room/DataStore collectors were found in the Prompt 46 audit.

---

*Prompt 46 — Battery & Background Execution Compliance. July 2026.*
