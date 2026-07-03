# Play Store Listing Content — Maisha Life Simulator

**Snapshot date:** July 3, 2026  
**Source of truth:** [README.md](../README.md) feature inventory (Prompt 40) and in-app voice ([UI_COPY_AUDIT.md](UI_COPY_AUDIT.md), Prompt 44).

> **Manual steps:** Paste finalized copy into Play Console → **Main store listing**. Capture screenshots per the shot list below. App icon (512×512) and feature graphic (1024×500) still need final art (Prompts 20/23/24) — this document does not replace those assets.

---

## Part 1: App title candidates (~30 characters)

Play Store title limit is **30 characters**. Character counts include spaces and punctuation.

| # | Title | Chars | Notes |
|---|-------|-------|-------|
| 1 | **Maisha: Life Simulator** | 24 | Clear genre keyword; brand name matches in-app branding |
| 2 | **Maisha — World Life Sim** | 25 | Hints at the 15-country roster without crowding the title |
| 3 | **Maisha: Life & Legacy** | 23 | Highlights generational play — distinctive vs. one-life-only sims |
| 4 | **Maisha: Your Life Story** | 25 | Warm, story-forward; less search-keyword dense |
| 5 | **Maisha Life Simulator** | 23 | Straightforward; no subtitle punctuation |

**Recommendation:** **Maisha: Life Simulator** for search clarity, or **Maisha: Life & Legacy** if you want the listing to stand out on legacy play in browse results.

---

## Part 2: Short description candidates (~80 characters)

Short description limit is **80 characters**. Shown in search and install surfaces.

| # | Short description | Chars |
|---|-------------------|-------|
| A | `Live a life anywhere in the world. Build family, career, and legacy—year by year.` | 79 |
| B | `A life sim across 15 countries. Date, work, raise kids, and pass on your legacy.` | 79 |
| C | `Year-by-year life simulation: school, career, relationships, health—and heirs.` | 76 |

**Recommendation:** **A** — closest to onboarding welcome copy; **B** if you want the country count visible in search.

---

## Part 3: Long description (final draft)

**Character count: 3,076 / 4,000**

```
Maisha (Swahili for "life") is a life simulation game where you live one year at a time—from childhood through school, work, love, and old age. Every choice nudges your stats, your relationships, and what comes next. When your story ends, look back on the life you lived—or pass the torch to your children and keep the family line going.

LIVE YEAR BY YEAR
Tap Age Up to grow older. Random events and choice dialogs shape your happiness, health, smarts, looks, and more. Your event log becomes the story of who you became.

FAMILY & RELATIONSHIPS
Start with a generated family, then date, marry, have children, or divorce. Strengthen bonds with ten interaction types—spend time together, give gifts, travel, compliment, argue, and more. Relationships have tiers, memories, and milestones. Make friends during school and work years. When parents come from different countries, children can show mixed heritage—and your story reflects it.

EDUCATION & CAREER
Progress through primary school, secondary exams, and university with exam names and systems that match your chosen country. Pick a course, graduate, and apply for jobs from country-specific career pools. Performance, promotions, layoffs, and firings are all part of the climb. A criminal record can hurt hiring chances—but a long clean streak can ease the penalty if you turn things around.

ASSETS & WEALTH
Earn a salary, buy property and possessions, manage upkeep, and track your net worth. Cash-only economy: no loans, just the choices you make with what you earn.

CRIME & CONSEQUENCES
Pickpocketing, shoplifting, and fraud are options—if you accept the risk of arrest, jail time, and a record that follows you. Consequences are real, but redemption is possible.

HEALTH
Illness can strike as you age. Visit a doctor—public or private care, depending on what's available—or let untreated conditions drain your health.

ACHIEVEMENTS
Unlock roughly thirty achievements across career, education, family, wealth, longevity, and mischief. Progress is global across all save slots, so milestones you earn stay earned.

LEGACY MODE
When you die, you don't have to stop at the summary screen. Continue as an adult child, inherit a share of the estate, and build the next generation. Your Ancestry timeline tracks deceased relatives and living heirs across generations—a family story that spans more than one life.

PLAY ANYWHERE IN THE WORLD
Choose from 15 countries: Kenya, Nigeria, South Africa, India, the United States, the United Kingdom, Japan, Mexico, the Philippines, Egypt, Brazil, Canada, Australia, Indonesia, and Thailand. Each brings localized names, jobs, assets, exams, holidays, and economy scaling. Relocate abroad when life events open the door.

YOUR LOOK
Customize a procedural avatar—skin tone and hairstyle—that ages with you and reacts to life's highs and lows.

THREE SAVE SLOTS
Run separate lives in parallel, or dedicate a slot to a single dynasty across generations.

Maisha saves locally on your device and plays offline. Create your character, tap Age Up, and see where your first life leads.
```

---

## Part 4: Feature highlight bullets

Use for scannable store sections, promotional graphics, or the top of a landing page. Each item maps to a shipped system.

- **Live across 15 countries** — local exams, jobs, assets, holidays, and economy flavor in every life
- **Age up year by year** — hundreds of events and choices that shape your stats and story
- **Relationships with memory** — date, marry, raise children, and build bonds through ten interaction types
- **Your legacy lives on** — continue as your heirs across generations with a full ancestry timeline
- **School to career** — country-specific education paths, hiring, promotions, and setbacks
- **Wealth or risk** — buy assets and grow net worth, or try crime and face arrest and consequences
- **~30 achievements** — career, family, wealth, longevity, and more; progress persists across all slots
- **Your avatar, your summary** — customize how you look and share a life-summary card when a life ends

---

## Part 5: Screenshot & promotional asset shot list

Priority order for **4–8 phone screenshots** (1080×1920 or 9:16). Capture on a **360dp-wide** device or emulator to match the design target.

| Priority | Screen / moment | What to show | Why |
|----------|-----------------|--------------|-----|
| **1 — Hero** | `LifeScreen` mid-playthrough | Avatar visible, stats populated, an **event choice dialog** open on a positive or interesting moment (not empty state) | First impression: this is a living story, not a menu |
| **2** | `AvatarPickerScreen` | Skin tone + hairstyle selection, or a small montage of 2–3 different looks | Sells personalization immediately |
| **3** | `FamilyScreen` or `PersonDetailSheet` | Relationship tier label, **memories timeline** with at least 2–3 entries, spouse or child visible | Differentiator: relationships have history |
| **4** | `AncestryScreen` | Multi-generation tree with **2+ deceased generations** and current character | Legacy Mode — rare in the genre |
| **5** | Character creation **country selector** or any **CountryFlag**-heavy UI | Search bar + several flags visible; ideally a non-default country selected | Worldwide roster selling point |
| **6** | `AchievementsScreen` | Grid with a mix of **unlocked and locked** achievements | Replayability and completion hook |
| **7** | `LifeSummaryScreen` or share-card preview | End-of-life stats, achievement badges, **Share My Life** affordance visible | Payoff moment — "this is a whole life" |
| **8** *(optional)* | `CelebrationOverlay` | Marriage, graduation, child, or achievement confetti moment | Polish and game-feel |

### Assets this shot list does **not** cover (still required manually)

| Asset | Size | Status |
|-------|------|--------|
| **App icon** | 512×512 PNG | Placeholder vector icons in repo — final art per Prompts 20/23/24 |
| **Feature graphic** | 1024×500 JPG/PNG | Promotional banner for store header — needs designed art, not an in-app screenshot |
| **Tablet screenshots** | Optional | Only if listing on tablets; same content plan, wider crop |

**Capture tips:** Use a life with interesting stats (not age 0). Hide debug overlays. Prefer light-mode screenshots unless dark mode is the store default. Avoid test AdMob banners if possible (use a build moment between ads, or crop carefully).

---

## Part 6: Category & content rating prep notes

### Suggested Play Store category

| Field | Recommendation | Notes |
|-------|----------------|-------|
| **Primary** | **Simulation** | Best fit for year-by-year life sim mechanics |
| **Secondary / alternate** | **Role Playing** | Reasonable if you compare to BitLife-style listings and Simulation feels crowded — review top competitors before locking in |
| **Tags** *(if offered)* | Life simulation, choices, family | Use only tags Play Console actually offers at submission time |

### Content rating (IARC questionnaire — answer honestly)

This prompt cannot complete the questionnaire. Relevant **built** content to disclose:

| Topic | In-app reality | Questionnaire relevance |
|-------|----------------|-------------------------|
| **Crime** | Pickpocket, shoplift, fraud; arrest, jail, criminal record | Likely "crime" or "illegal activities" — **non-violent**, no graphic violence |
| **Romance / sexuality** | Dating, marriage, divorce, children; **no explicit sexual content** | Mild romantic themes; no nudity |
| **Death** | Character death, life summary, legacy continuation; **gentle phrasing** in copy | Mortality / death themes — typically mild, not graphic |
| **Drugs / alcohol** | Check event JSON if any references appear in random events | Disclose if present in shipped content |
| **Gambling** | No casino/gambling mechanics identified in feature inventory | Likely "No" unless event content adds it |
| **User-generated content** | None — no chat, forums, or UGC | "No" |
| **Location sharing** | No GPS collection by app; AdMob may infer approximate location (see Prompt 47) | Separate from content rating; covered in Data Safety |
| **Ads** | Banner, interstitial, rewarded (Second Wind) | Ad-supported app disclosure in questionnaire |

**After questionnaire:** Play Console assigns an age rating (e.g. PEGI, ESRB). If you target children under 13, revisit AdMob child-directed settings and [PRIVACY_POLICY.md](PRIVACY_POLICY.md) — same cross-check as Prompt 47.

---

## Checklist before publishing listing

- [ ] Pick final title + short description from candidates above
- [ ] Paste long description into Play Console (re-count if you edit)
- [ ] Capture 4–8 screenshots per shot list
- [ ] Upload final **app icon** and **feature graphic**
- [ ] Set privacy policy URL ([PRIVACY_POLICY.md](PRIVACY_POLICY.md) hosted + `PRIVACY_POLICY_URL` in app)
- [ ] Complete **Data safety** ([PLAY_STORE_DATA_SAFETY_NOTES.md](PLAY_STORE_DATA_SAFETY_NOTES.md))
- [ ] Complete **content rating** questionnaire using Part 6 notes
- [ ] Proofread listing against README feature inventory — no unbuilt claims

---

*Maisha Life Simulator — Play Store listing draft (Prompt 48). Not legal advice; verify Play Console field limits at submission time.*
