# Figma AI Prompt — Maisha Life Simulator UI Redesign

**Copy everything below the line into Figma AI (Make / First Draft / redesign mode).**  
Attach `design/official-app-icon.png` as a style reference if the tool accepts image input.

---

## Prompt (paste from here)

Design a **complete, production-ready mobile UI system** for **Maisha: Life Simulator** — an Android life-simulation game in the BitLife / Age Sim genre, but warmer, more global, and PG-rated. **Massively improve** the current UI: elevate visual hierarchy, polish micro-interactions, unify components, and make every screen feel like a premium casual game — not a generic Material app.

### Product context

- **Platform:** Android phone, **360dp width minimum**, portrait-only, safe-area aware (notch + gesture nav).
- **Tech:** Jetpack Compose + Material 3 (design must map cleanly to Compose components).
- **Audience:** 13+ casual players worldwide; readable at a glance; thumb-friendly.
- **Core loop:** Tap **Age Up** once per in-game year → random **Life Event** dialog with choices → stats/relationships/career shift → repeat until death → **Life Summary** → optional **Continue Legacy** as an heir.
- **Scope:** 15 playable countries (Kenya, Nigeria, South Africa, Egypt, US, Canada, UK, France, Germany, India, Japan, Philippines, Indonesia, Brazil, Mexico). UI must feel **global**, not Kenya-centric.
- **Tone:** Warm, encouraging, lightly playful — never grim, manipulative, or casino-like. **No gambling, affairs, drugs, or shock UI.**

### Design north star

Think **BitLife clarity + Apple Fitness warmth + Duolingo delight**, with Maisha’s own identity:

- **Navy hero zones** for identity, context, and drama (headers, event cards, slot picker).
- **Cream/off-white canvas** for scrollable content (cards float above calm background).
- **Life Green** as the single sacred primary CTA color (Age Up only — don’t dilute it).
- **Gold** for money/net worth highlights; **teal** for secondary accents; **coral** for danger/negative only.
- **BitLife-style illustrated raster icons** for stats, nav, jobs, assets, achievements — friendly, chunky, slightly cartoon, not flat Material glyphs.
- **Procedural avatar** (round portrait, expression changes) is the emotional anchor on Life tab.

### Existing brand tokens (refine, don’t replace randomly)

| Token | Hex | Usage |
|-------|-----|--------|
| Navy Deep | `#0B1628` | Hero backgrounds, event scrim |
| Navy Surface | `#152238` | Gradients, app icon bg, splash |
| Navy Elevated | `#1E2F4A` | Hero gradient end, prison mode |
| Cream Background | `#F5F4F0` | Main scroll surfaces |
| Cream Muted | `#F2F2F7` | Inset panels, narrative quotes |
| Ink Primary | `#1C1C1E` | Body text on light |
| Ink Secondary | `#8E8E93` | Captions, hints |
| Life Green | `#34C759` | **Age Up**, active nav indicator |
| Life Green Pressed | `#1A7A32` | Pressed Age Up |
| Gold Accent | `#F4B942` | Money chips, achievement flair |
| Teal Primary | `#1A8A8A` | Career/education accents |
| Accent Pink | `#E91E8C` | Relationships, dating |
| Coral Negative | `#E85D5D` | Errors, crime, critical stats |

**Stat colors (use consistently on bars, chips, floaters):**  
Health `#4CAF79` · Happiness `#F4B942` · Smarts `#4B8EF0` · Looks `#A78BFA` · Money `#F4B942` · Relationship `#FF2D55` · Karma `#26A69A`

**Radii:** cards 16px · buttons 12px · Age Up pill 18px · bottom sheets 20px · avatars 20px circle with 3px accent ring.  
**Spacing scale:** 4 / 8 / 12 / 16 / 24 dp.  
**Typography:** Modern geometric sans (Inter, SF Pro, or similar). Bold headlines, comfortable 16px body, 26px line-height for story text.

### What to massively improve (pain points)

1. **Visual hierarchy on Life tab** — avatar, name, age, country flag, and net worth should read as one “character card”; stats should feel like a dashboard, not a list.
2. **Age Up button** — must be the most satisfying element on screen: full-width green pill, soft green glow shadow, loading state, disabled state when event dialog is open.
3. **Life Event dialog** — cinematic modal after aging: navy header with category icon (Education/Career/Finance/Family/Crime/Life), large avatar with expression, quote-style narrative panel, numbered choice cards with **stat preview chips** (+money, +happiness, etc.), “Event 2 of 3” progress when multiple events fire.
4. **Bottom navigation** — 5 tabs with illustrated icons + labels; active tab = green tint + subtle pill background; disabled state when incarcerated (Career/Assets greyed).
5. **Tab hero headers** (Career, Assets, Actions) — unified navy gradient banner with title, subtitle, gold money chips (cash, net worth, salary).
6. **Asset shop** — BitLife-style **large hero image cards** per item (car/house/bike), not tiny icons; price + upkeep visible; condition bar for owned assets.
7. **Family tab** — person cards with avatar, relationship tier badge, age; empty state with illustration + warm copy; bottom sheet for interactions (10 types: spend time, gift, travel, compliment, argue, etc.).
8. **Slot picker** — 3 save slots as premium cards: avatar, name, age, country flag, generation badge, net worth, last played; empty slot = dashed invite; navy atmospheric background.
9. **Character creation** — step feel: name, gender, country searchable list with flags, randomize avatar, live preview; **Start Life** CTA.
10. **Achievement unlock** — celebratory dialog with confetti suggestion, large category illustration, gold accent.
11. **Life Summary / death** — dignified, not morbid; archetype label, stat recap, share card preview, **Continue Legacy** vs new life.
12. **Consistency** — one card style, one chip style, one sheet style, one dialog style across all screens.

### Required screens (design ALL at 360×800 and 390×844)

#### A. Pre-game flow
1. **Splash** — navy `#152238`, centered globe+heart app icon (see attached), subtle scale-in.
2. **Onboarding** (5 slides) — illustrated full-bleed art per slide: Welcome, Age Up mechanic, Choices matter, 15 countries, Ready; dot pager + Skip + Next.
3. **Slot Picker** — 3 slots, settings gear top-right, banner ad placeholder bottom (labeled “Ad”).
4. **Character Creation** — form + avatar picker entry + country search.
5. **Avatar Picker** — skin tone swatches, hairstyle grid, randomize, save.

#### B. Main game (single scaffold, 5 tabs)
6. **Life Tab (default)** — character hero, 5 core stat bars (health/happiness/smarts/looks/money), event log list with tone colors (positive/neutral/negative/system), year quest card, bucket list teaser, **Age Up** fixed above bottom nav, settings + stats icons in top bar.
7. **Family Tab** — sections: Partner, Children, Parents/Siblings, Friends, Pets; empty states; Find Date / Seek Friendship CTAs when eligible.
8. **Career Tab** — hero (job title, salary, performance bar), education strip, apply/quite/retire, job list cards with illustrated job category art, side hustle / politics / military subsections when relevant.
9. **Assets Tab** — hero (cash, net worth, living standard), owned assets list with condition, shop grid with hero images, will/legal strip for adults.
10. **Actions Tab** — categorized action cards (Health, Crime, Leisure, Social Media, Skills, Bucket List, Relocation) with icon, title, description, meta pill; prison mode variant (limited actions, navy background).

#### C. Overlays & modals
11. **Life Event dialog** (multiple variants: Finance negative, Relationship, Education, Holiday positive, Crime).
12. **Person Detail bottom sheet** — relationship meter, tier label, memories timeline, interaction grid.
13. **Pet Detail sheet**.
14. **Achievement Unlocked dialog**.
15. **Celebration overlay** (graduation, marriage, promotion — confetti + banner text).
16. **Confirm dialogs** (purchase, break up, reset save).
17. **Arrest / Trial dialog** (lawyer tiers with prices).
18. **Year Recap bottom sheet** (optional — stat deltas for the year).

#### D. Meta screens
19. **Settings** — sound, haptics, language, notifications, reset saves (destructive).
20. **Achievements gallery** — category filters, locked/unlocked states, progress bar.
21. **Character Stats** — full stat breakdown.
22. **Ancestry / Legacy timeline** — generations vertical timeline.
23. **Life Summary (death)** — score, archetype, achievements earned this life, share card.
24. **Share card** — exportable 9:16 story format with avatar, name, age, highlights.

### Component library (build as Figma components + variants)

Create a **Maisha Design System** page with:

- **Buttons:** Primary (green, Title Case), Secondary (outline navy), Destructive (coral outline), Text, Icon.
- **Age Up Button** — component with Default / Pressed / Loading / Disabled.
- **Stat Bar** — label + icon + bar + value; variants for each stat type; critical low state (bar turns coral).
- **Stat Delta Floater** — +12 Happiness floating chip animation spec.
- **Cards:** Standard (white, 16 radius, 1dp shadow), Hero (navy gradient), Inset (cream muted), Empty State (illustration + title + body).
- **Chips:** Filter, Money (gold), Country, Relationship tier, Record badge (criminal/clean).
- **Nav:** Bottom tab bar (5 items), active/inactive/disabled.
- **List rows:** Event log row, job row, person row, asset row.
- **Sheets:** Bottom sheet handle, header, scroll body.
- **Dialogs:** Center modal + full-bleed event modal.
- **Inputs:** Text field, search field (country picker).
- **Avatar:** 64 / 88 / 120 sizes; rings for player vs NPC; expression variants (neutral, happy, sad, angry, surprised).
- **Illustrated icons:** 24px and 48px sets for stats, nav, jobs, achievements, empty states, onboarding.
- **Asset hero frames:** 16:9 WebP-style placeholders for car, house, motorbike, heirloom.

Use **auto-layout**, **constraints**, and **component properties** (boolean variants for states) throughout.

### Interaction & motion specs (annotate on a Motion page)

- Tab switch: 200ms crossfade + 8px horizontal slide.
- Age Up: 96% scale on press, spring back; background blur 16px while event dialog open.
- Event dialog enter: scale 0.88→1 + fade 240ms; choices stagger 55ms each.
- Choice tap: brief teal/green flash 140ms then dismiss.
- Achievement: scale 0.8→1 + confetti 1.8s.
- Stat bar changes: 600ms ease color/progress.
- Floating stat deltas: rise 48px + fade over 1.2s.

### Copy & voice (use exact terms)

- Primary CTA: **Age Up** (never “Next Year”).
- Green button label: **Live Another Year** or **Age Up** — pick one and stay consistent.
- Empty states: inviting, not shaming — e.g. “Your story is just getting started.”
- Title Case for screen titles and primary buttons; sentence case for descriptions.
- Currency displays are **country-local** (KSh, $, ₦, etc.) — show examples on Assets/Career heroes.

### Reference competitors (layout only — do NOT copy branding)

- **BitLife:** event dialog structure, asset hero cards, stat bars, tab density.
- **InstLife / Another Life:** card-based actions.
- Avoid: slot-machine visuals, neon casino palettes, cluttered RPG inventory grids.

### Deliverables I need from you

1. **Cover page** — mood board + refined color/type scale.
2. **Design system page** — all components listed above.
3. **All 24 screens** at mobile frames with realistic sample data (character: “Amina Okoro”, age 27, Nigeria, teacher, married, 1 child).
4. **Dark variant optional** — only if it doesn’t harm readability; app currently light-first.
5. **Redlines page** — spacing, tap targets (min 48dp), type sizes for developer handoff.
6. **Export notes** — which elements are raster illustrations vs vector vs solid fills.

### Constraints for developers

- No custom fonts that require expensive licenses — prefer Google Fonts (Inter, Nunito, or DM Sans).
- Illustrations should be **flat/semi-flat raster** style exportable as WebP @ drawable-nodpi.
- Keep tap targets ≥ 48dp; primary CTA ≥ 52dp height.
- Ad banner area: **320×50 adaptive** on Slot Picker bottom only.
- Preserve **5-tab bottom nav** structure — do not collapse into hamburger menu.

### Success criteria

When done, a player should feel:

1. “I immediately know who I am and what to tap next.”
2. “Age Up feels exciting every time.”
3. “Events read like a story, not a form.”
4. “The app looks like a shipped game, not a student project.”
5. “Countries and money feel real but UI stays clean.”

**Start with the Life Tab + Life Event dialog + Design System, then expand to all other screens.** Prioritize polish on the core loop before edge screens (Ancestry, Share card).

---

## End of prompt

### Optional follow-up prompts for Figma AI

After the first pass, run these iteratively:

1. *“Redesign only the Life Event dialog — more cinematic, stronger category color coding, clearer stat hints on choices, queue progress bar when Event 2 of 3.”*

2. *“Redesign the Assets shop as BitLife-style vertical cards with large illustrated hero images, price/upkeep footer, and owned vs shop states.”*

3. *“Create an illustrated icon set (48px) for: Health, Happiness, Smarts, Looks, Money, 5 nav tabs, 11 job categories, 7 achievement categories — chunky friendly game art, consistent stroke and shading.”*

4. *“Add a prison mode visual theme: desaturated navy background, restricted tab bar, Actions tab focused on prison activities — same design system, different atmosphere.”*

5. *“Design the death Life Summary screen: respectful tone, life archetype badge (e.g. ‘Quiet Provider’, ‘World Traveler’), Continue Legacy green button vs Start New Life outline.”*

### Handoff to engineering

When Figma output is approved:

- Map colors to `app/src/main/java/com/maisha/game/ui/theme/Color.kt`
- Map radii/spacing to `MaishaRadius` / `MaishaSpacing` in `Theme.kt`
- Export illustrations to `app/src/main/res/drawable-nodpi/img_*.webp`
- Update Compose screens under `app/src/main/java/com/maisha/game/ui/`
