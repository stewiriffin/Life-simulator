// app/src/main/java/com/maisha/game/domain/GameEngine.kt (modified — notification nudge hooks)
package com.maisha.game.domain

import com.maisha.game.data.events.EventRepository
import com.maisha.game.data.FlavorInterpolator
import com.maisha.game.data.model.AgingDetails
import com.maisha.game.data.model.AssetType
import com.maisha.game.data.model.AchievementProgress
import com.maisha.game.data.model.AvatarConfig
import com.maisha.game.data.model.Character
import com.maisha.game.data.model.EventChoice
import com.maisha.game.data.model.ExamType
import com.maisha.game.data.model.EyewearStyle
import com.maisha.game.data.model.FacialHairStyle
import com.maisha.game.data.model.Gender
import com.maisha.game.data.model.LifestyleOption
import com.maisha.game.data.model.LifeEvent
import com.maisha.game.data.model.Achievement
import com.maisha.game.data.model.Person
import com.maisha.game.data.model.RelationType
import com.maisha.game.data.model.SchoolStage
import com.maisha.game.data.model.StudyEffort
import com.maisha.game.data.model.WorkEffort
import com.maisha.game.data.model.ageStageFor
import com.maisha.game.data.model.AgeStage
import com.maisha.game.notifications.NotificationScheduler
import com.maisha.game.notifications.NudgeType
import com.maisha.game.util.clampRelationshipLevel
import com.maisha.game.util.formatMoney
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

sealed class AgeUpResult {
    data object NoEvent : AgeUpResult()
    data class SingleEvent(val event: LifeEvent) : AgeUpResult()
    data class MultipleEvents(val events: List<LifeEvent>) : AgeUpResult()
}

data class FamilyInteractionResult(
    val character: Character,
    val message: String
)

data class AgeUpOutcome(
    val character: Character,
    val result: AgeUpResult,
    val newlyUnlockedAchievements: List<Achievement> = emptyList(),
    val relationshipDecayNotices: List<com.maisha.game.data.model.RelationshipDecayNotice> = emptyList(),
    val newFriendName: String? = null
)

/**
 * Orchestrates the yearly life loop and player actions by delegating to domain engines.
 *
 * Single entry point for age-up sequencing; keeps Android out of pure engine logic.
 */
@Singleton
class GameEngine @Inject constructor(
    private val eventRepository: EventRepository,
    private val educationEngine: EducationEngine,
    private val careerEngine: CareerEngine,
    private val financeEngine: FinanceEngine,
    private val relationshipEngine: RelationshipEngine,
    private val mortalityEngine: MortalityEngine,
    private val crimeEngine: CrimeEngine,
    private val healthEngine: HealthEngine,
    private val achievementEngine: AchievementEngine,
    private val notificationScheduler: NotificationScheduler,
    private val relocationEngine: RelocationEngine,
    private val socialMediaEngine: SocialMediaEngine,
    private val skillEngine: SkillEngine,
    private val businessEngine: BusinessEngine
) {

    /**
     * Advances one in-game year: runs education/career (unless incarcerated), finance, relationships, health,
     * friends, prison, then career/exam/random events, mortality last, achievements only if still alive.
     *
     * @param triggeredEventIds One-time event ids already consumed in this slot.
     * @param achievementProgress Global unlock state for [AchievementEngine.checkAchievements].
     * @return [AgeUpOutcome] with optional pending events and newly unlocked achievements.
     */
    fun ageUp(
        character: Character,
        triggeredEventIds: Set<String>,
        achievementProgress: List<AchievementProgress>,
        slotId: Int
    ): AgeUpOutcome {
        if (!character.alive) {
            return AgeUpOutcome(character, AgeUpResult.NoEvent)
        }

        val preStage = character.education.stage
        val previousAge = character.age
        var updatedCharacter = character.copy(
            age = character.age + 1,
            yearsInCurrentCountry = character.yearsInCurrentCountry + 1,
            criminalRecord = character.criminalRecord.copy(crimeAttemptsThisYear = 0),
            career = character.career.copy(
                sideHustleDoneThisYear = false,
                workEffortThisYear = null
            )
        )
        updatedCharacter = applyAvatarVisualEvolution(updatedCharacter, previousAge)
        updatedCharacter = socialMediaEngine.resetYearlyFlags(updatedCharacter)
        var decayNotices = emptyList<com.maisha.game.data.model.RelationshipDecayNotice>()

        val incarceratedAtYearStart = updatedCharacter.criminalRecord.currentlyIncarcerated
        if (incarceratedAtYearStart) {
            // Education and career progression skipped while incarcerated.
        } else {
            updatedCharacter = educationEngine.enrollIfEligible(updatedCharacter)
            updatedCharacter = processEducationProgression(updatedCharacter, preStage)
            updatedCharacter = processCareerProgression(updatedCharacter)
        }

        updatedCharacter = processFinanceProgression(updatedCharacter)
        updatedCharacter = businessEngine.processBusinessYear(updatedCharacter)

        updatedCharacter = applyCultureShockPenalty(updatedCharacter)

        val tickResult = relationshipEngine.tickFamilyYear(updatedCharacter)
        updatedCharacter = updatedCharacter.copy(
            family = tickResult.family,
            stats = tickResult.stats
        )
        decayNotices = tickResult.decayNotices
        updatedCharacter = relationshipEngine.tickPetsYear(updatedCharacter).character
        updatedCharacter = relationshipEngine.applySpouseRelationshipEffect(
            updatedCharacter,
            netWorth = financeEngine.calculateNetWorth(updatedCharacter)
        )

        updatedCharacter = processHealthProgression(updatedCharacter)
        updatedCharacter = applyAvatarHealthVisuals(updatedCharacter)

        var newFriendName: String? = null
        if (!incarceratedAtYearStart) {
            relationshipEngine.generateFriendshipOpportunity(updatedCharacter)?.let { friend ->
                updatedCharacter = updatedCharacter.copy(family = updatedCharacter.family + friend)
                newFriendName = friend.name
            }
        }

        if (incarceratedAtYearStart) {
            updatedCharacter = crimeEngine.serveYear(updatedCharacter)
        }

        val (characterAfterEvents, result) = if (incarceratedAtYearStart) {
            updatedCharacter to rollEvents(updatedCharacter, triggeredEventIds).toAgeUpResult()
        } else {
            resolveCareerEvent(updatedCharacter)
                ?: resolveExamEvent(updatedCharacter)
                ?: (updatedCharacter to rollEvents(updatedCharacter, triggeredEventIds).toAgeUpResult())
        }

        val outcome = finalizeYear(characterAfterEvents, result, achievementProgress)
        scheduleNotificationNudges(slotId, outcome)
        return outcome.copy(
            relationshipDecayNotices = decayNotices,
            newFriendName = newFriendName
        )
    }

    private fun scheduleNotificationNudges(slotId: Int, outcome: AgeUpOutcome) {
        val character = outcome.character
        if (!character.alive) return

        if (character.activeConditions.any { !it.treated && it.yearsUntreated >= 2 }) {
            notificationScheduler.scheduleContextualNudge(
                slotId = slotId,
                nudgeType = NudgeType.UNTREATED_CONDITION,
                delayHours = 4L
            )
        }

        when (outcome.result) {
            is AgeUpResult.SingleEvent, is AgeUpResult.MultipleEvents -> {
                notificationScheduler.scheduleContextualNudge(
                    slotId = slotId,
                    nudgeType = NudgeType.PENDING_LIFE_DECISION,
                    delayHours = 6L
                )
            }
            AgeUpResult.NoEvent -> Unit
        }
    }

    private fun processHealthProgression(character: Character): Character =
        healthEngine.processHealthProgression(character)

    private fun applyCultureShockPenalty(character: Character): Character {
        if (!careerEngine.isCultureShockActive(character)) return character
        return character.copy(
            stats = character.stats.copy(
                happiness = com.maisha.game.util.clampStat(
                    character.stats.happiness - CULTURE_SHOCK_HAPPINESS_PENALTY
                )
            )
        )
    }

    private fun finalizeYear(
        character: Character,
        result: AgeUpResult,
        achievementProgress: List<AchievementProgress>
    ): AgeUpOutcome {
        val (finalCharacter, finalResult) = when (val death = mortalityEngine.checkDeath(character)) {
            is DeathResult.Died -> {
                val deadCharacter = mortalityEngine.applyDeath(
                    character,
                    death.cause,
                    death.ageAtDeath
                )
                deadCharacter to AgeUpResult.NoEvent
            }
            DeathResult.Alive -> character to result
        }
        val newlyUnlocked = if (finalCharacter.alive) {
            achievementEngine.checkAchievements(finalCharacter, achievementProgress)
        } else {
            emptyList()
        }
        return AgeUpOutcome(finalCharacter, finalResult, newlyUnlocked)
    }

    /** Picks a starter event for age 0 from JSON pool, respecting [triggeredEventIds]. */
    fun introEventsForNewborn(triggeredEventIds: Set<String>): AgeUpResult {
        val eligible = eventRepository.getEligibleEvents(age = 0, usedIds = triggeredEventIds)
        val event = eventRepository.pickRandomEvent(eligible) ?: return AgeUpResult.NoEvent
        return AgeUpResult.SingleEvent(event)
    }

    /**
     * Applies a player [choice] from a [LifeEvent]: study/work effort, relationship deltas, crime, doctor,
     * relocation, assets, GPA, university enrollment, and flat stat effects.
     */
    fun applyChoice(character: Character, choice: EventChoice, event: LifeEvent): Character {
        var updatedCharacter = character

        if (EventRepository.STUDY_EFFORT_TAG in event.tags) {
            val effort = studyEffortFromChoice(choice)
            updatedCharacter = educationEngine.applyStudyEffort(updatedCharacter, effort)
        }

        if (EventRepository.WORK_EFFORT_TAG in event.tags) {
            val effort = workEffortFromChoice(choice)
            updatedCharacter = careerEngine.applyWorkEffort(updatedCharacter, effort)
        }

        if (choice.siblingRelationshipEffect != 0) {
            updatedCharacter = applySiblingRelationshipEffect(
                updatedCharacter,
                choice.siblingRelationshipEffect
            )
        }

        if (choice.familyRelationshipEffect != 0) {
            updatedCharacter = applyFamilyRelationshipEffect(
                updatedCharacter,
                choice.familyRelationshipEffect
            )
        }

        if (choice.spouseRelationshipEffect != 0) {
            updatedCharacter = relationshipEngine.applySpouseRelationshipEffect(
                updatedCharacter,
                choice.spouseRelationshipEffect
            )
        }

        if (choice.childRelationshipEffect != 0) {
            updatedCharacter = applyChildRelationshipEffect(
                updatedCharacter,
                choice.childRelationshipEffect
            )
        }

        if (choice.triggersHaveChild) {
            updatedCharacter = relationshipEngine.haveChild(updatedCharacter)
        }

        if (choice.triggersCrime != null) {
            updatedCharacter = applyCrimeChoice(updatedCharacter, choice.triggersCrime)
        }

        if (choice.triggersIllnessRoll) {
            healthEngine.rollForIllness(updatedCharacter)?.let { condition ->
                updatedCharacter = healthEngine.addCondition(updatedCharacter, condition)
            }
        }

        if (choice.doctorCareTier != null) {
            var doctorCharacter = updatedCharacter
            if (doctorCharacter.activeConditions.none { !it.treated }) {
                healthEngine.rollForIllness(doctorCharacter)?.let { condition ->
                    doctorCharacter = healthEngine.addCondition(doctorCharacter, condition)
                }
            }
            val usePrivate = choice.doctorCareTier.equals("private", ignoreCase = true)
            updatedCharacter = when (
                val doctorResult = healthEngine.visitFirstUntreatedCondition(
                    doctorCharacter,
                    usePrivate
                )
            ) {
                is DoctorResult.Treated -> doctorResult.character
                is DoctorResult.Failed -> doctorResult.character
            }
        }

        if (choice.performanceEffect != 0) {
            updatedCharacter = careerEngine.applyPerformanceEffect(
                updatedCharacter,
                choice.performanceEffect
            )
        }

        if (choice.paroleEffect != 0) {
            updatedCharacter = crimeEngine.applyPrisonChoiceEffect(
                updatedCharacter,
                choice.paroleEffect
            )
        }

        if (choice.relocateToCountry != null) {
            val destination = com.maisha.game.data.CountryCatalog.getCountry(choice.relocateToCountry)
            updatedCharacter = relocationEngine.relocate(updatedCharacter, destination)
        }

        if (FlavorInterpolator.HOLIDAY_TAG in event.tags) {
            updatedCharacter = updatedCharacter.copy(lastHolidayAge = updatedCharacter.age)
        }

        if (choice.conditionEffect != 0) {
            updatedCharacter = if (choice.targetAssetType != null) {
                runCatching {
                    financeEngine.applyConditionToAssetType(
                        updatedCharacter,
                        AssetType.valueOf(choice.targetAssetType),
                        choice.conditionEffect
                    )
                }.getOrDefault(updatedCharacter)
            } else {
                financeEngine.applyConditionToFirstAsset(updatedCharacter, choice.conditionEffect)
            }
        }

        if (choice.forceConditionValue != null) {
            updatedCharacter = if (choice.targetAssetType != null) {
                runCatching {
                    financeEngine.setAssetConditionByType(
                        updatedCharacter,
                        AssetType.valueOf(choice.targetAssetType),
                        choice.forceConditionValue
                    )
                }.getOrDefault(updatedCharacter)
            } else {
                financeEngine.applyConditionToFirstAsset(
                    updatedCharacter,
                    choice.forceConditionValue - (updatedCharacter.assets.firstOrNull()?.condition ?: 0)
                )
            }
        }

        updatedCharacter = educationEngine.applyGpaEffect(updatedCharacter, choice.gpaEffect)

        if (choice.universityCourse != null) {
            updatedCharacter = educationEngine.applyToUniversity(
                updatedCharacter,
                choice.universityCourse
            )
        }

        if (choice.triggersExpulsion) {
            updatedCharacter = educationEngine.processExpulsion(updatedCharacter)
            updatedCharacter = relationshipEngine.applyExpulsionFamilyEffect(updatedCharacter)
        }

        if (choice.triggersDropout) {
            updatedCharacter = educationEngine.processDropout(updatedCharacter)
        }

        if (choice.economicShift != null) {
            updatedCharacter = financeEngine.applyEconomicShift(
                updatedCharacter,
                forced = choice.economicShift
            ).character
        }

        if (choice.grantHeirloom != null) {
            updatedCharacter = financeEngine.grantHeirloom(updatedCharacter, choice.grantHeirloom)
        }

        if (choice.followerEffect != 0 && updatedCharacter.socialMedia.hasAccount) {
            val newFollowers = (updatedCharacter.socialMedia.followers + choice.followerEffect)
                .coerceAtLeast(0)
            val isVerified = updatedCharacter.socialMedia.isVerified ||
                newFollowers >= SocialMediaEngine.VERIFIED_FOLLOWER_THRESHOLD
            updatedCharacter = updatedCharacter.copy(
                socialMedia = updatedCharacter.socialMedia.copy(
                    followers = newFollowers,
                    isVerified = isVerified
                )
            )
        }

        if (choice.businessValuationEffect != 0 || choice.businessRevenueEffect != 0) {
            updatedCharacter = businessEngine.applyBusinessEffects(
                updatedCharacter,
                choice.businessValuationEffect,
                choice.businessRevenueEffect
            )
        }

        val updatedStats = updatedCharacter.stats.applyEffects(choice.statEffects)
        val updatedLog = EventLogCap.prepend(updatedCharacter.eventLog, choice.resultText)
        return updatedCharacter.copy(stats = updatedStats, eventLog = updatedLog)
    }

    /** Delegates to [CareerEngine]; rejects immediately if incarcerated. */
    fun applyForJob(character: Character, jobId: String): Pair<Character, CareerResult> {
        if (character.criminalRecord.currentlyIncarcerated) {
            return character to CareerResult.Rejected
        }
        return careerEngine.applyForJob(character, jobId)
    }

    /** Delegates to [CareerEngine.quitJob]. */
    fun quitJob(character: Character): Character {
        return careerEngine.quitJob(character)
    }

    /** Delegates to [CareerEngine.retire]. */
    fun retire(character: Character): RetirementResult {
        return careerEngine.retire(character)
    }

    /** Delegates to [CareerEngine.executeSideHustle]. */
    fun executeSideHustle(
        character: Character,
        hustleType: com.maisha.game.data.model.HustleType
    ): SideHustleResult = careerEngine.executeSideHustle(character, hustleType)

    /** Delegates to [RelationshipEngine.adoptPet]. */
    fun adoptPet(
        character: Character,
        species: com.maisha.game.data.model.PetSpecies,
        name: String
    ): AdoptPetResult = relationshipEngine.adoptPet(character, species, name)

    fun createSocialMediaAccount(character: Character): SocialMediaResult =
        socialMediaEngine.createAccount(character)

    fun deleteSocialMediaAccount(character: Character): SocialMediaResult =
        socialMediaEngine.deleteAccount(character)

    fun postSocialMediaContent(character: Character): SocialMediaResult =
        socialMediaEngine.postContent(character)

    fun monetizeSocialMediaAccount(character: Character): SocialMediaResult =
        socialMediaEngine.monetizeAccount(character)

    fun practiceSkill(
        character: Character,
        skillType: com.maisha.game.data.model.SkillType
    ): SkillResult = skillEngine.practiceSkill(character, skillType)

    fun takeMasterclass(
        character: Character,
        skillType: com.maisha.game.data.model.SkillType
    ): SkillResult = skillEngine.takeMasterclass(character, skillType)

    fun masterclassCost(character: Character): Int = skillEngine.masterclassCost(character)

    fun startBusiness(
        character: Character,
        name: String,
        industry: com.maisha.game.data.model.BusinessIndustry,
        initialInvestment: Int
    ): BusinessResult = businessEngine.startBusiness(character, name, industry, initialInvestment)

    fun sellBusiness(character: Character, businessId: String): BusinessResult =
        businessEngine.sellBusiness(character, businessId)

    fun businessInvestmentTiers(character: Character): List<Int> =
        businessEngine.investmentTiers(character)

    /** Voluntary school leave — delegates to [EducationEngine.processDropout]. */
    fun dropOut(character: Character): Character {
        return educationEngine.processDropout(character)
    }

    /** Delegates to [CareerEngine.getEligibleJobs]. */
    fun getEligibleJobs(character: Character) = careerEngine.getEligibleJobs(character)

    /** Delegates to [FinanceEngine.purchaseAsset]. */
    fun purchaseAsset(character: Character, catalogId: String): PurchaseResult {
        return financeEngine.purchaseAsset(character, catalogId)
    }

    /** Delegates to [FinanceEngine.sellAsset]. */
    fun sellAsset(character: Character, assetId: String): Character {
        return financeEngine.sellAsset(character, assetId)
    }

    /** Delegates to [FinanceEngine.repairAsset]. */
    fun repairAsset(character: Character, assetId: String): RepairResult {
        return financeEngine.repairAsset(character, assetId)
    }

    /** Delegates to [FinanceEngine.calculateNetWorth]. */
    fun calculateNetWorth(character: Character): Int {
        return financeEngine.calculateNetWorth(character)
    }

    /** Delegates to [RelationshipEngine.progressRelationship]. */
    fun interactWithFamilyMember(
        character: Character,
        personId: String,
        interactionType: InteractionType,
        giftTier: GiftTier? = null
    ): FamilyInteractionResult {
        return relationshipEngine.progressRelationship(character, personId, interactionType, giftTier)
    }

    /** Delegates to [RelationshipEngine.findDatingProspects]. */
    fun findDatingProspects(character: Character) =
        relationshipEngine.findDatingProspects(character)

    /** Delegates to [RelationshipEngine.startDating]. */
    fun startDating(character: Character, prospect: Person): Character =
        relationshipEngine.startDating(character, prospect)

    /** Delegates to [RelationshipEngine.proposeMarriage]. */
    fun proposeMarriage(character: Character, personId: String) =
        relationshipEngine.proposeMarriage(character, personId)

    /** Delegates to [RelationshipEngine.breakUpOrDivorce]. */
    fun breakUpOrDivorce(character: Character, personId: String): Character =
        relationshipEngine.breakUpOrDivorce(character, personId)

    /** Delegates to [RelationshipEngine.haveChild]. */
    fun haveChild(character: Character): Character =
        relationshipEngine.haveChild(character)

    /** Delegates to [RelationshipEngine.applyLegacyFamilyMilestones]. */
    fun applyLegacyFamilyMilestones(character: Character): Character =
        relationshipEngine.applyLegacyFamilyMilestones(character)

    /** Delegates to [CrimeEngine.attemptCrime]. */
    fun attemptCrime(character: Character, crimeType: com.maisha.game.data.model.CrimeType): CrimeResult =
        crimeEngine.attemptCrime(character, crimeType)

    /** Delegates to [CrimeEngine.goToTrial]. */
    fun goToTrial(
        character: Character,
        lawyerTier: com.maisha.game.data.model.LawyerTier
    ): TrialResult = crimeEngine.goToTrial(
        character = character,
        lawyerTier = lawyerTier,
        netWorth = financeEngine.calculateNetWorth(character)
    )

    fun lawyerFee(
        character: Character,
        lawyerTier: com.maisha.game.data.model.LawyerTier
    ): Int = crimeEngine.lawyerFee(
        lawyerTier = lawyerTier,
        netWorth = financeEngine.calculateNetWorth(character)
    )

    fun canAffordLawyer(
        character: Character,
        lawyerTier: com.maisha.game.data.model.LawyerTier
    ): Boolean = crimeEngine.canAffordLawyer(
        character = character,
        lawyerTier = lawyerTier,
        netWorth = financeEngine.calculateNetWorth(character)
    )

    /** Delegates to [HealthEngine.visitDoctor]. */
    fun visitDoctor(
        character: Character,
        conditionId: String,
        usePrivateCare: Boolean
    ): DoctorResult = healthEngine.visitDoctor(character, conditionId, usePrivateCare)

    /** Toggles a recurring lifestyle subscription on or off. */
    fun setLifestyleOption(
        character: Character,
        option: LifestyleOption,
        enabled: Boolean
    ): Character = healthEngine.setLifestyleOption(character, option, enabled)

    private fun applyCrimeChoice(character: Character, crimeTypeName: String): Character {
        val crimeType = runCatching {
            com.maisha.game.data.model.CrimeType.valueOf(crimeTypeName.uppercase())
        }.getOrNull() ?: return character
        return when (val result = crimeEngine.attemptCrime(character, crimeType)) {
            is CrimeResult.Success -> result.character.copy(
                eventLog = EventLogCap.prepend(
                    result.character.eventLog,
                    "Got away with ${crimeType.name.lowercase()} and gained " +
                        "${formatMoney(result.moneyGained, result.character.countryCode)}."
                )
            )
            is CrimeResult.Caught -> result.character
        }
    }

    /**
     * Organic avatar changes: senior graying/wrinkles, teen style shifts, adult facial hair.
     */
    private fun applyAvatarVisualEvolution(character: Character, previousAge: Int): Character {
        val previousStage = ageStageFor(previousAge)
        val stage = ageStageFor(character.age)
        var config = character.avatarConfig

        if (previousStage != AgeStage.SENIOR && stage == AgeStage.SENIOR) {
            config = config.copy(
                hairColor = AvatarConfig.GRAY_HAIR_COLOR_INDEX,
                agingDetails = AgingDetails.WRINKLES_AND_GRAYING
            )
        }

        if (previousStage == AgeStage.CHILD && stage == AgeStage.TEEN &&
            Random.nextFloat() < TEEN_HAIRSTYLE_CHANGE_CHANCE
        ) {
            config = config.copy(hairStyle = Random.nextInt(AvatarConfig.HAIR_STYLE_COUNT))
        }

        if (character.gender == Gender.MALE &&
            config.facialHair == null &&
            stage in listOf(AgeStage.TEEN, AgeStage.ADULT) &&
            character.age >= FACIAL_HAIR_MIN_AGE &&
            Random.nextFloat() < FACIAL_HAIR_GROWTH_CHANCE
        ) {
            config = config.copy(facialHair = FacialHairStyle.entries.random())
        }

        return if (config == character.avatarConfig) character
        else character.copy(avatarConfig = config)
    }

    /** Equips glasses when the character has untreated Poor Eyesight. */
    private fun applyAvatarHealthVisuals(character: Character): Character {
        val hasPoorEyesight = character.activeConditions.any {
            !it.treated &&
                it.name.equals(HealthEngine.POOR_EYESIGHT_CONDITION, ignoreCase = true)
        }
        if (!hasPoorEyesight) return character
        if (character.avatarConfig.eyewear != null) return character
        return character.copy(
            avatarConfig = character.avatarConfig.copy(eyewear = EyewearStyle.GLASSES)
        )
    }

    private fun processEducationProgression(
        character: Character,
        preStage: SchoolStage
    ): Character {
        val stage = character.education.stage
        if (character.education.expelled) return character

        return when (stage) {
            SchoolStage.PRIMARY, SchoolStage.SECONDARY -> {
                if (preStage == stage) {
                    educationEngine.advanceGrade(character, StudyEffort.NORMAL)
                } else {
                    character
                }
            }
            SchoolStage.UNIVERSITY -> educationEngine.advanceUniversityYear(character)
            else -> character
        }
    }

    private fun processCareerProgression(character: Character): Character {
        if (character.career.isRetired) return character
        if (character.career.currentJob == null) return character
        return careerEngine.workYear(character, WorkEffort.NORMAL)
    }

    private fun processFinanceProgression(character: Character): Character {
        var updated = financeEngine.applyEconomicShift(character).character
        updated = financeEngine.applyPension(updated)
        updated = financeEngine.applyPetUpkeep(updated)
        if (updated.assets.isEmpty()) return updated
        updated = financeEngine.applyUpkeep(updated)
        updated = financeEngine.degradeAssets(updated)
        return updated
    }

    private fun resolveCareerEvent(character: Character): Pair<Character, AgeUpResult>? {
        if (character.career.currentJob == null) return null

        val jobTitle = character.career.currentJob!!.title

        if (careerEngine.shouldTriggerDownsizing(character)) {
            val (afterDownsizing, formerTitle) = careerEngine.applyDownsizing(character)
            val event = careerEngine.buildDownsizingEvent(afterDownsizing, formerTitle)
            return afterDownsizing to AgeUpResult.SingleEvent(event)
        }

        val (afterFiring, wasFired) = careerEngine.evaluateFiring(character)
        if (wasFired) {
            val event = careerEngine.buildFiringEvent(afterFiring, jobTitle)
            return afterFiring to AgeUpResult.SingleEvent(event)
        }

        val (afterPromotion, wasPromoted) = careerEngine.evaluatePromotion(character)
        if (wasPromoted) {
            val event = careerEngine.buildPromotionEvent(afterPromotion)
            return afterPromotion to AgeUpResult.SingleEvent(event)
        }

        return null
    }

    private fun resolveExamEvent(character: Character): Pair<Character, AgeUpResult>? {
        if (educationEngine.shouldTriggerPrimaryExam(character)) {
            val (afterExam, result) = educationEngine.takeExam(character, ExamType.KCPE)
            val event = educationEngine.buildExamResultEvent(ExamType.KCPE, result, afterExam)
            return afterExam to AgeUpResult.SingleEvent(event)
        }
        if (educationEngine.shouldTriggerSecondaryExam(character)) {
            val (afterExam, result) = educationEngine.takeExam(character, ExamType.KCSE)
            val event = educationEngine.buildExamResultEvent(ExamType.KCSE, result, afterExam)
            return afterExam to AgeUpResult.SingleEvent(event)
        }
        return null
    }

    private fun applySiblingRelationshipEffect(
        character: Character,
        delta: Int
    ): Character {
        val siblingIndex = character.family.indexOfFirst { it.relation == RelationType.SIBLING }
        if (siblingIndex == -1) return character
        val sibling = character.family[siblingIndex]
        val updatedSibling = sibling.copy(
            relationshipLevel = clampRelationshipLevel(sibling.relationshipLevel + delta)
        ).coerceRelationship()
        return character.copy(
            family = character.family.replaceAt(siblingIndex, updatedSibling)
        )
    }

    private fun applyFamilyRelationshipEffect(
        character: Character,
        delta: Int
    ): Character {
        if (character.family.isEmpty()) return character
        val memberIndex = character.family.indexOfFirst {
            it.relation == RelationType.MOTHER || it.relation == RelationType.FATHER
        }.let { if (it >= 0) it else 0 }
        val member = character.family[memberIndex]
        val updatedMember = member.copy(
            relationshipLevel = clampRelationshipLevel(member.relationshipLevel + delta)
        ).coerceRelationship()
        return character.copy(
            family = character.family.replaceAt(memberIndex, updatedMember)
        )
    }

    private fun applyChildRelationshipEffect(
        character: Character,
        delta: Int
    ): Character {
        if (character.family.none { it.relation == RelationType.CHILD }) return character
        val updatedFamily = character.family.map { person ->
            if (person.relation == RelationType.CHILD && person.alive) {
                person.copy(
                    relationshipLevel = clampRelationshipLevel(person.relationshipLevel + delta)
                ).coerceRelationship()
            } else {
                person
            }
        }
        return character.copy(family = updatedFamily)
    }

    private fun studyEffortFromChoice(choice: EventChoice): StudyEffort {
        return when {
            choice.label.contains("slack", ignoreCase = true) ||
                choice.label.contains("coast", ignoreCase = true) -> StudyEffort.SLACK
            choice.label.contains("hard", ignoreCase = true) ||
                choice.label.contains("grind", ignoreCase = true) -> StudyEffort.HARD
            else -> StudyEffort.NORMAL
        }
    }

    private fun workEffortFromChoice(choice: EventChoice): WorkEffort {
        return when {
            choice.label.contains("coast", ignoreCase = true) ||
                choice.label.contains("slack", ignoreCase = true) -> WorkEffort.COAST
            choice.label.contains("grind", ignoreCase = true) ||
                choice.label.contains("hard", ignoreCase = true) -> WorkEffort.GRIND
            else -> WorkEffort.NORMAL
        }
    }

    private fun rollEvents(character: Character, triggeredEventIds: Set<String>): List<LifeEvent> {
        // Target ~50% random-event years; career/exam events add on top for ~40-60% total dialog rate.
        val eventCount = when (Random.nextFloat()) {
            in 0f..0.50f -> 0
            in 0.50f..0.85f -> 1
            else -> 2
        }
        if (eventCount == 0) return emptyList()

        val pickedEvents = mutableListOf<LifeEvent>()

        if (relocationEngine.shouldOfferRelocation(character, triggeredEventIds)) {
            val destinations = relocationEngine.getRelocationOpportunities(character)
            if (destinations.isNotEmpty()) {
                pickedEvents.add(
                    relocationEngine.buildRelocationOpportunityEvent(character, destinations)
                )
                if (pickedEvents.size >= eventCount) return pickedEvents
            }
        }

        val eligible = eventRepository.getEligibleEvents(
            age = character.age,
            usedIds = triggeredEventIds,
            character = character
        ).toMutableList()

        repeat(eventCount - pickedEvents.size) {
            if (eligible.isEmpty()) return@repeat
            val event = eventRepository.pickRandomEvent(eligible) ?: return@repeat
            pickedEvents.add(event)
            eligible.remove(event)
        }

        return pickedEvents
    }

    private fun List<LifeEvent>.toAgeUpResult(): AgeUpResult = when (size) {
        0 -> AgeUpResult.NoEvent
        1 -> AgeUpResult.SingleEvent(first())
        else -> AgeUpResult.MultipleEvents(this)
    }

    private fun List<Person>.replaceAt(index: Int, person: Person): List<Person> =
        toMutableList().apply { this[index] = person }

    private companion object {
        private const val CULTURE_SHOCK_HAPPINESS_PENALTY = 10
        private const val TEEN_HAIRSTYLE_CHANGE_CHANCE = 0.35f
        private const val FACIAL_HAIR_GROWTH_CHANCE = 0.12f
        private const val FACIAL_HAIR_MIN_AGE = 16
    }
}
