// app/src/main/java/com/maisha/game/domain/GameEngine.kt (modified — notification nudge hooks)
package com.maisha.game.domain

import com.maisha.game.data.events.EventRepository
import com.maisha.game.data.FlavorInterpolator
import com.maisha.game.data.model.AssetType
import com.maisha.game.data.model.AchievementProgress
import com.maisha.game.data.model.Character
import com.maisha.game.data.model.EventChoice
import com.maisha.game.data.model.ExamType
import com.maisha.game.data.model.LifeEvent
import com.maisha.game.data.model.Achievement
import com.maisha.game.data.model.Person
import com.maisha.game.data.model.RelationType
import com.maisha.game.data.model.SchoolStage
import com.maisha.game.data.model.StudyEffort
import com.maisha.game.data.model.WorkEffort
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
    private val relocationEngine: RelocationEngine
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
        var updatedCharacter = character.copy(age = character.age + 1)
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

        val tickResult = relationshipEngine.tickFamilyYear(updatedCharacter)
        updatedCharacter = updatedCharacter.copy(family = tickResult.family)
        decayNotices = tickResult.decayNotices

        updatedCharacter = processHealthProgression(updatedCharacter)

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

    private fun processHealthProgression(character: Character): Character {
        var updated = character
        healthEngine.rollForIllness(updated)?.let { condition ->
            updated = healthEngine.addCondition(updated, condition)
        }
        updated = healthEngine.applyUntreatedConditions(updated)
        return updated
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

        updatedCharacter = educationEngine.applyGpaEffect(updatedCharacter, choice.gpaEffect)

        if (choice.universityCourse != null) {
            updatedCharacter = educationEngine.applyToUniversity(
                updatedCharacter,
                choice.universityCourse
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

    /** Delegates to [HealthEngine.visitDoctor]. */
    fun visitDoctor(
        character: Character,
        conditionId: String,
        usePrivateCare: Boolean
    ): DoctorResult = healthEngine.visitDoctor(character, conditionId, usePrivateCare)

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
        if (character.career.currentJob == null) return character
        return careerEngine.workYear(character, WorkEffort.NORMAL)
    }

    private fun processFinanceProgression(character: Character): Character {
        if (character.assets.isEmpty()) return character
        var updated = financeEngine.applyUpkeep(character)
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
}
