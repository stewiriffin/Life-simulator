# Play Store Listing Content — Maisha Life Simulator

**Snapshot date:** July 3, 2026  
**Source of truth:** [README.md](../README.md), `CountryCatalog.kt`, [UI_COPY_AUDIT.md](UI_COPY_AUDIT.md)

> **Manual steps:** Paste finalized copy into Play Console. Capture screenshots per shot list. App icon (512×512) and feature graphic (1024×500) still need final art.

---

## Part 1: App title candidates (~30 characters)

| # | Title | Chars | Notes |
|---|-------|-------|-------|
| 1 | **Maisha: Life Simulator** | 22 | Clear genre keyword |
| 2 | **Maisha — World Life Sim** | 23 | Hints at 15-country roster |
| 3 | **Maisha: Life & Legacy** | 21 | Highlights generational play |
| 4 | **Maisha: Your Life Story** | 23 | Warm, story-forward |
| 5 | **Maisha Life Simulator** | 21 | Straightforward |

**Recommendation:** **Maisha: Life Simulator** for search clarity, or **Maisha: Life & Legacy** for legacy differentiation.

---

## Part 2: Short description candidates (~80 characters)

| # | Short description | Chars |
|---|-------------------|-------|
| A | `Live life anywhere in the world. Build family, career, and legacy—year by year.` | 79 |
| B | `A life sim across 15 countries. Date, work, raise kids, and pass on your legacy.` | 79 |
| C | `Year-by-year life simulation: school, career, relationships, health—and heirs.` | 76 |

---

## Part 3: Long description (final draft)

**Character count: ~3,050 / 4,000**

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
Choose from 15 countries: Kenya, Nigeria, South Africa, Egypt, the United States, Canada, the United Kingdom, France, Germany, India, Japan, the Philippines, Indonesia, Brazil, and Mexico. Each brings localized names, jobs, assets, exams, holidays, and economy scaling. Relocate abroad when life events open the door.

YOUR LOOK
Customize a procedural avatar—skin tone and hairstyle—that ages with you and reacts to life's highs and lows.

THREE SAVE SLOTS
Run separate lives in parallel, or dedicate a slot to a single dynasty across generations.

Maisha saves locally on your device and plays offline. Create your character, tap Age Up, and see where your first life leads.
```

---

## Part 4: Feature highlight bullets

- **Live across 15 countries** — local exams, jobs, assets, holidays, and economy flavor
- **Age up year by year** — ~100 events and choices that shape your stats and story
- **Relationships with memory** — date, marry, raise children, ten interaction types
- **Your legacy lives on** — continue as heirs with a full ancestry timeline
- **School to career** — country-specific education, hiring, promotions, setbacks
- **Wealth or risk** — buy assets or try crime and face consequences
- **~30 achievements** — progress persists across all slots
- **Your avatar, your summary** — customize your look and share a life-summary card

---

## Part 5: Screenshot & promotional asset shot list

Priority order for **4–8 phone screenshots** (1080×1920 or 9:16). Capture on **360dp-wide** device.

| Priority | Screen / moment | What to show |
|----------|-----------------|--------------|
| **1 — Hero** | `LifeScreen` mid-playthrough | Avatar, stats, **event choice dialog** open |
| **2** | `AvatarPickerScreen` | Skin tone + hairstyle selection |
| **3** | `PersonDetailSheet` | Relationship tier, **memories timeline** |
| **4** | `AncestryScreen` | Multi-generation tree (2+ deceased gens) |
| **5** | Country selector | Search bar + several flags |
| **6** | `AchievementsScreen` | Mix of unlocked and locked |
| **7** | `LifeSummaryScreen` | End-of-life stats, share affordance |
| **8** *(optional)* | `CelebrationOverlay` | Confetti moment (18 particles) |

### Assets not covered by screenshots

| Asset | Size | Status |
|-------|------|--------|
| **App icon** | 512×512 PNG | Placeholder vector icons |
| **Feature graphic** | 1024×500 | Needs designed art |

---

## Part 6: Category & content rating prep notes

### Suggested Play Store category

| Field | Recommendation |
|-------|----------------|
| **Primary** | **Simulation** |
| **Secondary** | **Role Playing** (BitLife-style alternative) |

### Content rating (IARC questionnaire — verified P51)

| Topic | In-app reality | Questionnaire answer |
|-------|----------------|----------------------|
| **Crime** | Pickpocket, shoplift, fraud; arrest, jail — **non-violent only** | **Yes** — non-violent crime |
| **Romance / sexuality** | Dating, marriage, children; no explicit content | Mild romantic themes |
| **Death** | Gentle phrasing | Mild mortality themes |
| **Drugs / alcohol** | **No explicit references** in event JSON (P51 grep audit) | **No** |
| **Gambling** | **No gambling mechanics**; `investment_tip` is fixed-outcome savings group (P51) | **No** |
| **User-generated content** | None | **No** |
| **Ads** | Banner, interstitial, rewarded | Ad-supported disclosure |

#### Crime severity spot-check (P51)

No violent crime, weapons, assault, or murder in event JSON or `CrimeEngine` copy.

#### Drugs/alcohol note

One ambiguous line in `za_braai_weekend` ("Bring drinks only") — background family flavor, not substance-themed gameplay. Answer **No** for drugs/alcohol.

---

## Checklist before publishing listing

- [ ] Pick final title + short description
- [ ] Paste long description into Play Console
- [ ] Capture 4–8 screenshots per shot list
- [ ] Upload final **app icon** and **feature graphic**
- [ ] Set privacy policy URL
- [ ] Complete **Data safety** form
- [ ] Complete **content rating** questionnaire
- [ ] Proofread against `CountryCatalog.kt` — no unbuilt country claims

---

*Maisha Life Simulator — Play Store listing draft (Prompt 48; content rating verified Prompt 51).*
