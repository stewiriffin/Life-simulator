// app/src/main/java/com/maisha/game/domain/AchievementEngine.kt (new)
package com.maisha.game.domain

import com.maisha.game.data.AchievementCatalog
import com.maisha.game.data.model.Achievement
import com.maisha.game.data.model.AchievementProgress
import com.maisha.game.data.model.AssetType
import com.maisha.game.data.model.Character
import com.maisha.game.data.model.RelationType
import com.maisha.game.data.model.SchoolStage
import com.maisha.game.data.model.relationshipTierFor
import com.maisha.game.data.model.RelationshipTier
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AchievementEngine @Inject constructor(
    private val financeEngine: FinanceEngine
) {

    /**
     * Returns catalog achievements whose conditions pass and are not already unlocked in [currentProgress].
     *
     * Assumes [character] reflects post-year state. Does not persist — caller unlocks via repository.
     */
    fun checkAchievements(
        character: Character,
        currentProgress: List<AchievementProgress>
    ): List<Achievement> {
        val unlockedIds = currentProgress
            .filter { it.unlocked }
            .map { it.achievementId }
            .toSet()

        val netWorth = financeEngine.calculateNetWorth(character)

        return AchievementCatalog.all
            .filter { it.id !in unlockedIds }
            .filter { achievement -> checkCondition(achievement.id, character, netWorth) }
    }

    private fun checkCondition(achievementId: String, character: Character, netWorth: Int): Boolean =
        when (achievementId) {
            "first_job" -> checkFirstJob(character)
            "corner_office" -> checkCornerOffice(character)
            "career_changer" -> checkCareerChanger(character)
            "graduate" -> checkGraduate(character)
            "straight_as" -> checkStraightAs(character)
            "dropout" -> checkDropout(character)
            "tied_the_knot" -> checkTiedTheKnot(character)
            "first_child" -> checkFirstChild(character)
            "growing_family" -> checkGrowingFamily(character)
            "family_person" -> checkFamilyPerson(character)
            "six_figures" -> netWorth >= 100_000
            "first_million" -> netWorth >= 1_000_000
            "property_owner" -> checkPropertyOwner(character)
            "multiple_streams" -> checkMultipleStreams(character)
            "half_century" -> checkHalfCentury(character)
            "golden_years" -> checkGoldenYears(character)
            "centenarian" -> checkCentenarian(character)
            "brush_with_law" -> checkBrushWithLaw(character)
            "repeat_offender" -> checkRepeatOffender(character)
            "clean_record" -> checkCleanRecord(character)
            "world_citizen" -> checkWorldCitizen(character)
            "inseparable" -> checkInseparable(character)
            "second_generation" -> checkSecondGeneration(character)
            "dynasty_builder" -> checkDynastyBuilder(character)
            "true_friend" -> checkTrueFriend(character)
            "social_circle" -> checkSocialCircle(character)
            "passport_stamped" -> checkPassportStamped(character)
            "global_family" -> checkGlobalFamily(character)
            "home_away_from_home" -> checkHomeAwayFromHome(character)
            "world_traveler" -> checkWorldTraveler(character)
            "deep_roots" -> checkDeepRoots(character)
            else -> false
        }

    private fun checkFirstJob(character: Character): Boolean =
        character.career.currentJob != null || character.career.jobHistory.isNotEmpty()

    private fun checkCornerOffice(character: Character): Boolean =
        (character.career.currentJob?.level ?: 0) >= 3

    private fun checkCareerChanger(character: Character): Boolean =
        distinctJobCount(character) >= 3

    private fun checkGraduate(character: Character): Boolean =
        character.education.stage == SchoolStage.GRADUATED

    private fun checkStraightAs(character: Character): Boolean =
        character.education.gpa >= 3.7f &&
            character.education.stage in setOf(SchoolStage.UNIVERSITY, SchoolStage.GRADUATED)

    private fun checkDropout(character: Character): Boolean =
        character.education.expelled ||
            (character.education.kcpePassed == false) ||
            (character.education.kcseGrade?.uppercase() in setOf("D", "E"))

    private fun checkTiedTheKnot(character: Character): Boolean =
        character.isMarried()

    private fun checkFirstChild(character: Character): Boolean =
        childCount(character) >= 1

    private fun checkGrowingFamily(character: Character): Boolean =
        childCount(character) >= 3

    private fun checkFamilyPerson(character: Character): Boolean {
        val livingFamily = character.family.filter { it.alive }
        if (livingFamily.isEmpty()) return false
        return livingFamily.all { it.relationshipLevel >= 70 }
    }

    private fun checkPropertyOwner(character: Character): Boolean =
        character.assets.any { it.type == AssetType.HOUSE }

    private fun checkMultipleStreams(character: Character): Boolean =
        character.assets.size >= 3

    private fun checkHalfCentury(character: Character): Boolean =
        character.age >= 50

    private fun checkGoldenYears(character: Character): Boolean =
        character.age >= 80

    private fun checkCentenarian(character: Character): Boolean =
        character.age >= 100

    private fun checkBrushWithLaw(character: Character): Boolean =
        character.criminalRecord.timesArrested >= 1

    private fun checkRepeatOffender(character: Character): Boolean =
        character.criminalRecord.timesArrested >= 3

    private fun checkCleanRecord(character: Character): Boolean =
        character.age >= 60 && !character.criminalRecord.hasRecord

    private fun checkWorldCitizen(character: Character): Boolean =
        character.family.any {
            it.relation == RelationType.SPOUSE && it.countryCode != character.countryCode
        }

    private fun checkInseparable(character: Character): Boolean =
        character.family.any { relationshipTierFor(it.relationshipLevel) == RelationshipTier.INSEPARABLE }

    private fun checkSecondGeneration(character: Character): Boolean =
        character.generationNumber >= 2

    private fun checkDynastyBuilder(character: Character): Boolean =
        character.generationNumber >= 3

    private fun checkTrueFriend(character: Character): Boolean =
        character.family.any {
            it.relation == RelationType.FRIEND && it.alive && it.relationshipLevel >= 70
        }

    private fun checkSocialCircle(character: Character): Boolean =
        character.family.count { it.relation == RelationType.FRIEND && it.alive } >= 2

    private fun checkPassportStamped(character: Character): Boolean =
        character.relocationCount >= 1

    private fun checkGlobalFamily(character: Character): Boolean =
        character.family.any {
            it.relation == RelationType.CHILD && it.secondaryCountryCode != null
        }

    private fun checkHomeAwayFromHome(character: Character): Boolean {
        val origin = character.birthCountryCode
        return character.family.any { person ->
            person.alive &&
                person.countryCode != origin &&
                relationshipTierFor(person.relationshipLevel) == RelationshipTier.INSEPARABLE
        }
    }

    private fun checkWorldTraveler(character: Character): Boolean =
        character.relocationCount >= 2

    private fun checkDeepRoots(character: Character): Boolean =
        character.generationNumber >= 5

    private fun distinctJobCount(character: Character): Int {
        val titles = character.career.jobHistory.toMutableSet()
        character.career.currentJob?.title?.let { titles.add(it) }
        return titles.size
    }

    private fun childCount(character: Character): Int =
        character.family.count { it.relation == RelationType.CHILD }
}
