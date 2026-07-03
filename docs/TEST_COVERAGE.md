# Test coverage map (ARCHITECTURE.md §4)

Cross-reference of domain engine public APIs against JVM unit tests. Status values:

| Status | Meaning |
|--------|---------|
| **Tested** | Dedicated correctness test(s) exist |
| **Perf only** | Benchmark/timing test only — correctness covered elsewhere |
| **Indirect** | Exercised via `GameEngine` integration or related utility test |
| **Gap closed (P50)** | Gap confirmed in audit; test added in Prompt 50 |
| **Low priority** | No dedicated test; thin delegate or creation-time-only path |

Last audited: **2026-07-03** (Prompt 50).

---

## `GameEngine`

| Function | Status | Test(s) | Notes |
|----------|--------|---------|-------|
| `ageUp` | Tested | `GameEngineTest` (age increment, incarceration skip, mortality ordering, no achievements on death year); `PerformanceBenchmarkTest` (timing); `MemorySoakTest` (soak) | Correctness separate from benchmark |
| `introEventsForNewborn` | Low priority | — | Age-0 bootstrap; thin delegate |
| `applyChoice` | Tested | `GameEngineTest.applyChoice_clampsStatsToZeroAndOneHundred` | |
| `applyForJob` / `quitJob` | Indirect | `CareerEngineTest`, `GameEngineTest` | Career engine owns logic |
| `getEligibleJobs` | Indirect | `CareerEngineTest` | |
| `purchaseAsset` / `sellAsset` / `calculateNetWorth` | Indirect | `FinanceEngineTest` | |
| `interactWithFamilyMember` | Indirect | `RelationshipEngineTest` | |
| `findDatingProspects` / `startDating` / `proposeMarriage` / `breakUpOrDivorce` / `haveChild` | Indirect | `RelationshipEngineTest`, `MixedHeritageTest` | |
| `applyLegacyFamilyMilestones` | Indirect | `LegacyEngineTest`, `LegacyAncestryTest` | |
| `attemptCrime` / `visitDoctor` | Indirect | `CrimeEngineTest`, `HealthEngineTest`, `GameEngineTest` | |

---

## `EducationEngine`

| Function | Status | Test(s) | Notes |
|----------|--------|---------|-------|
| `enrollIfEligible` | Indirect | `EducationEngineTest` | Via advance paths |
| `advanceGrade` | Indirect | `EducationEngineTest` | |
| `applyStudyEffort` | Indirect | `EducationEngineTest` | |
| `advanceUniversityYear` | Indirect | `EducationEngineTest` | |
| `takeExam` | Tested | `EducationEngineTest.takeExam_highStatsPassMoreOftenThanLowStats` | |
| `applyToUniversity` | Tested | `EducationEngineTest.applyToUniversity_rejectsBelowMinimumGrade`, `applyToUniversity_acceptsStrongGrade` | |
| `isEligibleForUniversity` | Indirect | `EducationEngineTest` | |
| `shouldTriggerKcpe` | Gap closed (P50) | `EducationEngineTest.shouldTriggerKcpe_*` | |
| `shouldTriggerKcse` | Gap closed (P50) | `EducationEngineTest.shouldTriggerKcse_*` | |
| `buildExamResultEvent` | Low priority | — | Presentation builder |
| `applyGpaEffect` | Indirect | `EducationEngineTest` | |

---

## `CareerEngine`

| Function | Status | Test(s) | Notes |
|----------|--------|---------|-------|
| `isJobEligible` | Indirect | `CareerEngineTest` | |
| `getEligibleJobs` | Indirect | `CareerEngineTest` | |
| `applyForJob` | Tested | `CareerEngineTest.criminalRecord_reducesHireSuccessRate` | Criminal-record penalty |
| `workYear` | Indirect | `CareerEngineTest` | |
| `applyWorkEffort` | Tested | `CareerEngineTest` (grind vs coast performance) | |
| `evaluatePromotion` | Tested | `CareerEngineTest.evaluatePromotion_atThresholdPromotes`, `evaluatePromotion_oneBelowThresholdDoesNotPromote` | Boundary values |
| `evaluateFiring` | Tested | `CareerEngineTest.evaluateFiring_atThresholdDoesNotFire`, `evaluateFiring_oneBelowThresholdFires` | Boundary values |
| `quitJob` | Low priority | — | Thin state update |
| `applyPerformanceEffect` | Indirect | `CareerEngineTest` | |
| `buildPromotionEvent` / `buildFiringEvent` / `buildDownsizingEvent` | Low priority | — | Event builders |
| `shouldTriggerDownsizing` | Gap closed (P50) | `CareerEngineTest.shouldTriggerDownsizing_falseWhenUnemployed` | Positive trigger is probabilistic |
| `applyDownsizing` | Low priority | — | |
| `calculateAnnualSalary` | Tested | `BalanceTuningTest` | |

---

## `FinanceEngine`

| Function | Status | Test(s) | Notes |
|----------|--------|---------|-------|
| `purchaseAsset` | Tested | `FinanceEngineTest.purchaseAsset_rejectsInsufficientFunds`, `purchaseAsset_deductsMoneyOnSuccess` | |
| `sellAsset` | Indirect | `FinanceEngineTest` | |
| `applyUpkeep` | Indirect | `FinanceEngineTest` | |
| `degradeAssets` | Tested | `FinanceEngineTest.degradeAssets_reducesConditionOverYears` | Multi-year cumulative |
| `calculateNetWorth` | Indirect | `FinanceEngineTest`, `AchievementWealthTest` | |
| `applyConditionToAssetType` / `applyConditionToFirstAsset` | Low priority | — | |
| `meetsFinanceEventThreshold` | Indirect | `FinanceEngineTest` | |
| `recalculateValue` | Indirect | `FinanceEngineTest` | |

---

## `RelationshipEngine`

| Function | Status | Test(s) | Notes |
|----------|--------|---------|-------|
| `tickFamilyYear` | Tested | `RelationshipEngineTickTest`, `RelationshipEngineTest.tickFamilyYear_decaysNeglectedRelationships` | Decay toward neutral; skips recent interactions |
| `findDatingProspects` | Indirect | `RelationshipEngineTest` | |
| `generateFriendshipOpportunity` | Tested | `FriendshipGenerationTest` | |
| `progressRelationship` | Indirect | `RelationshipEngineTest` | |
| `proposeMarriage` | Tested | `RelationshipEngineTest.proposeMarriage_rejectsBelowThreshold` | |
| `startDating` / `breakUpOrDivorce` | Indirect | `RelationshipEngineTest` | |
| `haveChild` | Tested | `MixedHeritageTest`, `RelationshipEngineTest` | `secondaryCountryCode` for mixed heritage |
| `applySpouseRelationshipEffect` | Low priority | — | |
| `applyLegacyFamilyMilestones` | Indirect | `LegacyEngineTest` | |
| `getRelationshipTier` | Tested | `RelationshipTierTest` | |
| `canTravelTogether` | Low priority | — | |

---

## `CrimeEngine`

| Function | Status | Test(s) | Notes |
|----------|--------|---------|-------|
| `attemptCrime` | Low priority | — | Probabilistic; arrest path tested |
| `processArrest` | Tested | `CrimeEngineTest.crimeTypes_produceDifferentSentenceLengthsWhenCaught` | Sentencing by `CrimeType` |
| `serveYear` | Indirect | `GameEngineTest` (incarceration age-up) | |

Clean-streak hire redemption: `CrimeEngineTest`, `BalanceTuningTest`.

---

## `HealthEngine`

| Function | Status | Test(s) | Notes |
|----------|--------|---------|-------|
| `rollForIllness` | Tested | `HealthEngineTest` | Low health → higher illness rate |
| `addCondition` | Indirect | `HealthEngineTest` | |
| `applyUntreatedConditions` | Indirect | `HealthEngineTest` | |
| `visitDoctor` / `visitFirstUntreatedCondition` | Indirect | `HealthEngineTest` | |

---

## `MortalityEngine`

| Function | Status | Test(s) | Notes |
|----------|--------|---------|-------|
| `checkDeath` | Tested | `MortalityEngineTest` | Age curve + low-health elevation |
| `applyDeath` | Indirect | `MortalityEngineTest`, `GameEngineTest` | |
| `parseDeathCause` / `deathFlavorText` / `gentleCauseLabel` | Low priority | — | Presentation helpers |

---

## `AchievementEngine`

| Function | Status | Test(s) | Notes |
|----------|--------|---------|-------|
| `checkAchievements` | Tested | `AchievementEngineTest` (career/education/family/wealth/longevity/mischief samples); `AchievementWealthTest` (currency-scaled wealth tiers); `PerformanceBenchmarkTest` (timing) | Wealth thresholds post–Prompt 49 fix in `AchievementWealthTest` |

Representative IDs covered: `first_job`, `graduate`, `true_friend`, `second_generation`, `property_owner`, `six_figures`, `half_century`, `brush_with_law`, `clean_record`, `world_citizen`.

---

## `LegacyEngine`

| Function | Status | Test(s) | Notes |
|----------|--------|---------|-------|
| `eligibleHeirs` | Tested | `LegacyEngineTest` | |
| `createLegacyCharacter` | Tested | `LegacyEngineTest`, `LegacyAncestryTest` | |
| `buildAncestryEntry` | Indirect | `LegacyAncestryTest` | |
| `mapSurvivingParent` | **Tested** | `LegacyEngineTest` — father-deceased → mother; mother-deceased → father; no spouse → null | **Not a gap** (highest-priority audit item confirmed covered) |

---

## `RelocationEngine`

| Function | Status | Test(s) | Notes |
|----------|--------|---------|-------|
| `hasRelocated` | Tested | `RelocationEngineTest` | |
| `shouldOfferRelocation` | Gap closed (P50) | `RelocationEngineTest.shouldOfferRelocation false when *` | Eligibility gating |
| `getRelocationOpportunities` | Tested | `RelocationEngineTest` | |
| `buildRelocationOpportunityEvent` | Low priority | — | Event builder |
| `relocate` | Tested | `RelocationEngineTest.relocate switches country and clears current job` | Country cascade via job clear |

---

## `EventLogCap`

| Function | Status | Test(s) | Notes |
|----------|--------|---------|-------|
| `prepend` / `append` / `trim` | Tested | `EventLogCapTest`; `MemorySoakTest` | Death-marker preservation |

---

## `FamilyGenerator`

| Function | Status | Test(s) | Notes |
|----------|--------|---------|-------|
| `generateFamily` | Low priority | — | Character-creation bootstrap; delegates to `PersonGenerator` |

---

## Data layer & cross-cutting (not in §4 table but audited)

| Component | Status | Test(s) | Notes |
|-----------|--------|---------|-------|
| `EventRepository.getEligibleEvents` | Tested | `EventRepositoryAgeIndexTest` (age-bucket parity); **Gap closed (P50)** `EventRepositoryFilterTest` (`restrictedToCountry`, one-time, `career_system` exclusion) | Incarceration gated in `GameEngine`, not repository |
| `FlavorInterpolator` / placeholder resolution | Tested | `FlavorInterpolatorTest` | Separate from repository |
| `SerializationUtils.safeDeserialize` | Tested | `SerializationUtilsTest` | |
| `CharacterSaveMapper` | Gap closed (P50) | `CharacterSaveMapperTest` — family, gender, `isCorrupted` slot path, malformed `ancestryHistory`, `relocationHistory`, `criminalRecord`, `activeConditions` | |
| `CharacterRepository` corruption | Tested | `CharacterRepositoryCorruptionTest` (DB unavailable); `CharacterSaveMapperTest.toSlotSummary_corruptedRow_setsIsCorrupted` (`isCorrupted` row path) | Two distinct paths |
| `EventRepository.forTesting()` | Used | `TestFixtures.gameEngine()`, `EventRepositoryAgeIndexTest`, `EventRepositoryEdgeCaseTest`, `EventRepositoryFilterTest`, `PerformanceBenchmarkTest` | No production ctor in JVM tests |
| `NotificationScheduler.forTesting()` | Used | `TestFixtures.gameEngine()`, `NotificationSchedulerTest` | No production ctor in JVM tests |

---

## Test fixture convention

**Single entry point:** `app/src/test/java/com/maisha/game/domain/TestFixtures.kt`

- `character()`, `person()`, `job()`, `asset()`, `child()` — lightweight builders (not a framework).
- `gameEngine()` — wires `EventRepository.forTesting()` and `NotificationScheduler.forTesting()`.

Prompt 50 consolidated `LegacyEngineTest` child helpers onto `TestFixtures.child()`. Some older tests (`RelationshipEngineTickTest`, `MixedHeritageTest`, `FriendshipGenerationTest`) still hand-roll `Character`/`Person`; behavior is equivalent — migrate opportunistically, not required for correctness.

---

## Remaining low-priority gaps (no action in P50)

Thin delegates, event builders, and character-creation bootstrap (`FamilyGenerator`, `introEventsForNewborn`) are intentionally untested at unit level. Revisit only if bugs appear in those paths.
