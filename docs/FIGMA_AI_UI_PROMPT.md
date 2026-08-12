# Figma AI Prompt — Maisha Life Simulator UI

Paste the block below into Figma AI (or FigJam / Make Design). It is written so the model can produce a complete, production-quality mobile UI system without inventing a generic “AI purple dashboard.”

---

## PROMPT (copy from here)

**Role:** You are a principal product designer specializing in narrative life-simulation mobile games (BitLife / Character Life / Reigns energy) with premium **worldwide** storytelling craft. Design the full UI system for **Maisha**, an Android life simulator.

### Product essence
- **Name:** Maisha (Swahili for *life*) — brand must read as the hero of every first viewport. The name is Swahili; the **game world is global**, not Kenya-only.
- **Fantasy:** Live one human life at a time in **any of 15 countries** — school, career, love, crime, health, wealth, then continue the bloodline.
- **Core verb:** **Age Up** (one year per tap). Everything else supports that ritual.
- **Tone:** Warm, witty, consequential — never childish cartoon, never cold fintech. Think “passport stamps + family kitchens + city night buses worldwide,” not a single-city souvenir aesthetic.
- **Platform:** Android phone, **360dp width**, Material 3 patterns, thumb-zone primary CTA.
- **Audience:** 16–35, global (KE, NG, ZA, EG, US, CA, GB, FR, DE, IN, JP, PH, ID, BR, MX), bilingual-ready UI.
- **Localization of place:** Country flag, currency, exams, transport, hospital names, and school year labels must feel local to the chosen country. Never hardcode Nairobi/matatu/M-Pesa into universal chrome.

### Face / avatar source (non-negotiable)
- **Use [DiceBear Lorelei](https://www.dicebear.com/styles/lorelei/) faces as the sole illustrated face source** for player + NPC avatars in all screens and marketing frames.
- Style: `lorelei` (hand-drawn ink portrait). License: CC0.
- API reference for mocks / exports:  
  `https://api.dicebear.com/9.x/lorelei/png?seed={id}&size=128&radius=50&backgroundColor=152238&skinColor={hex}&hairColor={hex}`
- Seeds must be **stable per person** (person id / character name). Same seed = same face forever.
- Map game identity into DiceBear options: skin tone, hair color/style, glasses, beard, mouth for expression (happy/sad), slightly different scale by age stage.
- Do **not** invent custom face illustrations, photo stock, or emoji heads. Empty states / icons can stay geometric; **faces = DiceBear only**.
- Show circular crops on navy `#152238` fills; keep gold/teal chrome around them, not on top of the face art.

### Composition rules
1. First viewport of each key screen = **one composition**, not a dashboard collage.
2. Brand or life identity (avatar + name + age) is hero-level on Life; Age Up is the dominant CTA.
3. No card spam in heroes. Cards only where they hold interaction (quest, event choice, person).
4. One job per section: one headline + one short support line.
5. Full-bleed only for splash / death / celebration moments — not inset media tiles.

### Screens to design (complete set)

#### 1. Splash / cold start
- Full-bleed navy atmosphere, **MAISHA** oversized, tagline: “Live your story. One year at a time.”
- Soft gold particle drift. No buttons — brand moment only.

#### 2. Onboarding (3–4 slides)
- Large illustrative metaphor per slide (calendar year, branching choices, family tree).
- Minimal text. Final CTA: “Start your first life.”

#### 3. Slot picker
- Three life slots as vertical “life books” or passport covers — occupied slots show avatar, age, country flag, generation badge; empty slots invite “Begin.”
- Settings gear secondary.

#### 4. Character creation
- Country + gender + name + starting vibe.
- Avatar builder preview large and tactile (layered parts: face, hair, clothes).
- CTA: “Begin life.”

#### 5. Main Life loop (PRIMARY SCREEN — design at highest fidelity)
**Layout (top → bottom):**
- Header: Avatar (expressive face) + name + age + country flag + generation chip; settings & full-life dossier icons.
- Stats strip: Health / Happiness / Smarts / Looks / Money (+ Karma as secondary/subtle).
- Status row: education · career · net worth.
- **This Year’s Quests** (2 soft goals with progress bars — gold/teal).
- **Dynasty score** compact prestige chip (title + number).
- Event log: chronological “year story” entries with tone color (milestone gold, positive green, negative coral).
- Sticky/bottom **Age Up** gold pill button — magnetic, springy, impossible to miss.
- Bottom nav: Life · Family · Career · Assets · Actions.

#### 6. Event decision modal
- Blurred life behind.
- NPC or player avatar, short event title, category label, 2–3 choice buttons with clear consequence vibe (not spoiling exact numbers).
- Satisfying choose flash.

#### 7. Family tab
- Sectioned people list (parents, siblings, partner, kids, friends, pets).
- Person cards show bond tier (Estranged → Inseparable) as colored ribbon.
- FAB for dating / party when eligible.
- Detail sheet: interactions (spend time, gift, argue…) as icon grid.

#### 8. Career tab
- Current job hero with level stars and performance bar.
- Apply / quit / retire / side hustles / business / politics as clear action groups — not a wall of identical buttons.
- Promotion celebration moment: gold confetti + “Promoted!” banner.

#### 9. Assets / finance tab
- Net worth hero number in gold.
- Owned assets with condition bars; shop as secondary.

#### 10. Actions tab
- Grouped lifestyle board: Health, Opportunities, Crime (danger zone styling), Philanthropy (karma), Skills, Social, Immigration.
- Crime actions visually “risky” (coral edge) vs volunteer (teal calm).

#### 11. Achievements
- Grid by category with custom icons (Career, Education, Family, Wealth, Longevity, Mischief, Worldly).
- Locked = silhouette; unlocked = gold seal.
- Progress bars for wealth/longevity tiers.
- Unlock dialog: celebration + title + description.

#### 12. Full Life / Character stats dossier
- Passport-like summary: stats including **Karma**, education path, career arc, family, passports, ancestry teaser.

#### 13. Ancestry / Family Heritage
- Vertical timeline oldest → newest.
- Dynasty score header with prestige title (Seedling → Legendary bloodline).
- Generation nodes with flags and ages.

#### 14. Death / Life Summary
- Solemn but beautiful: lifespan, key milestones, share card preview, heir selection, **Second Wind** rewarded-ad revive teaser (tasteful, not spammy).
- CTA: Continue legacy / New life.

#### 15. Settings
- Sound, haptics, notifications, language, privacy, reset — quiet utility screen.

### Component system to deliver
- Color styles + text styles + spacing (4/8/12/16/24) + radii (12 button / 14 card / 20 sheet).
- Components: AgeUpButton, StatBar, PersonCard, ActionCard, EventModal, QuestCard, DynastyChip, AchievementTile, BottomNav, CelebrationOverlay variants (Marriage, Birth, Graduation, Promotion, Quest, Age 18/50/100).
- Avatar expression set: neutral, happy, sad, angry, surprised — driven by DiceBear `mouthVariant` on Lorelei faces.
- Empty states with custom geometric illustrations (not generic clipart); human faces stay DiceBear-only.

### Gamification UI must make visible
1. Year quests (in-life goals each year).
2. Dynasty prestige score & titles.
3. Karma meter (virtue / luck).
4. Celebration banners with distinct palettes per moment.
5. Event log emotional color coding.
6. Achievement progress + unlock ceremony.
7. Generation badge on header.

### Deliverables in Figma
1. **Design system page** (tokens, type, components).
2. **Flow page** — Slot → Create → Life loop → Event → Death → Legacy.
3. **Hi-fi phone frames** for screens 1–15 above (360×800 artboards).
4. **Prototype links:** Age Up → Event → Choice → Quest complete → Celebration.
5. **Marketing key visual:** one poster-like frame — Maisha wordmark + avatar aging silhouette + gold Age Up.

### Explicit anti-patterns (do not produce)
- Purple-on-white SaaS dashboards.
- Warm cream + terracotta “AI brochure” look.
- Dense newspaper columns / hairline editorial grids.
- Floating badges stuck on hero media.
- Inter / Roboto / Arial as display fonts.
- Soft UI bubbles with multi-layer neon glow spam.
- Inset rounded hero photos on the home screen.

### Acceptance bar
A stranger should feel in 3 seconds: “This is a life story game with weight and wit,” recognize **Maisha** as the brand, and know the next action is **Age Up**.

## END PROMPT

---

### Optional follow-ups for Figma AI
After the first pass, ask:
1. “Expand celebration overlay variants with unique particle palettes and banner typography.”
2. “Design share-card templates for Instagram Stories (9:16) using dynasty score + lifespan.”
3. “Create a light-mode experimental variant only for Settings accessibility — keep game loop dark.”
