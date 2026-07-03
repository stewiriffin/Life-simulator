# Play Store Data Safety — Working Notes

**Snapshot date:** July 3, 2026

> **Manual steps before submission:** (1) Host [PRIVACY_POLICY.md](PRIVACY_POLICY.md) at a public HTTPS URL. (2) Answer the under-13 / child-directed audience question. (3) Configure AdMob audience & ad tagging in AdMob console. (4) Fill Play Console **Data safety** using this doc as a draft.

---

## Part 1: Complete data inventory (honest)

### Collected / processed via third parties (disclose)

| Data | Collector | How | Purpose | Leaves device? |
|------|-----------|-----|---------|----------------|
| Advertising ID (GAID) | Google AdMob SDK | Automatic when ads load/show | Ad serving, frequency, measurement | Yes → Google |
| Device identifiers / app instance info | Google AdMob / Play services | SDK | Ad delivery, fraud prevention | Yes → Google |
| IP address | Google AdMob SDK | Network requests for ads | Approximate location inference, ad targeting | Yes → Google |
| Ad interactions (impressions, clicks) | Google AdMob SDK | Automatic | Ad performance, billing | Yes → Google |

**Permissions in manifest:** `INTERNET`, `com.google.android.gms.permission.AD_ID`

**Maisha does not:** send gameplay saves, character names, or settings to any first-party server.

### Stored locally only

| Data | Storage | Transmitted? |
|------|---------|--------------|
| Character / slot saves (Room) | On-device SQLite | **No** |
| Achievement progress (Room) | On-device | **No** |
| Settings (DataStore) | On-device | **No** |
| Share card bitmap | Temporary, user-initiated share | **Only if user shares via another app** |

### Not collected (confirm for form + policy)

| Category | Status |
|----------|--------|
| User accounts / email / phone | **Not collected** |
| Firebase Analytics | **Not integrated** (Prompt 19 deprioritized) |
| Firebase Crashlytics | **Not integrated** |
| Precise location (GPS) | **Not collected by app** |
| Real financial / health data | **Not collected** — in-game stats are fictional |
| Contacts, SMS, calendar | **Not collected** |

### Notifications (Prompt 18)

- WorkManager schedules local workers only.
- No notification payload sent to external servers.
- `POST_NOTIFICATIONS` permission (Android 13+) — user-granted.

### Code audit confirmation (July 2026)

```
grep: firebase, Crashlytics, Analytics → no matches in app source
grep: AD_ID → AndroidManifest.xml (AdMob)
dependencies: play-services-ads only (no firebase-bom)
```

---

## Part 2: Play Console Data safety mapping (draft answers)

### Does your app collect or share user data?

**Yes** — primarily because **AdMob** collects/shares data with Google for advertising.

### Suggested declarations for AdMob-related categories

| Play Console category (typical) | Collected? | Shared? | Purpose |
|---------------------------------|------------|---------|---------|
| **Device or other IDs** | Yes | Yes (with Google) | Advertising or marketing |
| **Approximate location** | Yes (via AdMob/IP) | Yes (with Google) | Advertising or marketing |
| **App interactions** (ad views/clicks) | Yes | Yes (with Google) | Advertising |
| **Personal info** (name, email) | **No** | — | — |
| **Financial info** | **No** (real) | — | — |
| **Diagnostics** | No (no Crashlytics) | — | — |

### Encryption

- Data in transit: HTTPS for ad requests (AdMob).
- Data at rest: Android app sandbox; no separate encryption layer on Room DB.

### Deletion request

- No account system → users delete via **in-app Reset All Data** or uninstall.
- AdMob data deletion → subject to Google's processes.

### Privacy policy URL

- Required on store listing **and** in-app (Settings → Privacy Policy).
- Set `PRIVACY_POLICY_URL` in `app/build.gradle.kts` after hosting.

---

## Part 3: AdMob console checklist (manual)

- [ ] Confirm production ad units vs test IDs in release builds
- [ ] Set **child-directed** / **under age of consent** tags per audience decision
- [ ] If child-directed: enable appropriate ad formats / non-personalized ads
- [ ] Complete AdMob **EU consent** / UMP if targeting EEA

---

## Part 4: Children's audience — decision required

Before finalizing store listing and AdMob settings, answer:

1. **Target age rating** on Play (e.g. Everyone 10+, Teen)?
2. **Is the app directed at children under 13?**
3. If yes → restricted ads configuration mandatory.

---

## Part 5: When to revisit this document

- Adding Firebase Analytics / Crashlytics (Prompt 19)
- Adding login, cloud save, or social features
- New third-party SDKs
- Google updates Data safety form or AdMob disclosure requirements

---

*Prompt 47 — Privacy Policy & Data Handling Documentation. Not legal advice.*
