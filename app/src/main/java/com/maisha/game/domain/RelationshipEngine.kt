// app/src/main/java/com/maisha/game/domain/RelationshipEngine.kt (modified — expanded interactions + country-aware)
package com.maisha.game.domain

import com.maisha.game.data.CountryCatalog
import com.maisha.game.data.DatingPool
import com.maisha.game.data.EconomyScaler
import com.maisha.game.data.NamePool
import com.maisha.game.data.model.AvatarConfig
import com.maisha.game.data.model.Character
import com.maisha.game.data.model.Gender
import com.maisha.game.data.model.MilestoneKind
import com.maisha.game.data.model.Person
import com.maisha.game.data.model.RelationType
import com.maisha.game.data.model.RelationshipDecayNotice
import com.maisha.game.data.model.RelationshipMilestone
import com.maisha.game.data.model.RelationshipTier
import com.maisha.game.data.model.Stats
import com.maisha.game.data.model.relationshipTierFor
import com.maisha.game.util.formatMoney
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

sealed class ProposalResult {
    data class Accepted(val character: Character) : ProposalResult()
    data object Rejected : ProposalResult()
}

@Singleton
class RelationshipEngine @Inject constructor() {

    fun getRelationshipTier(person: Person): RelationshipTier =
        relationshipTierFor(person.relationshipLevel)

    /**
     * Gentle annual drift toward neutral (50) for family not interacted with this year.
     * Spouse/child: 1 pt/year; others: 2 pts/year.
     */
    fun tickFamilyYear(character: Character): FamilyYearTickResult {
        val notices = mutableListOf<RelationshipDecayNotice>()
        val family = character.family.map { person ->
            val beforeTier = relationshipTierFor(person.relationshipLevel)
            val decayed = if (!person.interactedThisYear) {
                person.copy(relationshipLevel = driftTowardNeutral(person.relationshipLevel, person.relation))
            } else {
                person
            }
            val afterTier = relationshipTierFor(decayed.relationshipLevel)
            if (!person.interactedThisYear && beforeTier != afterTier) {
                notices += RelationshipDecayNotice(
                    personName = person.name,
                    previousTier = beforeTier,
                    newTier = afterTier
                )
            }
            decayed.copy(
                age = person.age + 1,
                complimentsThisYear = 0,
                interactedThisYear = false
            ).coerceRelationship()
        }
        return FamilyYearTickResult(family = family, decayNotices = notices)
    }

    fun canTravelTogether(person: Person): Boolean = Companion.canTravelTogether(person)

    fun findDatingProspects(character: Character): List<Person> {
        if (character.age < MIN_DATING_AGE || character.hasSpouse()) return emptyList()
        return DatingPool.generateProspects(character)
    }

    /**
     * Light annual chance of meeting a new friend during school or work years.
     * Returns a [Person] to add to family, or null if no opportunity this year.
     */
    fun generateFriendshipOpportunity(character: Character): Person? {
        if (character.age < MIN_FRIEND_AGE || character.age > MAX_FRIEND_AGE) return null
        val friendCount = character.family.count { it.relation == RelationType.FRIEND }
        if (friendCount >= MAX_FRIENDS) return null

        val chance = when {
            character.age in 6..17 -> SCHOOL_FRIEND_CHANCE
            character.age in 18..55 -> WORK_FRIEND_CHANCE
            else -> 0.04f
        }
        if (Random.nextFloat() >= chance) return null

        val gender = if (Random.nextBoolean()) Gender.MALE else Gender.FEMALE
        val friendAge = (character.age + Random.nextInt(-3, 4)).coerceAtLeast(MIN_FRIEND_AGE)
        return Person(
            id = UUID.randomUUID().toString(),
            name = NamePool.randomFullName(gender, character.countryCode),
            relation = RelationType.FRIEND,
            gender = gender,
            age = friendAge,
            relationshipLevel = Random.nextInt(40, 61),
            stats = Stats(
                health = Random.nextInt(50, 81),
                happiness = Random.nextInt(45, 76),
                looks = Random.nextInt(40, 81)
            ),
            avatarConfig = AvatarConfig.random(),
            countryCode = character.countryCode
        )
    }

    /** Logs legacy-continuation milestones on inherited family when Legacy Mode selects an heir. */
    fun applyLegacyFamilyMilestones(character: Character): Character {
        val updatedFamily = character.family.map { person ->
            if (person.milestones.any { it.kind == MilestoneKind.LEGACY_CONTINUED.name }) {
                person
            } else {
                person.copy(
                    milestones = person.milestones + RelationshipMilestone.fromKind(
                        age = character.age,
                        kind = MilestoneKind.LEGACY_CONTINUED,
                        subjectName = person.name
                    )
                )
            }
        }
        return character.copy(family = updatedFamily)
    }

    fun startDating(character: Character, prospect: Person): Character {
        if (character.hasSpouse()) return character
        val partner = prospect.copy(
            relation = RelationType.SPOUSE,
            dateOfPartnership = character.age,
            isMarried = false,
            milestones = prospect.milestones + RelationshipMilestone.fromKind(
                age = character.age,
                kind = MilestoneKind.STARTED_DATING,
                subjectName = prospect.name
            )
        )
        return character.copy(family = character.family + partner)
    }

    fun progressRelationship(
        character: Character,
        personId: String,
        interactionType: InteractionType,
        giftTier: GiftTier? = null
    ): FamilyInteractionResult {
        val memberIndex = character.family.indexOfFirst { it.id == personId }
        if (memberIndex == -1) {
            return FamilyInteractionResult(character, "Person not found.")
        }

        val member = character.family[memberIndex]
        if (interactionType == InteractionType.TRAVEL_TOGETHER &&
            character.criminalRecord.currentlyIncarcerated
        ) {
            return FamilyInteractionResult(
                character = character,
                message = "You can't travel while incarcerated."
            )
        }
        return when (interactionType) {
            InteractionType.SPEND_TIME -> applySpendTime(character, memberIndex, member)
            InteractionType.ARGUE -> applyArgue(character, memberIndex, member)
            InteractionType.ASK_FOR_MONEY -> applyAskForMoney(character, memberIndex, member)
            InteractionType.GIFT -> applyGift(character, memberIndex, member, giftTier)
            InteractionType.COMPLIMENT -> applyCompliment(character, memberIndex, member)
            InteractionType.INSULT -> applyInsult(character, memberIndex, member)
            InteractionType.TRAVEL_TOGETHER -> applyTravelTogether(character, memberIndex, member)
            InteractionType.ASK_FOR_ADVICE -> applyAskForAdvice(character, memberIndex, member)
            InteractionType.PRANK -> applyPrank(character, memberIndex, member)
            InteractionType.SET_UP_ON_DATE -> applySetUpOnDate(character, memberIndex, member)
        }
    }

    fun proposeMarriage(character: Character, personId: String): Pair<Character, ProposalResult> {
        val memberIndex = character.family.indexOfFirst { it.id == personId }
        if (memberIndex == -1) return character to ProposalResult.Rejected

        val partner = character.family[memberIndex]
        if (partner.relation != RelationType.SPOUSE || partner.isMarried) {
            return character to ProposalResult.Rejected
        }
        if (partner.relationshipLevel < PROPOSAL_THRESHOLD) {
            return character to ProposalResult.Rejected
        }

        val accepted = Random.nextFloat() < proposalAcceptChance(partner.relationshipLevel)
        return if (accepted) {
            val marriedPartner = partner.copy(
                isMarried = true,
                milestones = partner.milestones + RelationshipMilestone.fromKind(
                    age = character.age,
                    kind = MilestoneKind.MARRIED,
                    subjectName = partner.name
                )
            ).coerceRelationship()
            val updated = character.copy(
                family = character.family.replaceAt(memberIndex, marriedPartner),
                stats = character.stats.copy(
                    happiness = (character.stats.happiness + 10).coerceIn(0, 100)
                )
            )
            updated to ProposalResult.Accepted(updated)
        } else {
            val declinedPartner = partner.copy(
                relationshipLevel = (partner.relationshipLevel - 15).coerceIn(0, 100)
            ).coerceRelationship()
            val updated = character.copy(
                family = character.family.replaceAt(memberIndex, declinedPartner)
            )
            updated to ProposalResult.Rejected
        }
    }

    fun breakUpOrDivorce(character: Character, personId: String): Character {
        val memberIndex = character.family.indexOfFirst { it.id == personId }
        if (memberIndex == -1) return character

        val partner = character.family[memberIndex]
        if (partner.relation != RelationType.SPOUSE) return character

        val happinessPenalty = if (partner.isMarried) DIVORCE_HAPPINESS_PENALTY else BREAKUP_HAPPINESS_PENALTY
        val label = if (partner.isMarried) "Divorced" else "Broke up with"
        return character.copy(
            family = character.family.filterNot { it.id == personId },
            stats = character.stats.copy(
                happiness = (character.stats.happiness - happinessPenalty).coerceIn(0, 100)
            ),
            eventLog = character.eventLog + "$label ${partner.name} at age ${character.age}."
        )
    }

    fun haveChild(character: Character): Character {
        val spouse = character.family.firstOrNull {
            it.relation == RelationType.SPOUSE && it.isMarried
        } ?: return character

        val gender = if (Random.nextBoolean()) Gender.MALE else Gender.FEMALE
        val child = Person(
            id = UUID.randomUUID().toString(),
            name = NamePool.randomFullName(gender, character.countryCode),
            relation = RelationType.CHILD,
            gender = gender,
            age = 0,
            relationshipLevel = 60,
            stats = Stats(
                health = Random.nextInt(50, 81),
                happiness = Random.nextInt(50, 81)
            ),
            avatarConfig = AvatarConfig.random(),
            countryCode = character.countryCode
        )
        return character.copy(
            family = character.family + child,
            stats = character.stats.copy(
                happiness = (character.stats.happiness + 5).coerceIn(0, 100)
            )
        )
    }

    fun applySpouseRelationshipEffect(character: Character, delta: Int): Character {
        val spouseIndex = character.family.indexOfFirst { it.relation == RelationType.SPOUSE }
        if (spouseIndex == -1) return character
        val spouse = character.family[spouseIndex]
        val updated = spouse.copy(
            relationshipLevel = (spouse.relationshipLevel + delta).coerceIn(0, 100)
        ).coerceRelationship()
        return character.copy(family = character.family.replaceAt(spouseIndex, updated))
    }

    private fun applySpendTime(
        character: Character,
        memberIndex: Int,
        member: Person
    ): FamilyInteractionResult {
        val updatedMember = member.copy(
            relationshipLevel = (member.relationshipLevel + 10).coerceIn(0, 100)
        ).coerceRelationship()
        val updatedStats = character.stats.copy(
            happiness = (character.stats.happiness + 5).coerceIn(0, 100)
        )
        val message = when (member.relation) {
            RelationType.SPOUSE -> "You spent quality time with ${member.name}."
            RelationType.CHILD -> "You played with ${member.name}."
            else -> "You spent quality time with ${member.name}."
        }
        return FamilyInteractionResult(
            character = commitMemberUpdate(
                character, memberIndex, updatedMember,
                interactionType = InteractionType.SPEND_TIME,
                recordFirstQualityTime = true
            ).copy(stats = updatedStats),
            message = message
        )
    }

    private fun applyArgue(
        character: Character,
        memberIndex: Int,
        member: Person
    ): FamilyInteractionResult {
        val updatedMember = member.copy(
            relationshipLevel = (member.relationshipLevel - 10).coerceIn(0, 100)
        ).coerceRelationship()
        val updatedStats = character.stats.copy(
            happiness = (character.stats.happiness - 5).coerceIn(0, 100)
        )
        val message = when (member.relation) {
            RelationType.SPOUSE -> "You argued with ${member.name}."
            RelationType.CHILD -> "You scolded ${member.name}."
            else -> "You argued with ${member.name}."
        }
        return FamilyInteractionResult(
            character = commitMemberUpdate(
                character, memberIndex, updatedMember,
                interactionType = InteractionType.ARGUE,
                milestoneKind = MilestoneKind.BIG_ARGUMENT,
                subjectName = member.name
            ).copy(stats = updatedStats),
            message = message
        )
    }

    private fun applyAskForMoney(
        character: Character,
        memberIndex: Int,
        member: Person
    ): FamilyInteractionResult {
        if (member.relation == RelationType.SPOUSE || member.relation == RelationType.CHILD) {
            return FamilyInteractionResult(
                character = character,
                message = "That doesn't apply here."
            )
        }
        if (member.relationshipLevel <= 60) {
            return FamilyInteractionResult(
                character = character,
                message = "${member.name} isn't close enough to ask for money."
            )
        }
        val success = Random.nextFloat() < ASK_MONEY_SUCCESS_CHANCE
        return if (success) {
            val amount = EconomyScaler.scaleAmount(Random.nextInt(50, 201), character.countryCode)
            val marked = member.copy(interactedThisYear = true).coerceRelationship()
            FamilyInteractionResult(
                character = character.copy(
                    family = character.family.replaceAt(memberIndex, marked),
                    stats = character.stats.copy(money = character.stats.money + amount)
                ),
                message = "${member.name} gave you ${formatMoney(amount, character.countryCode)}."
            )
        } else {
            val updatedMember = member.copy(
                relationshipLevel = (member.relationshipLevel - 5).coerceIn(0, 100),
                interactedThisYear = true
            ).coerceRelationship()
            FamilyInteractionResult(
                character = character.copy(
                    family = character.family.replaceAt(memberIndex, updatedMember)
                ),
                message = "${member.name} refused to give you money."
            )
        }
    }

    private fun applyGift(
        character: Character,
        memberIndex: Int,
        member: Person,
        giftTier: GiftTier?
    ): FamilyInteractionResult {
        val tier = giftTier ?: GiftTier.SMALL
        val cost = EconomyScaler.scaleRelationshipCost(tier.baseCostKenya, character.countryCode, character.age)
        if (character.stats.money < cost) {
            return FamilyInteractionResult(
                character = character,
                message = "You can't afford that gift (${formatMoney(cost, character.countryCode)})."
            )
        }
        val updatedMember = member.copy(
            relationshipLevel = (member.relationshipLevel + tier.relationshipBoost).coerceIn(0, 100)
        ).coerceRelationship()
        return FamilyInteractionResult(
            character = commitMemberUpdate(
                character, memberIndex, updatedMember,
                interactionType = InteractionType.GIFT,
                milestoneKind = MilestoneKind.THOUGHTFUL_GIFT,
                subjectName = member.name,
                giftTier = tier
            ).copy(
                stats = character.stats.copy(
                    money = character.stats.money - cost,
                    happiness = (character.stats.happiness + 3).coerceIn(0, 100)
                )
            ),
            message = "You gave ${member.name} a gift. They loved it!"
        )
    }

    private fun applyCompliment(
        character: Character,
        memberIndex: Int,
        member: Person
    ): FamilyInteractionResult {
        val diminishing = (member.complimentsThisYear * 2).coerceAtMost(8)
        val boost = (5 - diminishing).coerceAtLeast(1)
        val updatedMember = member.copy(
            relationshipLevel = (member.relationshipLevel + boost).coerceIn(0, 100),
            complimentsThisYear = member.complimentsThisYear + 1
        ).coerceRelationship()
        return FamilyInteractionResult(
            character = commitMemberUpdate(
                character, memberIndex, updatedMember,
                interactionType = InteractionType.COMPLIMENT
            ).copy(
                stats = character.stats.copy(
                    happiness = (character.stats.happiness + 1).coerceIn(0, 100)
                )
            ),
            message = "You complimented ${member.name}. They smiled."
        )
    }

    private fun applyInsult(
        character: Character,
        memberIndex: Int,
        member: Person
    ): FamilyInteractionResult {
        val updatedMember = member.copy(
            relationshipLevel = (member.relationshipLevel - 12).coerceIn(0, 100)
        ).coerceRelationship()
        return FamilyInteractionResult(
            character = commitMemberUpdate(
                character, memberIndex, updatedMember,
                interactionType = InteractionType.INSULT,
                milestoneKind = MilestoneKind.INSULTED,
                subjectName = member.name
            ).copy(
                stats = character.stats.copy(
                    happiness = (character.stats.happiness + 2).coerceIn(0, 100)
                )
            ),
            message = "You threw a petty insult at ${member.name}. You felt a little satisfied — they did not."
        )
    }

    private fun applyTravelTogether(
        character: Character,
        memberIndex: Int,
        member: Person
    ): FamilyInteractionResult {
        if (!canTravelTogether(member)) {
            return FamilyInteractionResult(
                character = character,
                message = "Spend quality time with ${member.name} before planning a trip together."
            )
        }
        val cost = EconomyScaler.scaleRelationshipCost(
            TRAVEL_BASE_COST_KENYA,
            character.countryCode,
            character.age
        )
        if (character.stats.money < cost) {
            return FamilyInteractionResult(
                character = character,
                message = "You can't afford a trip (${formatMoney(cost, character.countryCode)})."
            )
        }
        val updatedMember = member.copy(
            relationshipLevel = (member.relationshipLevel + 18).coerceIn(0, 100)
        ).coerceRelationship()
        return FamilyInteractionResult(
            character = commitMemberUpdate(
                character, memberIndex, updatedMember,
                interactionType = InteractionType.TRAVEL_TOGETHER,
                milestoneKind = MilestoneKind.TRAVELED_TOGETHER,
                subjectName = member.name
            ).copy(
                stats = character.stats.copy(
                    money = character.stats.money - cost,
                    happiness = (character.stats.happiness + 8).coerceIn(0, 100)
                )
            ),
            message = "You and ${member.name} took a trip together. Great memories!"
        )
    }

    private fun applyAskForAdvice(
        character: Character,
        memberIndex: Int,
        member: Person
    ): FamilyInteractionResult {
        val updatedMember = member.copy(
            relationshipLevel = (member.relationshipLevel + 3).coerceIn(0, 100)
        ).coerceRelationship()
        return FamilyInteractionResult(
            character = commitMemberUpdate(
                character, memberIndex, updatedMember,
                interactionType = InteractionType.ASK_FOR_ADVICE
            ).copy(
                stats = character.stats.copy(
                    smarts = (character.stats.smarts + 2).coerceIn(0, 100)
                )
            ),
            message = "${member.name} shared some wisdom. You feel a bit smarter."
        )
    }

    private fun applyPrank(
        character: Character,
        memberIndex: Int,
        member: Person
    ): FamilyInteractionResult {
        val landedWell = Random.nextFloat() < 0.55f
        val (relDelta, happyDelta, message) = if (landedWell) {
            Triple(8, 6, "Your prank on ${member.name} landed perfectly. You both laughed.")
        } else {
            Triple(-6, 2, "Your prank on ${member.name} backfired. Oops.")
        }
        val updatedMember = member.copy(
            relationshipLevel = (member.relationshipLevel + relDelta).coerceIn(0, 100)
        ).coerceRelationship()
        return FamilyInteractionResult(
            character = commitMemberUpdate(
                character, memberIndex, updatedMember,
                interactionType = InteractionType.PRANK
            ).copy(
                stats = character.stats.copy(
                    happiness = (character.stats.happiness + happyDelta).coerceIn(0, 100)
                )
            ),
            message = message
        )
    }

    private fun applySetUpOnDate(
        character: Character,
        memberIndex: Int,
        member: Person
    ): FamilyInteractionResult {
        if (member.relation != RelationType.SIBLING && member.relation != RelationType.FRIEND) {
            return FamilyInteractionResult(
                character = character,
                message = "You can only set up siblings or friends on dates."
            )
        }
        val updatedMember = member.copy(
            relationshipLevel = (member.relationshipLevel + 6).coerceIn(0, 100)
        ).coerceRelationship()
        val countryName = CountryCatalog.getCountry(character.countryCode).displayName
        return FamilyInteractionResult(
            character = commitMemberUpdate(
                character, memberIndex, updatedMember,
                interactionType = InteractionType.SET_UP_ON_DATE,
                milestoneKind = MilestoneKind.SET_UP_ON_DATE,
                subjectName = member.name
            ),
            message = "You set ${member.name} up on a date. Word around $countryName is they're seeing someone new."
        )
    }

    private fun commitMemberUpdate(
        character: Character,
        memberIndex: Int,
        updatedMember: Person,
        interactionType: InteractionType,
        milestoneKind: MilestoneKind? = null,
        subjectName: String? = null,
        giftTier: GiftTier? = null,
        recordFirstQualityTime: Boolean = false
    ): Character {
        var person = updatedMember.copy(interactedThisYear = true)
        val milestones = person.milestones.toMutableList()
        if (recordFirstQualityTime && milestones.none { it.kind == MilestoneKind.QUALITY_TIME.name }) {
            milestones += RelationshipMilestone.fromKind(
                character.age,
                MilestoneKind.QUALITY_TIME,
                person.name,
                InteractionType.SPEND_TIME
            )
        }
        if (shouldRecordMilestone(interactionType, milestoneKind, giftTier) &&
            milestoneKind != null &&
            subjectName != null
        ) {
            milestones += RelationshipMilestone.fromKind(
                character.age,
                milestoneKind,
                subjectName,
                interactionType
            )
        }
        person = person.copy(milestones = milestones)
        return character.copy(family = character.family.replaceAt(memberIndex, person.coerceRelationship()))
    }

    private fun shouldRecordMilestone(
        interactionType: InteractionType,
        milestoneKind: MilestoneKind?,
        giftTier: GiftTier? = null
    ): Boolean {
        if (milestoneKind == null) return false
        return when (interactionType) {
            InteractionType.ARGUE,
            InteractionType.INSULT,
            InteractionType.TRAVEL_TOGETHER,
            InteractionType.SET_UP_ON_DATE -> true
            InteractionType.GIFT -> giftTier == GiftTier.MEDIUM || giftTier == GiftTier.LARGE
            else -> false
        }
    }

    private fun driftTowardNeutral(level: Int, relation: RelationType): Int {
        val step = decayPointsFor(relation)
        return when {
            level > 50 -> (level - step).coerceAtLeast(50)
            level < 50 -> (level + step).coerceAtMost(50)
            else -> 50
        }
    }

    private fun decayPointsFor(relation: RelationType): Int = when (relation) {
        RelationType.SPOUSE, RelationType.CHILD -> DECAY_POINTS_COHABITING
        else -> DECAY_POINTS_PER_YEAR
    }

    private fun proposalAcceptChance(relationshipLevel: Int): Float {
        return (0.5f + (relationshipLevel - PROPOSAL_THRESHOLD) * 0.02f).coerceIn(0.5f, 0.95f)
    }

    private fun List<Person>.replaceAt(index: Int, person: Person): List<Person> =
        toMutableList().apply { this[index] = person }

    companion object {
        fun canTravelTogether(person: Person): Boolean {
            val hasBond = person.milestones.any { it.kind == MilestoneKind.QUALITY_TIME.name }
            return person.relationshipLevel >= TRAVEL_MIN_RELATIONSHIP && hasBond
        }

        const val RELATIONSHIP_TAG = "relationship"
        const val REQUIRES_SPOUSE_TAG = "requires_spouse"
        const val REQUIRES_MARRIED_TAG = "requires_married"
        const val REQUIRES_CHILD_TAG = "requires_child"
        const val REQUIRES_PARENT_TAG = "requires_parent"
        const val REQUIRES_CHILD_SCHOOL_AGE_TAG = "requires_child_school_age"
        const val REQUIRES_SINGLE_TAG = "requires_single"

        const val PROPOSAL_THRESHOLD = 70
        const val TRAVEL_MIN_RELATIONSHIP = 40
        const val TRAVEL_BASE_COST_KENYA = 10_000
        private const val MIN_DATING_AGE = 18
        private const val MIN_FRIEND_AGE = 6
        private const val MAX_FRIEND_AGE = 65
        private const val MAX_FRIENDS = 4
        private const val SCHOOL_FRIEND_CHANCE = 0.10f
        private const val WORK_FRIEND_CHANCE = 0.07f
        private const val BREAKUP_HAPPINESS_PENALTY = 10
        private const val DIVORCE_HAPPINESS_PENALTY = 20
        private const val ASK_MONEY_SUCCESS_CHANCE = 0.7f
        private const val DECAY_POINTS_PER_YEAR = 1
        private const val DECAY_POINTS_COHABITING = 1
    }
}

fun Character.hasSpouse(): Boolean =
    family.any { it.relation == RelationType.SPOUSE }

fun Character.spouse(): Person? =
    family.firstOrNull { it.relation == RelationType.SPOUSE }

fun Character.isMarried(): Boolean =
    spouse()?.isMarried == true

fun Character.hasChild(): Boolean =
    family.any { it.relation == RelationType.CHILD }
