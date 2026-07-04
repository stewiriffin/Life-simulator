package com.maisha.game.domain

import com.maisha.game.data.EconomyScaler
import com.maisha.game.data.model.Business
import com.maisha.game.data.model.BusinessIndustry
import com.maisha.game.data.model.PoliticalOffice
import com.maisha.game.data.model.PoliticalState
import com.maisha.game.data.model.SocialMediaState
import com.maisha.game.data.model.Stats
import com.maisha.game.data.model.TaxPolicyType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PoliticsEngineTest {

    private val financeEngine = FinanceEngine()
    private val politicsEngine = PoliticsEngine(financeEngine)
    private val businessEngine = BusinessEngine(financeEngine)

    @Test
    fun launchCampaign_calculatesWinBasedOnInvestmentAndStats() {
        val weak = eligiblePolitician(
            smarts = 55,
            looks = 40,
            money = 5_000_000,
            followers = 0
        )
        val strong = eligiblePolitician(
            smarts = 95,
            looks = 90,
            money = 5_000_000,
            followers = 200_000
        )
        val minInvest = politicsEngine.minCampaignInvestment(PoliticalOffice.MAYOR, "KE")
        val weakChance = politicsEngine.campaignWinChance(weak, PoliticalOffice.MAYOR, minInvest)
        val strongChance = politicsEngine.campaignWinChance(
            strong,
            PoliticalOffice.MAYOR,
            minInvest * 3
        )
        assertTrue(
            "Strong campaign ($strongChance) should beat weak ($weakChance)",
            strongChance > weakChance + 0.15f
        )

        var wins = 0
        repeat(40) {
            val candidate = eligiblePolitician(
                smarts = 95,
                looks = 90,
                money = 5_000_000,
                followers = 200_000
            )
            when (
                politicsEngine.launchCampaign(
                    candidate,
                    PoliticalOffice.MAYOR,
                    minInvest * 3
                )
            ) {
                is CampaignResult.Won -> wins++
                is CampaignResult.Lost -> Unit
                is CampaignResult.Failed -> error("Expected eligible campaign")
            }
        }
        assertTrue("High-investment campaigns should win often (wins=$wins)", wins >= 15)
    }

    @Test
    fun passTaxPolicy_modifiesEconomyScalerAndApprovalRating() {
        val president = eligiblePolitician(
            smarts = 90,
            looks = 80,
            money = 1_000_000,
            followers = 50_000
        ).copy(
            politics = PoliticalState(
                currentOffice = PoliticalOffice.PRESIDENT,
                approvalRating = 60,
                campaignFunds = 0
            ),
            businesses = listOf(
                Business(
                    id = "b1",
                    name = "Test Co",
                    industry = BusinessIndustry.TECH,
                    valuation = 500_000,
                    revenue = 100_000,
                    employeeCount = 5
                )
            )
        )

        assertEquals(1.0f, EconomyScaler.policyBusinessRevenueMultiplier(null))
        assertTrue(EconomyScaler.policyBusinessRevenueMultiplier(TaxPolicyType.TAX_CUTS) > 1f)
        assertTrue(EconomyScaler.policyBusinessRevenueMultiplier(TaxPolicyType.WEALTH_TAX) < 1f)

        val taxCuts = financeEngine.passTaxPolicy(president, TaxPolicyType.TAX_CUTS)
        assertTrue(taxCuts is FinanceEngine.TaxPolicyResult.Success)
        val afterCuts = (taxCuts as FinanceEngine.TaxPolicyResult.Success).character
        assertEquals(TaxPolicyType.TAX_CUTS, afterCuts.politics.activeTaxPolicy)
        assertEquals(52, afterCuts.politics.approvalRating)
        assertEquals(1_000_000, afterCuts.stats.money)

        val wealthTax = financeEngine.passTaxPolicy(
            president.copy(politics = president.politics.copy(activeTaxPolicy = null)),
            TaxPolicyType.WEALTH_TAX
        )
        assertTrue(wealthTax is FinanceEngine.TaxPolicyResult.Success)
        val afterWealth = (wealthTax as FinanceEngine.TaxPolicyResult.Success).character
        assertEquals(TaxPolicyType.WEALTH_TAX, afterWealth.politics.activeTaxPolicy)
        assertEquals(70, afterWealth.politics.approvalRating)
        assertTrue(afterWealth.stats.money < president.stats.money)

        val boosted = businessEngine.processBusinessYear(afterCuts)
        val taxed = businessEngine.processBusinessYear(
            president.copy(
                politics = president.politics.copy(activeTaxPolicy = TaxPolicyType.WEALTH_TAX),
                businesses = president.businesses
            )
        )
        // Tax cuts should leave businesses at least as healthy on average as wealth tax.
        assertTrue(boosted.businesses.first().revenue >= taxed.businesses.first().revenue * 0.85f)
    }

    private fun eligiblePolitician(
        smarts: Int,
        looks: Int,
        money: Int,
        followers: Int
    ) = TestFixtures.character(
        age = 40,
        countryCode = "KE",
        stats = Stats(
            health = 80,
            happiness = 70,
            smarts = smarts,
            looks = looks,
            money = money
        ),
        socialMedia = SocialMediaState(hasAccount = followers > 0, followers = followers)
    ).copy(citizenships = listOf("KE"))
}
