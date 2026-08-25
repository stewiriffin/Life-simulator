// app/src/main/java/com/maisha/game/data/model/Finance.kt
package com.maisha.game.data.model

/**
 * Student-loan / tuition helpers shared by [com.maisha.game.domain.FinanceEngine]
 * and [com.maisha.game.domain.EducationEngine].
 *
 * Balances live on [EducationState] so old saves deserialize cleanly with defaults.
 */
object StudentFinance {
    /** Base domestic tuition (Kenya reference) before country scaling — per year. */
    const val BASE_TUITION_KENYA = 45_000

    /** Yearly interest applied to [EducationState.studentLoanBalance] once employed (percent). */
    const val LOAN_INTEREST_PERCENT_MIN = 4
    const val LOAN_INTEREST_PERCENT_MAX = 7

    /** Auto-repayment share of current job salary each year while employed. */
    const val LOAN_REPAY_SALARY_FRACTION = 0.08f

    /** Base campus work-study pay (Kenya reference) before country scaling. */
    const val CAMPUS_JOB_PAY_KENYA = 28_000

    /** Share of campus-job pay auto-applied to student loans. */
    const val CAMPUS_JOB_LOAN_FRACTION = 0.5f

    /** Base internship stipend (Kenya reference). */
    const val INTERNSHIP_STIPEND_KENYA = 35_000
}

/**
 * Snapshot of education-related liabilities for UI / net-worth math.
 */
data class StudentLiabilitySnapshot(
    val studentLoanBalance: Int,
    val tuitionPerYear: Int,
    val scholarshipActive: Boolean
) {
    companion object {
        fun from(education: EducationState) = StudentLiabilitySnapshot(
            studentLoanBalance = education.studentLoanBalance,
            tuitionPerYear = education.tuitionPerYear,
            scholarshipActive = education.scholarshipActive
        )
    }
}
