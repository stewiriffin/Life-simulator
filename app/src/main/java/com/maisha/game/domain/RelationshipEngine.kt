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
import kotlin.math.roundToInt
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

sealed class PartyResult {
    data class Success(val character: Character, val boost: Int) : PartyResult()
    data object InsufficientFunds : PartyResult()
    data object NoGuests : PartyResult()
    data object InvalidBudget : PartyResult()
}

sealed class StartDatingResult {
    data class Success(val character: Character) : StartDatingResult()
    data object InsufficientFunds : StartDatingResult()
    data object Ineligible : StartDatingResult()
}

sealed class HaveChildResult {
    data class Success(val character: Character) : HaveChildResult()
    data object NeedSpouse : HaveChildResult()
    data object InsufficientFunds : HaveChildResult()
}

sealed class AdoptChildResult {
    data class Success(val character: Character) : AdoptChildResult()
    data object TooYoung : AdoptChildResult()
    data object InsufficientFunds : AdoptChildResult()
    data object Ineligible : AdoptChildResult()
}

sealed class BreakUpResult {
    data class Success(val character: Character, val wasMarried: Boolean, val settlement: Int) : BreakUpResult()
    data object NotPartner : BreakUpResult()
}

sealed class SeekFriendshipResult {
    data class Success(val character: Character, val friendName: String) : SeekFriendshipResult()
    data class NoLuck(val character: Character) : SeekFriendshipResult()
    data object InsufficientFunds : SeekFriendshipResult()
    data object AlreadySocialized : SeekFriendshipResult()
    data object FriendsFull : SeekFriendshipResult()
    data object Ineligible : SeekFriendshipResult()
}

sealed class PetCareResult {
    data class Success(
        val character: Character,
        val messageKey: String,
        val messageArgs: List<String> = emptyList()
    ) : PetCareResult()
    data object InsufficientFunds : PetCareResult()
    data object AlreadyDone : PetCareResult()
    data object NotFound : PetCareResult()
    data object Ineligible : PetCareResult()
}

enum class PetCareAction {
    PLAY,
    FEED,
    VET
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
                    relationshipLevel = driftPetBond(aged.relationshipLevel),
                    playedThisYear = false,
                    caredThisYear = false
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
            family = applySocialStatusShifts(family),
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
     * Returns a [Person] with [RelationType.FRIEND] to add to [Character.family], or null.
     */
    fun generateFriendshipOpportunity(character: Character): Person? {
        if (character.age < MIN_FRIEND_AGE || character.age > MAX_FRIEND_AGE) return null
        val friendCount = character.family.count { it.isPlatonicAlly() }
        if (friendCount >= MAX_FRIENDS) return null

        val chance = when {
            character.age in 6..17 -> SCHOOL_FRIEND_CHANCE
            character.age in 18..55 -> WORK_FRIEND_CHANCE
            else -> 0.04f
        }
        if (Random.nextFloat() >= chance) return null

        return PersonGenerator.buildFriend(character, MIN_FRIEND_AGE)
    }

    /**
     * Hosts a party: spends [budget], boosts all living friends and siblings by 5–15
     * (scaled with budget size).
     */
    fun throwParty(character: Character, budget: Int): PartyResult {
        if (budget <= 0) return PartyResult.InvalidBudget
        if (character.stats.money < budget) return PartyResult.InsufficientFunds
        val guests = character.family.filter {
            it.alive && (it.isPlatonicAlly() || it.relation == RelationType.SIBLING)
        }
        if (guests.isEmpty()) return PartyResult.NoGuests

        val boost = partyBoostForBudget(budget, character.countryCode)
        val guestIds = guests.map { it.id }.toSet()
        val updatedFamily = applySocialStatusShifts(
            character.family.map { person ->
                if (person.id in guestIds) {
                    person.copy(
                        relationshipLevel = clampRelationshipLevel(person.relationshipLevel + boost),
                        interactedThisYear = true
                    ).coerceRelationship()
                } else {
                    person
                }
            }
        )
        return PartyResult.Success(
            character.copy(
                stats = character.stats.copy(
                    money = character.stats.money - budget,
                    happiness = clampStat(character.stats.happiness + (boost / 2).coerceAtLeast(2))
                ),
                family = updatedFamily,
                eventLog = EventLogCap.prepend(
                    character.eventLog,
                    "You threw a party (budget ${formatMoney(budget, character.countryCode)}). " +
                        "Friends and siblings grew closer (+$boost)."
                )
            ),
            boost = boost
        )
    }

    fun partyBoostForBudget(budget: Int, countryCode: String): Int {
        val minBudget = EconomyScaler.scaleAmount(PARTY_BUDGET_MIN_KENYA, countryCode).coerceAtLeast(1)
        val maxBudget = EconomyScaler.scaleAmount(PARTY_BUDGET_MAX_KENYA, countryCode).coerceAtLeast(minBudget + 1)
        val t = ((budget - minBudget).toFloat() / (maxBudget - minBudget)).coerceIn(0f, 1f)
        return (PARTY_BOOST_MIN + (PARTY_BOOST_MAX - PARTY_BOOST_MIN) * t).roundToInt()
            .coerceIn(PARTY_BOOST_MIN, PARTY_BOOST_MAX)
    }

    fun minPartyBudget(countryCode: String): Int =
        EconomyScaler.scaleAmount(PARTY_BUDGET_MIN_KENYA, countryCode)

    /**
     * FRIEND → BEST_FRIEND above [BEST_FRIEND_THRESHOLD]; non-family below [ENEMY_THRESHOLD] → ENEMY.
     * ENEMY can reconcile to FRIEND above [ENEMY_RECONCILE_THRESHOLD].
     */
    fun applySocialStatusShifts(family: List<Person>): List<Person> =
        family.map { applySocialStatusShift(it) }

    fun applySocialStatusShift(person: Person): Person {
        if (!person.isSocialCircleMember()) return person
        val level = person.relationshipLevel
        val newRelation = when {
            level < ENEMY_THRESHOLD -> RelationType.ENEMY
            person.relation == RelationType.ENEMY && level >= ENEMY_RECONCILE_THRESHOLD ->
                RelationType.FRIEND
            person.relation == RelationType.FRIEND && level > BEST_FRIEND_THRESHOLD ->
                RelationType.BEST_FRIEND
            person.relation == RelationType.BEST_FRIEND && level <= BEST_FRIEND_THRESHOLD &&
                level >= ENEMY_THRESHOLD -> RelationType.BEST_FRIEND
            person.relation == RelationType.ENEMY -> RelationType.ENEMY
            else -> person.relation
        }
        return if (newRelation == person.relation) person else person.copy(relation = newRelation)
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

    /** Adds prospect as dating [RelationType.SPOUSE] (not yet married); charges a first-date fee. */
    fun startDating(character: Character, prospect: Person): StartDatingResult {
        if (!character.alive || character.hasSpouse() || character.age < 18) {
            return StartDatingResult.Ineligible
        }
        if (character.criminalRecord.currentlyIncarcerated ||
            character.criminalRecord.awaitingTrial
        ) {
            return StartDatingResult.Ineligible
        }
        val fee = firstDateCost(character)
        if (character.stats.money < fee) return StartDatingResult.InsufficientFunds
        val acceptanceChance = (
            DATING_BASE_ACCEPT_CHANCE +
                character.stats.looks / 100f * DATING_LOOKS_WEIGHT +
                character.stats.happiness / 100f * DATING_HAPPINESS_WEIGHT
            ).coerceIn(0.20f, 0.92f)
        if (Random.nextFloat() > acceptanceChance) {
            return StartDatingResult.Ineligible
        }
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
        return StartDatingResult.Success(
            character.copy(
                stats = character.stats.copy(money = character.stats.money - fee),
                family = character.family + partner,
                eventLog = EventLogCap.prepend(
                    character.eventLog,
                    "Started dating ${partner.name} (first date ${formatMoney(fee, character.countryCode)})."
                )
            )
        )
    }

    fun firstDateCost(character: Character): Int =
        EconomyScaler.scaleAmount(FIRST_DATE_COST_KENYA, character.countryCode)

    fun childHospitalCost(character: Character): Int =
        EconomyScaler.scaleAmount(CHILD_HOSPITAL_COST_KENYA, character.countryCode)

    fun divorceSettlementCost(character: Character, partner: Person? = null): Int {
        val base = EconomyScaler.scaleAmount(DIVORCE_SETTLEMENT_KENYA, character.countryCode)
        return if (partner?.prenupSigned == true) {
            (base * PRENUP_SETTLEMENT_FRACTION).roundToInt()
        } else {
            base
        }
    }

    fun dateNightCost(character: Character): Int =
        EconomyScaler.scaleRelationshipCost(DATE_NIGHT_COST_KENYA, character.countryCode, character.age)

    fun seekFriendshipCost(character: Character): Int =
        EconomyScaler.scaleAmount(SEEK_FRIEND_COST_KENYA, character.countryCode)

    fun petFeedCost(character: Character): Int =
        EconomyScaler.scaleAmount(PET_FEED_COST_KENYA, character.countryCode)

    fun petVetCost(character: Character): Int =
        EconomyScaler.scaleAmount(PET_VET_COST_KENYA, character.countryCode)

    fun livingFriendCount(character: Character): Int =
        character.family.count { it.alive && it.isPlatonicAlly() }

    fun canSeekFriendship(character: Character): Boolean =
        character.alive &&
            character.age >= MIN_FRIEND_AGE &&
            !character.criminalRecord.currentlyIncarcerated &&
            !character.criminalRecord.awaitingTrial &&
            !character.lifestyle.socializedThisYear &&
            livingFriendCount(character) < MAX_FRIENDS

    /**
     * Paid attempt to meet a new friend this year. Higher success chance than age-up RNG.
     */
    fun seekFriendship(character: Character): SeekFriendshipResult {
        if (!character.alive ||
            character.age < MIN_FRIEND_AGE ||
            character.criminalRecord.currentlyIncarcerated ||
            character.criminalRecord.awaitingTrial
        ) {
            return SeekFriendshipResult.Ineligible
        }
        if (character.lifestyle.socializedThisYear) return SeekFriendshipResult.AlreadySocialized
        if (livingFriendCount(character) >= MAX_FRIENDS) return SeekFriendshipResult.FriendsFull
        val cost = seekFriendshipCost(character)
        if (character.stats.money < cost) return SeekFriendshipResult.InsufficientFunds

        var updated = character.copy(
            stats = character.stats.copy(money = character.stats.money - cost),
            lifestyle = character.lifestyle.copy(socializedThisYear = true)
        )
        val successChance = (
            SEEK_FRIEND_SUCCESS_CHANCE +
                character.stats.happiness / 100f * SEEK_FRIEND_HAPPINESS_WEIGHT +
                character.stats.looks / 100f * SEEK_FRIEND_LOOKS_WEIGHT
        ).coerceIn(0.20f, 0.90f)
        if (Random.nextFloat() >= successChance) {
            updated = updated.copy(
                eventLog = EventLogCap.prepend(
                    updated.eventLog,
                    "Went out to meet people, but nobody clicked."
                )
            )
            return SeekFriendshipResult.NoLuck(updated)
        }
        val friend = PersonGenerator.buildFriend(updated, MIN_FRIEND_AGE)
        updated = updated.copy(
            family = updated.family + friend,
            stats = updated.stats.copy(
                happiness = clampStat(updated.stats.happiness + 3)
            ),
            eventLog = EventLogCap.prepend(
                updated.eventLog,
                "You made a new friend: ${friend.name}."
            )
        )
        return SeekFriendshipResult.Success(updated, friend.name)
    }

    fun careForPet(character: Character, petId: String, action: PetCareAction): PetCareResult {
        if (!character.alive ||
            character.criminalRecord.currentlyIncarcerated ||
            character.criminalRecord.awaitingTrial
        ) {
            return PetCareResult.Ineligible
        }
        val index = character.pets.indexOfFirst { it.id == petId }
        if (index < 0) return PetCareResult.NotFound
        val pet = character.pets[index]
        return when (action) {
            PetCareAction.PLAY -> playWithPet(character, index, pet)
            PetCareAction.FEED -> feedPet(character, index, pet)
            PetCareAction.VET -> vetPet(character, index, pet)
        }
    }

    private fun playWithPet(character: Character, index: Int, pet: Pet): PetCareResult {
        if (pet.playedThisYear) return PetCareResult.AlreadyDone
        val updatedPet = pet.copy(
            relationshipLevel = clampRelationshipLevel(pet.relationshipLevel + 8),
            playedThisYear = true
        )
        val pets = character.pets.toMutableList().also { it[index] = updatedPet }
        val updated = character.copy(
            pets = pets,
            stats = character.stats.copy(
                happiness = clampStat(character.stats.happiness + 4)
            ),
            eventLog = EventLogCap.prepend(
                character.eventLog,
                "Played with ${pet.name}."
            )
        )
        return PetCareResult.Success(
            updated,
            messageKey = "msg_pet_play",
            messageArgs = listOf(pet.name)
        )
    }

    private fun feedPet(character: Character, index: Int, pet: Pet): PetCareResult {
        val cost = petFeedCost(character)
        if (character.stats.money < cost) return PetCareResult.InsufficientFunds
        val updatedPet = pet.copy(
            relationshipLevel = clampRelationshipLevel(pet.relationshipLevel + 5),
            health = clampStat(pet.health + 6)
        )
        val pets = character.pets.toMutableList().also { it[index] = updatedPet }
        val updated = character.copy(
            pets = pets,
            stats = character.stats.copy(money = character.stats.money - cost),
            eventLog = EventLogCap.prepend(
                character.eventLog,
                "Fed ${pet.name} (${formatMoney(cost, character.countryCode)})."
            )
        )
        return PetCareResult.Success(
            updated,
            messageKey = "msg_pet_feed",
            messageArgs = listOf(pet.name)
        )
    }

    private fun vetPet(character: Character, index: Int, pet: Pet): PetCareResult {
        if (pet.caredThisYear) return PetCareResult.AlreadyDone
        val cost = petVetCost(character)
        if (character.stats.money < cost) return PetCareResult.InsufficientFunds
        val updatedPet = pet.copy(
            health = clampStat(pet.health + 25),
            relationshipLevel = clampRelationshipLevel(pet.relationshipLevel + 3),
            caredThisYear = true
        )
        val pets = character.pets.toMutableList().also { it[index] = updatedPet }
        val updated = character.copy(
            pets = pets,
            stats = character.stats.copy(money = character.stats.money - cost),
            eventLog = EventLogCap.prepend(
                character.eventLog,
                "Took ${pet.name} to the vet (${formatMoney(cost, character.countryCode)})."
            )
        )
        return PetCareResult.Success(
            updated,
            messageKey = "msg_pet_vet",
            messageArgs = listOf(pet.name)
        )
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
            return FamilyInteractionResult(character, "msg_person_not_found")
        }

        val member = character.family[memberIndex]
        if (interactionType == InteractionType.TRAVEL_TOGETHER &&
            character.criminalRecord.currentlyIncarcerated
        ) {
            return FamilyInteractionResult(
                character = character,
                messageKey = "msg_travel_incarcerated"
            )
        }
        if (isParentingAction(interactionType) && !Companion.isMinorChild(member)) {
            return FamilyInteractionResult(
                character = character,
                messageKey = "msg_parenting_minor_only"
            )
        }
        if (isAdultChildAction(interactionType) && !Companion.isAdultChild(member)) {
            return FamilyInteractionResult(
                character = character,
                messageKey = "msg_adult_child_only"
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
            InteractionType.DATE_NIGHT -> applyDateNight(character, memberIndex, member)
            InteractionType.MAKE_PEACE -> applyMakePeace(character, memberIndex, member)
            InteractionType.FINANCIAL_SUPPORT -> applyFinancialSupport(character, memberIndex, member)
            InteractionType.CELEBRATE_MILESTONE -> applyCelebrateMilestone(character, memberIndex, member)
            InteractionType.DISCUSS_LIFE_CHOICES -> applyDiscussLifeChoices(character, memberIndex, member)
        }
    }

    /**
     * Marriage proposal to a dating partner. Requires relationship ≥ [PROPOSAL_THRESHOLD];
     * acceptance chance scales with level.
     */
    fun proposeMarriage(
        character: Character,
        personId: String,
        signPrenup: Boolean = false
    ): Pair<Character, ProposalResult> {
        val memberIndex = character.family.indexOfFirst { it.id == personId }
        if (memberIndex == -1) return character to ProposalResult.Rejected

        val partner = character.family[memberIndex]
        if (partner.relation != RelationType.SPOUSE || partner.isMarried) {
            return character to ProposalResult.Rejected
        }
        if (partner.relationshipLevel < PROPOSAL_THRESHOLD) {
            return character to ProposalResult.Rejected
        }

        val accepted = Random.nextFloat() < proposalAcceptChance(partner, character)
        return if (accepted) {
            val marriedPartner = partner.copy(
                isMarried = true,
                prenupSigned = signPrenup,
                milestones = RelationshipMilestoneCap.trim(
                    partner.milestones + RelationshipMilestone.fromKind(
                        age = character.age,
                        kind = MilestoneKind.MARRIED,
                        subjectName = partner.name
                    )
                )
            ).coerceRelationship()
            val prenupNote = if (signPrenup) " A prenuptial agreement was signed." else ""
            val updated = character.copy(
                family = character.family.replaceAt(memberIndex, marriedPartner),
                stats = character.stats.copy(
                    happiness = clampStat(character.stats.happiness + 10)
                ),
                eventLog = EventLogCap.prepend(
                    character.eventLog,
                    "Married ${partner.name} at age ${character.age}.$prenupNote"
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

    /** Removes spouse from family; married splits pay a scaled settlement (clamped to cash on hand). */
    fun breakUpOrDivorce(character: Character, personId: String): BreakUpResult {
        val memberIndex = character.family.indexOfFirst { it.id == personId }
        if (memberIndex == -1) return BreakUpResult.NotPartner

        val partner = character.family[memberIndex]
        if (partner.relation != RelationType.SPOUSE) return BreakUpResult.NotPartner

        val wasMarried = partner.isMarried
        val happinessPenalty = if (wasMarried) DIVORCE_HAPPINESS_PENALTY else BREAKUP_HAPPINESS_PENALTY
        val settlement = if (wasMarried) {
            divorceSettlementCost(character, partner).coerceAtMost(character.stats.money)
        } else {
            0
        }
        val label = if (wasMarried) "Divorced" else "Broke up with"
        val settlementNote = if (settlement > 0) {
            " Settlement ${formatMoney(settlement, character.countryCode)}."
        } else {
            ""
        }
        val updated = character.copy(
            family = character.family.filterNot { it.id == personId },
            stats = character.stats.copy(
                money = character.stats.money - settlement,
                happiness = clampStat(character.stats.happiness - happinessPenalty)
            ),
            eventLog = EventLogCap.prepend(
                character.eventLog,
                "$label ${partner.name} at age ${character.age}.$settlementNote"
            )
        )
        return BreakUpResult.Success(updated, wasMarried, settlement)
    }

    /**
     * Adds a newborn child when married; charges a scaled hospital fee.
     */
    fun haveChild(character: Character): HaveChildResult {
        val spouse = character.family.firstOrNull {
            it.relation == RelationType.SPOUSE && it.isMarried
        } ?: return HaveChildResult.NeedSpouse

        val fee = childHospitalCost(character)
        if (character.stats.money < fee) return HaveChildResult.InsufficientFunds

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
        return HaveChildResult.Success(
            character.copy(
                family = character.family + child,
                stats = character.stats.copy(
                    money = character.stats.money - fee,
                    happiness = clampStat(character.stats.happiness + 5)
                ),
                eventLog = EventLogCap.prepend(
                    character.eventLog,
                    "Welcomed $childName into the family (${formatMoney(fee, character.countryCode)} hospital fees)."
                )
            )
        )
    }

    /**
     * Adopt a child (single or married). Requires age 25+ and an agency fee.
     */
    fun adoptChild(character: Character): AdoptChildResult {
        if (!character.alive ||
            character.criminalRecord.currentlyIncarcerated ||
            character.criminalRecord.awaitingTrial
        ) {
            return AdoptChildResult.Ineligible
        }
        if (character.age < MIN_ADOPT_AGE) return AdoptChildResult.TooYoung

        val fee = adoptionAgencyCost(character)
        if (character.stats.money < fee) return AdoptChildResult.InsufficientFunds

        val gender = if (Random.nextBoolean()) Gender.MALE else Gender.FEMALE
        val childAge = Random.nextInt(MIN_ADOPTED_CHILD_AGE, MAX_ADOPTED_CHILD_AGE + 1)
        val childName = NamePool.randomFullName(gender, character.countryCode)
        val child = Person(
            id = UUID.randomUUID().toString(),
            name = childName,
            relation = RelationType.CHILD,
            gender = gender,
            age = childAge,
            relationshipLevel = 55,
            stats = Stats(
                health = Random.nextInt(55, 86),
                happiness = Random.nextInt(45, 76)
            ),
            avatarConfig = FamilyGenerator.inheritAvatarConfig(
                character.avatarConfig,
                character.avatarConfig
            ),
            countryCode = character.countryCode,
            isAdopted = true
        )
        return AdoptChildResult.Success(
            character.copy(
                family = character.family + child,
                stats = character.stats.copy(
                    money = character.stats.money - fee,
                    happiness = clampStat(character.stats.happiness + 8),
                    karma = clampStat(character.stats.karma + 3)
                ),
                eventLog = EventLogCap.prepend(
                    character.eventLog,
                    "Adopted $childName, age $childAge " +
                        "(${formatMoney(fee, character.countryCode)} agency fees)."
                )
            )
        )
    }

    fun adoptionAgencyCost(character: Character): Int =
        EconomyScaler.scaleAmount(ADOPTION_FEE_KENYA, character.countryCode)

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
        val messageKey = when (member.relation) {
            RelationType.SPOUSE -> "msg_spend_time_spouse"
            RelationType.CHILD -> "msg_spend_time_child"
            else -> "msg_spend_time_other"
        }
        return FamilyInteractionResult(
            character = commitMemberUpdate(
                character, memberIndex, updatedMember,
                interactionType = InteractionType.SPEND_TIME,
                recordFirstQualityTime = true
            ).copy(stats = updatedStats),
            messageKey = messageKey,
            messageArgs = listOf(member.name)
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
        val messageKey = when (member.relation) {
            RelationType.SPOUSE -> "msg_argue_spouse"
            RelationType.CHILD -> "msg_scold_child"
            else -> "msg_argue_other"
        }
        return FamilyInteractionResult(
            character = commitMemberUpdate(
                character, memberIndex, updatedMember,
                interactionType = InteractionType.ARGUE,
                milestoneKind = MilestoneKind.BIG_ARGUMENT,
                subjectName = member.name
            ).copy(stats = updatedStats),
            messageKey = messageKey,
            messageArgs = listOf(member.name)
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
                messageKey = "msg_ask_money_not_applicable"
            )
        }
        if (member.relationshipLevel <= 60) {
            return FamilyInteractionResult(
                character = character,
                messageKey = "msg_ask_money_not_close",
                messageArgs = listOf(member.name)
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
                messageKey = "msg_ask_money_success",
                messageArgs = listOf(member.name, formatMoney(amount, character.countryCode))
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
                messageKey = "msg_ask_money_refused",
                messageArgs = listOf(member.name)
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
                messageKey = "msg_gift_cannot_afford",
                messageArgs = listOf(formatMoney(cost, character.countryCode))
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
            messageKey = "msg_gift_success",
            messageArgs = listOf(member.name)
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
            messageKey = "msg_compliment_success",
            messageArgs = listOf(member.name)
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
            messageKey = "msg_insult_success",
            messageArgs = listOf(member.name)
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
                messageKey = "msg_travel_requires_quality_time",
                messageArgs = listOf(member.name)
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
                messageKey = "msg_travel_cannot_afford",
                messageArgs = listOf(formatMoney(cost, character.countryCode))
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
            messageKey = "msg_travel_success",
            messageArgs = listOf(member.name)
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
            messageKey = "msg_advice_success",
            messageArgs = listOf(member.name)
        )
    }

    private fun applyPrank(
        character: Character,
        memberIndex: Int,
        member: Person
    ): FamilyInteractionResult {
        val landedWell = Random.nextFloat() < 0.55f
        val (relDelta, happyDelta, messageKey) = if (landedWell) {
            Triple(8, 6, "msg_prank_success")
        } else {
            Triple(-6, 2, "msg_prank_backfired")
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
            messageKey = messageKey,
            messageArgs = listOf(member.name)
        )
    }

    private fun applySetUpOnDate(
        character: Character,
        memberIndex: Int,
        member: Person
    ): FamilyInteractionResult {
        if (member.relation != RelationType.SIBLING &&
            member.relation != RelationType.FRIEND &&
            member.relation != RelationType.BEST_FRIEND
        ) {
            return FamilyInteractionResult(
                character = character,
                messageKey = "msg_set_up_date_ineligible"
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
            messageKey = "msg_set_up_date_success",
            messageArgs = listOf(member.name, countryName)
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
            messageKey = "msg_homework_success",
            messageArgs = listOf(member.name)
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
                messageKey = "msg_allowance_cannot_afford",
                messageArgs = listOf(formatMoney(cost, character.countryCode))
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
            messageKey = "msg_allowance_success",
            messageArgs = listOf(member.name, formatMoney(cost, character.countryCode))
        )
    }

    private fun applyDateNight(
        character: Character,
        memberIndex: Int,
        member: Person
    ): FamilyInteractionResult {
        if (member.relation != RelationType.SPOUSE) {
            return FamilyInteractionResult(character, "msg_date_night_partner_only")
        }
        val cost = dateNightCost(character)
        if (character.stats.money < cost) {
            return FamilyInteractionResult(
                character,
                "msg_date_night_cannot_afford",
                listOf(formatMoney(cost, character.countryCode))
            )
        }
        val updatedMember = member.copy(
            relationshipLevel = clampRelationshipLevel(member.relationshipLevel + 12)
        )
        val updated = commitMemberUpdate(
            character = character.copy(
                stats = character.stats.copy(
                    money = character.stats.money - cost,
                    happiness = clampStat(character.stats.happiness + 6)
                )
            ),
            memberIndex = memberIndex,
            updatedMember = updatedMember,
            interactionType = InteractionType.DATE_NIGHT,
            milestoneKind = MilestoneKind.QUALITY_TIME,
            subjectName = member.name,
            recordFirstQualityTime = true
        )
        return FamilyInteractionResult(
            character = updated,
            messageKey = "msg_date_night_success",
            messageArgs = listOf(member.name, formatMoney(cost, character.countryCode))
        )
    }

    private fun applyMakePeace(
        character: Character,
        memberIndex: Int,
        member: Person
    ): FamilyInteractionResult {
        if (member.relation != RelationType.ENEMY) {
            return FamilyInteractionResult(character, "msg_make_peace_not_enemy")
        }
        val updatedMember = member.copy(
            relationshipLevel = clampRelationshipLevel(member.relationshipLevel + 18)
        )
        val updated = commitMemberUpdate(
            character = character.copy(
                stats = character.stats.copy(
                    happiness = clampStat(character.stats.happiness + 2)
                )
            ),
            memberIndex = memberIndex,
            updatedMember = updatedMember,
            interactionType = InteractionType.MAKE_PEACE
        )
        return FamilyInteractionResult(
            character = updated,
            messageKey = "msg_make_peace_success",
            messageArgs = listOf(member.name)
        )
    }

    private fun applyFinancialSupport(
        character: Character,
        memberIndex: Int,
        member: Person
    ): FamilyInteractionResult {
        val cost = EconomyScaler.scaleAmount(FINANCIAL_SUPPORT_COST_KENYA, character.countryCode)
        if (character.stats.money < cost) {
            return FamilyInteractionResult(
                character = character,
                messageKey = "msg_financial_support_cannot_afford",
                messageArgs = listOf(formatMoney(cost, character.countryCode))
            )
        }
        val updatedMember = member.copy(
            relationshipLevel = clampRelationshipLevel(member.relationshipLevel + 12)
        ).coerceRelationship()
        return FamilyInteractionResult(
            character = commitMemberUpdate(
                character.copy(
                    stats = character.stats.copy(
                        money = character.stats.money - cost,
                        karma = clampStat(character.stats.karma + 2)
                    )
                ),
                memberIndex,
                updatedMember,
                interactionType = InteractionType.FINANCIAL_SUPPORT,
                recordFirstQualityTime = true
            ),
            messageKey = "msg_financial_support_success",
            messageArgs = listOf(member.name, formatMoney(cost, character.countryCode))
        )
    }

    private fun applyCelebrateMilestone(
        character: Character,
        memberIndex: Int,
        member: Person
    ): FamilyInteractionResult {
        val cost = EconomyScaler.scaleAmount(CELEBRATE_MILESTONE_COST_KENYA, character.countryCode)
        if (character.stats.money < cost) {
            return FamilyInteractionResult(
                character = character,
                messageKey = "msg_celebrate_cannot_afford",
                messageArgs = listOf(formatMoney(cost, character.countryCode))
            )
        }
        val updatedMember = member.copy(
            relationshipLevel = clampRelationshipLevel(member.relationshipLevel + 15)
        ).coerceRelationship()
        return FamilyInteractionResult(
            character = commitMemberUpdate(
                character.copy(
                    stats = character.stats.copy(
                        money = character.stats.money - cost,
                        happiness = clampStat(character.stats.happiness + 6)
                    )
                ),
                memberIndex,
                updatedMember,
                interactionType = InteractionType.CELEBRATE_MILESTONE,
                milestoneKind = MilestoneKind.QUALITY_TIME,
                subjectName = member.name
            ),
            messageKey = "msg_celebrate_milestone_success",
            messageArgs = listOf(member.name)
        )
    }

    private fun applyDiscussLifeChoices(
        character: Character,
        memberIndex: Int,
        member: Person
    ): FamilyInteractionResult {
        val updatedMember = member.copy(
            relationshipLevel = clampRelationshipLevel(member.relationshipLevel + 8)
        ).coerceRelationship()
        return FamilyInteractionResult(
            character = commitMemberUpdate(
                character.copy(
                    stats = character.stats.copy(
                        happiness = clampStat(character.stats.happiness + 3),
                        smarts = clampStat(character.stats.smarts + 1)
                    )
                ),
                memberIndex,
                updatedMember,
                interactionType = InteractionType.DISCUSS_LIFE_CHOICES,
                recordFirstQualityTime = true
            ),
            messageKey = "msg_discuss_life_success",
            messageArgs = listOf(member.name)
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
            messageKey = "msg_discipline_success",
            messageArgs = listOf(member.name)
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
        person = applySocialStatusShift(
            person.copy(milestones = RelationshipMilestoneCap.trim(milestones))
        )
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

    private fun isAdultChildAction(type: InteractionType): Boolean = when (type) {
        InteractionType.FINANCIAL_SUPPORT,
        InteractionType.CELEBRATE_MILESTONE,
        InteractionType.DISCUSS_LIFE_CHOICES -> true
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

    private fun proposalAcceptChance(partner: Person, character: Character): Float {
        val relationshipFactor = 0.5f + (partner.relationshipLevel - PROPOSAL_THRESHOLD) * 0.02f
        val happinessFactor = character.stats.happiness / 100f * 0.10f
        val wealthStabilityFactor = if (character.stats.money >= COMFORTABLE_NET_WORTH_THRESHOLD) 0.05f else 0f
        return (relationshipFactor + happinessFactor + wealthStabilityFactor).coerceIn(0.45f, 0.97f)
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
        const val REQUIRES_FRIEND_TAG = "requires_friend"
        const val REQUIRES_BEST_FRIEND_TAG = "requires_best_friend"
        const val REQUIRES_ENEMY_TAG = "requires_enemy"

        const val BEST_FRIEND_THRESHOLD = 90
        const val ENEMY_THRESHOLD = 15
        const val ENEMY_RECONCILE_THRESHOLD = 40
        const val PARTY_BUDGET_MIN_KENYA = 15_000
        const val PARTY_BUDGET_MAX_KENYA = 80_000
        const val PARTY_BOOST_MIN = 5
        const val PARTY_BOOST_MAX = 15

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
        const val MAX_FRIENDS = 4
        private const val SCHOOL_FRIEND_CHANCE = 0.10f
        private const val WORK_FRIEND_CHANCE = 0.07f
        private const val SEEK_FRIEND_SUCCESS_CHANCE = 0.55f
        private const val SEEK_FRIEND_HAPPINESS_WEIGHT = 0.18f
        private const val SEEK_FRIEND_LOOKS_WEIGHT = 0.10f
        private const val DATING_BASE_ACCEPT_CHANCE = 0.40f
        private const val DATING_LOOKS_WEIGHT = 0.25f
        private const val DATING_HAPPINESS_WEIGHT = 0.15f
        private const val BREAKUP_HAPPINESS_PENALTY = 10
        private const val DIVORCE_HAPPINESS_PENALTY = 20
        private const val ASK_MONEY_SUCCESS_CHANCE = 0.7f
        private const val DECAY_POINTS_PER_YEAR = 1
        private const val DECAY_POINTS_COHABITING = 1

        const val FIRST_DATE_COST_KENYA = 2_000
        const val CHILD_HOSPITAL_COST_KENYA = 25_000
        const val DIVORCE_SETTLEMENT_KENYA = 40_000
        private const val PRENUP_SETTLEMENT_FRACTION = 0.25f
        const val ADOPTION_FEE_KENYA = 75_000
        const val MIN_ADOPT_AGE = 25
        private const val MIN_ADOPTED_CHILD_AGE = 2
        private const val MAX_ADOPTED_CHILD_AGE = 8
        const val DATE_NIGHT_COST_KENYA = 8_000
        const val SEEK_FRIEND_COST_KENYA = 5_000
        const val PET_FEED_COST_KENYA = 1_500
        const val PET_VET_COST_KENYA = 8_000
        const val FINANCIAL_SUPPORT_COST_KENYA = 5_000
        const val CELEBRATE_MILESTONE_COST_KENYA = 8_000

        fun allowanceCost(character: Character): Int =
            EconomyScaler.scaleAmount(ALLOWANCE_BASE_COST_KENYA, character.countryCode)

        fun isMinorChild(person: Person): Boolean =
            person.relation == RelationType.CHILD && person.alive && person.age < MINOR_CHILD_MAX_AGE

        fun isAdultChild(person: Person): Boolean =
            person.relation == RelationType.CHILD && person.alive && person.age >= MINOR_CHILD_MAX_AGE

        fun partyBudgetNiceKenya(): Int =
            (PARTY_BUDGET_MIN_KENYA + PARTY_BUDGET_MAX_KENYA) / 2
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

fun Character.hasFriend(): Boolean =
    family.any { it.isPlatonicAlly() && it.alive }

fun Character.hasBestFriend(): Boolean =
    family.any { it.relation == RelationType.BEST_FRIEND && it.alive }

fun Character.hasEnemy(): Boolean =
    family.any { it.relation == RelationType.ENEMY && it.alive }

/** Friends and best friends (not enemies). */
fun Person.isPlatonicAlly(): Boolean =
    relation == RelationType.FRIEND || relation == RelationType.BEST_FRIEND

/** Platonic social circle that can shift between friend / best friend / enemy. */
fun Person.isSocialCircleMember(): Boolean =
    relation == RelationType.FRIEND ||
        relation == RelationType.BEST_FRIEND ||
        relation == RelationType.ENEMY
