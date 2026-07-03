# Test coverage map (ARCHITECTURE.md §4)

Cross-reference of domain engine public APIs against JVM unit tests.

| Status | Meaning |
|--------|---------|
| **Tested** | Dedicated correctness test(s) exist |
| **Perf only** | Benchmark/timing test only — correctness covered elsewhere |
| **Indirect** | Exercised via `GameEngine` integration or related utility test |
| **Gap closed (P50)** | Gap confirmed in audit; test added in Prompt 50 |
| **Low priority** | No dedicated test; thin delegate or creation-time-only path |

Last audited: **2026-07-03** (Prompt 54).

---

## `GameEngine`

| Function | Status | Test(s) | Notes |
|----------|--------|---------|-------|
| `ageUp` | Tested | `GameEngineTest`; `PerformanceBenchmarkTest`; `MemorySoakTest` | Mortality ordering, incarceration skip, no achievements on death year |
| `introEventsForNewborn` | Low priority | — | Age-0 bootstrap |
| `applyChoice` | Tested | `GameEngineTest.applyChoice_clampsStatsToZeroAndOneHundred` | |
| `applyForJob` / `quitJob` | Indirect | `CareerEngineTest`, `GameEngineTest` | |
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
| `enrollIfEligible` | Indirect | `EducationEngineTest` | |
| `advanceGrade` | Indirect | `EducationEngineTest` | |
| `applyStudyEffort` | Indirect | `EducationEngineTest` | |
| `advanceUniversityYear` | Indirect | `EducationEngineTest` | |
| `takeExam` | Tested | `EducationEngineTest.takeExam_highStatsPassMoreOftenThanLowStats` | |
| `applyToUniversity` | Tested | `EducationEngineTest.applyToUniversity_*` | |
| `isEligibleForUniversity` | Indirect | `EducationEngineTest` | |
| `shouldTriggerPrimaryExam` / `shouldTriggerSecondaryExam` | Gap closed (P50/P53) | `EducationEngineTest` — multi-country KE/NG/US/GB | Country-agnostic logic |
| `buildExamResultEvent` | Low priority | — | Presentation builder |
| `applyGpaEffect` | Indirect | `EducationEngineTest` | |

---

## `CareerEngine`

| Function | Status | Test(s) | Notes |
|----------|--------|---------|-------|
| `isJobEligible` | Indirect | `CareerEngineTest` | |
| `getEligibleJobs` | Indirect | `CareerEngineTest` | |
| `applyForJob` | Tested | `CareerEngineTest.criminalRecord_reducesHireSuccessRate` | |
| `workYear` | Indirect | `CareerEngineTest` | |
| `applyWorkEffort` | Tested | `CareerEngineTest` (grind vs coast) | |
| `evaluatePromotion` | Tested | `CareerEngineTest.evaluatePromotion_*` | Boundary values |
| `evaluateFiring` | Tested | `CareerEngineTest.evaluateFiring_*` | Boundary values |
| `quitJob` | Low priority | — | |
| `applyPerformanceEffect` | Indirect | `CareerEngineTest` | |
| `buildPromotionEvent` / `buildFiringEvent` / `buildDownsizingEvent` | Low priority | — | |
| `shouldTriggerDownsizing` | Gap closed (P50) | `CareerEngineTest.shouldTriggerDownsizing_falseWhenUnemployed` | |
| `applyDownsizing` | Low priority | — | |
| `calculateAnnualSalary` | Tested | `BalanceTuningTest` | |

---

## `FinanceEngine`

| Function | Status | Test(s) | Notes |
|----------|--------|---------|-------|
| `purchaseAsset` | Tested | `FinanceEngineTest.purchaseAsset_*` | |
| `sellAsset` | Indirect | `FinanceEngineTest` | |
| `applyUpkeep` | Indirect | `FinanceEngineTest` | |
| `degradeAssets` | Tested | `FinanceEngineTest.degradeAssets_reducesConditionOverYears` | |
| `calculateNetWorth` | Indirect | `FinanceEngineTest`, `AchievementWealthTest` | |
| `applyConditionToAssetType` / `applyConditionToFirstAsset` | Low priority | — | |
| `meetsFinanceEventThreshold` | Indirect | `FinanceEngineTest` | |
| `recalculateValue` | Indirect | `FinanceEngineTest` | |

---

## `RelationshipEngine`

| Function | Status | Test(s) | Notes |
|----------|--------|---------|-------|
| `tickFamilyYear` | Tested | `RelationshipEngineTickTest`, `RelationshipEngineTest` | Decay toward neutral |
| `findDatingProspects` | Indirect | `RelationshipEngineTest` | |
| `generateFriendshipOpportunity` | Tested | `FriendshipGenerationTest` | |
| `progressRelationship` | Indirect | `RelationshipEngineTest` | |
| `proposeMarriage` | Tested | `RelationshipEngineTest.proposeMarriage_rejectsBelowThreshold` | |
| `startDating` / `breakUpOrDivorce` | Indirect | `RelationshipEngineTest` | |
| `haveChild` | Tested | `MixedHeritageTest`, `RelationshipEngineTest` | |
| `applySpouseRelationshipEffect` | Low priority | — | |
| `applyLegacyFamilyMilestones` | Indirect | `LegacyEngineTest` | |
| `getRelationshipTier` | Tested | `RelationshipTierTest` | |
| `canTravelTogether` | Low priority | — | |

---

## `CrimeEngine`

| Function | Status | Test(s) | Notes |
|----------|--------|---------|-------|
| `attemptCrime` | Low priority | — | Probabilistic |
| `processArrest` | Tested | `CrimeEngineTest.crimeTypes_produceDifferentSentenceLengthsWhenCaught` | |
| `serveYear` | Indirect | `GameEngineTest` (incarceration age-up) | |

---

## `HealthEngine`

| Function | Status | Test(s) | Notes |
|----------|--------|---------|-------|
| `rollForIllness` | Tested | `HealthEngineTest` | |
| `addCondition` | Indirect | `HealthEngineTest` | |
| `applyUntreatedConditions` | Indirect | `HealthEngineTest` | |
| `visitDoctor` / `visitFirstUntreatedCondition` | Indirect | `HealthEngineTest` | |

---

## `MortalityEngine`

| Function | Status | Test(s) | Notes |
|----------|--------|---------|-------|
| `checkDeath` | Tested | `MortalityEngineTest` | |
| `applyDeath` | Indirect | `MortalityEngineTest`, `GameEngineTest` | |
| `parseDeathCause` / `deathFlavorText` / `gentleCauseLabel` | Low priority | — | |

---

## `AchievementEngine`

| Function | Status | Test(s) | Notes |
|----------|--------|---------|-------|
| `checkAchievements` | Tested | `AchievementEngineTest`; `AchievementWealthTest`; `PerformanceBenchmarkTest` | |

---

## `LegacyEngine`

| Function | Status | Test(s) | Notes |
|----------|--------|---------|-------|
| `eligibleHeirs` | Tested | `LegacyEngineTest` | |
| `createLegacyCharacter` | Tested | `LegacyEngineTest`, `LegacyAncestryTest` | |
| `buildAncestryEntry` | Indirect | `LegacyAncestryTest` | |
| `mapSurvivingParent` | Tested | `LegacyEngineTest` | |

---

## `RelocationEngine`

| Function | Status | Test(s) | Notes |
|----------|--------|---------|-------|
| `hasRelocated` | Tested | `RelocationEngineTest` | |
| `shouldOfferRelocation` | Gap closed (P50) | `RelocationEngineTest` | |
| `getRelocationOpportunities` | Tested | `RelocationEngineTest` | |
| `buildRelocationOpportunityEvent` | Low priority | — | |
| `relocate` | Tested | `RelocationEngineTest.relocate switches country and clears current job` | |

---

## Cap utilities

### `EventLogCap`

| Function | Status | Test(s) | Notes |
|----------|--------|---------|-------|
| `prepend` / `append` / `trim` | Tested | `EventLogCapTest`; `MemorySoakTest` | Death-marker prefix at line start only (P54) |

### `AncestryHistoryCap`

| Function | Status | Test(s) | Notes |
|----------|--------|---------|-------|
| `trim` | Tested | `AncestryHistoryCapTest`; `MemorySoakTest` | |

### `RelationshipMilestoneCap`

| Function | Status | Test(s) | Notes |
|----------|--------|---------|-------|
| `trim` / `trimFamily` | Tested | `RelationshipMilestoneCapTest`; `MemorySoakTest` | Max 25 per person (P54) |

---

## `FamilyGenerator`

| Function | Status | Test(s) | Notes |
|----------|--------|---------|-------|
| `generateFamily` | Low priority | — | Character-creation bootstrap |

---

## Data layer & cross-cutting

| Component | Status | Test(s) | Notes |
|-----------|--------|---------|-------|
| `EventRepository.getEligibleEvents` | Tested | `EventRepositoryAgeIndexTest`; `EventRepositoryFilterTest` (P50) | |
| `FlavorInterpolator` | Tested | `FlavorInterpolatorTest` | |
| `SerializationUtils.safeDeserialize` | Tested | `SerializationUtilsTest` | |
| `CharacterSaveMapper` | Gap closed (P50) | `CharacterSaveMapperTest` | |
| `CharacterRepository` corruption | Tested | `CharacterRepositoryCorruptionTest` | |
| `CountryCatalog` content | Tested | `CountryCatalogContentTest`, `GlobalContentCoverageTest` | |
| `NotificationScheduler` | Tested | `NotificationSchedulerTest` | |
| `ExpressionResolver` | Tested | `ExpressionResolverTest` | |
| `EnglishOrdinals` | Tested | `EnglishOrdinalsTest` | |
| `Stats` edge cases | Tested | `StatsEdgeCaseTest` | |

---

## Test file inventory (38 classes)

**Domain:** `AchievementEngineTest`, `AncestryHistoryCapTest`, `BalanceTuningTest`, `CareerEngineTest`, `CrimeEngineTest`, `EducationEngineTest`, `EffortResolverTest`, `EventLogCapTest`, `FinanceEngineTest`, `FriendshipGenerationTest`, `GameEngineTest`, `HealthEngineTest`, `LegacyAncestryTest`, `LegacyEngineTest`, `MemorySoakTest`, `MixedHeritageTest`, `MortalityEngineTest`, `PerformanceBenchmarkTest`, `RelationshipEngineTest`, `RelationshipEngineTickTest`, `RelationshipMilestoneCapTest`, `RelocationEngineTest`, `TestFixtures`

**Data:** `AchievementWealthTest`, `CountryCatalogContentTest`, `GlobalContentCoverageTest`, `FlavorInterpolatorTest`, `EventRepositoryAgeIndexTest`, `EventRepositoryEdgeCaseTest`, `EventRepositoryFilterTest`, `CharacterSaveMapperTest`, `CharacterRepositoryCorruptionTest`

**Model/UI/Util/Notifications:** `StatsEdgeCaseTest`, `RelationshipTierTest`, `ExpressionResolverTest`, `EnglishOrdinalsTest`, `SerializationUtilsTest`, `NotificationSchedulerTest`

---

## Test fixture convention

**Single entry point:** `app/src/test/java/com/maisha/game/domain/TestFixtures.kt`

- `character()`, `person()`, `job()`, `asset()`, `child()` — lightweight builders.
- `gameEngine()` — wires `EventRepository.forTesting()` and `NotificationScheduler.forTesting()`.

---

## How to run

```bash
./gradlew testDebugUnitTest
./gradlew testDebugUnitTest --tests "com.maisha.game.domain.*"
```

---

*Updated through Prompt 54 (July 2026).*
