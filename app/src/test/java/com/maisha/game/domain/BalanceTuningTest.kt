// app/src/test/java/com/maisha/game/domain/BalanceTuningTest.kt (new — Prompt 28)
package com.maisha.game.domain

import com.maisha.game.data.model.CareerState
import com.maisha.game.data.model.Character
import com.maisha.game.data.model.CriminalRecord
import com.maisha.game.data.model.EducationState
import com.maisha.game.data.model.Gender
import com.maisha.game.data.model.Job
import com.maisha.game.data.model.SchoolStage
import com.maisha.game.data.model.Stats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BalanceTuningTest {

    private val careerEngine = CareerEngine(HealthEngine(), RelocationEngine())
    private val financeEngine = FinanceEngine()

    @Test
    fun promotionSalary_usesBaseOnly_noDoubleLevelMultiplier() {
        val job = Job(
            id = "teacher",
            title = "Teacher",
            minEducation = SchoolStage.GRADUATED,
            baseSalary = 500_000,
            level = 5
        )
        assertEquals(500_000, careerEngine.calculateAnnualSalary(job))
    }

    @Test
    fun multiAssetUpkeep_appliesStackingDiscount() {
        val character = Character(
            name = "Test",
            gender = Gender.MALE,
            birthYear = 2000,
            age = 30,
            stats = Stats(money = 1_000_000),
            assets = listOf(
                asset(upkeep = 2_000),
                asset(upkeep = 3_000),
                asset(upkeep = 4_000)
            )
        )
        val after = financeEngine.applyUpkeep(character)
        // Raw annual = (2+3+4)*12 = 108_000; 3 assets => 90% => 97_200
        assertEquals(902_800, after.stats.money)
    }

    @Test
    fun cleanStreak_reducesHirePenalty() {
        val freshRecord = baseCharacter().copy(
            criminalRecord = CriminalRecord(hasRecord = true, timesArrested = 1, lastArrestAge = 20),
            age = 25,
            stats = Stats(smarts = 60),
            education = EducationState(stage = SchoolStage.GRADUATED, gpa = 3.0f)
        )
        val redeemed = freshRecord.copy(
            criminalRecord = CriminalRecord(hasRecord = true, timesArrested = 1, lastArrestAge = 20),
            age = 36
        )
        var freshHires = 0
        var redeemedHires = 0
        repeat(500) {
            if (careerEngine.applyForJob(freshRecord, "teacher").second is CareerResult.Hired) freshHires++
            if (careerEngine.applyForJob(redeemed, "teacher").second is CareerResult.Hired) redeemedHires++
        }
        assertTrue("Clean streak should improve hire rate", redeemedHires > freshHires)
    }

    private fun baseCharacter() = Character(
        name = "Player",
        gender = Gender.MALE,
        birthYear = 2000,
        age = 22,
        stats = Stats(smarts = 55),
        education = EducationState(stage = SchoolStage.GRADUATED, gpa = 2.8f),
        career = CareerState()
    )

    private fun asset(upkeep: Int) = com.maisha.game.data.model.Asset(
        id = "a$upkeep",
        type = com.maisha.game.data.model.AssetType.MOTORBIKE,
        name = "Test",
        purchasePrice = 100_000,
        currentValue = 100_000,
        condition = 100,
        monthlyUpkeep = upkeep
    )
}
