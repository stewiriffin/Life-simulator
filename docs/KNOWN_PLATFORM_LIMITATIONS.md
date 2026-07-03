# Known Platform Limitations

Constraints that affect Maisha on real devices but **cannot be fully fixed in app code**. Documented so debugging time is not wasted chasing phantom app bugs.

## OEM battery management (critical for this app's market)

Maisha targets budget Android devices common in Kenya and across Africa — itel, Tecno, Infinix, and similar brands. These often ship with aggressive battery skins (HiOS, XOS, etc.) that:

- Restrict background execution more than stock Android
- Auto-sleep apps not on a user whitelist
- Delay or skip WorkManager jobs even when notifications are enabled in system settings

**This is platform/OEM behavior, not an app defect.** WorkManager is still the correct API, but it does not guarantee delivery on every device every day.

### What the app does (within policy)

| Approach | Status |
|----------|--------|
| WorkManager periodic + one-time workers | Implemented |
| No network/charging constraints on reminder work | Yes |
| `ExistingPeriodicWorkPolicy.KEEP` on daily schedule | Yes — avoids resetting window every cold start |
| 24h repeat + 6h flex window | Yes — lets OS batch under Doze |
| Elapsed-time gate (20+ hours since last open) in `DailyReminderWorker` | Yes |
| General "check back" notification copy (no promised clock time) | Yes |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` / exemption prompts | **Deliberately not implemented** |

### What we do NOT do (deliberate)

- **No battery-optimization exemption flow** — Play policy restricts this for casual games.
- **No foreground service** for reminders.
- **No exact-alarm scheduling** — would fight Doze.

### If notifications seem "broken" in the field

Before rewriting notification code, check:

1. Device manufacturer battery settings (auto-start / background activity)
2. Android notification permission for Maisha (Android 13+)
3. In-app Settings → Notifications toggle
4. Whether the user opened the app recently (`lastOpenedTimestamp` suppresses daily reminder if < 20 hours)

## WorkManager timing expectations

- Under **Doze**, work is deferred to maintenance windows — normal.
- **~15-minute jitter** and multi-hour flex are expected; the daily worker does not assume a fixed clock time.

## Ad preloading (battery-related, not background)

`AdManager.preloadInterstitial()` / `preloadRewarded()` run on:

- `MainActivity.onCreate` (foreground entry)
- After an interstitial/rewarded ad is dismissed
- `LifeSummaryScreen` entry (rewarded preload for Second Wind)

There is **no** periodic or background schedule for ad loads.

## Flow collection lifecycle

Long-lived collectors use `viewModelScope` or `stateIn(..., SharingStarted.WhileSubscribed(5_000))`. Composable observation uses `collectAsStateWithLifecycle`. No leaked Room/DataStore collectors found in Prompt 46 audit.

---

*Prompt 46 — Battery & Background Execution Compliance. July 2026.*
