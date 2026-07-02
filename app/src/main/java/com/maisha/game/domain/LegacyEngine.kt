package com.maisha.game.domain

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

@Singleton
class LegacyEngine @Inject constructor() {

    fun eligibleHeirs(deceased: Character): List<Person> =
        deceased.family
            .filter { person ->
                person.relation == RelationType.CHILD && person.alive && person.age >= MIN_HEIR_AGE
            }
            .sortedByDescending { it.age }

    fun createLegacyCharacter(deceased: Character, heir: Person): Character {
        require(heir.relation == RelationType.CHILD && heir.alive) {
            "Legacy heir must be a living child of the deceased character."
        }

        val inheritedMoney = calculateMoneyInheritance(deceased)
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
            family = newFamily,
            education = educationForAge(heir.age),
            career = CareerState(),
            assets = emptyList(),
            criminalRecord = CriminalRecord(),
            activeConditions = emptyList(),
            eventLog = listOf(
                "Continued the family legacy as ${deceased.name}'s heir at age ${heir.age}."
            )
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

    private fun calculateMoneyInheritance(deceased: Character): Int {
        val livingChildren = deceased.family.count { person ->
            person.relation == RelationType.CHILD && person.alive
        }
        if (livingChildren <= 0) return 0
        return (deceased.stats.money.coerceAtLeast(0) / livingChildren).coerceAtLeast(0)
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
    }
}
