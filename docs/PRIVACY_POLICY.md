# Maisha — Privacy Policy

**Last updated:** July 3, 2026

> **Developer note:** This file is the source text for your privacy policy. Google Play requires a **publicly hosted URL** (not a GitHub repo link alone). Host this content at a stable HTTPS URL and set that URL in `app/build.gradle.kts` → `PRIVACY_POLICY_URL` before release.

---

## Introduction

Maisha ("the app," "we") is a single-player life simulation game for Android. This policy explains what information is involved when you use the app, what stays on your device, and what third parties may process.

We do not operate user accounts. You do not sign in with email, phone, or social login.

---

## What data the app itself stores (on your device only)

The following is saved **locally on your phone** using Android Room and DataStore. It is **not transmitted to our servers** — we do not run backend servers for gameplay saves.

| Data | Purpose | Where |
|------|---------|--------|
| Character saves (name, age, stats, family, career, assets, event log, etc.) | Gameplay across up to three save slots | Device storage (Room database) |
| Achievement progress | Unlocked achievements (app-wide, not per slot) | Device storage (Room database) |
| Settings (sound, haptics, notifications, language, onboarding tips seen) | Your preferences | Device storage (DataStore) |

**Fictional game data:** In-game money, health, relationships, and similar values describe a **fictional character**, not your real finances or medical information.

**Deletion:** Uninstalling the app removes local data. You can also delete all saves and reset settings using **Settings → Reset All Data** inside the app.

---

## What data we do NOT collect directly

The app's own code does **not** collect:

- Email, phone number, or account credentials (no login system)
- Your real name, address, or government ID
- Precise GPS location
- Real health or financial records
- Contacts, photos, or files from your device (except when **you** choose to share a life-summary image via the system share sheet)

---

## Third-party advertising (Google AdMob)

The app displays ads through **Google AdMob** (Google Mobile Ads SDK). When ads are shown or loaded, **Google** may collect and process information according to [Google's Privacy Policy](https://policies.google.com/privacy) and [Google's advertising policies](https://policies.google.com/technologies/ads).

Typical categories processed by the AdMob SDK (not by Maisha's own servers) include:

- **Device or other identifiers** (e.g. advertising ID on supported devices)
- **IP address** (may be used to infer approximate location)
- **Ad interaction data** (e.g. impressions, clicks) for ad serving and measurement

We use test ad unit IDs in debug builds; production builds use Google's ad network as configured in the AdMob console.

**Your choices:** You can reset or limit ad personalization in your device **Google → Ads** settings.

The app declares the `AD_ID` permission because the AdMob SDK may access the advertising identifier where permitted by device and policy settings.

---

## Notifications

Optional local reminders use **Android WorkManager** on your device. No notification content or schedule is sent to our servers. Enabling notifications requires Android's notification permission (Android 13+).

---

## Sharing your life summary

If you tap **Share My Life**, the app generates an image on your device and opens the **system share sheet**. You choose where (if anywhere) to send it. We do not receive or store shared images.

---

## Internet permission

The app uses the internet connection to **load and display ads** through AdMob. Gameplay saves do not require cloud sync.

---

## Children's privacy

**Action required before Play Store submission:** You must decide whether Maisha is directed at children under 13. That choice affects Google Play age ratings, AdMob child-directed treatment, and whether ads must be non-personalized.

---

## Data retention

- **On-device game data:** Retained until you delete it, reset all data in Settings, or uninstall the app.
- **AdMob / Google:** Governed by Google's policies; not controlled by Maisha.

---

## Changes to this policy

We may update this policy when the app changes (e.g. new analytics, new third-party SDKs). The "Last updated" date at the top will change.

---

## Contact

**Replace before publication:**

- **Developer:** [Your name or studio name]
- **Email:** [your-contact@example.com]

For questions about **Google's** use of data in ads, see [Google's privacy policy](https://policies.google.com/privacy).

---

*Maisha Life Simulator — privacy policy draft (Prompt 47). Not legal advice.*
