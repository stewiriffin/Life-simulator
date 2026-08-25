// app/src/main/java/com/maisha/game/domain/GameEngine.kt (modified — notification nudge hooks)
package com.maisha.game.domain

import com.maisha.game.data.EconomyScaler
import com.maisha.game.data.events.EventRepository
import com.maisha.game.data.FlavorInterpolator
import com.maisha.game.data.model.AgingDetails
import com.maisha.game.data.model.AssetType
import com.maisha.game.data.model.AchievementProgress
import com.maisha.game.data.model.AvatarConfig
import com.maisha.game.data.model.Character
import com.maisha.game.data.model.ClubPracticeIntensity
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
import com.maisha.game.util.clampStat
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
    val messageKey: String,
    val messageArgs: List<String> = emptyList()
)

sealed class StudySessionResult {
    data class Success(val character: Character) : StudySessionResult()
    data object Ineligible : StudySessionResult()
}

data class AgeUpOutcome(
    val character: Character,
    val result: AgeUpResult,
    val newlyUnlockedAchievements: List<Achievement> = emptyList(),
    val relationshipDecayNotices: List<com.maisha.game.data.model.RelationshipDecayNotice> = emptyList(),
    val newFriendName: String? = null,
    val newlyUnlockedMilestones: List<MilestoneUnlock> = emptyList(),
    val fameTierUp: Boolean = false,
    val completedBucketGoals: Int = 0
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
    private val businessEngine: BusinessEngine,
    private val politicsEngine: PoliticsEngine,
    private val legacyEngine: LegacyEngine,
    private val milestoneEngine: MilestoneEngine,
    private val bucketListEngine: BucketListEngine,
    private val leisureEngine: LeisureEngine
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
                workEffortThisYear = null,
                partTimeWorkedThisYear = false
            )
        )
        updatedCharacter = applyAvatarVisualEvolution(updatedCharacter, previousAge)
        updatedCharacter = socialMediaEngine.resetYearlyFlags(updatedCharacter)
        if (updatedCharacter.lifestyle.socializedThisYear) {
            updatedCharacter = updatedCharacter.copy(
                lifestyle = updatedCharacter.lifestyle.copy(socializedThisYear = false)
            )
        }
        val immigrationTick = relocationEngine.tickImmigrationYear(updatedCharacter)
        updatedCharacter = immigrationTick.character
        var decayNotices = emptyList<com.maisha.game.data.model.RelationshipDecayNotice>()

        val incarceratedAtYearStart = updatedCharacter.criminalRecord.currentlyIncarcerated
        if (incarceratedAtYearStart) {
            // Education and career progression skipped while incarcerated.
        } else {
            updatedCharacter = educationEngine.tickSchoolYear(updatedCharacter)
            updatedCharacter = educationEngine.enrollIfEligible(updatedCharacter)
            updatedCharacter = processEducationProgression(updatedCharacter, preStage)
            updatedCharacter = processCareerProgression(updatedCharacter)
        }

        updatedCharacter = processFinanceProgression(updatedCharacter)
        updatedCharacter = businessEngine.processBusinessYear(updatedCharacter)
        val governanceTick = politicsEngine.tickGovernance(updatedCharacter)
        updatedCharacter = governanceTick.character

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
                updatedCharacter = updatedCharacter.copy(
                    family = updatedCharacter.family + friend,
                    eventLog = EventLogCap.prepend(
                        updatedCharacter.eventLog,
                        "You made a new friend: ${friend.name}."
                    )
                )
                newFriendName = friend.name
            }
        }

        if (incarceratedAtYearStart) {
            updatedCharacter = crimeEngine.serveYear(updatedCharacter)
        }

        val (characterAfterEvents, result) = if (governanceTick.impeachmentEvent != null) {
            updatedCharacter to AgeUpResult.SingleEvent(governanceTick.impeachmentEvent)
        } else if (incarceratedAtYearStart) {
            updatedCharacter to rollEvents(updatedCharacter, triggeredEventIds).toAgeUpResult()
        } else {
            resolveStatPressureEvent(updatedCharacter)
                ?: resolveCareerEvent(updatedCharacter)
                ?: resolveExpulsionHearingEvent(updatedCharacter)
                ?: resolveGraduationCareerEvent(updatedCharacter)
                ?: resolveUniversityEnrollmentEvent(updatedCharacter)
                ?: resolveExamEvent(updatedCharacter)
                ?: (updatedCharacter to rollEvents(updatedCharacter, triggeredEventIds).toAgeUpResult())
        }

        val depth = applyGamifyDepthSystems(
            beforeYear = character,
            afterYear = characterAfterEvents
        )
        val outcome = finalizeYear(depth.character, result, achievementProgress)
        scheduleNotificationNudges(slotId, outcome)
        return outcome.copy(
            relationshipDecayNotices = decayNotices,
            newFriendName = newFriendName,
            newlyUnlockedMilestones = depth.milestones,
            fameTierUp = depth.fameTierUp,
            completedBucketGoals = depth.completedBucketGoals
        )
    }

    private data class GamifyDepthResult(
        val character: Character,
        val milestones: List<MilestoneUnlock>,
        val fameTierUp: Boolean,
        val completedBucketGoals: Int
    )

    private fun applyGamifyDepthSystems(
        beforeYear: Character,
        afterYear: Character
    ): GamifyDepthResult {
        var updated = afterYear.copy(skillShowcaseDoneThisYear = false)
        val fameBefore = updated.socialMedia.fameTier
        updated = socialMediaEngine.syncFameTier(updated)
        val fameTierUp = updated.socialMedia.fameTier.ordinal > fameBefore.ordinal

        val netWorth = financeEngine.calculateNetWorth(updated)
        val completedBefore = updated.bucketList.count { it.completed }
        updated = bucketListEngine.evaluate(updated, netWorth)
        val completedBucketGoals = updated.bucketList.count { it.completed } - completedBefore

        val milestones = milestoneEngine.checkNewUnlocks(beforeYear, updated, netWorth)
        updated = milestoneEngine.applyUnlocks(updated, milestones)
        return GamifyDepthResult(updated, milestones, fameTierUp, completedBucketGoals)
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

    sealed class VolunteerResult {
        data class Success(val character: Character) : VolunteerResult()
        data object Ineligible : VolunteerResult()
    }

    sealed class DonationResult {
        data class Success(val character: Character, val karmaGained: Int) : DonationResult()
        data object InsufficientFunds : DonationResult()
        data object InvalidAmount : DonationResult()
    }

    /**
     * Volunteer work: raises hidden karma at the cost of happiness and health effort.
     */
    fun volunteer(character: Character): VolunteerResult {
        if (!character.alive || character.criminalRecord.currentlyIncarcerated) {
            return VolunteerResult.Ineligible
        }
        val updated = character.copy(
            stats = character.stats.copy(
                karma = clampStat(character.stats.karma + VOLUNTEER_KARMA_GAIN),
                happiness = clampStat(character.stats.happiness - VOLUNTEER_HAPPINESS_COST),
                health = clampStat(character.stats.health - VOLUNTEER_HEALTH_COST)
            ),
            eventLog = EventLogCap.prepend(
                character.eventLog,
                "You volunteered at a soup kitchen. Tired, but lighter in spirit."
            )
        )
        return VolunteerResult.Success(updated)
    }

    /**
     * Donates [amount] cash; karma gain scales with share of net worth given.
     */
    fun donateToCharity(character: Character, amount: Int): DonationResult {
        if (amount <= 0) return DonationResult.InvalidAmount
        if (character.stats.money < amount) return DonationResult.InsufficientFunds
        val netWorth = financeEngine.calculateNetWorth(character).coerceAtLeast(1)
        val share = amount.toFloat() / netWorth
        val karmaGained = (share * DONATION_KARMA_SCALE).toInt()
            .coerceIn(DONATION_KARMA_MIN, DONATION_KARMA_MAX)
        val updated = character.copy(
            stats = character.stats.copy(
                money = character.stats.money - amount,
                karma = clampStat(character.stats.karma + karmaGained),
                happiness = clampStat(character.stats.happiness + 2)
            ),
            eventLog = EventLogCap.prepend(
                character.eventLog,
                "You donated ${formatMoney(amount, character.countryCode)} to charity."
            )
        )
        return DonationResult.Success(updated, karmaGained)
    }

    fun donationTiers(countryCode: String): List<Int> = DONATION_TIERS_KENYA.map {
        EconomyScaler.scaleAmount(it, countryCode)
    }

    fun leisureCost(activity: LeisureActivity, countryCode: String): Int =
        leisureEngine.cost(activity, countryCode)

    fun performLeisure(character: Character, activity: LeisureActivity): LeisureResult =
        leisureEngine.perform(character, activity)

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
            when (val birth = relationshipEngine.haveChild(updatedCharacter)) {
                is HaveChildResult.Success -> updatedCharacter = birth.character
                HaveChildResult.NeedSpouse,
                HaveChildResult.InsufficientFunds -> Unit
            }
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

        if (choice.examPrepAction != null) {
            val prepChoice = runCatching {
                com.maisha.game.data.model.ExamPrepChoice.valueOf(choice.examPrepAction)
            }.getOrNull()
            if (prepChoice != null) {
                updatedCharacter = educationEngine.resolveExamWithPrepChoice(
                    updatedCharacter,
                    prepChoice
                )
            }
        }

        if (choice.expulsionHearingAction != null) {
            val hearingChoice = runCatching {
                com.maisha.game.data.model.ExpulsionHearingChoice.valueOf(choice.expulsionHearingAction)
            }.getOrNull()
            if (hearingChoice != null) {
                updatedCharacter = educationEngine.resolveExpulsionHearing(
                    updatedCharacter,
                    hearingChoice
                )
                if (hearingChoice == com.maisha.game.data.model.ExpulsionHearingChoice.DEFIANT) {
                    updatedCharacter = relationshipEngine.applyExpulsionFamilyEffect(updatedCharacter)
                }
            }
        }

        if (choice.universityMajor != null) {
            val major = runCatching {
                com.maisha.game.data.model.UniversityMajor.valueOf(choice.universityMajor)
            }.getOrNull()
            val funding = runCatching {
                com.maisha.game.data.model.UniversityFunding.valueOf(
                    choice.universityFunding ?: "LOAN"
                )
            }.getOrDefault(com.maisha.game.data.model.UniversityFunding.LOAN)
            if (major != null) {
                when (
                    val enrolled = educationEngine.enrollInUniversity(
                        updatedCharacter,
                        major,
                        funding
                    )
                ) {
                    is EducationEngine.UniversityEnrollResult.Success ->
                        updatedCharacter = enrolled.character
                    EducationEngine.UniversityEnrollResult.ScholarshipDenied,
                    EducationEngine.UniversityEnrollResult.InsufficientFunds -> {
                        val fallback = educationEngine.enrollInUniversity(
                            updatedCharacter,
                            major,
                            com.maisha.game.data.model.UniversityFunding.LOAN
                        )
                        if (fallback is EducationEngine.UniversityEnrollResult.Success) {
                            val reason = when (enrolled) {
                                EducationEngine.UniversityEnrollResult.ScholarshipDenied ->
                                    "Scholarship denied — enrolled on a student loan instead."
                                else ->
                                    "Not enough cash — enrolled on a student loan instead."
                            }
                            updatedCharacter = fallback.character.copy(
                                eventLog = EventLogCap.prepend(
                                    fallback.character.eventLog,
                                    reason
                                )
                            )
                        }
                    }
                    else -> Unit
                }
            }
        } else if (choice.universityCourse != null) {
            updatedCharacter = educationEngine.applyToUniversity(
                updatedCharacter,
                choice.universityCourse
            )
        }

        if (choice.careerTrackStart != null) {
            val track = runCatching {
                com.maisha.game.data.model.CareerTrack.valueOf(choice.careerTrackStart)
            }.getOrNull()
            if (track != null && track != com.maisha.game.data.model.CareerTrack.NONE) {
                updatedCharacter = careerEngine.startCareerTrack(updatedCharacter, track)
            }
            updatedCharacter = educationEngine.clearPendingCareerTrackOffer(updatedCharacter)
        }

        if (choice.triggersExpulsion && choice.expulsionHearingAction == null) {
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

        if (choice.portfolioReturnPercent != null) {
            updatedCharacter = financeEngine.applyPortfolioReturn(
                updatedCharacter,
                choice.portfolioReturnPercent
            )
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

    fun throwParty(character: Character, budget: Int): PartyResult =
        relationshipEngine.throwParty(character, budget)

    fun takeDrivingTest(character: Character): DrivingTestResult =
        educationEngine.takeDrivingTest(character)

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

    fun showcaseSkill(
        character: Character,
        skillType: com.maisha.game.data.model.SkillType
    ): SkillResult = skillEngine.showcaseSkill(character, skillType)

    fun masterclassCost(character: Character): Int = skillEngine.masterclassCost(character)

    fun adoptBucketGoal(character: Character, templateId: String): BucketAdoptResult =
        bucketListEngine.adopt(character, templateId)

    fun availableBucketTemplates(character: Character): List<BucketTemplate> =
        bucketListEngine.availableTemplates(character)

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

    fun renewVisa(character: Character): VisaRenewalResult =
        relocationEngine.renewVisa(character)

    fun applyForCitizenship(character: Character): CitizenshipApplicationResult =
        relocationEngine.applyForCitizenship(character)

    fun launchCampaign(
        character: Character,
        office: com.maisha.game.data.model.PoliticalOffice,
        investment: Int
    ): CampaignResult = politicsEngine.launchCampaign(character, office, investment)

    fun passTaxPolicy(
        character: Character,
        type: com.maisha.game.data.model.TaxPolicyType
    ): FinanceEngine.TaxPolicyResult = financeEngine.passTaxPolicy(character, type)

    fun rentOutProperty(character: Character, assetId: String): FinanceEngine.RentalResult =
        financeEngine.rentOutProperty(character, assetId)

    fun evictTenant(character: Character, assetId: String): FinanceEngine.RentalResult =
        financeEngine.evictTenant(character, assetId)

    /** Delegates to [FinanceEngine.purchaseAsset]. */
    fun purchaseAsset(character: Character, catalogId: String): PurchaseResult {
        return financeEngine.purchaseAsset(character, catalogId)
    }

    /** Delegates to [FinanceEngine.sellAsset]. */
    fun investFunds(character: Character, amount: Int): FinanceEngine.InvestmentResult =
        financeEngine.investFunds(character, amount)

    fun withdrawFunds(character: Character, amount: Int): FinanceEngine.InvestmentResult =
        financeEngine.withdrawFunds(character, amount)

    fun depositSavings(character: Character, amount: Int): FinanceEngine.InvestmentResult =
        financeEngine.depositSavings(character, amount)

    fun withdrawSavings(character: Character, amount: Int): FinanceEngine.InvestmentResult =
        financeEngine.withdrawSavings(character, amount)

    fun setLivingStandard(
        character: Character,
        standard: com.maisha.game.data.model.LivingStandard
    ): Character = financeEngine.setLivingStandard(character, standard)

    fun setPlannedWorkEffort(character: Character, effort: WorkEffort): Character =
        careerEngine.setPlannedWorkEffort(character, effort)

    fun setPlannedStudyEffort(character: Character, effort: StudyEffort): Character =
        educationEngine.setPlannedStudyEffort(character, effort)

    fun performStudySession(character: Character): StudySessionResult {
        if (!character.alive) return StudySessionResult.Ineligible
        val stage = character.education.stage
        if (stage != SchoolStage.PRIMARY &&
            stage != SchoolStage.SECONDARY &&
            stage != SchoolStage.UNIVERSITY
        ) {
            return StudySessionResult.Ineligible
        }
        if (character.education.expelled || character.education.droppedOutFrom != null) {
            return StudySessionResult.Ineligible
        }
        val withEffort = educationEngine.setPlannedStudyEffort(character, StudyEffort.HARD)
        val updated = when (stage) {
            SchoolStage.UNIVERSITY -> {
                val smartsDelta = EffortResolver.studySmartsDelta(StudyEffort.HARD)
                val happinessDelta = EffortResolver.studyHappinessDelta(StudyEffort.HARD)
                withEffort.copy(
                    stats = withEffort.stats.copy(
                        smarts = clampStat(withEffort.stats.smarts + smartsDelta),
                        happiness = clampStat(withEffort.stats.happiness + happinessDelta)
                    ),
                    eventLog = EventLogCap.prepend(
                        withEffort.eventLog,
                        "You crammed hard for upcoming exams."
                    )
                )
            }
            else -> educationEngine.applyStudyEffort(withEffort, StudyEffort.HARD)
        }
        return StudySessionResult.Success(updated)
    }

    fun joinSchoolClub(character: Character, club: com.maisha.game.data.model.SchoolClub): Character =
        educationEngine.joinSchoolClub(character, club)

    fun performClubActivity(
        character: Character,
        intensity: ClubPracticeIntensity = ClubPracticeIntensity.NORMAL
    ): ClubActivityResult =
        educationEngine.performClubActivity(character, intensity)

    fun claimClubMajorEvent(character: Character): ClubActivityResult =
        educationEngine.claimClubMajorEvent(character)

    fun hostClubFundraiser(character: Character): ClubActivityResult =
        educationEngine.hostClubFundraiser(character)

    fun challengeRivalSchool(character: Character): ClubActivityResult =
        educationEngine.challengeRivalSchool(character)

    fun leaveSchoolClub(character: Character): Character =
        educationEngine.leaveSchoolClub(character, fired = false)

    fun enrollInUniversity(
        character: Character,
        major: com.maisha.game.data.model.UniversityMajor,
        funding: com.maisha.game.data.model.UniversityFunding
    ): EducationEngine.UniversityEnrollResult =
        educationEngine.enrollInUniversity(character, major, funding)

    fun performCampusJob(character: Character): EducationEngine.UniversityActionResult =
        educationEngine.performCampusJob(character)

    fun performInternship(character: Character): EducationEngine.UniversityActionResult =
        educationEngine.performInternship(character)

    fun repayStudentLoan(
        character: Character,
        amount: Int
    ): FinanceEngine.StudentLoanRepayResult =
        financeEngine.repayStudentLoan(character, amount)

    fun resolveExpulsionHearing(
        character: Character,
        choice: com.maisha.game.data.model.ExpulsionHearingChoice
    ): Character = educationEngine.resolveExpulsionHearing(character, choice)

    fun serveDetention(character: Character): SchoolDisciplineResult =
        educationEngine.serveDetention(character)

    fun apologizeToPrincipal(character: Character): SchoolDisciplineResult =
        educationEngine.apologizeToPrincipal(character)

    fun performSchoolActivity(
        character: Character,
        activity: com.maisha.game.data.model.SchoolActivity,
        targetPersonId: String? = null
    ): SchoolActionResult = educationEngine.performSchoolActivity(character, activity, targetPersonId)

    fun availableSchoolActivities(character: Character): List<com.maisha.game.data.model.SchoolActivity> =
        educationEngine.availableSchoolActivities(character)

    fun handleSchoolPersonInteraction(
        character: Character,
        personId: String,
        action: com.maisha.game.data.model.SchoolPersonAction
    ): SchoolInteractionResult = educationEngine.handleSchoolPersonInteraction(character, personId, action)

    fun schoolGiftCost(character: Character): Int = educationEngine.schoolGiftCost(character)

    fun performExamPrepAction(
        character: Character,
        choice: com.maisha.game.data.model.ExamPrepChoice
    ): ExamPrepResult = educationEngine.performExamPrepAction(character, choice)

    fun examPreparednessPercent(character: Character): Int =
        educationEngine.examPreparednessPercent(character)

    fun refreshExamSchedule(character: Character): Character =
        educationEngine.refreshExamSchedule(character)

    fun calculateExamPassChance(character: Character): Float =
        educationEngine.calculateExamPassChance(character)

    fun startCareerTrack(character: Character, track: com.maisha.game.data.model.CareerTrack): Character {
        val started = careerEngine.startCareerTrack(character, track)
        return if (
            started.career.careerTrack == track &&
            started.education.pendingCareerTrackOffer
        ) {
            educationEngine.clearPendingCareerTrackOffer(started)
        } else {
            started
        }
    }

    fun practiceCareerTrack(character: Character): CareerTrackPracticeResult =
        careerEngine.practiceCareerTrack(character)

    fun performPrisonActivity(
        character: Character,
        activity: com.maisha.game.data.model.PrisonActivity
    ): PrisonActivityResult = crimeEngine.performPrisonActivity(character, activity)

    fun updateWill(character: Character, will: Map<String, Int>?): Character {
        if (will == null) {
            return character.copy(will = null)
        }
        require(legacyEngine.isValidWill(character, will)) {
            "Will shares must total exactly 100% among living spouse and children."
        }
        return character.copy(will = will)
    }

    fun willBeneficiaries(character: Character) = legacyEngine.willBeneficiaries(character)

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
    fun startDating(character: Character, prospect: Person): StartDatingResult =
        relationshipEngine.startDating(character, prospect)

    /** Delegates to [RelationshipEngine.proposeMarriage]. */
    fun proposeMarriage(character: Character, personId: String, signPrenup: Boolean = false) =
        relationshipEngine.proposeMarriage(character, personId, signPrenup)

    /** Delegates to [RelationshipEngine.adoptChild]. */
    fun adoptChild(character: Character): AdoptChildResult =
        relationshipEngine.adoptChild(character)

    /** Delegates to [CareerEngine.workPartTime]. */
    fun workPartTime(character: Character, job: com.maisha.game.data.model.PartTimeJob): PartTimeJobResult =
        careerEngine.workPartTime(character, job)

    /** Delegates to [FinanceEngine.renovateAsset]. */
    fun renovateAsset(character: Character, assetId: String): RenovateResult =
        financeEngine.renovateAsset(character, assetId)

    /** Delegates to [FinanceEngine.setPortfolioStrategy]. */
    fun setPortfolioStrategy(
        character: Character,
        strategy: com.maisha.game.data.model.PortfolioStrategy
    ): Character = financeEngine.setPortfolioStrategy(character, strategy)

    /** Delegates to [CrimeEngine.requestExpungement]. */
    fun requestExpungement(character: Character): ExpungementResult =
        crimeEngine.requestExpungement(character)

    /** Delegates to [RelationshipEngine.breakUpOrDivorce]. */
    fun breakUpOrDivorce(character: Character, personId: String): BreakUpResult =
        relationshipEngine.breakUpOrDivorce(character, personId)

    /** Delegates to [RelationshipEngine.haveChild]. */
    fun haveChild(character: Character): HaveChildResult =
        relationshipEngine.haveChild(character)

    fun seekFriendship(character: Character): SeekFriendshipResult =
        relationshipEngine.seekFriendship(character)

    fun careForPet(character: Character, petId: String, action: PetCareAction): PetCareResult =
        relationshipEngine.careForPet(character, petId, action)

    fun firstDateCost(character: Character): Int = relationshipEngine.firstDateCost(character)

    fun childHospitalCost(character: Character): Int = relationshipEngine.childHospitalCost(character)

    fun divorceSettlementCost(character: Character): Int =
        relationshipEngine.divorceSettlementCost(character)

    fun dateNightCost(character: Character): Int = relationshipEngine.dateNightCost(character)

    fun seekFriendshipCost(character: Character): Int =
        relationshipEngine.seekFriendshipCost(character)

    fun petFeedCost(character: Character): Int = relationshipEngine.petFeedCost(character)

    fun petVetCost(character: Character): Int = relationshipEngine.petVetCost(character)

    fun canSeekFriendship(character: Character): Boolean =
        relationshipEngine.canSeekFriendship(character)

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
                    educationEngine.advanceGrade(character, character.education.plannedStudyEffort)
                } else {
                    character
                }
            }
            SchoolStage.UNIVERSITY -> educationEngine.advanceUniversityYear(character)
            else -> character
        }
    }

    private fun processCareerProgression(character: Character): Character {
        val afterWork = when {
            character.career.isRetired || character.career.currentJob == null -> character
            else -> careerEngine.workYear(character, character.career.plannedWorkEffort)
        }
        return careerEngine.tickCareerTrackYear(afterWork)
    }

    private fun processFinanceProgression(character: Character): Character {
        var updated = financeEngine.applyEconomicShift(character).character
        updated = financeEngine.applyPortfolioMarketTick(updated)
        updated = financeEngine.applySavingsInterest(updated)
        updated = financeEngine.applyPension(updated)
        updated = financeEngine.applyPetUpkeep(updated)
        if (updated.assets.isNotEmpty()) {
            updated = financeEngine.applyUpkeep(updated)
            updated = financeEngine.collectRent(updated)
            updated = financeEngine.degradeAssets(updated)
        }
        updated = financeEngine.applyCostOfLiving(updated)
        updated = financeEngine.tickStudentLoan(updated)
        return updated
    }

    private fun resolveStatPressureEvent(character: Character): Pair<Character, AgeUpResult>? {
        val stats = character.stats
        return when {
            stats.health < 30 -> {
                character to AgeUpResult.SingleEvent(
                    LifeEvent(
                        id = "stat_health_warning_${character.age}",
                        minAge = character.age,
                        maxAge = character.age,
                        text = "Your poor health is starting to affect daily life. Even simple tasks feel heavier this year.",
                        choices = listOf(
                            EventChoice(
                                label = "Rest and recover",
                                statEffects = mapOf("health" to 6, "happiness" to 1),
                                resultText = "You slow down and let your body recover."
                            ),
                            EventChoice(
                                label = "Push through anyway",
                                statEffects = mapOf("health" to -4, "happiness" to -2),
                                performanceEffect = -4,
                                resultText = "You force yourself onward, but your body pays for it."
                            )
                        ),
                        tags = listOf("stats_system", "one_time")
                    )
                )
            }
            stats.happiness < 30 -> {
                character to AgeUpResult.SingleEvent(
                    LifeEvent(
                        id = "stat_happiness_warning_${character.age}",
                        minAge = character.age,
                        maxAge = character.age,
                        text = "Your low mood colors everything this year. Relationships and motivation both feel harder to maintain.",
                        choices = listOf(
                            EventChoice(
                                label = "Reach out to someone",
                                statEffects = mapOf("happiness" to 5),
                                familyRelationshipEffect = 5,
                                resultText = "Talking helps more than you expected."
                            ),
                            EventChoice(
                                label = "Bottle it up",
                                statEffects = mapOf("happiness" to -3, "health" to -2),
                                resultText = "You keep it to yourself and feel even heavier."
                            )
                        ),
                        tags = listOf("stats_system", "one_time")
                    )
                )
            }
            stats.smarts >= 85 && character.education.stage == SchoolStage.SECONDARY -> {
                character to AgeUpResult.SingleEvent(
                    LifeEvent(
                        id = "stat_smarts_opportunity_${character.age}",
                        minAge = character.age,
                        maxAge = character.age,
                        text = "Teachers notice how sharp you are and suggest extra academic opportunities.",
                        choices = listOf(
                            EventChoice(
                                label = "Take the challenge",
                                statEffects = mapOf("smarts" to 3, "happiness" to -1),
                                gpaEffect = 0.15f,
                                resultText = "The extra pressure is real, but you grow from it."
                            ),
                            EventChoice(
                                label = "Stay balanced",
                                statEffects = mapOf("happiness" to 3),
                                resultText = "You pass on the pressure and protect your peace."
                            )
                        ),
                        tags = listOf("stats_system", "one_time")
                    )
                )
            }
            stats.looks >= 85 && character.age >= 16 -> {
                character to AgeUpResult.SingleEvent(
                    LifeEvent(
                        id = "stat_looks_opportunity_${character.age}",
                        minAge = character.age,
                        maxAge = character.age,
                        text = "Your looks attract extra attention this year, opening a few social doors.",
                        choices = listOf(
                            EventChoice(
                                label = "Lean into it",
                                statEffects = mapOf("happiness" to 4),
                                followerEffect = 150,
                                resultText = "The attention boosts your confidence and your profile."
                            ),
                            EventChoice(
                                label = "Keep a low profile",
                                statEffects = mapOf("smarts" to 1),
                                resultText = "You stay grounded and avoid the noise."
                            )
                        ),
                        tags = listOf("stats_system", "one_time")
                    )
                )
            }
            else -> null
        }
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

    private fun resolveUniversityEnrollmentEvent(character: Character): Pair<Character, AgeUpResult>? {
        val event = educationEngine.buildUniversityEnrollmentEvent(character) ?: return null
        return character to AgeUpResult.SingleEvent(event)
    }

    private fun resolveGraduationCareerEvent(character: Character): Pair<Character, AgeUpResult>? {
        val event = educationEngine.buildGraduationCareerEvent(character) ?: return null
        return character to AgeUpResult.SingleEvent(event)
    }

    private fun resolveExpulsionHearingEvent(character: Character): Pair<Character, AgeUpResult>? {
        val event = educationEngine.buildExpulsionHearingEvent(character) ?: return null
        return character to AgeUpResult.SingleEvent(event)
    }

    private fun resolveExamEvent(character: Character): Pair<Character, AgeUpResult>? {
        val scheduled = educationEngine.refreshExamSchedule(character)
        val prompt = educationEngine.buildExamPromptEvent(scheduled) ?: return null
        return scheduled to AgeUpResult.SingleEvent(prompt)
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
            val event = eventRepository.pickRandomEvent(eligible, character) ?: return@repeat
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
        private const val VOLUNTEER_KARMA_GAIN = 5
        private const val VOLUNTEER_HAPPINESS_COST = 3
        private const val VOLUNTEER_HEALTH_COST = 2
        private const val DONATION_KARMA_SCALE = 40f
        private const val DONATION_KARMA_MIN = 1
        private const val DONATION_KARMA_MAX = 15
        private val DONATION_TIERS_KENYA = listOf(100, 1_000, 10_000)
        private const val FACIAL_HAIR_MIN_AGE = 16
    }
}
