# Figma AI Prompt — Maisha Life Simulator UI
# Source of truth: `App design assistance.zip` (Figma Make prototype in `.ui-ref/`)

Paste the block below into Figma AI (or FigJam / Make Design).

---

## PROMPT (copy from here)

**Role:** You are a principal product designer specializing in narrative life-simulation mobile games with premium worldwide storytelling craft. Design / refine the UI for **Maisha**, matching the shipped Figma Make reference (`App.tsx` in the design zip).

### Product essence
- **Name:** Maisha (Swahili for *life*) — brand hero on splash and slot picker.
- **World:** Global 15-country roster; local flavor (currency, exams, transport) — never Kenya-only chrome.
- **Core verb:** **Live Another Year** (Age Up). Green primary CTA.
- **Faces:** DiceBear Lorelei only — stable seeds per person.
- **Platform:** Android phone, **360dp width**, thumb-zone primary CTA.

### Visual system (from Make prototype — non-negotiable)
- **Hybrid surfaces:** Cream game loop `#F5F4F0` / `#F2F2F7` + white cards; **navy gradient heroes** `#0B1628 → #152238 → #1E2F4A`.
- **Primary green** `#34C759` for Age Up, active bottom nav, positive chips.
- **Gold** `#F4B942` for money / dynasty prestige accents.
- **Coral** `#E85D5D` for danger / negative log tones.
- **Stat colors:** Health `#4CAF79`, Happy `#F4B942`, Smarts `#4B8EF0`, Looks `#A78BFA`.
- **Typography:** Display = Fraunces (or soft serif); Body = DM Sans. Not Inter/Roboto as display.
- **Radii:** Cards 16, Age Up pill 18, sheets 20, avatars ~20.
- **Event log:** Journal strips — age header + hairline + tone dot. No card spam in the feed.
- **Bottom nav:** White bar, green active icon + top indicator pill. Tabs: Life · Family · Career · Assets · Actions.

### Primary Life screen (match Make fidelity)
1. Navy hero: DiceBear avatar (rounded 20), name, flag + country, Gen chip + dynasty chip, oversized age number.
2. Four compact stat bars in the hero (white tracks).
3. Cream body: status card, year quests, journal event feed.
4. Sticky green **Live Another Year →** button with soft green glow shadow.
5. Event modal: dark header strip + white body + choice list (selected = ink fill).

### Anti-patterns
- Full-app dark-only (heroes are navy; loop is cream).
- Gold Age Up button (use green).
- Purple SaaS / cream-terracotta brochure / newspaper grids.
- Gambling UI in production Android (prototype Activities “Gambling” is reference-only — do not ship).

### Acceptance
In 3 seconds: recognize Maisha, feel a life-story journal, and know the next tap is **Live Another Year**.

## END PROMPT
