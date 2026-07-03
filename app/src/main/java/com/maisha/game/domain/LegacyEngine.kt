// app/src/main/java/com/maisha/game/domain/LegacyEngine.kt (modified — estate settlement before inheritance)
package com.maisha.game.domain

import com.maisha.game.data.model.AncestryEntry
import com.maisha.game.data.model.CareerState
import com.maisha.game.data.model.Character
import com.maisha.game.data.model.CriminalRecord
import com.maisha.game.data.model.EducationState
import com.maisha.game.data.model.Gender
import com.maisha.game.data.model.Person
import com.maisha.game.data.model.RelationType
import com.maisha.game.data.model.SchoolStage
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

data class EstateSettlement(
    val distributableCash: Int,
    val totalDeductions: Int,
    val logLines: List<String>
)

@Singleton
class LegacyEngine @Inject constructor(
    private val mortalityEngine: MortalityEngine,
    private val financeEngine: FinanceEngine
) {

    /**
     * Living children aged [MIN_HEIR_AGE]+, sorted oldest first — candidates for legacy continuation.
     */
    fun eligibleHeirs(deceased: Character): List<Person> =
        deceased.family
            .filter { person ->
                person.relation == RelationType.CHILD && person.alive && person.age >= MIN_HEIR_AGE
            }
            .sortedByDescending { it.age }

    /**
     * Builds the next playable [Character] from a chosen heir: settles the estate, splits remaining
     * cash among siblings, remaps family, appends deceased to [Character.ancestryHistory], and
     * increments [Character.generationNumber].
     *
     * @param heir Must be a living [RelationType.CHILD] of [deceased].
     */
    fun createLegacyCharacter(deceased: Character, heir: Person): Character {
        require(heir.relation == RelationType.CHILD && heir.alive) {
            "Legacy heir must be a living child of the deceased character."
        }

        val settlement = calculateEstateSettlement(deceased)
        val inheritedMoney = calculateMoneyInheritance(deceased, settlement.distributableCash)
        val heirlooms = deceased.assets.filter { it.isHeirloom }
        val survivingParent = mapSurvivingParent(deceased.gender, deceased.family)
        val siblings = deceased.family
            .filter { person ->
                person.relation == RelationType.CHILD && person.alive && person.id != heir.id
            }
            .map { sibling ->
                sibling.copy(
                    relation = RelationType.SIBLING,
                    relationshipLevel = sibling.relationshipLevel
                )
            }
        val friends = deceased.family.filter { person ->
            person.relation == RelationType.FRIEND && person.alive
        }

        val newFamily = buildList {
            survivingParent?.let { add(it) }
            addAll(siblings)
            addAll(friends)
        }

        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val deceasedEntry = buildAncestryEntry(deceased)
        val carriedHistory = AncestryHistoryCap.trim(deceased.ancestryHistory + deceasedEntry)

        val settlementLog = settlement.logLines.map { line ->
            "Estate settlement: $line"
        }
        val legacyLog = buildList {
            addAll(settlementLog)
            if (heirlooms.isNotEmpty()) {
                add("Family heirlooms passed down: ${heirlooms.joinToString { it.name }}.")
            }
            add("Continued the family legacy as ${deceased.name}'s heir at age ${heir.age}.")
        }

        return Character(
            name = heir.name,
            age = heir.age,
            gender = heir.gender,
            stats = heir.stats.copy(money = heir.stats.money + inheritedMoney),
            birthYear = currentYear - heir.age,
            alive = true,
            countryCode = heir.countryCode,
            birthCountryCode = heir.countryCode,
            avatarConfig = heir.avatarConfig,
            generationNumber = deceased.generationNumber + 1,
            ancestryHistory = carriedHistory,
            family = newFamily,
            education = educationForAge(heir.age),
            career = CareerState(),
            assets = heirlooms,
            criminalRecord = CriminalRecord(),
            activeConditions = emptyList(),
            eventLog = legacyLog
        )
    }

    /**
     * Deducts final expenses (medical bills, legal fees, outstanding debts) from the deceased's
     * cash before heirs receive their share. The distributable pool never goes below zero.
     */
    fun calculateEstateSettlement(deceased: Character): EstateSettlement {
        val liquidatedAssetValue = deceased.assets
            .filter { !it.isHeirloom }
            .sumOf { it.currentValue }
        val grossCash = deceased.stats.money.coerceAtLeast(0) + liquidatedAssetValue
        var deductions = 0
        val logLines = mutableListOf<String>()

        if (liquidatedAssetValue > 0) {
            logLines += "Standard assets were sold to settle the estate."
        }

        val untreated = deceased.activeConditions.filter { !it.treated }
        if (untreated.isNotEmpty()) {
            val medicalBills = untreated.sumOf { condition ->
                ESTATE_MEDICAL_BASE + condition.severity * ESTATE_MEDICAL_PER_SEVERITY
            }
            deductions += medicalBills
            logLines += "Unsettled medical care was paid from the estate."
        }

        if (deceased.criminalRecord.hasRecord) {
            val legalFees = ESTATE_LEGAL_BASE +
                deceased.criminalRecord.timesArrested * ESTATE_LEGAL_PER_ARREST
            deductions += legalFees
            logLines += "Outstanding legal fees were settled before inheritance."
        }

        val netWorth = financeEngine.calculateNetWorth(deceased)
        if (netWorth < grossCash) {
            val liabilityGap = grossCash - netWorth
            if (liabilityGap > 0) {
                deductions += liabilityGap
                logLines += "Final debts were cleared from what remained."
            }
        }

        val distributable = (grossCash - deductions).coerceAtLeast(0)
        if (deductions > 0 && distributable == 0) {
            logLines += "The estate covered its obligations; little was left to pass on."
        }

        return EstateSettlement(
            distributableCash = distributable,
            totalDeductions = deductions,
            logLines = logLines
        )
    }

    /** Snapshot of a finished life for [Character.ancestryHistory], using [MortalityEngine] for cause text. */
    fun buildAncestryEntry(deceased: Character): AncestryEntry {
        val cause = mortalityEngine.parseDeathCause(deceased) ?: DeathCause.OLD_AGE
        return AncestryEntry(
            generationNumber = deceased.generationNumber,
            characterName = deceased.name,
            countryCode = deceased.birthCountryCode,
            relocatedTo = deceased.relocationHistory,
            ageAtDeath = deceased.age,
            cause = mortalityEngine.gentleCauseLabel(cause)
        )
    }

    /**
     * Asymmetric parent mapping: if the deceased was the father, the surviving spouse
     * becomes MOTHER; if the deceased was the mother, the surviving spouse becomes FATHER.
     */
    fun mapSurvivingParent(deceasedGender: Gender, family: List<Person>): Person? {
        val spouse = family.firstOrNull { it.relation == RelationType.SPOUSE && it.alive } ?: return null
        val parentType = when (deceasedGender) {
            Gender.MALE -> RelationType.MOTHER
            Gender.FEMALE -> RelationType.FATHER
        }
        return spouse.copy(
            relation = parentType,
            isMarried = false,
            dateOfPartnership = null
        )
    }

    private fun calculateMoneyInheritance(deceased: Character, distributableCash: Int): Int {
        val livingChildren = deceased.family.count { person ->
            person.relation == RelationType.CHILD && person.alive
        }
        if (livingChildren <= 0) return 0
        return (distributableCash / livingChildren).coerceAtLeast(0)
    }

    private fun educationForAge(age: Int): EducationState = when {
        age < 6 -> EducationState(stage = SchoolStage.NONE)
        age < 14 -> EducationState(
            stage = SchoolStage.PRIMARY,
            currentGrade = (age - 5).coerceAtLeast(1)
        )
        age < 18 -> EducationState(
            stage = SchoolStage.SECONDARY,
            currentGrade = (age - 13).coerceAtLeast(1)
        )
        age < 22 -> EducationState(stage = SchoolStage.UNIVERSITY)
        else -> EducationState(stage = SchoolStage.GRADUATED)
    }

    companion object {
        const val MIN_HEIR_AGE = 16
        private const val ESTATE_MEDICAL_BASE = 15_000
        private const val ESTATE_MEDICAL_PER_SEVERITY = 8_000
        private const val ESTATE_LEGAL_BASE = 20_000
        private const val ESTATE_LEGAL_PER_ARREST = 12_000
    }
}
