// app/src/main/java/com/maisha/game/domain/RelationshipEngine.kt (modified — expanded interactions + country-aware)
package com.maisha.game.domain

import com.maisha.game.data.CountryCatalog
import com.maisha.game.data.DatingPool
import com.maisha.game.data.EconomyScaler
import com.maisha.game.data.NamePool
import com.maisha.game.data.PetCatalog
import com.maisha.game.data.model.AvatarConfig
import com.maisha.game.data.model.Character
import com.maisha.game.data.model.Gender
import com.maisha.game.data.model.MilestoneKind
import com.maisha.game.data.model.Person
import com.maisha.game.data.model.Pet
import com.maisha.game.data.model.PetSpecies
import com.maisha.game.data.model.RelationType
import com.maisha.game.data.model.RelationshipDecayNotice
import com.maisha.game.data.model.RelationshipMilestone
import com.maisha.game.data.model.RelationshipTier
import com.maisha.game.data.model.Stats
import com.maisha.game.data.model.relationshipTierFor
import com.maisha.game.util.clampRelationshipLevel
import com.maisha.game.util.clampStat
import com.maisha.game.util.formatMoney
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

sealed class ProposalResult {
    data class Accepted(val character: Character) : ProposalResult()
    data object Rejected : ProposalResult()
}

sealed class AdoptPetResult {
    data class Success(val character: Character) : AdoptPetResult()
    data object InsufficientFunds : AdoptPetResult()
    data object MaxPetsReached : AdoptPetResult()
    data object Ineligible : AdoptPetResult()
}

data class PetYearTickResult(val character: Character)

@Singleton
class RelationshipEngine @Inject constructor(
    private val financeEngine: FinanceEngine
) {

    /** Maps [Person.relationshipLevel] to a display tier (ESTRANGED through INSEPARABLE). */
    fun getRelationshipTier(person: Person): RelationshipTier =
        relationshipTierFor(person.relationshipLevel)

    /**
     * Parents react sharply when the player is expelled — significant relationship penalty.
     * Call after [EducationEngine.processExpulsion] sets [com.maisha.game.data.model.EducationState.expelled].
     */
    fun applyExpulsionFamilyEffect(character: Character): Character {
        if (!character.education.expelled) return character
        val updatedFamily = character.family.map { person ->
            if (person.relation == RelationType.MOTHER || person.relation == RelationType.FATHER) {
                person.copy(
                    relationshipLevel = clampRelationshipLevel(
                        person.relationshipLevel + EXPULSION_PARENT_RELATIONSHIP_PENALTY
                    )
                )
            } else {
                person
            }
        }
        return character.copy(family = updatedFamily)
    }

    /**
     * Adopts a shelter pet: deducts scaled adoption fee and appends to [Character.pets].
     * Capped at [MAX_PETS]; blocked when incarcerated or awaiting trial.
     */
    fun adoptPet(character: Character, species: PetSpecies, name: String): AdoptPetResult {
        if (!character.alive ||
            character.criminalRecord.currentlyIncarcerated ||
            character.criminalRecord.awaitingTrial
        ) {
            return AdoptPetResult.Ineligible
        }
        if (character.pets.size >= MAX_PETS) {
            return AdoptPetResult.MaxPetsReached
        }
        val catalogEntry = PetCatalog.findBySpecies(species) ?: return AdoptPetResult.Ineligible
        val adoptionFee = EconomyScaler.scaleAmount(catalogEntry.adoptionFee, character.countryCode)
        if (character.stats.money < adoptionFee) {
            return AdoptPetResult.InsufficientFunds
        }

        val trimmedName = name.trim().ifEmpty { catalogEntry.defaultName }
        val pet = Pet(
            id = UUID.randomUUID().toString(),
            name = trimmedName,
            species = species
        )
        val updated = character.copy(
            stats = character.stats.copy(money = character.stats.money - adoptionFee),
            pets = character.pets + pet,
            eventLog = EventLogCap.prepend(
                character.eventLog,
                "Adopted ${catalogEntry.displayName.lowercase()} $trimmedName."
            )
        )
        return AdoptPetResult.Success(updated)
    }

    /**
     * Ages pets, applies species mortality rolls, and applies happiness loss when a companion dies.
     */
    fun tickPetsYear(character: Character): PetYearTickResult {
        if (character.pets.isEmpty()) return PetYearTickResult(character)

        var happiness = character.stats.happiness
        var eventLog = character.eventLog
        val survivors = mutableListOf<Pet>()

        for (pet in character.pets) {
            val aged = pet.copy(
                age = pet.age + 1,
                health = clampStat(pet.health - healthDecayForAge(pet.age + 1))
            )
            if (rollsPetDeathThisYear(aged)) {
                happiness = clampStat(happiness - PET_DEATH_HAPPINESS_PENALTY)
                eventLog = EventLogCap.prepend(
                    eventLog,
                    "${aged.name} (${speciesLabel(aged.species)}) passed away at age ${aged.age}."
                )
            } else {
                survivors += aged.copy(
                    relationshipLevel = driftPetBond(aged.relationshipLevel)
                )
            }
        }

        return PetYearTickResult(
            character.copy(
                pets = survivors,
                stats = character.stats.copy(happiness = happiness),
                eventLog = eventLog
            )
        )
    }

    /** Public for unit tests — species-specific mortality probability for one yearly roll. */
    fun rollsPetDeathThisYear(pet: Pet): Boolean {
        val chance = petMortalityChance(pet)
        if (chance <= 0f) return false
        return Random.nextFloat() < chance
    }

    fun petMortalityChance(pet: Pet): Float = when (pet.species) {
        PetSpecies.FISH -> when {
            pet.age < 2 -> 0.03f
            pet.age == 2 -> 0.40f
            else -> 0.95f
        }
        PetSpecies.BIRD -> when {
            pet.age < 8 -> 0.02f + pet.age * 0.01f
            pet.age < 12 -> 0.10f + (pet.age - 8) * 0.18f
            else -> 0.92f
        }
        PetSpecies.DOG, PetSpecies.CAT -> when {
            pet.age < 10 -> 0.01f + pet.age * 0.005f
            pet.age < 15 -> 0.08f + (pet.age - 10) * 0.16f
            else -> 0.93f
        }
        PetSpecies.EXOTIC -> when {
            pet.age < 12 -> 0.02f + pet.age * 0.008f
            pet.age < 18 -> 0.10f + (pet.age - 12) * 0.12f
            else -> 0.90f
        }
    }

    private fun healthDecayForAge(age: Int): Int = when {
        age < 5 -> 0
        age < 10 -> 1
        else -> 2
    }

    private fun driftPetBond(level: Int): Int {
        val step = 1
        return when {
            level > 55 -> (level - step).coerceAtLeast(55)
            level < 55 -> (level + step).coerceAtMost(55)
            else -> 55
        }
    }

    private fun speciesLabel(species: PetSpecies): String = when (species) {
        PetSpecies.DOG -> "dog"
        PetSpecies.CAT -> "cat"
        PetSpecies.BIRD -> "bird"
        PetSpecies.FISH -> "fish"
        PetSpecies.EXOTIC -> "exotic pet"
    }

    /**
     * Gentle annual drift toward neutral (50) for family not interacted with this year.
     * Spouse/child: 1 pt/year; others: 2 pts/year.
     */
    fun tickFamilyYear(character: Character): FamilyYearTickResult {
        val notices = mutableListOf<RelationshipDecayNotice>()
        // Support costs use pre-tick ages (care for the year just lived).
        val withSupport = financeEngine.applyChildSupport(character)
        val family = withSupport.family.map { person ->
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
        return FamilyYearTickResult(
            family = family,
            decayNotices = notices,
            stats = withSupport.stats
        )
    }

    /** True when relationship is CLOSE or INSEPARABLE and person is alive. */
    fun canTravelTogether(person: Person): Boolean = Companion.canTravelTogether(person)

    /** Dating prospects when single and past [MIN_DATING_AGE]; empty if already married. */
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

        return PersonGenerator.buildFriend(character, MIN_FRIEND_AGE)
    }

    /** Logs legacy-continuation milestones on inherited family when Legacy Mode selects an heir. */
    fun applyLegacyFamilyMilestones(character: Character): Character {
        val updatedFamily = character.family.map { person ->
            if (person.milestones.any { it.kind == MilestoneKind.LEGACY_CONTINUED.name }) {
                person
            } else {
                person.copy(
            milestones = RelationshipMilestoneCap.trim(
                person.milestones + RelationshipMilestone.fromKind(
                    age = character.age,
                    kind = MilestoneKind.LEGACY_CONTINUED,
                    subjectName = person.name
                )
            )
                )
            }
        }
        return character.copy(family = updatedFamily)
    }

    /** Adds prospect as dating [RelationType.SPOUSE] (not yet married) with STARTED_DATING milestone. */
    fun startDating(character: Character, prospect: Person): Character {
        if (character.hasSpouse()) return character
        val partner = prospect.copy(
            relation = RelationType.SPOUSE,
            dateOfPartnership = character.age,
            isMarried = false,
            milestones = RelationshipMilestoneCap.trim(
                prospect.milestones + RelationshipMilestone.fromKind(
                    age = character.age,
                    kind = MilestoneKind.STARTED_DATING,
                    subjectName = prospect.name
                )
            )
        )
        return character.copy(family = character.family + partner)
    }

    /**
     * Player interaction with a family member: spend time, argue, gifts, travel, etc.
     *
     * Sets [Person.interactedThisYear] on success. Travel blocked while incarcerated.
     */
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
        if (isParentingAction(interactionType) && !Companion.isMinorChild(member)) {
            return FamilyInteractionResult(
                character = character,
                message = "That only works with your children under 18."
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
            InteractionType.HELP_WITH_HOMEWORK -> applyHelpWithHomework(character, memberIndex, member)
            InteractionType.PAY_ALLOWANCE -> applyPayAllowance(character, memberIndex, member)
            InteractionType.DISCIPLINE -> applyDiscipline(character, memberIndex, member)
        }
    }

    /**
     * Marriage proposal to a dating partner. Requires relationship ≥ [PROPOSAL_THRESHOLD];
     * acceptance chance scales with level.
     */
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
                milestones = RelationshipMilestoneCap.trim(
                    partner.milestones + RelationshipMilestone.fromKind(
                        age = character.age,
                        kind = MilestoneKind.MARRIED,
                        subjectName = partner.name
                    )
                )
            ).coerceRelationship()
            val updated = character.copy(
                family = character.family.replaceAt(memberIndex, marriedPartner),
                stats = character.stats.copy(
                    happiness = clampStat(character.stats.happiness + 10)
                )
            )
            updated to ProposalResult.Accepted(updated)
        } else {
            val declinedPartner = partner.copy(
                relationshipLevel = clampRelationshipLevel(partner.relationshipLevel - 15)
            ).coerceRelationship()
            val updated = character.copy(
                family = character.family.replaceAt(memberIndex, declinedPartner)
            )
            updated to ProposalResult.Rejected
        }
    }

    /** Removes spouse from family; larger happiness hit if [Person.isMarried]. */
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
                happiness = clampStat(character.stats.happiness - happinessPenalty)
            ),
            eventLog = EventLogCap.prepend(
                character.eventLog,
                "$label ${partner.name} at age ${character.age}."
            )
        )
    }

    /**
     * Adds a newborn child when married; may set [Person.secondaryCountryCode] for cross-country spouses.
     */
    fun haveChild(character: Character): Character {
        val spouse = character.family.firstOrNull {
            it.relation == RelationType.SPOUSE && it.isMarried
        } ?: return character

        val gender = if (Random.nextBoolean()) Gender.MALE else Gender.FEMALE
        val isCrossCountry = character.countryCode != spouse.countryCode
        val childName = if (isCrossCountry) {
            mixedHeritageChildName(gender, character.countryCode, spouse.countryCode)
        } else {
            NamePool.randomFullName(gender, character.countryCode)
        }
        val child = Person(
            id = UUID.randomUUID().toString(),
            name = childName,
            relation = RelationType.CHILD,
            gender = gender,
            age = 0,
            relationshipLevel = 60,
            stats = Stats(
                health = Random.nextInt(50, 81),
                happiness = Random.nextInt(50, 81)
            ),
            avatarConfig = FamilyGenerator.inheritAvatarConfig(
                character.avatarConfig,
                spouse.avatarConfig
            ),
            countryCode = character.countryCode,
            secondaryCountryCode = if (isCrossCountry) spouse.countryCode else null
        )
        return character.copy(
            family = character.family + child,
            stats = character.stats.copy(
                happiness = clampStat(character.stats.happiness + 5)
            )
        )
    }

    private fun mixedHeritageChildName(
        gender: Gender,
        primaryCountry: String,
        secondaryCountry: String
    ): String {
        // First name from one parent's pool, surname from the other — reads naturally on cards.
        val firstFromSecondary = Random.nextBoolean()
        val firstPool = NamePool.getNamePool(if (firstFromSecondary) secondaryCountry else primaryCountry)
        val surnamePool = NamePool.getNamePool(if (firstFromSecondary) primaryCountry else secondaryCountry)
        return "${firstPool.randomFirstName(gender)} ${surnamePool.randomSurname()}"
    }

    /**
     * Adjusts spouse [Person.relationshipLevel]: explicit [delta] from event choices,
     * or passive drift when [netWorth] is supplied (annual tick from [GameEngine.ageUp]).
     */
    fun applySpouseRelationshipEffect(
        character: Character,
        delta: Int = 0,
        netWorth: Int? = null
    ): Character {
        val spouseIndex = character.family.indexOfFirst { it.relation == RelationType.SPOUSE }
        if (spouseIndex == -1) return character

        val spouseDelta = when {
            delta != 0 -> delta
            netWorth != null -> passiveSpouseDelta(character, netWorth)
            else -> 0
        }
        if (spouseDelta == 0) return character

        val spouse = character.family[spouseIndex]
        val updated = spouse.copy(
            relationshipLevel = clampRelationshipLevel(spouse.relationshipLevel + spouseDelta)
        ).coerceRelationship()
        return character.copy(family = character.family.replaceAt(spouseIndex, updated))
    }

    private fun passiveSpouseDelta(character: Character, netWorth: Int): Int {
        if (character.criminalRecord.currentlyIncarcerated) {
            return SPOUSE_PASSIVE_INCARCERATION_PENALTY
        }
        return when {
            character.stats.happiness >= 70 && netWorth >= WEALTHY_NET_WORTH_THRESHOLD ->
                SPOUSE_PASSIVE_PROSPERITY_BOOST
            character.stats.happiness >= 60 && netWorth >= COMFORTABLE_NET_WORTH_THRESHOLD ->
                SPOUSE_PASSIVE_STABILITY_BOOST
            netWorth < STRUGGLING_NET_WORTH_THRESHOLD -> SPOUSE_PASSIVE_FINANCIAL_STRESS
            character.stats.happiness < 35 && netWorth < COMFORTABLE_NET_WORTH_THRESHOLD ->
                SPOUSE_PASSIVE_LOW_MOOD
            else -> 0
        }
    }

    private fun applySpendTime(
        character: Character,
        memberIndex: Int,
        member: Person
    ): FamilyInteractionResult {
        val updatedMember = member.copy(
            relationshipLevel = clampRelationshipLevel(member.relationshipLevel + 10)
        ).coerceRelationship()
        val updatedStats = character.stats.copy(
            happiness = clampStat(character.stats.happiness + 5)
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
            relationshipLevel = clampRelationshipLevel(member.relationshipLevel - 10)
        ).coerceRelationship()
        val updatedStats = character.stats.copy(
            happiness = clampStat(character.stats.happiness - 5)
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
                relationshipLevel = clampRelationshipLevel(member.relationshipLevel - 5),
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
            relationshipLevel = clampRelationshipLevel(member.relationshipLevel + tier.relationshipBoost)
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
                    happiness = clampStat(character.stats.happiness + 3)
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
            relationshipLevel = clampRelationshipLevel(member.relationshipLevel + boost),
            complimentsThisYear = member.complimentsThisYear + 1
        ).coerceRelationship()
        return FamilyInteractionResult(
            character = commitMemberUpdate(
                character, memberIndex, updatedMember,
                interactionType = InteractionType.COMPLIMENT
            ).copy(
                stats = character.stats.copy(
                    happiness = clampStat(character.stats.happiness + 1)
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
            relationshipLevel = clampRelationshipLevel(member.relationshipLevel - 12)
        ).coerceRelationship()
        return FamilyInteractionResult(
            character = commitMemberUpdate(
                character, memberIndex, updatedMember,
                interactionType = InteractionType.INSULT,
                milestoneKind = MilestoneKind.INSULTED,
                subjectName = member.name
            ).copy(
                stats = character.stats.copy(
                    happiness = clampStat(character.stats.happiness + 2)
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
            relationshipLevel = clampRelationshipLevel(member.relationshipLevel + 18)
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
                    happiness = clampStat(character.stats.happiness + 8)
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
            relationshipLevel = clampRelationshipLevel(member.relationshipLevel + 3)
        ).coerceRelationship()
        return FamilyInteractionResult(
            character = commitMemberUpdate(
                character, memberIndex, updatedMember,
                interactionType = InteractionType.ASK_FOR_ADVICE
            ).copy(
                stats = character.stats.copy(
                    smarts = clampStat(character.stats.smarts + 2)
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
            relationshipLevel = clampRelationshipLevel(member.relationshipLevel + relDelta)
        ).coerceRelationship()
        return FamilyInteractionResult(
            character = commitMemberUpdate(
                character, memberIndex, updatedMember,
                interactionType = InteractionType.PRANK
            ).copy(
                stats = character.stats.copy(
                    happiness = clampStat(character.stats.happiness + happyDelta)
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
            relationshipLevel = clampRelationshipLevel(member.relationshipLevel + 6)
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

    private fun applyHelpWithHomework(
        character: Character,
        memberIndex: Int,
        member: Person
    ): FamilyInteractionResult {
        val updatedMember = member.copy(
            relationshipLevel = clampRelationshipLevel(
                member.relationshipLevel + HOMEWORK_RELATIONSHIP_BOOST
            ),
            stats = member.stats.copy(
                smarts = clampStat(member.stats.smarts + HOMEWORK_CHILD_SMARTS_BOOST)
            )
        ).coerceRelationship()
        return FamilyInteractionResult(
            character = commitMemberUpdate(
                character, memberIndex, updatedMember,
                interactionType = InteractionType.HELP_WITH_HOMEWORK
            ).copy(
                stats = character.stats.copy(
                    happiness = clampStat(character.stats.happiness + HOMEWORK_HAPPINESS_DELTA),
                    health = clampStat(character.stats.health + HOMEWORK_HEALTH_DELTA)
                )
            ),
            message = "You helped ${member.name} with homework. They seem more confident."
        )
    }

    private fun applyPayAllowance(
        character: Character,
        memberIndex: Int,
        member: Person
    ): FamilyInteractionResult {
        val cost = allowanceCost(character)
        if (character.stats.money < cost) {
            return FamilyInteractionResult(
                character = character,
                message = "You can't afford allowance (${formatMoney(cost, character.countryCode)})."
            )
        }
        val updatedMember = member.copy(
            relationshipLevel = clampRelationshipLevel(
                member.relationshipLevel + ALLOWANCE_RELATIONSHIP_BOOST
            )
        ).coerceRelationship()
        return FamilyInteractionResult(
            character = commitMemberUpdate(
                character, memberIndex, updatedMember,
                interactionType = InteractionType.PAY_ALLOWANCE
            ).copy(
                stats = character.stats.copy(
                    money = character.stats.money - cost,
                    happiness = clampStat(character.stats.happiness + ALLOWANCE_HAPPINESS_DELTA)
                )
            ),
            message = "You gave ${member.name} ${formatMoney(cost, character.countryCode)} allowance."
        )
    }

    private fun applyDiscipline(
        character: Character,
        memberIndex: Int,
        member: Person
    ): FamilyInteractionResult {
        val updatedMember = member.copy(
            relationshipLevel = clampRelationshipLevel(
                member.relationshipLevel + DISCIPLINE_RELATIONSHIP_DELTA
            )
        ).coerceRelationship()
        return FamilyInteractionResult(
            character = commitMemberUpdate(
                character, memberIndex, updatedMember,
                interactionType = InteractionType.DISCIPLINE,
                milestoneKind = MilestoneKind.BIG_ARGUMENT,
                subjectName = member.name
            ).copy(
                stats = character.stats.copy(
                    happiness = clampStat(character.stats.happiness + DISCIPLINE_HAPPINESS_DELTA),
                    health = clampStat(character.stats.health + DISCIPLINE_HEALTH_DELTA)
                )
            ),
            message = "You disciplined ${member.name}. The house is quieter — and tenser."
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
        person = person.copy(milestones = RelationshipMilestoneCap.trim(milestones))
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
            InteractionType.DISCIPLINE,
            InteractionType.TRAVEL_TOGETHER,
            InteractionType.SET_UP_ON_DATE -> true
            InteractionType.GIFT -> giftTier == GiftTier.MEDIUM || giftTier == GiftTier.LARGE
            else -> false
        }
    }

    private fun isParentingAction(type: InteractionType): Boolean = when (type) {
        InteractionType.HELP_WITH_HOMEWORK,
        InteractionType.PAY_ALLOWANCE,
        InteractionType.DISCIPLINE -> true
        else -> false
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
        const val REQUIRES_CHILD_TODDLER_TAG = "requires_child_toddler"
        const val REQUIRES_CHILD_PRIMARY_TAG = "requires_child_primary"
        const val REQUIRES_CHILD_TEEN_TAG = "requires_child_teen"
        const val REQUIRES_SINGLE_TAG = "requires_single"
        const val REQUIRES_MIXED_HERITAGE_TAG = "requires_mixed_heritage"
        const val REQUIRES_MIXED_HERITAGE_CHILD_TAG = "requires_mixed_heritage_child"
        const val REQUIRES_PET_TAG = "requires_pet"

        const val MAX_PETS = 5
        const val PET_DEATH_HAPPINESS_PENALTY = 25
        const val MINOR_CHILD_MAX_AGE = 18
        const val ALLOWANCE_BASE_COST_KENYA = 3_000
        const val CHILD_TODDLER_MIN_AGE = 2
        const val CHILD_TODDLER_MAX_AGE = 4
        const val CHILD_PRIMARY_MIN_AGE = 6
        const val CHILD_PRIMARY_MAX_AGE = 10
        const val CHILD_TEEN_MIN_AGE = 14
        const val CHILD_TEEN_MAX_AGE = 17

        private const val HOMEWORK_RELATIONSHIP_BOOST = 8
        private const val HOMEWORK_CHILD_SMARTS_BOOST = 2
        private const val HOMEWORK_HAPPINESS_DELTA = 2
        private const val HOMEWORK_HEALTH_DELTA = -1
        private const val ALLOWANCE_RELATIONSHIP_BOOST = 10
        private const val ALLOWANCE_HAPPINESS_DELTA = 2
        private const val DISCIPLINE_RELATIONSHIP_DELTA = -8
        private const val DISCIPLINE_HAPPINESS_DELTA = -4
        private const val DISCIPLINE_HEALTH_DELTA = -1

        const val PROPOSAL_THRESHOLD = 70
        const val TRAVEL_MIN_RELATIONSHIP = 40
        const val EXPULSION_PARENT_RELATIONSHIP_PENALTY = -30
        private const val SPOUSE_PASSIVE_INCARCERATION_PENALTY = -15
        private const val SPOUSE_PASSIVE_FINANCIAL_STRESS = -8
        private const val SPOUSE_PASSIVE_LOW_MOOD = -5
        private const val SPOUSE_PASSIVE_STABILITY_BOOST = 3
        private const val SPOUSE_PASSIVE_PROSPERITY_BOOST = 5
        private const val WEALTHY_NET_WORTH_THRESHOLD = 200_000
        private const val COMFORTABLE_NET_WORTH_THRESHOLD = 100_000
        private const val STRUGGLING_NET_WORTH_THRESHOLD = 20_000
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

        fun allowanceCost(character: Character): Int =
            EconomyScaler.scaleAmount(ALLOWANCE_BASE_COST_KENYA, character.countryCode)

        fun isMinorChild(person: Person): Boolean =
            person.relation == RelationType.CHILD && person.alive && person.age < MINOR_CHILD_MAX_AGE
    }

    fun allowanceCost(character: Character): Int = Companion.allowanceCost(character)
}

fun Character.hasSpouse(): Boolean =
    family.any { it.relation == RelationType.SPOUSE }

fun Character.spouse(): Person? =
    family.firstOrNull { it.relation == RelationType.SPOUSE }

fun Character.isMarried(): Boolean =
    spouse()?.isMarried == true

fun Character.hasChild(): Boolean =
    family.any { it.relation == RelationType.CHILD }

fun Character.hasPet(): Boolean = pets.isNotEmpty()

fun Character.hasMixedHeritageParents(): Boolean {
    val mother = family.find { it.relation == RelationType.MOTHER }
    val father = family.find { it.relation == RelationType.FATHER }
    return mother != null && father != null && mother.countryCode != father.countryCode
}

fun Character.hasMixedHeritageContext(): Boolean {
    if (secondaryCountryCode != null) return true
    if (family.any { it.secondaryCountryCode != null }) return true
    return hasMixedHeritageParents()
}

fun Character.hasMixedHeritageChild(): Boolean =
    family.any { it.relation == RelationType.CHILD && it.secondaryCountryCode != null }
